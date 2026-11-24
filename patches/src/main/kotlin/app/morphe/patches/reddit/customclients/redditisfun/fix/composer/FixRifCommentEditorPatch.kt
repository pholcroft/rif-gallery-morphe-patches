package app.morphe.patches.reddit.customclients.redditisfun.fix.composer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.reddit.customclients.AppCompatibility
import app.morphe.patches.reddit.customclients.ExtensionPatches
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private val composerLayoutPatch = resourcePatch(default = false) {
    execute {
        document("res/layout/comment_reply_dialog.xml").use { layout ->
            val editors = layout.getElementsByTagName("com.andrewshu.android.reddit.layout.MonitoringEditText")
            val editor = (0 until editors.length).map { editors.item(it) as Element }
                .single { it.getAttribute("android:id") == "@id/reply_body" }
            val composer = editor.parentNode as Element
            val scroll = composer.parentNode as Element
            check(composer.tagName == "LinearLayout" && scroll.tagName == "ScrollView") {
                "Unexpected RiF comment composer layout"
            }

            // Let the dialog constrain the form. Only the text area gives up height as it fills.
            val frame = layout.createElement("FrameLayout")
            for (attribute in listOf("android:padding", "android:layout_width", "android:layout_height")) {
                frame.setAttribute(attribute, scroll.getAttribute(attribute))
            }
            frame.appendChild(composer)
            scroll.parentNode.replaceChild(frame, scroll)
            editor.setAttribute("android:layout_weight", "1")
            editor.setAttribute("android:scrollbars", "vertical")
            editor.setAttribute("android:imeOptions", "flagNoFullscreen")
        }
    }
}

private val createCommentComposer = Fingerprint(
    definingClass = "Lcom/andrewshu/android/reddit/comments/reply/t;",
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;"),
    returnType = "Landroid/view/View;",
)

@Suppress("unused")
val fixRifCommentEditorPatch = bytecodePatch(
    name = "Fix comment editor keyboard resizing",
    description = "Keeps the reply caret and compose buttons visible while typing with the keyboard open.",
    default = true,
) {
    compatibleWith(AppCompatibility.RedditIsFunFree)
    dependsOn(ExtensionPatches.RIF, composerLayoutPatch)

    execute {
        val method = createCommentComposer.method
        val instructions = method.implementation!!.instructions
        val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
        val root = (instructions[returnIndex] as OneRegisterInstruction).registerA
        method.addInstruction(
            returnIndex,
            "invoke-static/range { v$root .. v$root }, " +
                "Lapp/morphe/extension/redditisfun/CommentEditorInsets;->keepAboveKeyboard(Landroid/view/View;)V",
        )
    }
}
