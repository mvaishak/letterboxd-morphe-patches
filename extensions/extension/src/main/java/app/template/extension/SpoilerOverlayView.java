package app.template.extension;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.Random;

/**
 * An opaque cover drawn over the ratings row while a film is unwatched, for the
 * "Hide ratings until watched" patch. Tapping it runs a short reveal animation and
 * then calls {@link OnReveal}, which removes the overlay and restores the ratings.
 *
 * <p>Three looks, chosen at patch time:
 * <ul>
 *   <li>{@link #PANEL} — a flat panel with an eye glyph and a "Tap to reveal" label.</li>
 *   <li>{@link #BURST} — a static speckle of particles over the panel; they fly outward on tap.</li>
 *   <li>{@link #SHIMMER} — the particles drift and twinkle continuously until tapped.</li>
 * </ul>
 *
 * <p>Every mode paints a fully opaque backing first, so the rating underneath never
 * shows through. Only {@link #SHIMMER} runs an animation loop while idle, and it stops
 * as soon as the view is detached or the reveal finishes.
 */
final class SpoilerOverlayView extends View {

    interface OnReveal {
        void onReveal();
    }

    static final int PANEL = 0;
    static final int SHIMMER = 1;
    static final int BURST = 2;

    private static final long REVEAL_MS = 360L;

    private final int mode;
    private OnReveal listener;

    private final Paint backing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Random rnd = new Random();
    private final float density;

    private float[] px, py, phase;
    private int[] baseAlpha;
    private int count;

    private boolean revealing;
    private boolean dead;
    private float revealT;
    private ValueAnimator animator;

    SpoilerOverlayView(Context c, int mode) {
        super(c);
        this.mode = mode;
        this.density = c.getResources().getDisplayMetrics().density;
        setClickable(true);

        backing.setColor(pickSurface(c));

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1.5f));
        stroke.setColor(0xFFAEB8C2);

        dots.setColor(0xFFB8C2CC);
        dots.setStrokeCap(Paint.Cap.ROUND);
        dots.setStrokeWidth(dp(1.7f));

        label.setColor(0xFFE0E4E8);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(dp(mode == PANEL ? 13f : 12f));
        label.setTypeface(AppFont.semibold(c));
        label.setLetterSpacing(0.01f);
    }

    void setOnRevealListener(OnReveal l) {
        this.listener = l;
    }

    // --- sizing ----------------------------------------------------------------

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (mode == PANEL || w <= 0 || h <= 0) {
            count = 0;
            return;
        }
        float cell = dp(9f);
        count = clamp(Math.round((w * h) / (cell * cell)), 48, 900);
        px = new float[count];
        py = new float[count];
        phase = new float[count];
        baseAlpha = new int[count];
        for (int i = 0; i < count; i++) {
            px[i] = rnd.nextFloat() * w;
            py[i] = rnd.nextFloat() * h;
            phase[i] = rnd.nextFloat() * (float) (Math.PI * 2.0);
            baseAlpha[i] = 120 + rnd.nextInt(136);
        }
        if (mode == SHIMMER) postInvalidateOnAnimation();
    }

    // --- drawing -------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float visible = 1f - revealT;

        // A full rectangle, not a rounded rect: rounded corners would leave the
        // endpoint star and the RatingView stars (which sit in the row's bottom
        // corners) poking through. Coverage first.
        backing.setAlpha((int) (255 * visible));
        canvas.drawRect(0f, 0f, w, h, backing);

        if (mode == PANEL) {
            drawEye(canvas, w, h, visible);
        } else {
            drawParticles(canvas, w, h);
        }

        drawLabel(canvas, w, h, visible);

        if (mode == SHIMMER && !revealing && !dead && isAttachedToWindow()) {
            stepShimmer(w, h);
            postInvalidateOnAnimation();
        }
    }

    private void drawEye(Canvas canvas, int w, int h, float visible) {
        int a = (int) (255 * visible);
        stroke.setAlpha(a);
        dots.setAlpha(a);
        float cx = w / 2f;
        float cy = h * 0.40f;
        float ew = Math.min(w, h) * 0.16f;
        float eh = ew * 0.62f;
        canvas.drawOval(cx - ew, cy - eh, cx + ew, cy + eh, stroke);
        canvas.drawCircle(cx, cy, eh * 0.55f, dots);
    }

    private void drawParticles(Canvas canvas, int w, int h) {
        if (count == 0 || px == null) return;
        float cx = w / 2f;
        float cy = h / 2f;
        for (int i = 0; i < count; i++) {
            float x = px[i];
            float y = py[i];
            int alpha = baseAlpha[i];
            if (revealing) {
                float push = revealT * revealT * 2.4f;
                x = px[i] + (px[i] - cx) * push;
                y = py[i] + (py[i] - cy) * push;
                alpha = (int) (alpha * (1f - revealT));
            } else if (mode == SHIMMER) {
                alpha = (int) (alpha * (0.55f + 0.45f * (float) Math.sin(phase[i])));
            }
            dots.setAlpha(clamp(alpha, 0, 255));
            canvas.drawPoint(x, y, dots);
        }
    }

    private void drawLabel(Canvas canvas, int w, int h, float visible) {
        label.setAlpha((int) (255 * visible));
        float baseline = mode == PANEL ? h * 0.78f : h / 2f + label.getTextSize() * 0.36f;
        canvas.drawText("Tap to reveal", w / 2f, baseline, label);
    }

    private void stepShimmer(int w, int h) {
        for (int i = 0; i < count; i++) {
            px[i] += (rnd.nextFloat() - 0.5f) * 0.9f;
            py[i] += (rnd.nextFloat() - 0.5f) * 0.9f;
            if (px[i] < 0) px[i] += w; else if (px[i] > w) px[i] -= w;
            if (py[i] < 0) py[i] += h; else if (py[i] > h) py[i] -= h;
            phase[i] += 0.18f;
        }
    }

    // --- reveal ------------------------------------------------------------

    @Override
    public boolean performClick() {
        super.performClick();
        startReveal();
        return true;
    }

    private void startReveal() {
        if (revealing || dead) return;
        revealing = true;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(REVEAL_MS);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                revealT = (float) a.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                if (listener != null) listener.onReveal();
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        dead = true;
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    // --- helpers ---------------------------------------------------------

    private int pickSurface(Context c) {
        String[] names = {"morphe_my_surface_elevated", "gray202830", "gray1C242C", "gray283038"};
        for (String n : names) {
            try {
                int id = c.getResources().getIdentifier(n, "color", c.getPackageName());
                if (id != 0) return c.getResources().getColor(id, c.getTheme());
            } catch (Throwable ignored) {
            }
        }
        return 0xFF1C1C1C;
    }

    private float dp(float v) {
        return v * density;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
