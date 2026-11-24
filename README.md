# RiF Gallery Patches

A Morphe patch source for **rif is fun 5.6.22**. It combines Patcheddit's Reddit API compatibility fixes with fixes for broken gallery metadata and the comment reply editor.

## Install in Morphe Manager

1. Open [Add RiF Gallery Patches to Morphe](https://morphe.software/add-source?github=pholcroft/rif-gallery-morphe-patches) on Android. You can also paste [the source manifest URL](https://raw.githubusercontent.com/pholcroft/rif-gallery-morphe-patches/main/patches-bundle.json) into **Sources → Add source**.
2. Select your original RiF 5.6.22 APK. In the patch picker, choose **RiF Gallery Patches** as the source and leave **Reddit API fixes**, **Repair broken gallery images**, and **Fix comment editor keyboard resizing** enabled. This source already supplies the Patcheddit patches for RiF, so leave the separate Patcheddit source unselected for this build.
3. Patch and install using your usual Morphe Manager installation so Android can accept an in-place update with the same signing key. Open RiF and sign in to Reddit.

The comment editor patch targets the free RiF 5.6.22 package. It keeps the compose actions visible while long replies scroll inside the editor. The gallery patch targets both the free and Golden Platinum packages. The free package was tested on Android 15. Reddit sign-in in RiF and the Golden Platinum build have yet to be verified.

## Build

The source is a fork of [Patcheddit 2.4.0](https://github.com/wchill/patcheddit/tree/v2.4.0). With access to the Morphe package registry, build from source using <code>./gradlew :patches:buildAndroid :patches:generatePatchesList</code>.

The published bundle can also be rebuilt without registry access using [the packaging script](scripts/build-rif-gallery-bundle.py). It takes the original [Patcheddit 2.4.0 bundle](https://github.com/wchill/patcheddit/releases/tag/v2.4.0), Morphe Desktop 2.1.1, JDK 21, the <code>lib</code> directory from Gradle 9.4.1, and Android platform/build-tools 36. Run the script with <code>--help</code> for arguments. It compiles the RiF patches and merges their DEX code into the pinned upstream bundle.

## Credits and license

This app uses code from Patcheddit. To learn more, visit https://reddit.com/r/patcheddit

The gallery parser repair is adapted from [MojiRS's Morphe RiF patches v1.9.0](https://github.com/MojiRS/morphe-rif-patches/tree/v1.9.0). See [LICENSE](LICENSE) and [NOTICE](NOTICE) for license terms.
