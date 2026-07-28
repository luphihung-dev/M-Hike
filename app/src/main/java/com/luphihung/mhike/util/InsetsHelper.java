package com.luphihung.mhike.util;

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

    /**
     * Keeps the given root view clear of the navigation bar, display cutouts
     * and the on-screen keyboard.
     *
     * <p>Edge-to-edge windows are not resized by adjustResize on their own, so
     * the keyboard inset has to be applied here as well. Without it the window
     * keeps its full height and the keyboard simply covers whatever sits at the
     * bottom of the screen — on the hike form that is the description field and
     * the save button. Taking the larger of the two insets avoids padding twice
     * when the keyboard is already taller than the navigation bar.
     */
    public static void applySystemBarPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, 0, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
    }
}
