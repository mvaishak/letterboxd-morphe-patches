package app.template.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single {@link SharedPreferences} store shared by every patch's runtime code and by
 * {@link ModSettingsFragment}. Keys are namespaced per feature.
 *
 * <p>{@link #load(Context)} is called from {@code LetterboxdApplication.onCreate} (by the
 * "Mod settings" patch) and, defensively, from feature code that has a {@link Context} to hand.
 * Every accessor is null- and exception-safe: if the store never loaded, callers get their
 * supplied default and the patch behaves as if there were no settings screen at all.
 */
public final class Prefs {

    public static final String NAME = "morphe_letterboxd";

    // "Hide ratings until watched"
    public static final String KEY_HIDE_RATINGS_ENABLED = "hide_ratings_enabled";
    public static final String KEY_HIDE_RATINGS_STYLE = "hide_ratings_style";

    private static SharedPreferences sp;

    private Prefs() {}

    public static void load(Context context) {
        try {
            if (sp == null && context != null) {
                Context app = context.getApplicationContext();
                sp = (app != null ? app : context)
                        .getSharedPreferences(NAME, Context.MODE_PRIVATE);
            }
        } catch (Throwable ignored) {
        }
    }

    /** True only if the store loaded and the user has explicitly set this key. */
    public static boolean has(String key) {
        try {
            return sp != null && sp.contains(key);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        try {
            return sp != null ? sp.getBoolean(key, fallback) : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static String getString(String key, String fallback) {
        try {
            if (sp == null) return fallback;
            String value = sp.getString(key, fallback);
            return (value == null || value.isEmpty()) ? fallback : value;
        } catch (Throwable t) {
            return fallback;
        }
    }
}
