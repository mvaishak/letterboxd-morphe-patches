package app.template.extension.settings;

import android.os.Bundle;
import android.preference.ListPreference;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(Prefs.NAME);

        PreferenceScreen screen = pm.createPreferenceScreen(getActivity());
        setPreferenceScreen(screen);

        buildHideRatings(screen);
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
