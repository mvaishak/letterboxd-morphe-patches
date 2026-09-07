package app.template.extension;

import android.app.Activity;

/**
 * Sends the tabs NavController to the member's watchlist — the same destination
 * ({@code Route.MemberWatchlist}) Letterboxd's own {@code handleAppShortcuts} navigates to for
 * {@code letterboxd://shortcut/watchlist}, but called directly and synchronously from the
 * bottom-nav tap rather than through the deep-link coroutine (which also flipped the selected
 * tab to Profile in a second async step, hence the earlier two-tap confusion).
 *
 * <p>The bottom bar stays put and the real watchlist screen — with its own toolbar and filter
 * chrome — is what loads; the caller returns {@code true} from the listener so the Watchlist
 * item shows as selected like any other tab.
 */
public final class WatchlistNav {

    private static final String PKG = "com.letterboxd.letterboxd";

    private WatchlistNav() {}

    public static void navigate(Activity activity) {
        try {
            Object navController = activity.getClass().getMethod("getNavController").invoke(activity);
            if (navController == null) return;

            String memberId = currentMemberId();
            if (memberId == null || memberId.isEmpty()) return;

            Object route = Class.forName(PKG + ".ui.navigation.Route$MemberWatchlist")
                    .getConstructor(String.class, String.class)
                    .newInstance(memberId, null);

            Class.forName("androidx.navigation.NavController")
                    .getMethod("navigate", Object.class)
                    .invoke(navController, route);
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
