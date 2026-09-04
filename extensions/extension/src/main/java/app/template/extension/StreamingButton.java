package app.template.extension;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import app.template.extension.settings.AccentPresets;
import app.template.extension.settings.Prefs;

import java.lang.reflect.Method;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Open in player" — adds a button next to Trailer on a film's page that opens the film in
 * Stremio or Nuvio, matching {@code trailer_button}'s Material3 icon-button style but tinted with
 * the current accent colour. Injected at the top of {@code FilmHeaderFragment.onViewCreated}.
 *
 * <p>The IMDb id comes from {@code FilmViewModel.getFilm()} (a {@code StateFlow<Film>}) — the
 * same model Letterboxd's own "View on IMDb" links use — via {@code Film.getLinks()}, all by
 * reflection since none of it is a stable public API. Film data loads asynchronously, so this is
 * reactive like "Hide ratings until watched": a layout listener retries until the film is loaded
 * (or errors), then gives up either way — no button appears if the title has no IMDb link.
 *
 * <p>Stremio's deep link ({@code stremio:///detail/movie/<imdbId>/<imdbId>}) is documented and
 * confirmed. Nuvio's is not publicly documented; this opens the film's IMDb page instead and lets
 * Nuvio claim it if it is registered as a link handler on the device — best effort until a real
 * scheme is confirmed.
 */
public final class StreamingButton {

    private static final String TAG = "morphe_streaming_button";
    private static final Pattern IMDB_ID = Pattern.compile("(tt\\d+)");

    private static final WeakHashMap<View, Boolean> ATTACHED = new WeakHashMap<>();

    private static Method mGetViewModel;
    private static Method mGetFilm;
    private static Method mGetValue;
    private static Method mGetLinks;
    private static Method mGetType;
    private static Method mGetUrl;

    private StreamingButton() {}

