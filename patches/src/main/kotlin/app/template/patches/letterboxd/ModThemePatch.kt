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
    "gray334455" to "#FF2E2E2E", // colorPrimary / histogram bars
    "gray445566" to "#FF2E2E2E",
)

/**
 * The same surface greys pointed at the device's Material You palette (Android 12+ only). Values
 * are the wallpaper-palette references the OS resolves from the device theme.
 * This only recolours the named surface colours — the app-bar / tab-strip style flattening the
 * patch-time "Material You theme" also does is not applied at runtime.
 */
private val MATERIALYOU_SURFACES = mapOf(
    "gray0D1012" to "@android:color/system_neutral1_1000",
    "gray14181C" to "@android:color/system_neutral1_900",
    "gray181C20" to "@android:color/system_neutral1_900",
    "windowBackground" to "@android:color/system_neutral1_900",
    "gray1C242C" to "@android:color/system_neutral2_900",
    "gray202830" to "@android:color/system_neutral1_800",
    "gray283038" to "@android:color/system_neutral1_800",
    "gray223344" to "@android:color/system_neutral2_800",
    "gray2C3440" to "@android:color/system_neutral2_800",
    "gray303840" to "@android:color/system_neutral1_700",
    "gray334455" to "@android:color/system_neutral2_700",
    "gray445566" to "@android:color/system_neutral2_700",
)

/**
 * Emits the runtime overlay tables loaded by `ModThemeApi31`: `assets/morphe/oled.arsc`,
 * `materialyou.arsc`, and one `accent_<key>.arsc` per accent preset.
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

        buildColorOverlay(
            sourceManifest = manifest,
            sourcePublic = public,
            packageName = packageName,
            outputFile = get("assets/morphe/materialyou.arsc", copy = false),
            colors = MATERIALYOU_SURFACES,
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
    name = "Mod theme",
    description = "Adds \"Surface style\" (Stock / Material You / Pure black OLED) and " +
        "\"Accent colour\" controls to the \"Mod settings\" screen on Android 12+. They repaint " +
        "Letterboxd's dark surfaces / green accent at runtime via resource overlays; changing " +
        "either prompts for a restart. Needs the \"Mod settings\" patch.",
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
