package app.morphe.extension.redditisfun;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;

/** Fits the editor inside the reply card while keeping RiF's original spacing. */
public final class ReplyViewport extends ScrollView {
    static final String EDITOR_TAG = "morphe_reply_body";

    private int editorLimit = Integer.MAX_VALUE;
    private int originalMinimumLines = -1;

    public ReplyViewport(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        View form = getChildAt(0);
        EditText editor = findViewWithTag(EDITOR_TAG);
        if (form == null || editor == null) return;

        if (originalMinimumLines < 0) originalMinimumLines = editor.getMinLines();
        int minimumLines = originalMinimumLines;
        int limit = Integer.MAX_VALUE;
        if (MeasureSpec.getMode(heightSpec) != MeasureSpec.UNSPECIFIED) {
            int fixedHeight = form.getMeasuredHeight() - editor.getMeasuredHeight();
            int available = MeasureSpec.getSize(heightSpec) - getPaddingTop() - getPaddingBottom() - fixedHeight;
            // If the screen cannot fit the fixed rows and one line, the original outer
            // scrolling still lets the user reach the caret and buttons without clipping them.
            int textPadding = editor.getCompoundPaddingTop() + editor.getCompoundPaddingBottom();
            int lineHeight = editor.getLineHeight();
            if (available < originalMinimumLines * lineHeight + textPadding) minimumLines = 1;
            limit = Math.max(lineHeight + textPadding, available);
        }

        if (limit != editorLimit || minimumLines != editor.getMinLines()) {
            editorLimit = limit;
            editor.setMinLines(minimumLines);
            editor.setMaxHeight(limit);
            editor.forceLayout();
            form.forceLayout();
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}
