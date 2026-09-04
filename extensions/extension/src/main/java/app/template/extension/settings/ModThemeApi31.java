package app.template.extension.settings;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The Android 12+ half of {@link ModTheme}: copies the chosen overlay {@code .arsc} files out of
 * assets, wraps each in a {@link ResourcesLoader}, and adds the whole set to each {@link Resources}
 * instance once. Later loaders win, so the accent overlay is added after OLED (they touch disjoint
 * resources, so ordering is only a safety net).
 *
 * <p>Restart-based: the loader set is fixed at process start, there is no removal path.
 */
@RequiresApi(31)
final class ModThemeApi31 {

    private static final List<ResourcesLoader> LOADERS = new ArrayList<>();
    private static final Set<Resources> APPLIED =
            Collections.newSetFromMap(new WeakHashMap<Resources, Boolean>());
    private static boolean prepared;

    private ModThemeApi31() {}

    static synchronized void prepare(Context context, boolean oled, String accent) {
        if (prepared) return;
        prepared = true;

        Context app = context.getApplicationContext();
        if (app == null) app = context;

        if (oled) {
            addLoader(app, "morphe/oled.arsc", "morphe-oled.arsc");
        }
        if (accent != null && !accent.isEmpty() && !"green".equals(accent)) {
            addLoader(app, "morphe/accent_" + accent + ".arsc", "morphe-accent-" + accent + ".arsc");
        }
    }

    static synchronized void applyTo(Resources resources) {
        if (resources == null || LOADERS.isEmpty() || APPLIED.contains(resources)) return;
        try {
            resources.addLoaders(LOADERS.toArray(new ResourcesLoader[0]));
            APPLIED.add(resources);
        } catch (Throwable ignored) {
        }
    }

    private static void addLoader(Context app, String assetName, String cacheName) {
        try {
            File file = new File(app.getCodeCacheDir(), cacheName);
            copyAsset(app, assetName, file);

            ResourcesProvider provider;
            try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(
                    file, ParcelFileDescriptor.MODE_READ_ONLY)) {
                provider = ResourcesProvider.loadFromTable(descriptor, null);
            }
            ResourcesLoader loader = new ResourcesLoader();
            loader.addProvider(provider);
            LOADERS.add(loader);
        } catch (Throwable ignored) {
        }
    }

    private static void copyAsset(Context context, String assetName, File output) throws Exception {
        try (InputStream input = context.getAssets().open(assetName);
             FileOutputStream out = new FileOutputStream(output, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
