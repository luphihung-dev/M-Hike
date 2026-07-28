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

    /** Gap left between the focused field and the top of the keyboard, in dp. */
    private static final int BREATHING_ROOM_DP = 16;

    /** A covered strip smaller than this fraction of the window is not a keyboard. */
    private static final int KEYBOARD_MIN_FRACTION = 5;

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
     * Same as {@link #applySystemBarPadding(View)}, but also lifts the field the
     * user is typing in above the keyboard.
     *
     * <p>The keyboard is found by measuring the window rather than by reading a
     * WindowInsets value. The activity never opts the window out of fitting the
     * system windows, so the decor consumes the keyboard inset before any
     * listener here could see it and it always reads as absent. The visible
     * display frame reports the strip the keyboard covers whichever way the
     * window is set up.
     *
     * <p>Two things then have to happen. The scrolling view is laid out by the
     * app bar's scrolling behaviour, which measures it against the height of the
     * whole CoordinatorLayout, so its visible area never shrinks: bottom padding
     * inside it, with clipping turned off, is what creates room to scroll the
     * last fields up. And nothing scrolls on its own, because the field still
     * counts as on screen with the keyboard merely drawn over it, so the
     * distance is worked out and scrolled explicitly.
     */
    public static void applyFormInsets(View root, NestedScrollView form) {
        applySystemBarPadding(root);
        form.setClipToPadding(false);

        // Only react when the keyboard opens or the user moves to another field,
        // otherwise scrolling would retrigger this listener and loop.
        final boolean[] keyboardWasOpen = {false};
        final View[] lastFocused = {null};

        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect visible = new Rect();
            root.getWindowVisibleDisplayFrame(visible);
            int windowHeight = root.getRootView().getHeight();
            int covered = windowHeight - visible.bottom;
            boolean keyboardOpen = covered > windowHeight / KEYBOARD_MIN_FRACTION;

            int wanted = keyboardOpen ? covered : 0;
            if (form.getPaddingBottom() != wanted) {
                form.setPadding(form.getPaddingLeft(), form.getPaddingTop(),
                        form.getPaddingRight(), wanted);
            }

            View focused = form.findFocus();
            boolean movedField = focused != lastFocused[0];
            if (keyboardOpen && (!keyboardWasOpen[0] || movedField)) {
                liftAbove(form, focused, visible.bottom);
            }
            keyboardWasOpen[0] = keyboardOpen;
            lastFocused[0] = focused;
        });
    }

    /** Scrolls the form so the focused field sits above the given screen line. */
    private static void liftAbove(NestedScrollView form, View focused, int keyboardTop) {
        if (focused == null) {
            return;
        }
        int[] focusedAt = new int[2];
        focused.getLocationOnScreen(focusedAt);

        float density = form.getResources().getDisplayMetrics().density;
        int gap = (int) (BREATHING_ROOM_DP * density);
        int hiddenBy = focusedAt[1] + focused.getHeight() + gap - keyboardTop;

        if (hiddenBy > 0) {
            form.post(() -> form.smoothScrollBy(0, hiddenBy));
        }
    }
}
