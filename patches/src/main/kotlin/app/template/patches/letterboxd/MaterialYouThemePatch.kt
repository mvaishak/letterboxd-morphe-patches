package app.template.patches.letterboxd

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Letterboxd hard-codes its dark palette as named colours (e.g. `@color/gray181C20`)
 * that the theme and ~400 component styles reference directly, so runtime dynamic
 * colour would recolour almost nothing.
 *
 * Instead, this patch redefines the dark background / surface greys under the
 * `-v31` resource qualifier as references to Android's wallpaper-derived palette.
 * Android 12+ then renders tinted chrome; Android 11 and below keep the original
 * values untouched.
 *
 * Only dark surface greys are remapped. Text, icon, divider and hint greys, the
 * Letterboxd green, white and black are left alone so contrast and the brand
 * accent are preserved. Higher `system_neutralN_M` numbers are darker tones.
 */
private val DYNAMIC_NEUTRAL_MAP = mapOf(
    "gray0D1012" to "@android:color/system_neutral1_1000",
    "gray14181C" to "@android:color/system_neutral1_900",
    "gray181C20" to "@android:color/system_neutral1_900", // android:colorBackground
    "gray1C242C" to "@android:color/system_neutral2_900",
    "gray202830" to "@android:color/system_neutral1_800",
    "gray283038" to "@android:color/system_neutral1_800",
    "gray223344" to "@android:color/system_neutral2_800",
    "gray2C3440" to "@android:color/system_neutral2_800",
    "gray303840" to "@android:color/system_neutral1_700",
    "gray334455" to "@android:color/system_neutral2_700", // colorPrimaryDark
    "gray445566" to "@android:color/system_neutral2_700", // colorPrimary
    "windowBackground" to "@android:color/system_neutral1_900",
)

/** Top app bar and bottom nav share the tinted window background. */
private const val BAR_BACKGROUND = "@android:color/system_neutral1_900"

@Suppress("unused")
val materialYouThemePatch = resourcePatch(
    name = "Material You theme",
    description = "Tints Letterboxd's dark chrome (backgrounds, surfaces, top and bottom bars) with " +
        "the device's wallpaper palette on Android 12+. Letterboxd's green (ratings, stars, primary " +
        "actions) is kept. No effect on Android 11 and below or on Jetpack Compose screens. Overlaps " +
        "\"Match bottom nav to top bar color\" on one style item — enable one, not both.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    execute {
        // 1. -v31 palette overrides: named colour -> wallpaper palette reference.
        document("res/values-v31/colors.xml").use { document ->
            val resources = document.documentElement
                ?: throw PatchException("res/values-v31/colors.xml has no root element")

            DYNAMIC_NEUTRAL_MAP.forEach { (name, value) ->
                upsertColor(document, resources, name, value)
            }
        }

        // 2. Point the app bar and bottom nav backgrounds at the same dynamic
        //    neutral so the top and bottom chrome match the tinted background.
        document("res/values/styles.xml").use { document ->
            setStyleItem(document, "Widget.Letterboxd.AppBarLayout", "android:background", BAR_BACKGROUND)
            setStyleItem(document, "Widget.Letterboxd.AppBarLayout", "liftOnScrollColor", BAR_BACKGROUND)
            setStyleItem(document, "Widget.Letterboxd.BottomNavigationView", "android:background", BAR_BACKGROUND)
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
