package app.template.extension.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * One-time "what's new / how to open Mod settings" dialog, shown the first time the app is opened
 * after a patch. Injected into {@code MainActivity.onCreate} by the "Mod settings" patch.
 */
public final class ModWelcome {

    private ModWelcome() {}

    /** Bump when {@link #BODY} changes so returning users see it once more. */
    private static final int BUILD = 1;
    private static final String KEY = "welcome_build";

    private static final String TITLE = "Letterboxd Mods";

    private static final String BODY =
            "Thanks for installing.\n\n"
          + "Open the Mods screen any time:\n"
          + "  •  long-press the Letterboxd app icon, or\n"
          + "  •  long-press the settings gear on your profile tab.\n\n"
          + "What's included\n"
          + "  •  Appearance: OLED surface, custom accent (presets or any hex), bottom-nav style\n"
          + "  •  Hide ratings until watched, with a tap-to-reveal cover\n"
          + "  •  Hide the Video Store row from the Films tab\n"
          + "  •  Brighter \"watched by\" stars";

    /** Injected after {@code super.onCreate} in {@code MainActivity}. */
    public static void maybeShow(final Activity activity) {
        try {
            if (activity == null) return;
            Prefs.load(activity);
            if (String.valueOf(BUILD).equals(Prefs.getString(KEY, ""))) return;

            activity.getWindow().getDecorView().post(new Runnable() {
                @Override public void run() {
                    show(activity);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void show(Activity activity) {
        try {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            new AlertDialog.Builder(activity)
                    .setTitle(TITLE)
                    .setMessage(BODY)
                    .setPositiveButton("Got it", null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override public void onDismiss(DialogInterface d) {
                            Prefs.putString(KEY, String.valueOf(BUILD));
                        }
                    })
                    .show();
        } catch (Throwable ignored) {
        }
    }
}
