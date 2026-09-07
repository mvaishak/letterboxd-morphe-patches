package app.template.extension.settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;

/**
 * One-time "what's new / how to open Mod settings" dialog, shown once after a patch. Injected into
 * {@code MainActivity.onResume} (not {@code onCreate}) so it fires after the splash / login flow,
 * and it is only marked seen once the user actually dismisses it.
 */
public final class ModWelcome {

    private ModWelcome() {}

    /** Bump when {@link #BODY} changes so returning users see it once more. */
    private static final int BUILD = 3;
    private static final String KEY = "welcome_build";

    private static final String TITLE = "What's new";

    // One block per release. Keep it to what changed, short bullets, no wall of text — the full
    // feature list lives in the Mods screen itself. Bump BUILD above every time this changes.
    private static final String BODY =
            "Bottom navigation\n"
          + "•  Choose which tabs the bar shows, up to five\n"
          + "•  New Watchlist tab, with the usual filters and sort\n"
          + "•  Set which tab the app opens on at launch\n\n"
          + "Home tabs\n"
          + "•  Show, hide or reorder Films / Reviews / Lists / Journal\n"
          + "•  Home opens on whichever tab you put first\n\n"
          + "Film pages\n"
          + "•  Runtime can read 1h 47m instead of 107 mins\n\n"
          + "New patch: Hide ads\n"
          + "•  Stops the banner ads on free accounts from loading\n\n"
          + "Open the Mods screen by long-pressing the Letterboxd app "
          + "icon, or the settings gear on your profile tab.";

    private static volatile boolean shown = false;
    private static volatile boolean scheduled = false;

    /** Injected at the top of {@code MainActivity.onResume} (fires repeatedly — guarded). */
    public static void maybeShow(final Activity activity) {
        try {
            if (shown || scheduled || activity == null) return;
            Prefs.load(activity);
            if (String.valueOf(BUILD).equals(Prefs.getString(KEY, ""))) {
                shown = true;
                return;
            }

            scheduled = true;
            activity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (shown) return;
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            scheduled = false; // a later onResume (real MainActivity) retries
                            return;
                        }
                        shown = true;
                        ModDialog.show(activity, TITLE, format(activity, BODY), "Got it", null, null, null,
                                new Runnable() {
                                    @Override public void run() {
                                        Prefs.putString(KEY, String.valueOf(BUILD));
                                    }
                                });
                    } catch (Throwable t) {
                        scheduled = false;
                    }
                }
            }, 2200L);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Turns the plain {@link #BODY} into something readable: a line that has bullets under it is
     * a section heading (bold, white); bullet lines get a hanging indent so wrapped text lines up
     * under the first word instead of falling back to the margin. Keeps the source string simple
     * to edit each release — just section names, "• " bullets, and blank lines between.
     */
    private static CharSequence format(Context ctx, String raw) {
        try {
            float d = ctx.getResources().getDisplayMetrics().density;
            int hang = Math.round(16 * d);
            String[] lines = raw.split("\n", -1);
            SpannableStringBuilder sb = new SpannableStringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int start = sb.length();
                sb.append(line);
                if (i < lines.length - 1) sb.append('\n');
                int end = start + line.length();
                if (line.startsWith("•")) {
                    sb.setSpan(new LeadingMarginSpan.Standard(0, hang), start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (!line.isEmpty() && i + 1 < lines.length && lines[i + 1].startsWith("•")) {
                    sb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new ForegroundColorSpan(0xFFFFFFFF), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return sb;
        } catch (Throwable t) {
            return raw;
        }
    }
}
