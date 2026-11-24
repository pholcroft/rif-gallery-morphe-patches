package app.morphe.extension.redditisfun;

import android.graphics.Rect;
import android.view.View;

public final class CommentEditorInsets {
    private CommentEditorInsets() {}

    public static void keepAboveKeyboard(View content) {
        final int paddingBottom = content.getPaddingBottom();
        final Rect visibleFrame = new Rect();
        final int[] location = new int[2];

        // The visible frame already excludes the docked keyboard. Add only the part that still
        // overlaps this dialog, since Android may have resized its window before this draw.
        content.getViewTreeObserver().addOnPreDrawListener(() -> {
            content.getWindowVisibleDisplayFrame(visibleFrame);
            if (visibleFrame.isEmpty() || content.getHeight() == 0) return true;

            content.getLocationOnScreen(location);
            int overlap = Math.max(0, location[1] + content.getHeight() - visibleFrame.bottom);
            int bottom = paddingBottom + overlap;
            if (content.getPaddingBottom() == bottom) return true;

            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                    content.getPaddingRight(), bottom);
            return false;
        });
    }
}
