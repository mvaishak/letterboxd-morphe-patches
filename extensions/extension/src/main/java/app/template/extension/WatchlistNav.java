package app.template.extension;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Opens the current member's watchlist as a self-contained full-screen screen laid over
 * everything, with its own back arrow. It hosts Letterboxd's own {@code WatchlistFilmsFragment}
 * (built the way {@code MeFragment} builds it) in a container added to {@code android.R.id.content}
 * and pushed onto the back stack, so system back / the arrow dismiss it.
 *
 * <p>Earlier this re-fired the {@code letterboxd://shortcut/watchlist} deep link, but that path
 * selects the Profile tab and then navigates a nested controller in two async steps — it left the
 * bottom bar and the shown content out of sync (first tap landed on Profile, a second was needed
 * for the watchlist). A plain fragment transaction has none of that coupling.
 */
public final class WatchlistNav {

    private static final String TAG = "morphe_watchlist_screen";
    private static final String PKG = "com.letterboxd.letterboxd";

    private WatchlistNav() {}

    public static void open(Activity activity) {
        try {
            if (!(activity instanceof FragmentActivity)) return;
            FragmentActivity host = (FragmentActivity) activity;
            final FragmentManager fm = host.getSupportFragmentManager();
            if (fm.isStateSaved() || fm.findFragmentByTag(TAG) != null) return;

            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null) return;

            String memberId = currentMemberId();
            if (memberId == null || memberId.isEmpty()) return;
            Fragment listFragment = buildWatchlistFragment(memberId);
            if (listFragment == null) return;

            int bg = themeColor(activity, android.R.attr.colorBackground, 0xFF14181C);
            float d = activity.getResources().getDisplayMetrics().density;

            final FrameLayout overlay = new FrameLayout(activity);
            overlay.setId(View.generateViewId());
            overlay.setClickable(true);
            overlay.setBackgroundColor(bg);

            LinearLayout column = new LinearLayout(activity);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setPadding(0, statusBarHeight(activity), 0, 0);
            overlay.addView(column, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            column.addView(toolbar(activity, d, new Runnable() {
                @Override public void run() {
                    if (!fm.isStateSaved()) fm.popBackStack();
                }
            }), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.round(56 * d)));

            FrameLayout container = new FrameLayout(activity);
            container.setId(View.generateViewId());
            column.addView(container, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            content.addView(overlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // Clean up the overlay once the fragment is really gone. A FragmentLifecycleCallbacks
            // (an abstract class) is used rather than OnBackStackChangedListener — the latter's
            // Java-8 default methods pull in a `$-CC` desugar class that isn't present in the
            // target APK's fragment library, which crashes with NoClassDefFoundError.
            final FragmentManager.FragmentLifecycleCallbacks[] cleanup =
                    new FragmentManager.FragmentLifecycleCallbacks[1];
            cleanup[0] = new FragmentManager.FragmentLifecycleCallbacks() {
                @Override public void onFragmentViewDestroyed(FragmentManager fmgr, Fragment f) {
                    if (!TAG.equals(f.getTag())) return;
                    if (fmgr.findFragmentByTag(TAG) != null) return; // config change, not a pop
                    ViewParent p = overlay.getParent();
                    if (p instanceof ViewGroup) ((ViewGroup) p).removeView(overlay);
                    fmgr.unregisterFragmentLifecycleCallbacks(cleanup[0]);
                }
            };
            fm.registerFragmentLifecycleCallbacks(cleanup[0], false);

            fm.beginTransaction()
                    .replace(container.getId(), listFragment, TAG)
                    .addToBackStack(TAG)
                    .commit();
        } catch (Throwable ignored) {
        }
    }

    // --- reflection into Letterboxd -------------------------------------

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

    /**
     * {@code WatchlistFilmsFragment.Companion.newInstance(false, memberId, false,
     * new WatchlistRequester(memberId))} — the same call {@code MeFragment} makes for its
     * watchlist tab.
     */
    private static Fragment buildWatchlistFragment(String memberId) {
        try {
            Class<?> requesterType = Class.forName(PKG + ".api.requester.Requester");
            Constructor<?> ctor = Class.forName(PKG + ".api.requester.WatchlistRequester")
                    .getConstructor(String.class);
            Object requester = ctor.newInstance(memberId);

            Class<?> fragmentType = Class.forName(PKG + ".ui.fragments.members.WatchlistFilmsFragment");
            Object companion = fragmentType.getField("Companion").get(null);
            Method newInstance = companion.getClass().getMethod(
                    "newInstance", boolean.class, String.class, boolean.class, requesterType);
            Object frag = newInstance.invoke(companion, false, memberId, false, requester);
            return frag instanceof Fragment ? (Fragment) frag : null;
        } catch (Throwable t) {
            return null;
        }
    }

    // --- chrome -------------------------------------------------------

    private static View toolbar(Activity ctx, float d, final Runnable onBack) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Math.round(6 * d), 0, Math.round(16 * d), 0);

        TextView back = new TextView(ctx);
        back.setText("←");
        back.setTextColor(0xFFFFFFFF);
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
        back.setGravity(Gravity.CENTER);
        int hit = Math.round(44 * d);
        back.setWidth(hit);
        back.setHeight(hit);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onBack.run(); }
        });
        bar.addView(back);

        TextView title = new TextView(ctx);
        title.setText("Watchlist");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Math.round(8 * d);
        bar.addView(title, lp);
        return bar;
    }

    private static int themeColor(Activity ctx, int attr, int fallback) {
        try {
            TypedValue tv = new TypedValue();
            if (ctx.getTheme().resolveAttribute(attr, tv, true) && tv.data != 0) return tv.data;
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static int statusBarHeight(Activity ctx) {
        try {
            int id = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) return ctx.getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {
        }
        return Math.round(24 * ctx.getResources().getDisplayMetrics().density);
    }
}
