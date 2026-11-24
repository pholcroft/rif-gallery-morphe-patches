package app.morphe.extension.redditisfun;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;

public final class CommentEditorInsets {
    private CommentEditorInsets() {}

    public static void keepAboveKeyboard(View content) {
        View editor = content.findViewWithTag(ReplyViewport.EDITOR_TAG);
        if (editor == null) return;
        View viewport = (View) editor.getParent().getParent();
        View card = (View) viewport.getParent();
        FrameLayout.LayoutParams position = (FrameLayout.LayoutParams) card.getLayoutParams();
        int originalGravity = position.gravity;
        int originalBottom = content.getPaddingBottom();
        Rect visible = new Rect();
        int[] origin = new int[2];
        Point displaySize = new Point();
        int tolerance = ViewConfiguration.get(content.getContext()).getScaledTouchSlop();

        content.getViewTreeObserver().addOnPreDrawListener(() -> {
            content.getWindowVisibleDisplayFrame(visible);
            if (visible.isEmpty() || content.getHeight() == 0) return true;

            boolean docked;
            if (Build.VERSION.SDK_INT >= 30) {
                docked = Api30.hasDockedKeyboard(content, visible.bottom, tolerance);
            } else {
                content.getDisplay().getSize(displaySize);
                docked = displaySize.y - visible.bottom > tolerance;
            }

            content.getLocationOnScreen(origin);
            int overlap = docked ? Math.max(0, origin[1] + content.getHeight() - visible.bottom) : 0;
            int bottom = originalBottom + overlap;
            int gravity = docked ? (originalGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | Gravity.BOTTOM
                    : originalGravity;

            boolean changed = false;
            if (position.gravity != gravity) {
                position.gravity = gravity;
                card.setLayoutParams(position);
                changed = true;
            }
            if (content.getPaddingBottom() != bottom) {
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                        content.getPaddingRight(), bottom);
                changed = true;
            }
            return !changed;
        });
    }

    private static final class Api30 {
        static boolean hasDockedKeyboard(View content, int visibleBottom, int tolerance) {
            WindowInsets insets = content.getRootWindowInsets();
            if (insets == null || !insets.isVisible(WindowInsets.Type.ime())) return false;
            if (insets.getInsets(WindowInsets.Type.ime()).bottom > 0) return true;

            // A resized dialog can report zero remaining IME inset even when it is docked.
            WindowManager manager = content.getContext().getSystemService(WindowManager.class);
            int navigation = insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom;
            return manager != null && manager.getCurrentWindowMetrics().getBounds().bottom
                    - navigation - visibleBottom > tolerance;
        }
    }
}
