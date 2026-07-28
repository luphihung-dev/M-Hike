package com.luphihung.mhike.util;

import android.graphics.Rect;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

/**
 * Android 15+ draws apps edge-to-edge, so screens must pad themselves
 * around the system bars. This helper pads the bottom/left/right of a
 * root view; the top inset is handled by each screen's app bar.
 */
public final class InsetsHelper {

    /** Gap left between the focused field and whatever covers the form, in dp. */
    private static final int BREATHING_ROOM_DP = 16;

    private InsetsHelper() {
        // Utility class; not meant to be instantiated.
    }

    /** Keeps the given root view clear of the navigation bar and display cutouts. */
    public static void applySystemBarPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, 0, bars.right, bars.bottom);
            return windowInsets;
        });
    }

    /**
     * Same as {@link #applySystemBarPadding(View)}, but also keeps the field the
     * user is typing in on screen.
     *
     * <p>Nothing here tries to work out whether a keyboard is open, because both
     * of the usual signals read as zero on this screen: the decor consumes the
     * keyboard inset before a listener on the layout can see it, and comparing
     * the visible frame against the root view is self-cancelling whenever the
     * window really is resized, since the root shrinks by the same amount.
     *
     * <p>The question that can always be answered is the useful one anyway — is
     * the focused field below the visible area? If it is, that is exactly how
     * far to scroll, whatever is covering it. Room to scroll comes from padding
     * the form by however much of it falls outside the visible area, with
     * clipping turned off.
     */
    public static void applyFormInsets(View root, NestedScrollView form) {
        applySystemBarPadding(root);
        form.setClipToPadding(false);

        // Scrolling triggers another layout pass, so remember what has already
        // been handled rather than reacting to every one of them.
        final View[] handledField = {null};
        final int[] handledAtBottom = {0};

        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect visible = new Rect();
            root.getWindowVisibleDisplayFrame(visible);

            int[] formAt = new int[2];
            form.getLocationOnScreen(formAt);
            int formBelowFold = Math.max(0, formAt[1] + form.getHeight() - visible.bottom);
            if (form.getPaddingBottom() != formBelowFold) {
                form.setPadding(form.getPaddingLeft(), form.getPaddingTop(),
                        form.getPaddingRight(), formBelowFold);
                return; // that change lays out again; act on the next pass
            }

            View focused = form.findFocus();
            if (focused == null) {
                handledField[0] = null;
                return;
            }
            if (focused == handledField[0] && visible.bottom == handledAtBottom[0]) {
                return; // already dealt with this field at this size
            }
            handledField[0] = focused;
            handledAtBottom[0] = visible.bottom;

            int[] focusedAt = new int[2];
            focused.getLocationOnScreen(focusedAt);
            float density = form.getResources().getDisplayMetrics().density;
            int hiddenBy = focusedAt[1] + focused.getHeight()
                    + (int) (BREATHING_ROOM_DP * density) - visible.bottom;

            if (hiddenBy > 0) {
                form.post(() -> form.smoothScrollBy(0, hiddenBy));
            }
        });
    }
}