    public static void enforce(final Fragment fragment) {
        try {
            final String app = Prefs.streamingApp();
            if (app == null || "off".equals(app)) return;

            final View wrapper = fragment.getView();
            if (wrapper == null || Boolean.TRUE.equals(ATTACHED.get(wrapper))) return;
            ATTACHED.put(wrapper, Boolean.TRUE);

            final ViewTreeObserver.OnGlobalLayoutListener[] self =
                    new ViewTreeObserver.OnGlobalLayoutListener[1];
            self[0] = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    View trailer = byId(wrapper, "trailer_button");
                    if (trailer == null || !(trailer.getParent() instanceof ViewGroup)) return;
                    ViewGroup row = (ViewGroup) trailer.getParent();
                    if (row.findViewWithTag(TAG) != null) {
                        detach(wrapper, self[0]);
                        return;
                    }

                    Object film;
                    try {
                        film = readFilm(fragment);
                    } catch (Throwable t) {
                        detach(wrapper, self[0]);
                        return;
                    }
                    if (film == null) return; // still loading — try again next layout pass

                    String imdbId = findImdbId(film);
                    if (imdbId != null) addButton(row, trailer, imdbId, app);
                    detach(wrapper, self[0]); // film resolved either way — nothing more to wait for
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
    }

    // --- film / IMDb id -----------------------------------------------------

    private static Object readFilm(Fragment fragment) throws Exception {
        Class<?> fragClass = fragment.getClass();
        if (mGetViewModel == null) mGetViewModel = fragClass.getMethod("access$getViewModel", fragClass);
        Object viewModel = mGetViewModel.invoke(null, fragment);
        if (viewModel == null) return null;

        if (mGetFilm == null) mGetFilm = viewModel.getClass().getMethod("getFilm");
        Object stateFlow = mGetFilm.invoke(viewModel);
        if (stateFlow == null) return null;

        if (mGetValue == null) mGetValue = stateFlow.getClass().getMethod("getValue");
        return mGetValue.invoke(stateFlow);
    }

    private static String findImdbId(Object film) {
        try {
            if (mGetLinks == null) mGetLinks = film.getClass().getMethod("getLinks");
            Object linksObj = mGetLinks.invoke(film);
            if (!(linksObj instanceof List)) return null;

            for (Object link : (List<?>) linksObj) {
                if (mGetType == null) mGetType = link.getClass().getMethod("getType");
                Object type = mGetType.invoke(link);
                if (type == null || !"Imdb".equals(type.getClass().getSimpleName())) continue;

                if (mGetUrl == null) mGetUrl = link.getClass().getMethod("getUrl");
                Object url = mGetUrl.invoke(link);
                if (url == null) continue;
                Matcher m = IMDB_ID.matcher(url.toString());
                if (m.find()) return m.group(1);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // --- button --------------------------------------------------------------

    private static void addButton(ViewGroup row, View trailer, final String imdbId, final String app) {
        try {
            android.content.Context ctx = row.getContext();
            int accent = AccentPresets.previewColor(
                    Prefs.getString(Prefs.KEY_THEME_ACCENT, "green"),
                    Prefs.getString(Prefs.KEY_THEME_ACCENT_HEX, ""));
            int onAccent = AccentPresets.isLight(accent) ? 0xFF141414 : 0xFFFFFFFF;
            float density = ctx.getResources().getDisplayMetrics().density;

            // MaterialButton has no 4-arg (Context, AttributeSet, defStyleAttr, defStyleRes)
            // constructor to force the exact "Widget.Material3.Button.Icon" style by resource id,
            // but its plain constructor already resolves the app's own default filled-button style
            // via the `materialButtonStyle` theme attribute — close enough, and icon gravity /
            // insets below make it read the same as trailer_button (icon-then-text, no extra inset).
            MaterialButton button = new MaterialButton(ctx);
            button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            button.setInsetTop(0);
            button.setInsetBottom(0);

            button.setTag(TAG);
            button.setText("nuvio".equals(app) ? "Nuvio" : "Stremio");
            button.setAllCaps(true);
            button.setIncludeFontPadding(false);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            button.setLetterSpacing(0.1f);
            button.setTypeface(AppFont.regular(ctx));
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            int padStart = Math.round(14f * density), padEnd = Math.round(16f * density);
            button.setPaddingRelative(padStart, button.getPaddingTop(), padEnd, button.getPaddingBottom());

            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(onAccent);
            button.setIconTint(ColorStateList.valueOf(onAccent));
            int iconSize = Math.round(14f * density);
            button.setIcon(new PlayGlyph(iconSize, onAccent));
            button.setIconSize(iconSize);

            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    launch(v, app, imdbId);
                }
            });

            int marginEnd = dimenOrDefault(row.getContext(), "activity_horizontal_margin", 16f, density);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(marginEnd);
            row.addView(button, row.indexOfChild(trailer) + 1, lp);
        } catch (Throwable ignored) {
        }
    }

    private static void launch(View v, String app, String imdbId) {
        try {
            Uri uri = "stremio".equals(app)
                    ? Uri.parse("stremio:///detail/movie/" + imdbId + "/" + imdbId)
                    // Nuvio's own scheme is unconfirmed; fall back to the film's IMDb page and let
                    // Nuvio claim it if it's registered as a link handler.
                    : Uri.parse("https://www.imdb.com/title/" + imdbId + "/");
            v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
        } catch (Throwable ignored) {
        }
    }

    private static int dimenOrDefault(android.content.Context ctx, String name, float fallbackDp, float density) {
        try {
            int id = ctx.getResources().getIdentifier(name, "dimen", ctx.getPackageName());
            if (id != 0) return ctx.getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {
        }
        return Math.round(fallbackDp * density);
    }

    private static View byId(View root, String name) {
        try {
            int id = root.getResources().getIdentifier(name, "id", root.getContext().getPackageName());
            return id == 0 ? null : root.findViewById(id);
        } catch (Throwable t) {
            return null;
        }
    }

    /** A simple filled play triangle — trailer_button has its own icon; this echoes its shape. */
    private static final class PlayGlyph extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final int size;

        PlayGlyph(int size, int color) {
            this.size = size;
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            float w = b.width(), h = b.height();
            path.reset();
            path.moveTo(b.left + w * 0.22f, b.top + h * 0.12f);
            path.lineTo(b.left + w * 0.22f, b.top + h * 0.88f);
            path.lineTo(b.left + w * 0.86f, b.top + h * 0.5f);
            path.close();
            canvas.drawPath(path, paint);
        }

        @Override public int getIntrinsicWidth() { return size; }
        @Override public int getIntrinsicHeight() { return size; }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { paint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
