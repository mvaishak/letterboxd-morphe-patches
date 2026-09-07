package app.template.extension;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * Opens the current member's watchlist by re-firing Letterboxd's own {@code
 * letterboxd://shortcut/watchlist} deep link at {@code MainActivity}. That link is the launcher
 * shortcut the app already ships; {@code MainActivity.handleDeeplink} routes it through
 * {@code handleAppShortcuts}, which selects the Profile tab and navigates to the member's
 * watchlist. Reusing it keeps the Watchlist nav item on the app's own, tested path — no
 * NavController or route construction from here.
 */
public final class WatchlistNav {

    private WatchlistNav() {}

    public static void open(Activity activity) {
        try {
            if (activity == null) return;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("letterboxd://shortcut/watchlist"));
            intent.setClassName(activity, "com.letterboxd.letterboxd.MainActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        } catch (Throwable ignored) {
        }
    }
}
