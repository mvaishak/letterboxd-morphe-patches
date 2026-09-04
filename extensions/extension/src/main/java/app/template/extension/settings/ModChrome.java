package app.template.extension.settings;

import android.view.View;

/**
 * Runtime chrome tweaks that used to be baked in by resource patches, now gated on {@link Prefs}
 * so the "Mod settings" screen can toggle them. Applied once per screen setup; a change takes
 * effect on the next app start.
 */
public final class ModChrome {

    private ModChrome() {}

    /** Injected at the top of {@code MainActivity.setup} with the {@code BottomNavigationView}. */
    public static void applyBottomNav(View bottomNav) {
        try {
            if (bottomNav == null) return;
            Prefs.load(bottomNav.getContext());
            if (Prefs.getBoolean(Prefs.KEY_MATCH_BOTTOM_NAV, true)) {
                bottomNav.setBackgroundColor(0xFF000000); // @color/black100, the top bar colour
            }
        } catch (Throwable ignored) {
        }
    }
}
