package app.template.extension;

import android.app.Activity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Opens (and closes) the member's watchlist for the synthetic Watchlist bottom-nav item.
 *
 * <p>{@code Route.MemberWatchlist} lives inside the Member (profile) tab's nav graph — it isn't a
 * peer of the five tab destinations. Navigating to it cross-graph wedged the back stack, so this
 * mirrors Letterboxd's own {@code handleAppShortcuts}: select the Profile tab through the bottom
 * bar (a normal, fast tab switch that brings its NavController to the front), then on the next
 * frame navigate to the watchlist inside it. {@link #showing} tracks that state; when the user
 * taps any other bottom-nav item, {@link NavItems#onMenuSelected} calls {@link #dismiss} first so
 * that tab switch lands clean in a single tap.
 */
public final class WatchlistNav {

    private static final String PKG = "com.letterboxd.letterboxd";

    /** True while the watchlist has been layered on the Profile tab by {@link #open}. */
    public static volatile boolean showing = false;

    private WatchlistNav() {}

    public static void open(final Activity activity) {
        try {
            if (showing) return;
            final BottomNavigationView bar = findBar(activity);
            int profileId = activity.getResources().getIdentifier(
                    "nav_profile", "id", activity.getPackageName());
            final String memberId = currentMemberId();
            if (bar == null || profileId == 0 || memberId == null || memberId.isEmpty()) return;

            bar.setSelectedItemId(profileId);
            bar.post(new Runnable() {
                @Override public void run() {
                    try {
                        Object nc = navController(activity);
                        if (nc == null) return;
                        Object route = Class.forName(PKG + ".ui.navigation.Route$MemberWatchlist")
                                .getConstructor(String.class, String.class)
                                .newInstance(memberId, null);
                        nc.getClass().getMethod("navigate", Object.class).invoke(nc, route);
                        showing = true;
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** Pop the watchlist back off the Profile tab so a pending tab switch starts from a clean root. */
    public static void dismiss(Activity activity) {
        showing = false;
        try {
            Object nc = navController(activity);
            if (nc != null) nc.getClass().getMethod("popBackStack").invoke(nc);
        } catch (Throwable ignored) {
        }
    }

    public static BottomNavigationView findBar(Activity activity) {
        try {
            int id = activity.getResources().getIdentifier(
                    "bottom_navigation", "id", activity.getPackageName());
            android.view.View v = id == 0 ? null : activity.findViewById(id);
            return v instanceof BottomNavigationView ? (BottomNavigationView) v : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object navController(Activity activity) throws Exception {
        return activity.getClass().getMethod("getNavController").invoke(activity);
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
