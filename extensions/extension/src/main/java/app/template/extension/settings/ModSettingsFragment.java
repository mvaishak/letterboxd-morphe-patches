package app.template.extension.settings;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.widget.Toast;

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

        SwitchPreference oled = new SwitchPreference(getActivity());
        oled.setKey(Prefs.KEY_THEME_OLED);
        oled.setTitle("Pure black (OLED)");
        oled.setDefaultValue(Boolean.FALSE);
        oled.setOnPreferenceChangeListener(promptRestartOnChange);
        if (ModTheme.isSupported()) {
            oled.setSummary("Repaint Letterboxd's dark surfaces true black. "
                    + "Enable \"Match top bar colour\" too for a black navigation bar.");
        } else {
            oled.setEnabled(false);
            oled.setSummary("Needs Android 12 or newer");
        }
        category.addPreference(oled);

        ListPreference accent = new ListPreference(getActivity());
        accent.setKey(Prefs.KEY_THEME_ACCENT);
        accent.setTitle("Accent colour");
        accent.setDialogTitle("Accent colour");
        accent.setEntries(new CharSequence[] {
                "Letterboxd green", "Amber", "Orange", "Coral", "Pink",
                "Violet", "Blue", "Teal", "Mono (near-white)", "Custom (hex)",
        });
        accent.setEntryValues(new CharSequence[] {
                "green", "amber", "orange", "coral", "pink",
                "violet", "blue", "teal", "mono", "custom",
        });
        accent.setDefaultValue("green");
        accent.setOnPreferenceChangeListener(promptRestartOnChange);
        if (ModTheme.isSupported()) {
            accent.setSummary("%s");
        } else {
            accent.setEnabled(false);
            accent.setSummary("Needs Android 12 or newer");
        }
        category.addPreference(accent);

        EditTextPreference customHex = new EditTextPreference(getActivity());
        customHex.setKey(Prefs.KEY_THEME_ACCENT_HEX);
        customHex.setTitle("Custom accent hex");
        customHex.setDialogTitle("Colour hex, e.g. #5AA9FF");
        customHex.setSummary("Used when Accent colour is set to \"Custom (hex)\"");
        if (ModTheme.isSupported()) {
            customHex.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = String.valueOf(newValue).trim();
                    try {
                        AccentMath.parseHex(value);
                    } catch (Throwable t) {
                        Toast.makeText(getActivity(), "Enter a colour like #5AA9FF",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    Preference list = findPreference(Prefs.KEY_THEME_ACCENT);
                    if (list instanceof ListPreference) {
                        ((ListPreference) list).setValue("custom");
                    }
                    RestartHelper.promptRestart(getActivity());
                    return true;
                }
            });
        } else {
            customHex.setEnabled(false);
        }
        category.addPreference(customHex);
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
