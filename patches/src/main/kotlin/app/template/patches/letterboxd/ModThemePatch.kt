package app.template.patches.letterboxd

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.letterboxd.theme.buildColorOverlay
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD

/**
 * Letterboxd's dark surface greys, remapped to true-black tones. Names match `res/values/public.xml`
 * (all frozen public colour ids); values are the OLED tones from [materialYouThemePatch] — elevated
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

/** Emits `assets/morphe/oled.arsc`, loaded at runtime by `ModThemeApi31` when the pref is on. */
internal val modThemeResourcePatch = resourcePatch {
    execute {
        buildColorOverlay(
            sourceManifest = get("AndroidManifest.xml"),
            sourcePublic = get("res/values/public.xml"),
            packageName = packageMetadata.packageName,
            outputFile = get("assets/morphe/oled.arsc", copy = false),
            colors = OLED_SURFACES,
        )
    }
}

@Suppress("unused")
val modThemePatch = bytecodePatch(
    name = "Mod theme (OLED)",
    description = "Adds a \"Pure black (OLED)\" switch to the \"Mod settings\" screen on Android 12+. " +
        "When on, Letterboxd's dark surfaces are repainted true black at runtime via a resource " +
        "overlay; toggling it prompts for a restart. Needs the \"Mod settings\" patch.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    dependsOn(modThemeResourcePatch, modSettingsPatch)

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
