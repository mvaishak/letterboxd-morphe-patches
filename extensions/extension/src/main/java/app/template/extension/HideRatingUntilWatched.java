package app.template.extension;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.util.TypedValue;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * Merged into Letterboxd by the "Hide ratings until watched" patch.
 *
 * <p>{@code FilmRatingsHistogramFragment}'s root view is {@code @id/ratingsViewWrapper} — the whole
 * community-ratings section. Inside it, the endpoint stars, histogram, average and {@code RatingView}
 * all live in one unnamed horizontal row (the parent of {@code @id/ratingsView}). While the film is
 * unwatched, that row and the friends/global toggle are hidden and a "Tap to show ratings" control
 * is added below the section title. Tapping it reveals the ratings for the current visit; leaving
 * and returning to the film hides them again.
 *
 * <p>The relationship and the rating data load asynchronously in unpredictable order, and a
 * coroutine re-shows the row when rating data arrives, so enforcement is reactive: a global layout
 * listener re-checks every pass and re-hides while the film is unwatched.
 *
 * <p>Everything is reflection-based and exception-safe. Any failure ({@code ERROR}) restores the
 * ratings and detaches — it fails open, never hiding ratings for a watched film.
 */
public final class HideRatingUntilWatched {

    private static final int WATCHED = 1;
    private static final int NOT_WATCHED = 0;
    private static final int UNKNOWN = -1;
    private static final int ERROR = -2;

    private static final String PLACEHOLDER_TAG = "morphe_reveal_ratings";

    private static final WeakHashMap<View, Boolean> ATTACHED = new WeakHashMap<>();
    private static final WeakHashMap<View, Boolean> REVEALED = new WeakHashMap<>();

    private static Method mGetModel;
    private static Method mGetRelationship;
    private static Method mGetValue;
    private static Method mGetWatched;

    private HideRatingUntilWatched() {}

    /** Injected at the top of {@code FilmRatingsHistogramFragment.onViewCreated}. */
    public static void enforce(final Fragment fragment) {
        try {
            final View wrapper = fragment.getView();
            if (wrapper == null || Boolean.TRUE.equals(ATTACHED.get(wrapper))) return;
            ATTACHED.put(wrapper, Boolean.TRUE);

            final ViewTreeObserver.OnGlobalLayoutListener[] self =
                    new ViewTreeObserver.OnGlobalLayoutListener[1];
            self[0] = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Boolean.TRUE.equals(REVEALED.get(wrapper))) {
                        detach(wrapper, self[0]);
                        return;
                    }
                    int state;
                    try {
                        state = readWatchedState(fragment);
                    } catch (Throwable t) {
                        state = ERROR;
                    }
                    if (state == WATCHED || state == ERROR) {
                        restore(wrapper);
                        detach(wrapper, self[0]);
                    } else {
                        mask(wrapper);
                    }
                }
            };
            wrapper.getViewTreeObserver().addOnGlobalLayoutListener(self[0]);
            self[0].onGlobalLayout();
        } catch (Throwable ignored) {
        }
    }

    private static void detach(View v, ViewTreeObserver.OnGlobalLayoutListener l) {
        try {
            ViewTreeObserver vto = v.getViewTreeObserver();
            if (vto.isAlive()) vto.removeOnGlobalLayoutListener(l);
        } catch (Throwable ignored) {
        }
        ATTACHED.remove(v);
    }

    // --- masking -------------------------------------------------------------

    private static void mask(View wrapper) {
        View row = ratingRow(wrapper);
        if (row != null && row.getVisibility() != View.GONE) row.setVisibility(View.GONE);

        View toggle = byId(wrapper, "ratingsToggleIcon");
        if (toggle != null && toggle.getVisibility() != View.GONE) toggle.setVisibility(View.GONE);

        ensurePlaceholder(wrapper);
    }

    private static void restore(final View wrapper) {
        View row = ratingRow(wrapper);
        if (row != null && row.getVisibility() != View.VISIBLE) row.setVisibility(View.VISIBLE);

        View toggle = byId(wrapper, "ratingsToggleIcon");
        if (toggle != null && toggle.getVisibility() != View.VISIBLE) toggle.setVisibility(View.VISIBLE);

        View section = sectionOf(wrapper);
        if (section instanceof ViewGroup) {
            View ph = ((ViewGroup) section).findViewWithTag(PLACEHOLDER_TAG);
            if (ph != null) ((ViewGroup) section).removeView(ph);
        }
    }

    private static void ensurePlaceholder(final View wrapper) {
        View section = sectionOf(wrapper);
        if (!(section instanceof RelativeLayout)) return;
        RelativeLayout parent = (RelativeLayout) section;
        if (parent.findViewWithTag(PLACEHOLDER_TAG) != null) return;

        final TextView tv = new TextView(parent.getContext());
        tv.setTag(PLACEHOLDER_TAG);
        tv.setText("Tap to show ratings");
        tv.setAllCaps(false);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTextColor(0xFF9AA7B0);
        int padV = Math.round(10f * parent.getResources().getDisplayMetrics().density);
        tv.setPadding(0, padV, 0, padV);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        View title = byId(wrapper, "sectionTitle");
        if (title != null && title.getId() != View.NO_ID) {
            lp.addRule(RelativeLayout.BELOW, title.getId());
        }
        tv.setLayoutParams(lp);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                REVEALED.put(wrapper, Boolean.TRUE);
                restore(wrapper);
            }
        });
        parent.addView(tv);
    }

    /** The unnamed horizontal row holding the endpoint stars, histogram, average and RatingView. */
    private static View ratingRow(View wrapper) {
        View graph = byId(wrapper, "ratingsView");
        if (graph == null) return null;
        ViewParent p = graph.getParent();
        return (p instanceof View) ? (View) p : null;
    }

    /** The RelativeLayout holding the section title, toggle icon and the rating row. */
    private static View sectionOf(View wrapper) {
        View row = ratingRow(wrapper);
        if (row == null) return null;
        ViewParent p = row.getParent();
        return (p instanceof View) ? (View) p : null;
    }

    private static View byId(View root, String name) {
        try {
            int id = root.getResources().getIdentifier(name, "id", root.getContext().getPackageName());
            return id == 0 ? null : root.findViewById(id);
        } catch (Throwable t) {
            return null;
        }
    }

    // --- watched state -----------------------------------------------------

    private static int readWatchedState(Fragment fragment) throws Exception {
        if (mGetModel == null) {
            mGetModel = fragment.getClass().getDeclaredMethod("getModel");
            mGetModel.setAccessible(true);
        }
        Object vm = mGetModel.invoke(fragment);
        if (vm == null) return UNKNOWN;

        if (mGetRelationship == null) mGetRelationship = vm.getClass().getMethod("getMemberRelationship");
        Object stateFlow = mGetRelationship.invoke(vm);
        if (stateFlow == null) return UNKNOWN;

        if (mGetValue == null) mGetValue = stateFlow.getClass().getMethod("getValue");
        Object relationship = mGetValue.invoke(stateFlow);
        if (relationship == null) return UNKNOWN;

        if (mGetWatched == null) mGetWatched = relationship.getClass().getMethod("getWatched");
        Object watched = mGetWatched.invoke(relationship);
        return (watched instanceof Boolean && (Boolean) watched) ? WATCHED : NOT_WATCHED;
    }
}
