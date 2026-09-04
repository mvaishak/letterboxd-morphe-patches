package app.template.extension.settings;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Standalone host for {@link ModSettingsFragment}. Added to the Letterboxd manifest by the
 * "Mod settings" patch and launched from a launcher long-press shortcut.
 *
 * <p>Framework {@code Activity} + {@code android.preference} are used deliberately: they need no
 * extra runtime dependency and no {@code preferenceTheme} attribute on the host theme. The manifest
 * theme is {@code @android:style/Theme.Material.NoActionBar}; this class draws its own header and
 * applies the status-/navigation-bar insets itself.
 */
@SuppressWarnings("deprecation")
public class ModSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D0D0D);

        root.addView(buildHeader(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final FrameLayout container = new FrameLayout(this);
        final int containerId = View.generateViewId();
        container.setId(containerId);
        root.addView(container, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                v.setPadding(0, insets.getSystemWindowInsetTop(), 0,
                        insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        setContentView(root);

        if (getFragmentManager().findFragmentById(containerId) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(containerId, new ModSettingsFragment())
                    .commit();
        }
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(Color.WHITE);
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
        back.setPadding(0, 0, dp(18), 0);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(back);

        TextView title = new TextView(this);
        title.setText("Letterboxd Mods");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        header.addView(title);

        return header;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
