package app.template.patches.letterboxd

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * A colour value in each of the patch's modes.
 *
 * - [fallback] — plain ARGB used on Android 11 and below (no dynamic palette there).
 * - [dynamic]  — wallpaper-palette reference, only valid under `-v31`.
 * - [oled]     — near-black used by the "Pure black (OLED)" mode on every API level.
 */
private data class Tone(val fallback: String, val dynamic: String, val oled: String)

/**
 * Letterboxd hard-codes its dark palette as named colours (e.g. `@color/gray181C20`)
 * that the theme and ~400 component styles reference directly, so runtime dynamic
 * colour would recolour almost nothing. This patch redefines the dark background /
 * surface greys instead.
 *
 * Text, icon and hint greys, the Letterboxd green, white and pure black are left
 * alone so contrast and the brand accent are preserved. Higher `system_neutralN_M`
 * numbers are darker tones.
 */
private val PALETTE = mapOf(
    "gray0D1012" to Tone("#FF0D1012", "@android:color/system_neutral1_1000", "#FF000000"),
    "gray14181C" to Tone("#FF14181C", "@android:color/system_neutral1_900", "#FF000000"),
    "gray181C20" to Tone("#FF181C20", "@android:color/system_neutral1_900", "#FF000000"), // colorBackground
    "windowBackground" to Tone("#FF181C20", "@android:color/system_neutral1_900", "#FF000000"),
    "gray1C242C" to Tone("#FF1C242C", "@android:color/system_neutral2_900", "#FF0A0A0A"),
    "gray202830" to Tone("#FF202830", "@android:color/system_neutral1_800", "#FF0A0A0A"),
    "gray283038" to Tone("#FF283038", "@android:color/system_neutral1_800", "#FF0A0A0A"),
    "gray223344" to Tone("#FF223344", "@android:color/system_neutral2_800", "#FF101010"),
    "gray2C3440" to Tone("#FF2C3440", "@android:color/system_neutral2_800", "#FF101010"),
    "gray303840" to Tone("#FF303840", "@android:color/system_neutral1_700", "#FF101010"),
    "gray334455" to Tone("#FF334455", "@android:color/system_neutral2_700", "#FF161616"), // colorPrimaryDark
    "gray445566" to Tone("#FF445566", "@android:color/system_neutral2_700", "#FF161616"), // colorPrimary
)

/**
 * Indirection colours the patch creates and the chrome style-edits point at, so
 * the edits in the (unqualified) styles.xml always resolve — `@android:color/system_*`
 * would crash when inflated on Android 11 and below.
 */
private val CHROME = mapOf(
    "morphe_my_surface" to Tone("#FF181C20", "@android:color/system_neutral1_900", "#FF000000"),
    "morphe_my_surface_elevated" to Tone("#FF202830", "@android:color/system_neutral1_800", "#FF0A0A0A"),
    "morphe_my_divider" to Tone("#FF334455", "@android:color/system_neutral2_600", "#FF2A2A2A"),
)

@Suppress("unused")
val materialYouThemePatch = resourcePatch(
    name = "Material You theme",
    description = "Repaints Letterboxd's dark chrome — window background, surfaces, cards, the top " +
        "bar, tab strip and bottom nav. 'Wallpaper tint' follows the device's Material You palette on " +
        "Android 12+ (no effect below). 'Pure black (OLED)' forces near-black on any version. The " +
        "Letterboxd green, white and text greys are kept. No effect on Jetpack Compose screens. " +
        "Overlaps \"Match bottom nav to top bar color\" — enable one, not both.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    val surfaceStyle by stringOption(
        key = "surfaceStyle",
        default = "wallpaper",
        values = mapOf(
            "Wallpaper tint (Android 12+)" to "wallpaper",
            "Pure black (OLED)" to "black",
        ),
        title = "Surface style",
        description = "How the dark chrome is recoloured.",
    )

    execute {
        val oled = surfaceStyle == "black"

        // res/values/colors.xml — base values that must resolve on every API level.
        document("res/values/colors.xml").use { document ->
            val resources = document.documentElement
                ?: throw PatchException("res/values/colors.xml has no root element")

            // Always create the chrome indirection colours.
            CHROME.forEach { (name, tone) ->
                upsertColor(document, resources, name, if (oled) tone.oled else tone.fallback)
            }
            // In OLED mode also flatten the raw palette here so it works below Android 12.
            if (oled) PALETTE.forEach { (name, tone) -> upsertColor(document, resources, name, tone.oled) }
        }

        // res/values-v31/colors.xml — Android 12+ overrides.
        document("res/values-v31/colors.xml").use { document ->
            val resources = document.documentElement
                ?: throw PatchException("res/values-v31/colors.xml has no root element")

            CHROME.forEach { (name, tone) ->
                upsertColor(document, resources, name, if (oled) tone.oled else tone.dynamic)
            }
            PALETTE.forEach { (name, tone) ->
                upsertColor(document, resources, name, if (oled) tone.oled else tone.dynamic)
            }
        }

        // res/values/styles.xml — flatten the chrome onto the indirection colours.
        document("res/values/styles.xml").use { document ->
            setStyleItem(document, "Widget.Letterboxd.AppBarLayout", "android:background", "@color/morphe_my_surface")
            setStyleItem(document, "Widget.Letterboxd.AppBarLayout", "liftOnScrollColor", "@color/morphe_my_surface")
            setStyleItem(document, "Widget.Letterboxd.TabLayout", "android:background", "@color/morphe_my_surface")
            setStyleItem(document, "Widget.Letterboxd.BottomNavigationView", "android:background", "@color/morphe_my_surface")
            setStyleItem(document, "Widget.Letterboxd.BottomSheet.Modal", "backgroundTint", "@color/morphe_my_surface_elevated")
            setStyleItem(document, "Widget.Letterboxd.Divider", "dividerColor", "@color/morphe_my_divider")
        }
    }
}

/** Replace the value of `<color name="[name]">` in [resources], or add it if absent. */
private fun upsertColor(document: Document, resources: Element, name: String, value: String) {
    val colors = resources.getElementsByTagName("color")
    for (i in 0 until colors.length) {
        val color = colors.item(i) as Element
        if (color.getAttribute("name") == name) {
            color.textContent = value
            return
        }
    }
    resources.appendChild(
        document.createElement("color").apply {
            setAttribute("name", name)
            textContent = value
        },
    )
}

/** Replace `<item name="[itemName]">` inside `<style name="[styleName]">`, or add it if absent. */
private fun setStyleItem(document: Document, styleName: String, itemName: String, value: String) {
    val styles = document.getElementsByTagName("style")
    val style = (0 until styles.length)
        .map { styles.item(it) as Element }
        .firstOrNull { it.getAttribute("name") == styleName }
        ?: throw PatchException("Style \"$styleName\" not found in res/values/styles.xml")

    val items = style.getElementsByTagName("item")
    for (i in 0 until items.length) {
        val item = items.item(i) as Element
        if (item.getAttribute("name") == itemName) {
            item.textContent = value
            return
        }
    }
    style.appendChild(
        document.createElement("item").apply {
            setAttribute("name", itemName)
            textContent = value
        },
    )
}
