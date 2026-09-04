package app.template.extension.settings;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

/**
 * Shown when a preference that only re-applies on process start is changed: an alert that offers
 * to restart Letterboxd now ("OK") or leave it for later.
 */
public final class RestartHelper {

    private RestartHelper() {}

    /** {@code context} must be an Activity context (the dialog needs a UI context). */
    public static void promptRestart(final Context context) {
        if (context == null) return;
        try {
            new AlertDialog.Builder(context)
                    .setTitle("Restart required")
                    .setMessage("This change becomes visible after Letterboxd restarts.")
                    .setNegativeButton("Later", null)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            restart(context);
                        }
                    })
                    .show();
        } catch (Throwable ignored) {
        }
    }

    /** Relaunches the app: schedules the launcher intent a moment out, then ends this process. */
    public static void restart(Context context) {
        try {
            Context app = context.getApplicationContext();
            Intent launch = app.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pending = PendingIntent.getActivity(
                        app, 0, launch,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
                if (alarms != null && pending != null) {
                    alarms.set(AlarmManager.RTC, System.currentTimeMillis() + 150L, pending);
                }
            }
        } catch (Throwable ignored) {
        }
        Runtime.getRuntime().exit(0);
    }
}
