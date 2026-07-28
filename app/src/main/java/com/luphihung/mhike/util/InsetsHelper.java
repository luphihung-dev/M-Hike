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

    /** Space kept between the app bar and the field being edited, in dp. */
    private static final int FIELD_TOP_MARGIN_DP = 8;

    /** Scratch space added below the form so the last fields can reach the top, in dp. */
    private static final int SCROLL_HEADROOM_DP = 320;

    /** Long enough for the keyboard to have finished sliding up, in milliseconds. */
    private static final int KEYBOARD_SETTLE_MS = 300;

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
     * Scrolls a field to the top of the form when it is tapped, so the keyboard
     * cannot cover what is being typed.
     *
     * <p>Nothing here measures the keyboard. Earlier attempts did, and on this
     * screen every signal reads as nothing: the decor consumes the keyboard
     * inset before a listener on the layout can see it, and comparing the
     * visible frame against the root view cancels itself out when the window is
     * resized, because the root shrinks by the same amount. Focus, on the other
     * hand, is unambiguous — the field the user tapped is the field to show.
     *
     * <p>Moving a field to the top needs somewhere to scroll to, so headroom is
     * added below the form while a field is being edited and taken away again
     * afterwards. The scroll is delayed until the keyboard has finished sliding
     * up, otherwise it would be measured against a screen that is still moving.
     */
    public static void keepFocusedFieldVisible(NestedScrollView form, View... fields) {
        View content = form.getChildAt(0);
        if (content == null) {
            return;
        }
        float density = form.getResources().getDisplayMetrics().density;
        int headroom = (int) (SCROLL_HEADROOM_DP * density);
        int topMargin = (int) (FIELD_TOP_MARGIN_DP * density);
        int restingBottom = content.getPaddingBottom();

        for (View field : fields) {
            if (field == null) {
                continue;
            }
            field.setOnFocusChangeListener((view, hasFocus) -> {
                if (!hasFocus) {
                    // Drop the headroom once nothing on the form is being edited.
                    form.post(() -> {
                        if (form.findFocus() == null) {
                            setBottomPadding(content, restingBottom);
                        }
                    });
                    return;
                }
                setBottomPadding(content, restingBottom + headroom);
                view.postDelayed(() -> form.smoothScrollTo(0,
                        Math.max(0, distanceIntoForm(view, form) - topMargin)),
                        KEYBOARD_SETTLE_MS);
            });
        }
    }

    /** Distance from the top of the scrolling content down to this view. */
    private static int distanceIntoForm(View view, NestedScrollView form) {
        int distance = 0;
        View step = view;
        while (step != null && step != form) {
            distance += step.getTop();
            step = step.getParent() instanceof View ? (View) step.getParent() : null;
        }
        return distance;
    }

    private static void setBottomPadding(View view, int bottom) {
        if (view.getPaddingBottom() != bottom) {
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bottom);
        }
    }
}
