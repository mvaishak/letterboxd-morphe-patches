package app.template.extension.settings;

import android.app.Activity;
import android.os.Bundle;

/**
 * Standalone host for {@link ModSettingsFragment}. Added to the Letterboxd manifest by the
 * "Mod settings" patch and launched from a launcher long-press shortcut (and, later, from a
 * row inside Letterboxd's own settings screen).
 *
 * <p>Framework {@code Activity} + {@code android.preference} are used deliberately: they need no
 * extra runtime dependency and no {@code preferenceTheme} attribute on the host theme.
 */
@SuppressWarnings("deprecation")
public class ModSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Letterboxd Mods");

        if (getFragmentManager().findFragmentById(android.R.id.content) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new ModSettingsFragment())
                    .commit();
        }
    }
}
