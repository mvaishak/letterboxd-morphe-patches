package app.template.extension;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Random;

/**
 * An opaque cover drawn over the ratings row while a film is unwatched, for the
 * "Hide ratings until watched" patch. Tapping it runs a short reveal animation and
 * then calls {@link OnReveal}, which removes the overlay and restores the ratings.
 *
 * <p>Five looks, chosen at patch time or from Mod settings:
 * <ul>
 *   <li>{@link #PANEL} — a flat panel with an eye glyph; fades out on tap.</li>
 *   <li>{@link #BURST} — a static speckle of particles over the panel; they fly outward on tap.</li>
 *   <li>{@link #SHIMMER} — the particles drift and twinkle continuously until tapped.</li>
 *   <li>{@link #CRUMBLE} — a flat panel like {@link #PANEL}, but it dissolves in a staggered grid
 *       of shrinking blocks on tap instead of a plain fade.</li>
 *   <li>{@link #CONFETTI} — like {@link #BURST}, but the speckle is coloured, rotated rectangles
 *       tinted from the current accent colour, and they tumble outward with a slight fall on tap.</li>
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
    static final int CRUMBLE = 3;
    static final int CONFETTI = 4;

    private static final long REVEAL_MS = 360L;
    private static final long CRUMBLE_REVEAL_MS = 520L;
    private static final long CONFETTI_REVEAL_MS = 620L;

    private final int mode;
    private final int accent;
    private OnReveal listener;

    private final Paint backing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piece = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pieceRect = new RectF();

    private final Random rnd = new Random();
    private final float density;

    // Particle field: BURST, SHIMMER, CONFETTI.
    private float[] px, py, phase;
    private int[] baseAlpha;
    private int count;
    // CONFETTI only: per-piece colour, size and spin.
    private int[] pieceColor;
    private float[] pieceSize, pieceRotation, pieceSpin;
    // CRUMBLE only: a grid of blocks, each with its own reveal delay.
    private int cols, rows;
    private float blockW, blockH;
    private float[] blockDelay;

    private boolean revealing;
    private boolean dead;
    private float revealT;
    private ValueAnimator animator;

    SpoilerOverlayView(Context c, int mode, int accentArgb) {
        super(c);
        this.mode = mode;
        this.accent = 0xFF000000 | accentArgb;
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
        label.setTextSize(dp(mode == PANEL || mode == CRUMBLE ? 13f : 12f));
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
        if (w <= 0 || h <= 0) return;

        if (mode == PANEL) {
            count = 0;
        } else if (mode == CRUMBLE) {
            count = 0;
            setUpBlocks(w, h);
        } else {
            setUpParticles(w, h);
        }
    }

    private void setUpParticles(int w, int h) {
        float cell = dp(9f);
        count = clamp(Math.round((w * h) / (cell * cell)), 48, 900);
        px = new float[count];
        py = new float[count];
        phase = new float[count];
        baseAlpha = new int[count];
        if (mode == CONFETTI) {
            pieceColor = new int[count];
            pieceSize = new float[count];
            pieceRotation = new float[count];
            pieceSpin = new float[count];
            int[] palette = confettiPalette();
            for (int i = 0; i < count; i++) {
                pieceColor[i] = palette[rnd.nextInt(palette.length)];
                pieceSize[i] = dp(2.2f + rnd.nextFloat() * 2.4f);
                pieceRotation[i] = rnd.nextFloat() * 360f;
                pieceSpin[i] = (rnd.nextFloat() - 0.5f) * 10f;
            }
        }
        for (int i = 0; i < count; i++) {
            px[i] = rnd.nextFloat() * w;
            py[i] = rnd.nextFloat() * h;
            phase[i] = rnd.nextFloat() * (float) (Math.PI * 2.0);
            baseAlpha[i] = 120 + rnd.nextInt(136);
        }
        if (mode == SHIMMER) postInvalidateOnAnimation();
    }

    private void setUpBlocks(int w, int h) {
        float cell = dp(16f);
        cols = clamp(Math.round(w / cell), 4, 40);
        rows = clamp(Math.round(h / cell), 3, 30);
        blockW = w / (float) cols;
        blockH = h / (float) rows;
        blockDelay = new float[cols * rows];
        for (int i = 0; i < blockDelay.length; i++) {
            // Spread each block's local reveal window across the first 60% of the animation,
            // so by revealT == 1 every block has had time to finish its own fade + shrink.
            blockDelay[i] = rnd.nextFloat() * 0.6f;
        }
    }

    // --- drawing -------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float visible = 1f - revealT;

        if (mode == CRUMBLE && revealing) {
            drawCrumble(canvas, w, h);
        } else {
            // A full rectangle, not a rounded rect: rounded corners would leave the endpoint
            // star and the RatingView stars (which sit in the row's bottom corners) poking
            // through. Coverage first.
            backing.setAlpha((int) (255 * visible));
            canvas.drawRect(0f, 0f, w, h, backing);

            if (mode == PANEL || mode == CRUMBLE) {
                drawEye(canvas, w, h, visible);
            } else if (mode == CONFETTI) {
                drawConfetti(canvas, w, h);
                drawLabel(canvas, w, h, visible);
            } else {
                drawParticles(canvas, w, h);
                // BURST has no icon to anchor on, so it keeps the caption; PANEL/CRUMBLE's eye
                // glyph and SHIMMER's animated field are self-explanatory enough without one.
                if (mode == BURST) drawLabel(canvas, w, h, visible);
            }
        }

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

    private void drawConfetti(Canvas canvas, int w, int h) {
        if (count == 0 || px == null) return;
        float cx = w / 2f;
        float cy = h / 2f;
        for (int i = 0; i < count; i++) {
            float x = px[i];
            float y = py[i];
            float rot = pieceRotation[i];
            int alpha = baseAlpha[i];
            if (revealing) {
                float push = revealT * revealT * 2.8f;
                float fall = revealT * revealT * dp(28f);
                x = px[i] + (px[i] - cx) * push;
                y = py[i] + (py[i] - cy) * push + fall;
                rot += pieceSpin[i] * revealT * 36f;
                alpha = (int) (alpha * (1f - revealT));
            }
            piece.setColor(pieceColor[i]);
            piece.setAlpha(clamp(alpha, 0, 255));
            float s = pieceSize[i];
            canvas.save();
            canvas.rotate(rot, x, y);
            pieceRect.set(x - s, y - s * 0.6f, x + s, y + s * 0.6f);
            canvas.drawRect(pieceRect, piece);
            canvas.restore();
        }
    }

    /** Each block shrinks toward its own centre and fades as its local progress advances. */
    private void drawCrumble(Canvas canvas, int w, int h) {
        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < cols; col++) {
                int i = r * cols + col;
                float local = clamp01((revealT - blockDelay[i]) / 0.4f);
                if (local >= 1f) continue; // fully gone
                float left = col * blockW, top = r * blockH;
                float shrink = local * 0.5f;
                float insetX = blockW * shrink * 0.5f, insetY = blockH * shrink * 0.5f;
                backing.setAlpha((int) (255 * (1f - local)));
                canvas.drawRect(left + insetX, top + insetY,
                        left + blockW - insetX, top + blockH - insetY, backing);
            }
        }
        drawEye(canvas, w, h, 1f - revealT);
    }

    private void drawLabel(Canvas canvas, int w, int h, float visible) {
        label.setAlpha((int) (255 * visible));
        float baseline = (mode == PANEL || mode == CRUMBLE) ? h * 0.78f
                : h / 2f + label.getTextSize() * 0.36f;
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

    /** Black/white plus two accent tones — confetti reads as festive without clashing. */
    private int[] confettiPalette() {
        int light = mix(accent, 0xFFFFFFFF, 0.5f);
        int dark = mix(accent, 0xFF000000, 0.35f);
        return new int[]{accent, light, dark, 0xFFFFFFFF, 0xFF1A1A1A};
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
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
        long duration = mode == CRUMBLE ? CRUMBLE_REVEAL_MS
                : mode == CONFETTI ? CONFETTI_REVEAL_MS : REVEAL_MS;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
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

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
