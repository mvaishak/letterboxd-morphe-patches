package app.template.patches.letterboxd

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * `MainActivity.setup(BottomNavigationView, Tab)` configures the bottom navigation bar (menu,
 * tint, listeners) once at startup. Injecting there lets the extension paint the bar's background
 * from a preference instead of hard-coding it in `styles.xml`, so the "Mod settings" screen can
 * turn it off.
 */
internal object MainActivitySetupBottomNavFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/MainActivity;",
    name = "setup",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Lcom/google/android/material/bottomnavigation/BottomNavigationView;",
        "Lcom/letterboxd/letterboxd/MainActivity\$Tab;",
    ),
)

@Suppress("unused")
val bottomNavColorPatch = bytecodePatch(
    name = "Match bottom nav to top bar color",
    description = "Paints Letterboxd's bottom navigation bar black (#000000), matching the top bar, " +
        "instead of the default slate. Can be toggled from the \"Mod settings\" screen if that " +
        "patch is also enabled; the change applies on the next app start. With \"Material You " +
        "theme\" also on, this wins — turn it off to keep the Material You nav tint.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    extendWith("extensions/extension.mpe")

    execute {
        MainActivitySetupBottomNavFingerprint.method.addInstruction(
            0,
            "invoke-static { p1 }, Lapp/template/extension/settings/ModChrome;->applyBottomNav(Landroid/view/View;)V",
        )
    }
}
