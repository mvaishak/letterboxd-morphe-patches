package app.template.extension;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Letterboxd ships its own "Graphik" font family ({@code res/font/graphik_app_*}). Text drawn by
 * the extension (the spoiler overlay's label, the tap-to-show-ratings link) falls back to the
 * plain system font unless it explicitly loads that family — which reads as visibly out of place
 * next to the app's own type. Cached after the first (successful or failed) lookup.
 */
final class AppFont {

    private AppFont() {}

    private static Typeface semibold;
    private static boolean tried;

    static Typeface semibold(Context context) {
        if (!tried) {
            tried = true;
            try {
                int id = context.getResources().getIdentifier(
                        "graphik_app_semibold", "font", context.getPackageName());
                if (id != 0) semibold = context.getResources().getFont(id);
            } catch (Throwable ignored) {
            }
        }
        return semibold != null ? semibold : Typeface.DEFAULT_BOLD;
    }
}
