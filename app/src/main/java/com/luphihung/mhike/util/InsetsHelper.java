package com.luphihung.mhike.util;

import android.graphics.Rect;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Android 15+ draws apps edge-to-edge, so screens must pad themselves
 * around the system bars. This helper pads the bottom/left/right of a
 * root view; the top inset is handled by each screen's app bar.
 */
public final class InsetsHelper {

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
     * Same as {@link #applySystemBarPadding(View)}, but also keeps the keyboard
     * from covering the bottom of a scrolling form.
     *
     * <p>Padding the root alone is not enough here. The scrolling view is laid
     * out by the app bar's scrolling behaviour, which measures it against the
     * height of the whole CoordinatorLayout and ignores that view's padding, so
     * the visible area never shrinks — the keyboard just covers the last fields
     * and there is no extra range to scroll them into view. Adding the same
     * amount as bottom padding <em>inside</em> the scrolling view, with clipping
     * turned off, gives it exactly that range instead.
     *
     * <p>The keyboard also arrives after the field has taken focus, so nothing
     * would scroll on its own; once the padding is in place the focused field is
     * asked back on screen.
     */
    public static void applyFormInsets(View root, View scrollable) {
        scrollable.setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = applyBars(view, windowInsets);
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            // The root already reserves the navigation bar, so only the part of
            // the keyboard that reaches past it still covers the form.
            int hidden = Math.max(0, keyboard.bottom - bars.bottom);
            scrollable.setPadding(scrollable.getPaddingLeft(), scrollable.getPaddingTop(),
                    scrollable.getPaddingRight(), hidden);

            if (hidden > 0) {
                View focused = scrollable.findFocus();
                if (focused != null) {
                    focused.post(() -> focused.requestRectangleOnScreen(
                            new Rect(0, 0, focused.getWidth(), focused.getHeight()), false));
                }
            }
            return windowInsets;
        });
    }

    /** Pads a root view around the navigation bar and any display cutout. */
    private static Insets applyBars(View root, WindowInsetsCompat windowInsets) {
        Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout());
        root.setPadding(bars.left, 0, bars.right, bars.bottom);
        return bars;
    }
}
