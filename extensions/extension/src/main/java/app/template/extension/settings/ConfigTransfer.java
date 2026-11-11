package app.template.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

import app.template.extension.artwork.ArtworkStore;

import org.json.JSONException;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned backup of user preferences and local artwork assignments. */
public final class ConfigTransfer {

    public static final int FORMAT = 2;
    private static final String APP = "letterboxd-morphe-patches";

    private static final boolean STR = false;
    private static final boolean BOOL = true;

    /** key -> is it a boolean. The single source of truth for what's shareable. */
    private static final Map<String, Boolean> KEYS = new LinkedHashMap<>();

    static {
        KEYS.put(Prefs.KEY_THEME_SURFACE, STR);
        KEYS.put(Prefs.KEY_THEME_OLED, BOOL);
        KEYS.put(Prefs.KEY_THEME_ACCENT, STR);
        KEYS.put(Prefs.KEY_THEME_ACCENT_HEX, STR);
        KEYS.put(Prefs.KEY_MATCH_BOTTOM_NAV, BOOL);
        KEYS.put(Prefs.KEY_NAV_INDICATOR, STR);
        KEYS.put(Prefs.KEY_NAV_ITEMS, STR);
        KEYS.put(Prefs.KEY_LAUNCH_TAB, STR);
        KEYS.put(Prefs.KEY_HOME_TABS, STR);
        KEYS.put(Prefs.KEY_HIDE_VIDEO_STORE, BOOL);
        KEYS.put(Prefs.KEY_HIDE_SPONSORED_SPOTLIGHT, BOOL);
        KEYS.put(Prefs.KEY_ARTWORK_MODE, STR);
        KEYS.put(Prefs.KEY_IMDB_TRIVIA, BOOL);
        KEYS.put(Prefs.KEY_HIDE_WHERE_TO_WATCH, BOOL);
        KEYS.put(Prefs.KEY_RUNTIME_HHMM, BOOL);
        KEYS.put(Prefs.KEY_OPEN_IN_PLAYER, BOOL);
        KEYS.put(Prefs.KEY_STREAMING_APP, STR);
        KEYS.put(Prefs.KEY_HIDE_RATINGS_ENABLED, BOOL);
        KEYS.put(Prefs.KEY_HIDE_RATINGS_STYLE, STR);
        KEYS.put(Prefs.KEY_HIDE_RATINGS_ANIMATION, STR);
        KEYS.put(Prefs.KEY_HIDE_RATINGS_CONFETTI_COLOR, STR);
        KEYS.put(Prefs.KEY_HIDE_RATINGS_HAPTIC, BOOL);
    }

    private ConfigTransfer() {}

    public static String export(Context ctx) throws JSONException {
        JSONObject settings = new JSONObject();
        Map<String, ?> values = preferences(ctx).getAll();
        for (Map.Entry<String, Boolean> entry : KEYS.entrySet()) {
            String key = entry.getKey();
            if (!values.containsKey(key)) continue;
            Object value = values.get(key);
            if (!validValue(key, entry.getValue(), value)) {
                throw new JSONException("Invalid saved preference: " + key);
            }
            settings.put(key, value);
        }
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("app", APP);
        root.put("settings", settings);
        root.put("artwork", new ArtworkStore(ctx).backup());
        return root.toString(2);
    }

    public static final class ImportResult {
        public final int settings;
        public final int skippedSettings;
        public final ArtworkStore.Merge artwork;

        ImportResult(int settings, int skippedSettings, ArtworkStore.Merge artwork) {
            this.settings = settings;
            this.skippedSettings = skippedSettings;
            this.artwork = artwork;
        }
    }

    public static ImportResult importJson(Context ctx, String text) throws JSONException {
        JSONObject root = new JSONObject(text);
        Object version = root.opt("format");
        if (!APP.equals(root.opt("app")) || !(version instanceof Integer)
                || ((Integer) version != 1 && (Integer) version != FORMAT)) {
            throw new JSONException("Unknown Mods backup format");
        }
        JSONObject settings = root.getJSONObject("settings");
        SharedPreferences.Editor editor = preferences(ctx).edit();
        int applied = 0;
        int skipped = 0;
        for (Map.Entry<String, Boolean> entry : KEYS.entrySet()) {
            String key = entry.getKey();
            if (!settings.has(key)) continue;
            Object value = settings.get(key);
            if (!validValue(key, entry.getValue(), value)) {
                skipped++;
                continue;
            }
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else editor.putString(key, (String) value);
            applied++;
        }
        ArtworkStore.Merge artwork = (Integer) version == FORMAT
                ? new ArtworkStore(ctx).mergeBackup(root.getJSONObject("artwork"))
                : new ArtworkStore.Merge();
        if (applied > 0 && !editor.commit()) {
            throw new IllegalStateException("Couldn't save settings. Some artwork may have been restored.");
        }
        return new ImportResult(applied, skipped, artwork);
    }

    static boolean resetSettings(Context ctx) {
        SharedPreferences.Editor editor = preferences(ctx).edit();
        for (String key : KEYS.keySet()) editor.remove(key);
        return editor.commit();
    }

    static SharedPreferences preferences(Context ctx) {
        return ctx.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE);
    }

    private static boolean validValue(String key, boolean bool, Object value) {
        return bool ? value instanceof Boolean : value instanceof String && validString(key, (String) value);
    }

    /** Light per-key checks. CSV / free-form keys pass through — their readers already ignore junk. */
    private static boolean validString(String key, String v) {
        switch (key) {
            case Prefs.KEY_ARTWORK_MODE:
                return oneOf(v, "automatic", "local", "off");
            case Prefs.KEY_THEME_SURFACE:
                return v.isEmpty() || v.equals("stock") || v.equals("oled");
            case Prefs.KEY_NAV_INDICATOR:
                return oneOf(v, "stock", "nopill", "white", "accent", "accentPill");
            case Prefs.KEY_LAUNCH_TAB:
                return oneOf(v, "last", "popular", "search", "activity", "watchlist", "profile");
            case Prefs.KEY_STREAMING_APP:
                return oneOf(v, "stremio", "nuvio");
            case Prefs.KEY_HIDE_RATINGS_STYLE:
                return oneOf(v, "panel", "link", "shimmer", "burst");
            case Prefs.KEY_HIDE_RATINGS_ANIMATION:
                return oneOf(v, "default", "crumble", "confetti");
            case Prefs.KEY_HIDE_RATINGS_CONFETTI_COLOR:
                return oneOf(v, "accent", "letterboxd", "red");
            case Prefs.KEY_THEME_ACCENT_HEX:
                if (v.isEmpty()) return true;
                try {
                    AccentMath.parseHex(v);
                    return true;
                } catch (Throwable t) {
                    return false;
                }
            default:
                return v.length() <= 512; // theme_accent, nav_items, home_tabs — reader-sanitised
        }
    }

    private static boolean oneOf(String v, String... allowed) {
        for (String a : allowed) {
            if (a.equals(v)) return true;
        }
        return false;
    }
}
