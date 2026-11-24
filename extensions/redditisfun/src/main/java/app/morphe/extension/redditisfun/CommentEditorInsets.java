package app.morphe.extension.redditisfun;

import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

public final class CommentEditorInsets {
    private CommentEditorInsets() {}

    public static void keepAboveKeyboard(View content) {
        final int paddingBottom = content.getPaddingBottom();
        final Rect visibleFrame = new Rect();
        final int[] location = new int[2];

        // The observer belongs to this dialog's view tree and is discarded with it.
        // Check the measured bounds as well as the insets so adjustResize is never counted twice.
        content.getViewTreeObserver().addOnPreDrawListener(() -> {
            content.getLocationOnScreen(location);
            content.getWindowVisibleDisplayFrame(visibleFrame);
            int visibleBottom = visibleFrame.bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                visibleBottom = Math.min(visibleBottom, Api30.imeTop(content, visibleBottom));
            }
            int overlap = Math.max(0, location[1] + content.getHeight() - visibleBottom);
            int bottom = paddingBottom + overlap;
            if (content.getPaddingBottom() == bottom) return true;

            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                    content.getPaddingRight(), bottom);
            return false;
        });
    }

    private static final class Api30 {
        static int imeTop(View content, int fallback) {
            WindowInsets insets = content.getRootWindowInsets();
            if (insets == null || !insets.isVisible(WindowInsets.Type.ime())) return fallback;
            int imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom;
            WindowManager manager = content.getContext().getSystemService(WindowManager.class);
            if (imeBottom == 0 || manager == null) return fallback;
            return manager.getCurrentWindowMetrics().getBounds().bottom - imeBottom;
        }
    }
}
