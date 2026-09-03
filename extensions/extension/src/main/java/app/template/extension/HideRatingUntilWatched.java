package app.template.extension;

import android.view.View;
import android.view.ViewTreeObserver;

import androidx.fragment.app.Fragment;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * Merged into Letterboxd by the "Hide ratings until watched" patch.
 *
 * <p>{@code FilmRatingsHistogramFragment}'s root view is {@code @id/ratingsViewWrapper} — the whole
 * community-ratings section (average, histogram, endpoint stars). This keeps that view {@code GONE}
 * until {@code FilmViewModel.getMemberRelationship().getValue().getWatched()} reports the film as
 * watched, then stops interfering and lets the app control it.
 *
 * <p>The film-page relationship and the rating data load asynchronously in unpredictable order, and
 * a coroutine re-shows the section when rating data arrives, so enforcement is reactive: a global
 * layout listener re-checks every pass and re-hides while the film is unwatched.
 *
 * <p>Everything is reflection-based and exception-safe. Any failure ({@code ERROR}) detaches and
 * leaves the ratings visible — it fails open, never hiding ratings for a watched film.
 */
public final class HideRatingUntilWatched {

    private static final int WATCHED = 1;
    private static final int NOT_WATCHED = 0;
    private static final int UNKNOWN = -1;
    private static final int ERROR = -2;

    private static final WeakHashMap<View, Boolean> ATTACHED = new WeakHashMap<>();

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
                    int state;
                    try {
                        state = readWatchedState(fragment);
                    } catch (Throwable t) {
                        state = ERROR;
                    }
                    if (state == WATCHED || state == ERROR) {
                        detach(wrapper, self[0]);
                    } else if (wrapper.getVisibility() != View.GONE) {
                        wrapper.setVisibility(View.GONE);
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
