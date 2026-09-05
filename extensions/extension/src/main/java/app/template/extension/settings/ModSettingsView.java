package app.template.extension.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The mod settings screen, built as plain views so the toggles and section headers can be styled
 * and tinted with the chosen accent. Writes to the same {@link Prefs} store the patches read.
 */
final class ModSettingsView extends ScrollView {

    private static final String[] NAV_LABELS = {
            "Stock", "No pill", "No pill, white icon", "No pill, accent icon", "Accent pill",
    };
    private static final String[] NAV_VALUES = { "stock", "nopill", "white", "accent", "accentPill" };

    private static final String[] REVEAL_LABELS = {
            "Frosted panel", "Frosted panel (crumble)", "Tap-to-show link",
            "Shimmer", "Shimmer (crumble)", "Tap to burst", "Confetti",
    };
    private static final String[] REVEAL_VALUES = {
            "panel", "panel_crumble", "link", "shimmer", "shimmer_crumble", "burst", "confetti",
    };


    private final Context ctx;
    private final float density;
    private final int accent;
    private final LinearLayout column;

    private View revealRow;
    private TextView revealValue;

    ModSettingsView(Context context) {
        super(context);
        this.ctx = context;
        this.density = context.getResources().getDisplayMetrics().density;
        Prefs.load(context);
        this.accent = 0xFF000000 | AccentPresets.previewColor(
                Prefs.getString(Prefs.KEY_THEME_ACCENT, "green"),
                Prefs.getString(Prefs.KEY_THEME_ACCENT_HEX, ""));

        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(20), dp(4), dp(20), dp(28));
        addView(column, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        boolean themeAvailable = ModTheme.isSupported();

        boolean materialYouActive = ctx.getResources().getIdentifier(
                "morphe_my_surface", "color", ctx.getPackageName()) != 0;

        header("Theme");
        if (!themeAvailable) {
            column.addView(disabledRow("Pure black (OLED)", "Needs Android 12 or newer", null));
        } else if (materialYouActive) {
            column.addView(disabledRow("Pure black (OLED)", "Disabled — tap to find out why",
                    materialYouConflictExplainer()));
        } else {
            PillToggle oled = new PillToggle(ctx);
            View oledRow = toggleRow(oled, "Pure black (OLED)",
                    "True-black surfaces; elevated bits stay a faint grey",
                    Prefs.KEY_THEME_OLED, false, true);
            oled.setChecked("oled".equals(Prefs.surface()), false);
            oled.setOnToggle(new PillToggle.OnToggle() {
                @Override public void onToggle(boolean checked) {
                    Prefs.putString(Prefs.KEY_THEME_SURFACE, checked ? "oled" : "stock");
                    Prefs.putBoolean(Prefs.KEY_THEME_OLED, checked);
                    RestartHelper.promptRestart(ctx);
                }
            });
            column.addView(oledRow);
        }

        if (materialYouActive) {
            column.addView(disabledRow("Match bottom nav to top bar", "Disabled — tap to find out why",
                    materialYouConflictExplainer()));
        } else {
            column.addView(toggleRow("Match bottom nav to top bar",
                    "Paint the bottom navigation bar black to match the top bar",
                    Prefs.KEY_MATCH_BOTTOM_NAV, true, true));
        }

        if (themeAvailable) {
            column.addView(accentRow());
            column.addView(choiceRow("Bottom nav selected style", null,
                    labelFor(NAV_LABELS, NAV_VALUES, Prefs.getString(Prefs.KEY_NAV_INDICATOR, "stock")),
                    new Runnable() {
                        @Override public void run() {
                            new NavStyleDialog(ctx, Prefs.getString(Prefs.KEY_NAV_INDICATOR, "stock"),
                                    accent, new NavStyleDialog.OnPick() {
                                        @Override public void onPick(String value) {
                                            Prefs.putString(Prefs.KEY_NAV_INDICATOR, value);
                                            rebuildAndRestart();
                                        }
                                    }).show();
                        }
                    }));
        }

        header("Home");
        column.addView(toggleRow("Hide Video Store",
                "Remove the Video Store promo row from the Films tab",
                Prefs.KEY_HIDE_VIDEO_STORE, false, true));
        column.addView(toggleRow("Hide Where to Watch",
                "Remove the \"Where to watch\" section from a film's page",
                Prefs.KEY_HIDE_WHERE_TO_WATCH, false, false));

        header("Streaming");
        column.addView(toggleRow("Open in player",
                "Add a small button beside Trailer that opens the film in Stremio",
                Prefs.KEY_OPEN_IN_PLAYER, false, false));

        header("Ratings");
        final PillToggle hideRatings = new PillToggle(ctx);
        column.addView(toggleRow(hideRatings, "Hide ratings until watched",
                "Cover a film's community rating until you mark it watched",
                Prefs.KEY_HIDE_RATINGS_ENABLED, true, false));
        revealRow = choiceRow("Reveal style", null,
                labelFor(REVEAL_LABELS, REVEAL_VALUES, Prefs.getString(Prefs.KEY_HIDE_RATINGS_STYLE, "panel")),
                new Runnable() {
                    @Override public void run() {
                        new RevealStyleDialog(ctx,
                                Prefs.getString(Prefs.KEY_HIDE_RATINGS_STYLE, "panel"), accent,
                                new RevealStyleDialog.OnPick() {
                                    @Override public void onPick(String value) {
                                        Prefs.putString(Prefs.KEY_HIDE_RATINGS_STYLE, value);
                                        if (revealValue != null) {
                                            revealValue.setText(labelFor(REVEAL_LABELS, REVEAL_VALUES, value));
                                        }
                                    }
                                }).show();
                    }
                });
        column.addView(revealRow);
        setRevealEnabled(Prefs.getBoolean(Prefs.KEY_HIDE_RATINGS_ENABLED, true));
        hideRatings.setOnToggle(new PillToggle.OnToggle() {
            @Override public void onToggle(boolean checked) {
                Prefs.putBoolean(Prefs.KEY_HIDE_RATINGS_ENABLED, checked);
                setRevealEnabled(checked);
            }
        });
    }

