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
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The Android 12+ half of {@link ModTheme}: copies the overlay {@code .arsc} out of assets, wraps
 * it in a {@link ResourcesLoader}, and adds that loader to each {@link Resources} instance once.
 */
@RequiresApi(31)
final class ModThemeApi31 {

    private static final String OVERLAY_ASSET = "morphe/oled.arsc";
    private static final String OVERLAY_CACHE = "morphe-oled.arsc";

    private static ResourcesProvider provider;
    private static ResourcesLoader loader;

    private static final Set<Resources> APPLIED =
            Collections.newSetFromMap(new WeakHashMap<Resources, Boolean>());

    private ModThemeApi31() {}

    static synchronized void load(Context context) {
        if (loader != null) return;
        try {
            Context app = context.getApplicationContext();
            if (app == null) app = context;

            File file = new File(app.getCodeCacheDir(), OVERLAY_CACHE);
            copyAsset(app, OVERLAY_ASSET, file);

            try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(
                    file, ParcelFileDescriptor.MODE_READ_ONLY)) {
                provider = ResourcesProvider.loadFromTable(descriptor, null);
            }
            loader = new ResourcesLoader();
            loader.addProvider(provider);
        } catch (Throwable t) {
            provider = null;
            loader = null;
        }
    }

    static synchronized void applyTo(Resources resources) {
        if (resources == null || loader == null || APPLIED.contains(resources)) return;
        try {
            resources.addLoaders(loader);
            APPLIED.add(resources);
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
