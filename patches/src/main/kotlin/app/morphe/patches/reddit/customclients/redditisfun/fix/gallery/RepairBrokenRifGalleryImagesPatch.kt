/*
 * Parser hook adapted from MojiRS's GPL-3.0 Morphe patch (v1.9.0).
 * https://github.com/MojiRS/morphe-rif-patches
 */

package app.morphe.patches.reddit.customclients.redditisfun.fix.gallery

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.reddit.customclients.AppCompatibility
import app.morphe.patches.reddit.customclients.ExtensionPatches
import app.morphe.patches.reddit.customclients.redditisfun.misc.settings.rifMorpheSettingsPatch

private val galleryParser = fingerprint {
    returns("Lcom/andrewshu/android/reddit/browser/redditgallery/GalleryThreadThing;")
    parameters("Ljava/io/InputStream;")
}

@Suppress("unused")
val repairBrokenRifGalleryImagesPatch = bytecodePatch(
    name = "Repair broken gallery images",
    description = "Keeps usable photos when Reddit returns missing or broken gallery image metadata.",
    default = true,
) {
    compatibleWith(*AppCompatibility.RedditIsFun)
    dependsOn(ExtensionPatches.RIF, rifMorpheSettingsPatch)

    execute {
        galleryParser.method.addInstructions(
            0,
            """
                invoke-static { p1 }, Lapp/morphe/extension/redditisfun/GalleryRepair;->repairGallery(Ljava/io/InputStream;)Ljava/io/InputStream;
                move-result-object p1
            """,
        )
    }
}
