package app.template.patches.letterboxd

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import com.android.tools.smali.dexlib2.AccessFlags
import org.w3c.dom.Element

private const val SETTINGS_ACTIVITY = "app.template.extension.settings.ModSettingsActivity"
private const val LABEL_STRING = "morphe_mod_settings"

/**
 * Registers [SETTINGS_ACTIVITY] in the manifest, adds a launcher long-press shortcut that opens
 * it, and a string for the shortcut label. The activity class itself ships in the extension.
 */
internal val modSettingsResourcePatch = resourcePatch {
    execute {
        document("res/values/strings.xml").use { document ->
            val resources = document.documentElement
            val already = document.getElementsByTagName("string").let { nodes ->
                (0 until nodes.length).any { (nodes.item(it) as Element).getAttribute("name") == LABEL_STRING }
            }
            if (!already) {
                resources.appendChild(
                    document.createElement("string").apply {
                        setAttribute("name", LABEL_STRING)
                        textContent = "Letterboxd Mods"
                    },
                )
            }
        }

        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            application.appendChild(
                document.createElement("activity").apply {
                    setAttribute("android:name", SETTINGS_ACTIVITY)
                    setAttribute("android:exported", "false")
                    setAttribute("android:label", "@string/$LABEL_STRING")
                    // A platform theme: framework android.preference.* needs the
                    // ?android:attr/preference*Style chain that Theme.Material defines.
                    // The app's own Theme.Letterboxd is Material3/AppCompat and lacks it,
                    // which crashes the PreferenceFragment on inflation. NoActionBar —
                    // ModSettingsActivity draws its own header and applies insets.
                    setAttribute("android:theme", "@android:style/Theme.Material.NoActionBar")
                },
            )
        }

        val shortcuts = get("res/xml/shortcuts.xml")
        if (shortcuts.exists()) {
            document("res/xml/shortcuts.xml").use { document ->
                val intent = document.createElement("intent").apply {
                    setAttribute("android:action", "android.intent.action.VIEW")
                    setAttribute("android:targetPackage", "com.letterboxd.letterboxd")
                    setAttribute("android:targetClass", SETTINGS_ACTIVITY)
                }
                val shortcut = document.createElement("shortcut").apply {
                    setAttribute("android:shortcutId", "morphemods")
                    setAttribute("android:enabled", "true")
                    setAttribute("android:shortcutShortLabel", "@string/$LABEL_STRING")
                    appendChild(intent)
                }
                document.documentElement.appendChild(shortcut)
            }
        }
    }
}

internal object LetterboxdApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/LetterboxdApplication;",
    name = "onCreate",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = emptyList(),
)

@Suppress("unused")
val modSettingsPatch = bytecodePatch(
    name = "Mod settings",
    description = "Adds a \"Letterboxd Mods\" settings screen — reachable from a launcher " +
        "long-press shortcut — where options for the other patches can be changed without " +
        "re-patching. Changes apply the next time the relevant screen is opened; some need an " +
        "app restart.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    dependsOn(modSettingsResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        LetterboxdApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, Lapp/template/extension/settings/Prefs;->load(Landroid/content/Context;)V",
        )
    }
}
