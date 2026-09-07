package app.template.extension;

import android.app.Activity;

import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;

/**
 * Sends the tabs NavController to the member's watchlist — the same destination
 * ({@code Route.MemberWatchlist}) Letterboxd's own {@code handleAppShortcuts} navigates to for
 * {@code letterboxd://shortcut/watchlist}, but called directly and synchronously from the
 * bottom-nav tap rather than through the deep-link coroutine (which also flipped the selected
 * tab to Profile in a second async step, hence an earlier two-tap confusion).
 *
 * <p>Crucially it navigates with the <em>same</em> {@link NavOptions} the app's own tab switches
 * use — {@code launchSingleTop}, {@code restoreState}, and {@code popUpTo(graph start)} with
 * {@code saveState} — so the watchlist sits in the back stack as a peer of the other tabs. Bare
 * {@code navigate(route)} instead stacked it on top of the current tab, so the next tab tap only
 * popped it and a second tap was needed to actually switch.
 *
 * <p>The bottom bar stays put and the real watchlist screen (its own toolbar and filter chrome
 * included) is what loads; the caller returns {@code true} so the Watchlist item shows selected.
 */
public final class WatchlistNav {

    private static final String PKG = "com.letterboxd.letterboxd";

    private WatchlistNav() {}

    public static void navigate(Activity activity) {
        try {
            Object ncObj = activity.getClass().getMethod("getNavController").invoke(activity);
            if (!(ncObj instanceof NavController)) return;
            NavController nc = (NavController) ncObj;

            String memberId = currentMemberId();
            if (memberId == null || memberId.isEmpty()) return;

            Object route = Class.forName(PKG + ".ui.navigation.Route$MemberWatchlist")
                    .getConstructor(String.class, String.class)
                    .newInstance(memberId, null);

            NavGraph graph = nc.getGraph();
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(graph.getStartDestinationId(), false, true)
                    .build();
            nc.navigate(route, options);
        } catch (Throwable ignored) {
        }
    }

    private static String currentMemberId() {
        try {
            Class<?> mgr = Class.forName(PKG + ".services.CurrentMemberManager");
            Object instance = mgr.getField("INSTANCE").get(null);
            Object id = mgr.getMethod("getMemberId").invoke(instance);
            return id instanceof String ? (String) id : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
