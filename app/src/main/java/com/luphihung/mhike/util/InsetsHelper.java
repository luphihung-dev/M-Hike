package com.luphihung.mhike.util;

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

    /** Gap left between the focused field and the top of the keyboard, in dp. */
    private static final int BREATHING_ROOM_DP = 16;

    private InsetsHelper() {
        // Utility class; not meant to be instantiated.
    }

    /** Keeps the given root view clear of the navigation bar and display cutouts. */
    public static void applySystemBarPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            applyBars(view, windowInsets);
            return windowInsets;
        });
    }

    /**
     * Same as {@link #applySystemBarPadding(View)}, but also lifts the field the
     * user is typing in above the keyboard.
     *
     * <p>Two things are needed. The scrolling view is laid out by the app bar's
     * scrolling behaviour, which measures it against the height of the whole
     * CoordinatorLayout and ignores that view's padding, so its visible area
     * never shrinks when the keyboard opens: extra bottom padding inside it,
     * with clipping turned off, is what creates room to scroll the last fields
     * up. And because the view still counts as on screen — the keyboard merely
     * covers it — asking politely for it to be revealed does nothing, so the
     * distance is worked out and scrolled explicitly.
     */
    public static void applyFormInsets(View root, NestedScrollView form) {
        form.setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = applyBars(view, windowInsets);
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            // The root already reserves the navigation bar, so only the part of
            // the keyboard reaching past it still covers the form.
            form.setPadding(form.getPaddingLeft(), form.getPaddingTop(),
                    form.getPaddingRight(), Math.max(0, keyboard.bottom - bars.bottom));

            if (windowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
                form.post(() -> liftFocusedField(view, form, keyboard.bottom));
            }
            return windowInsets;
        });
    }

    /**
     * Scrolls the focused field far enough that it clears the top of the
     * keyboard. Everything is measured against the root view, whose height is
     * the height of the window, so the keyboard inset can be used directly.
     */
    private static void liftFocusedField(View root, NestedScrollView form, int keyboardHeight) {
        View focused = form.findFocus();
        if (focused == null) {
            return;
        }
        int keyboardTop = root.getHeight() - keyboardHeight;
        if (keyboardTop <= 0) {
            return;
        }
        int[] rootAt = new int[2];
        int[] focusedAt = new int[2];
        root.getLocationInWindow(rootAt);
        focused.getLocationInWindow(focusedAt);

        float density = form.getResources().getDisplayMetrics().density;
        int gap = (int) (BREATHING_ROOM_DP * density);
        int focusedBottom = focusedAt[1] - rootAt[1] + focused.getHeight();

        int hiddenBy = focusedBottom + gap - keyboardTop;
        if (hiddenBy > 0) {
            form.smoothScrollBy(0, hiddenBy);
        }
    }

    /** Pads a root view around the navigation bar and any display cutout. */
    private static Insets applyBars(View root, WindowInsetsCompat windowInsets) {
        Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout());
        root.setPadding(bars.left, 0, bars.right, bars.bottom);
        return bars;
    }
}
