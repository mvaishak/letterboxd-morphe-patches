package app.template.patches.letterboxd

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.letterboxd.theme.ACCENT_OVERLAYS
import app.template.patches.letterboxd.theme.buildColorOverlay
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD

/**
 * Letterboxd's dark surface greys, remapped to true-black tones. Names match `res/values/public.xml`
 * (all frozen public colour ids); values are OLED tones — elevated
 * surfaces stay a faint grey so histogram bars etc. don't vanish on black.
 */
private val OLED_SURFACES = mapOf(
    "gray0D1012" to "#FF000000",
    "gray14181C" to "#FF000000",
    "gray181C20" to "#FF000000", // colorBackground
    "windowBackground" to "#FF000000",
    "gray1C242C" to "#FF121212",
    "gray202830" to "#FF121212",
    "gray283038" to "#FF121212",
    "gray223344" to "#FF1C1C1C",
    "gray2C3440" to "#FF1C1C1C",
    "gray303840" to "#FF1C1C1C",
    "gray334455" to "#FF2E2E2E", // colorPrimary / histogram bars — kept subtle
    "gray445566" to "#FF2E2E2E",
    // Unfilled rating stars (log sheet etc.) tint with colorPrimaryDark (= @color/gray334455);
    // a direct, lighter value here keeps them visible without brightening the histogram bars.
    "colorPrimaryDark" to "#FF4A4A4A",
)

/**
 * Emits the runtime overlay tables loaded by `ModThemeApi31`: `assets/morphe/oled.arsc`
 * and one `accent_<key>.arsc` per accent preset. (Material You surface tint is the patch-time
 * `materialYouThemePatch`, not a runtime overlay.)
 */
internal val modThemeResourcePatch = resourcePatch {
    execute {
        val manifest = get("AndroidManifest.xml")
        val public = get("res/values/public.xml")
        val packageName = packageMetadata.packageName

        buildColorOverlay(
            sourceManifest = manifest,
            sourcePublic = public,
            packageName = packageName,
            outputFile = get("assets/morphe/oled.arsc", copy = false),
            colors = OLED_SURFACES,
        )

        ACCENT_OVERLAYS.forEach { (key, colors) ->
            buildColorOverlay(
                sourceManifest = manifest,
                sourcePublic = public,
                packageName = packageName,
                outputFile = get("assets/morphe/accent_$key.arsc", copy = false),
                colors = colors,
            )
        }
    }
}

@Suppress("unused")
val modThemePatch = bytecodePatch(
    name = "Appearance",
    description = "In-app appearance controls, adjustable from the Letterboxd Mods screen without " +
        "re-patching: a true-black OLED surface, a custom accent colour (presets or any hex), and " +
        "the bottom-navigation selected style. Applied at runtime via resource overlays on " +
        "Android 12 and later. Needs the \"Mod settings\" patch.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    // bottomNavColorPatch carries the MainActivity.setup hook that ModChrome uses for the
    // bottom-nav selected style (the pill); the selected-icon colour is done by an overlay here.
    dependsOn(modThemeResourcePatch, modSettingsPatch, bottomNavColorPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // Same method modSettingsPatch hooks; ModTheme.initialize also calls Prefs.load, so the
        // order the two prepends land in does not matter.
        LetterboxdApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, Lapp/template/extension/settings/ModTheme;->initialize(Landroid/content/Context;)V",
        )
    }
}
