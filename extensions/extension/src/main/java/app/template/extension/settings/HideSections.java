package app.template.extension.settings;

import android.view.View;

import androidx.fragment.app.Fragment;

/**
 * Static, one-shot section hiding for "Mod settings" toggles that don't need the reactive
 * per-visit logic "Hide ratings until watched" does — just hide the fragment's own root view at
 * {@code onViewCreated} if the pref says so. Fails open (never hides anything) on any error.
 */
public final class HideSections {

    private HideSections() {}

    /** Injected at the top of {@code WhereToWatchFragment.onViewCreated}. */
    public static void enforceWhereToWatch(Fragment fragment) {
        try {
            if (!Prefs.hideWhereToWatch()) return;
            View view = fragment.getView();
            if (view != null) view.setVisibility(View.GONE);
        } catch (Throwable ignored) {
        }
    }
}
