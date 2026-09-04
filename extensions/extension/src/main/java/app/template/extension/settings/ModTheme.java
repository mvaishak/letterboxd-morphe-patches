package app.template.extension.settings;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

/**
 * Runtime theme overlays for the "Mod settings" screen. Slice 1: a single "Pure black (OLED)"
 * overlay ({@code assets/morphe/oled.arsc}), loaded via {@code ResourcesLoader} on Android 12+.
 *
 * <p>Toggling the pref triggers a restart (see {@link RestartHelper}), so there is no in-session
 * "remove overlay" path — at process start the overlay is either loaded everywhere or not at all.
 */
public final class ModTheme {

    private ModTheme() {}

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 31;
    }

    /** Injected at the top of {@code LetterboxdApplication.onCreate}. */
    public static void initialize(Context context) {
        try {
            if (!isSupported() || context == null) return;

            Prefs.load(context);
            boolean oled = Prefs.getBoolean(Prefs.KEY_THEME_OLED, false);
            String accent = Prefs.getString(Prefs.KEY_THEME_ACCENT, "green");
            boolean hasAccent = accent != null && !accent.isEmpty() && !"green".equals(accent);
            if (!oled && !hasAccent) return;

            ModThemeApi31.prepare(context, oled, accent);
            ModThemeApi31.applyTo(context.getResources());

            Context appContext = context.getApplicationContext();
            if (appContext instanceof Application) {
                ((Application) appContext).registerActivityLifecycleCallbacks(new Lifecycle());
            }
        } catch (Throwable ignored) {
        }
    }

    private static final class Lifecycle implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {
            if (activity != null) ModThemeApi31.applyTo(activity.getResources());
        }

        @Override public void onActivityCreated(Activity a, Bundle b) {}
        @Override public void onActivityStarted(Activity a) {}
        @Override public void onActivityResumed(Activity a) {}
        @Override public void onActivityPaused(Activity a) {}
        @Override public void onActivityStopped(Activity a) {}
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        @Override public void onActivityDestroyed(Activity a) {}
    }
}
