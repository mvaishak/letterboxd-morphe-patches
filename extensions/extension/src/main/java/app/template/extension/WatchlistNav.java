package app.template.extension;

import android.app.Activity;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Opens the member's watchlist ({@code Route.MemberWatchlist}).
 *
 * <p>That destination lives inside the Member (profile) tab's nav graph — it is not reachable
 * from the Popular tab's controller, and navigating to it cross-graph wedged the back stack
 * (leaving it took a system back and several seconds). So this mirrors what Letterboxd's own
 * {@code handleAppShortcuts} does for {@code letterboxd://shortcut/watchlist}: select the Profile
 * tab through the bottom bar (a normal, fast tab switch), then on the next frame — once that
 * tab's NavController is the active one — navigate to the watchlist within it. Leaving is then a
 * normal tab switch.
 */
public final class WatchlistNav {

    private static final String PKG = "com.letterboxd.letterboxd";

    private WatchlistNav() {}

    public static void open(final Activity activity) {
        try {
            final BottomNavigationView bar = findBar(activity);
            int profileId = activity.getResources().getIdentifier(
                    "nav_profile", "id", activity.getPackageName());
            final String memberId = currentMemberId();
            if (bar == null || profileId == 0 || memberId == null || memberId.isEmpty()) return;

            bar.setSelectedItemId(profileId);
            bar.post(new Runnable() {
                @Override public void run() {
                    try {
                        Object nc = activity.getClass().getMethod("getNavController").invoke(activity);
                        if (nc == null) return;
                        Object route = Class.forName(PKG + ".ui.navigation.Route$MemberWatchlist")
                                .getConstructor(String.class, String.class)
                                .newInstance(memberId, null);
                        nc.getClass().getMethod("navigate", Object.class).invoke(nc, route);
                    } catch (Throwable t) {
                        Log.d("MorpheNav", "watchlist navigate error", t);
                    }
                }
            });
        } catch (Throwable t) {
            Log.d("MorpheNav", "watchlist open error", t);
        }
    }

    static BottomNavigationView findBar(Activity activity) {
        try {
            int id = activity.getResources().getIdentifier(
                    "bottom_navigation", "id", activity.getPackageName());
            android.view.View v = id == 0 ? null : activity.findViewById(id);
            return v instanceof BottomNavigationView ? (BottomNavigationView) v : null;
        } catch (Throwable t) {
            return null;
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
