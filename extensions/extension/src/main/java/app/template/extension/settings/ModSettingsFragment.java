package app.template.extension.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

/**
 * The mod settings screen, built in code (no preference XML shipped). Every preference writes to
 * the {@link Prefs#NAME} store that the patches' runtime code reads.
 *
 * <p>Ordering matters with the framework preference API: the screen must be attached to the
 * {@link PreferenceManager} (via {@link #setPreferenceScreen}) before {@code setDependency} can
 * resolve a sibling by key, so dependencies are wired only after every preference has been added.
 *
 * <p>Phase 1 exposes only "Hide ratings until watched". More feature groups are added here as
 * each patch is wired to read {@link Prefs}.
 */
@SuppressWarnings("deprecation")
public class ModSettingsFragment extends PreferenceFragment {

    /** Set on preferences whose patch only re-reads the value on process start. */
    private final Preference.OnPreferenceChangeListener promptRestartOnChange =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    RestartHelper.promptRestart(getActivity());
                    return true;
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(Prefs.NAME);

        // Migrate the old boolean OLED key to the surface-style key.
        android.content.SharedPreferences sp = pm.getSharedPreferences();
        if (!sp.contains(Prefs.KEY_THEME_SURFACE) && sp.getBoolean(Prefs.KEY_THEME_OLED, false)) {
            sp.edit().putString(Prefs.KEY_THEME_SURFACE, "oled").apply();
        }

        PreferenceScreen screen = pm.createPreferenceScreen(getActivity());
        setPreferenceScreen(screen);

        buildHideRatings(screen);
        buildHome(screen);
        buildNavigationBar(screen);
        buildTheme(screen);
    }

    private void buildTheme(PreferenceScreen screen) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle("Theme");
        screen.addPreference(category);

        ListPreference surface = new ListPreference(getActivity());
        surface.setKey(Prefs.KEY_THEME_SURFACE);
        surface.setTitle("Surface style");
        surface.setDialogTitle("Surface style");
        surface.setEntries(new CharSequence[] {
                "Stock", "Material You (wallpaper)", "Pure black (OLED)",
        });
        surface.setEntryValues(new CharSequence[] { "stock", "wallpaper", "oled" });
        surface.setDefaultValue("stock");
        surface.setOnPreferenceChangeListener(promptRestartOnChange);
        if (ModTheme.isSupported()) {
            surface.setSummary("%s — enable \"Match top bar colour\" too for a black navigation bar");
        } else {
            surface.setEnabled(false);
            surface.setSummary("Needs Android 12 or newer");
        }
        category.addPreference(surface);

        final Preference accent = new Preference(getActivity());
        accent.setTitle("Accent colour");
        if (ModTheme.isSupported()) {
            refreshAccent(accent);
            accent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String key = Prefs.getString(Prefs.KEY_THEME_ACCENT, "green");
                    String hex = Prefs.getString(Prefs.KEY_THEME_ACCENT_HEX, "");
                    new AccentPickerDialog(getActivity(), key, hex,
                            new AccentPickerDialog.OnAccentChosen() {
                                @Override
                                public void onChosen(String accentKey, String customHex) {
                                    getPreferenceManager().getSharedPreferences().edit()
                                            .putString(Prefs.KEY_THEME_ACCENT, accentKey)
                                            .putString(Prefs.KEY_THEME_ACCENT_HEX, customHex)
                                            .apply();
                                    refreshAccent(accent);
                                    RestartHelper.promptRestart(getActivity());
                                }
                            }).show();
                    return true;
                }
            });
        } else {
            accent.setEnabled(false);
            accent.setSummary("Needs Android 12 or newer");
        }
        category.addPreference(accent);
    }

    private void refreshAccent(Preference accent) {
        String key = Prefs.getString(Prefs.KEY_THEME_ACCENT, "green");
        String hex = Prefs.getString(Prefs.KEY_THEME_ACCENT_HEX, "");
        int argb = AccentPresets.previewColor(key, hex);
        String label = AccentPresets.CUSTOM.equals(key)
                ? "Custom  " + (hex.isEmpty() ? "" : hex)
                : AccentPresets.LABELS.get(key);
        accent.setSummary(label == null ? "Letterboxd green" : label);

        android.graphics.drawable.GradientDrawable dot =
                new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dot.setColor(0xFF000000 | argb);
        int size = Math.round(22 * getResources().getDisplayMetrics().density);
        dot.setSize(size, size);
        accent.setIcon(dot);
    }

    private void buildNavigationBar(PreferenceScreen screen) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle("Navigation bar");
        screen.addPreference(category);

        SwitchPreference matchColor = new SwitchPreference(getActivity());
        matchColor.setKey(Prefs.KEY_MATCH_BOTTOM_NAV);
        matchColor.setTitle("Match top bar colour");
        matchColor.setSummary("Paint the bottom navigation bar black to match the top bar");
        matchColor.setDefaultValue(Boolean.TRUE);
        matchColor.setOnPreferenceChangeListener(promptRestartOnChange);
        category.addPreference(matchColor);
    }

    private void buildHome(PreferenceScreen screen) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle("Home");
        screen.addPreference(category);

        SwitchPreference hideVideoStore = new SwitchPreference(getActivity());
        hideVideoStore.setKey(Prefs.KEY_HIDE_VIDEO_STORE);
        hideVideoStore.setTitle("Hide Video Store");
        hideVideoStore.setSummary("Remove the Video Store promo row from the Films tab");
        hideVideoStore.setDefaultValue(Boolean.TRUE);
        hideVideoStore.setOnPreferenceChangeListener(promptRestartOnChange);
        category.addPreference(hideVideoStore);
    }

    private void buildHideRatings(PreferenceScreen screen) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle("Hide ratings until watched");
        screen.addPreference(category);

        SwitchPreference enabled = new SwitchPreference(getActivity());
        enabled.setKey(Prefs.KEY_HIDE_RATINGS_ENABLED);
        enabled.setTitle("Enabled");
        enabled.setSummary("Cover a film's community rating until you mark it watched");
        enabled.setDefaultValue(Boolean.TRUE);
        category.addPreference(enabled);

        ListPreference style = new ListPreference(getActivity());
        style.setKey(Prefs.KEY_HIDE_RATINGS_STYLE);
        style.setTitle("Reveal style");
        style.setDialogTitle("Reveal style");
        style.setEntries(new CharSequence[] {
                "Frosted panel", "Tap-to-show link", "Shimmer", "Tap to burst",
        });
        style.setEntryValues(new CharSequence[] {
                "panel", "link", "shimmer", "burst",
        });
        style.setDefaultValue("panel");
        style.setSummary("%s");
        category.addPreference(style);

        // Both preferences are now in the attached hierarchy, so this resolves.
        style.setDependency(Prefs.KEY_HIDE_RATINGS_ENABLED);
    }
}
