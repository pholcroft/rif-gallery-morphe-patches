#!/usr/bin/env python3
"""Build the published bundle on top of the pinned upstream Patcheddit release."""

import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile


ROOT = Path(__file__).resolve().parents[1]
BASE_SHA256 = "9dbc347385043e444ec6c5de75642c8cdc9510b04431998d4ee7948feb0535c1"
VERSION = "2.4.2"
PROJECT_URL = "https://github.com/pholcroft/rif-gallery-morphe-patches"
GALLERY_JAVA_SOURCE = ROOT / (
    "extensions/redditisfun/src/main/java/app/morphe/extension/redditisfun/GalleryRepair.java"
)
COMPOSER_JAVA_SOURCE = GALLERY_JAVA_SOURCE.with_name("CommentEditorInsets.java")
GALLERY_PATCH_SOURCE = ROOT / (
    "patches/src/main/kotlin/app/morphe/patches/reddit/customclients/redditisfun/"
    "fix/gallery/RepairBrokenRifGalleryImagesPatch.kt"
)
COMPOSER_PATCH_SOURCE = ROOT / (
    "patches/src/main/kotlin/app/morphe/patches/reddit/customclients/redditisfun/"
    "fix/composer/FixRifCommentEditorPatch.kt"
)
RIF_EXTENSION = "extensions/redditisfun.mpe"
DIAGNOSTICS_STRINGS = "settings/values/morphe_diagnostics_strings.xml"


def run(*command):
    subprocess.run([str(arg) for arg in command], check=True)


def jar_from_classes(output, root):
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as jar:
        for entry in sorted(root.rglob("*")):
            if entry.is_file():
                jar.write(entry, entry.relative_to(root).as_posix())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", type=Path, required=True, help="Published Patcheddit 2.4.0 .mpp")
    parser.add_argument("--morphe-cli", type=Path, required=True, help="Morphe Desktop 2.1.1 JAR")
    parser.add_argument("--java-home", type=Path, required=True, help="JDK 21")
    parser.add_argument("--compiler-lib", type=Path, required=True, help="Gradle 9.4.1 lib directory")
    parser.add_argument("--android-sdk", type=Path, required=True, help="Android SDK with platform and build tools 36")
    parser.add_argument("--out", type=Path, required=True, help="Output .mpp")
    args = parser.parse_args()

    payload = args.base.read_bytes()
    if hashlib.sha256(payload).hexdigest() != BASE_SHA256:
        parser.error("The upstream bundle must match the pinned Patcheddit 2.4.0 release.")

    executable = ".exe" if os.name == "nt" else ""
    java = args.java_home / "bin" / ("java" + executable)
    javac = args.java_home / "bin" / ("javac" + executable)
    android = args.android_sdk / "platforms/android-36/android.jar"
    d8 = args.android_sdk / "build-tools/36.0.0/lib/d8.jar"
    args.out.parent.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix="rif-gallery-bundle-") as temp:
        work = Path(temp)
        base = work / "patcheddit.jar"
        base.write_bytes(payload)

        extension_classes = work / "extension-classes"
        patch_classes = work / "patch-classes"
        extension_classes.mkdir()
        patch_classes.mkdir()
        run(javac, "--release", "8", "-classpath", android, "-d", extension_classes, GALLERY_JAVA_SOURCE, COMPOSER_JAVA_SOURCE)
        run(
            java, "-cp", args.compiler_lib / "*", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib", "-no-reflect", "-jvm-target", "17",
            "-module-name", "rif_gallery_repair",
            "-classpath", os.pathsep.join((str(base), str(args.morphe_cli))),
            f"-Xfriend-paths={base}", "-d", patch_classes, GALLERY_PATCH_SOURCE, COMPOSER_PATCH_SOURCE,
        )
        extension_jar = work / "extension.jar"
        patch_jar = work / "patch.jar"
        jar_from_classes(extension_jar, extension_classes)
        jar_from_classes(patch_jar, patch_classes)

        with zipfile.ZipFile(base) as upstream:
            upstream_dex = work / "upstream.dex"
            upstream_extension = work / "upstream-extension.dex"
            upstream_dex.write_bytes(upstream.read("classes.dex"))
            upstream_extension.write_bytes(upstream.read(RIF_EXTENSION))
            patch_dex = work / "patch-dex.zip"
            extension_dex = work / "extension-dex.zip"
            run(java, "-cp", d8, "com.android.tools.r8.D8", "--min-api", "26",
                "--lib", android, "--output", patch_dex, upstream_dex, patch_jar)
            run(java, "-cp", d8, "com.android.tools.r8.D8", "--min-api", "21",
                "--lib", android, "--output", extension_dex, upstream_extension, extension_jar)

            manifest = (
                "Manifest-Version: 1.0\r\n"
                "Name: RiF Gallery Patches\r\n"
                "Description: Morphe patches with RiF gallery and comment editor repairs\r\n"
                f"Version: {VERSION}\r\n"
                "Timestamp: 1795478400000\r\n"
                f"Source: {PROJECT_URL}\r\n"
                "Author: pholcroft and upstream contributors\r\n"
                f"Website: {PROJECT_URL}\r\n"
                "License: GPL-3.0; additional attribution and name terms in NOTICE\r\n"
                "Patcher-Version: 2.0.0\r\n\r\n"
            ).encode()
            with zipfile.ZipFile(patch_dex) as compiled, zipfile.ZipFile(extension_dex) as extension:
                replacements = {
                    "META-INF/MANIFEST.MF": manifest,
                    "classes.dex": compiled.read("classes.dex"),
                    RIF_EXTENSION: extension.read("classes.dex"),
                    DIAGNOSTICS_STRINGS: (
                        ROOT / "patches/src/main/resources" / DIAGNOSTICS_STRINGS
                    ).read_bytes(),
                }
                if compiled.namelist() != ["classes.dex"] or extension.namelist() != ["classes.dex"]:
                    raise ValueError("Morphe requires one patch DEX and one DEX per extension.")

            if not replacements.keys() <= set(upstream.namelist()):
                raise ValueError("Pinned bundle is missing an expected Morphe entry.")
            with zipfile.ZipFile(args.out, "w", zipfile.ZIP_DEFLATED) as bundle:
                for entry in upstream.infolist():
                    bundle.writestr(entry, replacements.get(entry.filename, upstream.read(entry)))
                for entry in sorted(patch_classes.rglob("*")):
                    if entry.is_file():
                        name = entry.relative_to(patch_classes).as_posix()
                        if name in upstream.namelist():
                            raise ValueError(f"Patch class or metadata already exists: {name}")
                        bundle.write(entry, name)
                for notice in ("LICENSE", "NOTICE"):
                    bundle.write(ROOT / notice, f"META-INF/{notice}")

    print(f"Built {args.out}")


if __name__ == "__main__":
    main()