    // --- rows -----------------------------------------------------------

    private void header(String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text.toUpperCase());
        tv.setTextColor(accent);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(22);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        column.addView(tv);
    }

    private View toggleRow(String title, String subtitle, final String key,
                           final boolean def, final boolean restart) {
        return toggleRow(new PillToggle(ctx), title, subtitle, key, def, restart);
    }

    private View toggleRow(final PillToggle toggle, String title, String subtitle,
                           final String key, boolean def, final boolean restart) {
        LinearLayout row = rowBase();
        row.addView(titleBlock(title, subtitle), textLp());

        toggle.setAccent(accent);
        toggle.setChecked(Prefs.getBoolean(key, def), false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        row.addView(toggle, tlp);

        toggle.setOnToggle(new PillToggle.OnToggle() {
            @Override public void onToggle(boolean checked) {
                Prefs.putBoolean(key, checked);
                if (restart) RestartHelper.promptRestart(ctx);
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggle.performClick(); }
        });
        return row;
    }

    private View choiceRow(String title, String subtitle, String value, final Runnable onClick) {
        LinearLayout row = rowBase();
        row.addView(titleBlock(title, subtitle), textLp());

        TextView v = new TextView(ctx);
        v.setText(value);
        v.setTextColor(0xFFB0B0B0);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        v.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(v);
        if (title.equals("Reveal style")) revealValue = v;

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClick.run(); }
        });
        return row;
    }

    private View accentRow() {
        LinearLayout row = rowBase();
        row.addView(titleBlock("Accent colour", null), textLp());

        final int argb = 0xFF000000 | AccentPresets.previewColor(
                Prefs.getString(Prefs.KEY_THEME_ACCENT, "green"),
                Prefs.getString(Prefs.KEY_THEME_ACCENT_HEX, ""));
        View dot = new View(ctx);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(argb);
        d.setStroke(dp(2), 0x33FFFFFF);
        dot.setBackground(d);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(24), dp(24));
        dlp.gravity = Gravity.CENTER_VERTICAL;
        row.addView(dot, dlp);

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AccentPickerDialog(ctx,
                        Prefs.getString(Prefs.KEY_THEME_ACCENT, "green"),
                        Prefs.getString(Prefs.KEY_THEME_ACCENT_HEX, ""),
                        new AccentPickerDialog.OnAccentChosen() {
                            @Override public void onChosen(String accentKey, String customHex) {
                                Prefs.putString(Prefs.KEY_THEME_ACCENT, accentKey);
                                Prefs.putString(Prefs.KEY_THEME_ACCENT_HEX, customHex);
                                rebuildAndRestart();
                            }
                        }).show();
            }
        });
        return row;
    }

    private View disabledRow(String title, String subtitle, final Runnable onTap) {
        LinearLayout row = rowBase();
        row.setAlpha(onTap != null ? 0.7f : 0.45f);
        row.addView(titleBlock(title, subtitle), textLp());
        if (onTap != null) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { onTap.run(); }
            });
        } else {
            row.setClickable(false);
        }
        return row;
    }

    /**
     * A new user seeing "Disabled — the Material You theme patch is active" under a greyed-out
     * switch has no way to know that's a deliberate design choice rather than a bug — explain it
     * in full when tapped instead of relying on a one-line subtitle to carry that.
     */
    private Runnable materialYouConflictExplainer() {
        return new Runnable() {
            @Override public void run() {
                ModDialog.show(ctx,
                        "Handled by Material You",
                        "This is off because you also patched \"Material You theme\" — it already " +
                                "controls this. Disable that patch to use OLED here.",
                        "Got it", null, null, null);
            }
        };
    }

    // --- helpers ------------------------------------------------------

    private void setRevealEnabled(boolean enabled) {
        if (revealRow != null) {
            revealRow.setAlpha(enabled ? 1f : 0.4f);
            revealRow.setClickable(enabled);
        }
    }

    private void rebuildAndRestart() {
        RestartHelper.promptRestart(ctx);
    }

    private LinearLayout rowBase() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(15), 0, dp(15));
        row.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);
        return row;
    }

    private View titleBlock(String title, String subtitle) {
        LinearLayout block = new LinearLayout(ctx);
        block.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextColor(0xFFEDEDED);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        block.addView(t);

        if (subtitle != null) {
            TextView s = new TextView(ctx);
            s.setText(subtitle);
            s.setTextColor(0xFF9AA0A6);
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = dp(2);
            s.setLayoutParams(slp);
            block.addView(s);
        }
        return block;
    }

    private LinearLayout.LayoutParams textLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(14);
        lp.gravity = Gravity.CENTER_VERTICAL;
        return lp;
    }

    private static String labelFor(String[] labels, String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) return labels[i];
        }
        return labels[0];
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
