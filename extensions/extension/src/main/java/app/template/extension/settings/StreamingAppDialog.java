package app.template.extension.settings;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/** "Open in player" chooser — which app the film-page button launches, if any. */
final class StreamingAppDialog extends Dialog {

    interface OnPick {
        void onPick(String value);
    }

    private static final String[] LABELS = { "Off", "Stremio", "Nuvio" };
    private static final String[] VALUES = { "off", "stremio", "nuvio" };
    private static final int[] BADGE_COLOR = { 0xFF3A3A3A, 0xFF6B4EE6, 0xFF1FB6A6 };

    private final float density;
    private final int accent;

    StreamingAppDialog(Context context, String current, int accentArgb, final OnPick onPick) {
        super(context);
        this.density = context.getResources().getDisplayMetrics().density;
        this.accent = 0xFF000000 | accentArgb;
        build(current, onPick);
    }

    private void build(String current, final OnPick onPick) {
        Window window = getWindow();
        if (window != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF161616);
            bg.setCornerRadius(dp(20));
            window.setBackgroundDrawable(bg);
        }

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(14));

        TextView head = new TextView(getContext());
        head.setText("Open in player");
        head.setTextColor(0xFFFFFFFF);
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f);
        head.setTypeface(head.getTypeface(), Typeface.BOLD);
        head.setPadding(0, 0, 0, dp(4));
        root.addView(head);

        TextView sub = new TextView(getContext());
        sub.setText("Adds a button next to Trailer that opens the film in this app.");
        sub.setTextColor(0xFF9AA0A6);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams sublp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sublp.bottomMargin = dp(10);
        sub.setLayoutParams(sublp);
        root.addView(sub);

        for (int i = 0; i < LABELS.length; i++) {
            final String value = VALUES[i];
            boolean sel = value.equals(current);

            Badge badge = new Badge(getContext(), LABELS[i], BADGE_COLOR[i]);
            root.addView(OptionCard.build(getContext(), density, badge, 40f, LABELS[i], sel, accent,
                    new Runnable() {
                        @Override public void run() {
                            onPick.onPick(value);
                            dismiss();
                        }
                    }));
        }

        setContentView(root);
        if (window != null) {
            window.setLayout(
                    Math.min(getContext().getResources().getDisplayMetrics().widthPixels - dp(44), dp(420)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    /** A small rounded, coloured badge with the app's initial — stands in for a real app icon. */
    private final class Badge extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final String initial;

        Badge(Context c, String label, int color) {
            super(c);
            this.initial = label.substring(0, 1);
            p.setColor(color);
            text.setColor(0xFFF2F2F2);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            r.set(0, 0, w, h);
            canvas.drawRoundRect(r, dp(10), dp(10), p);
            text.setTextSize(h * 0.5f);
            canvas.drawText(initial, w / 2f, h / 2f - (text.ascent() + text.descent()) / 2f, text);
        }
    }
}
