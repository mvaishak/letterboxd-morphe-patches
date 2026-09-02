package app.template.patches.letterboxd

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * A surface colour in each of the patch's modes.
 *
 * - [fallback] — plain ARGB used on Android 11 and below (no dynamic palette there).
 * - [dynamic]  — wallpaper-palette reference, only valid under `-v31`.
 * - [oled]     — used by the "Pure black (OLED)" mode on every API level: true black
 *   backgrounds, with elevated surfaces kept just visible against them.
 */
private data class Tone(val fallback: String, val dynamic: String, val oled: String)

/** A 3-stop accent ramp (darkest → brightest), tuned to read on dark / black backgrounds. */
private data class Accent(val dim: String, val primary: String, val bright: String)

/**
 * Letterboxd hard-codes its dark palette as named colours (e.g. `@color/gray181C20`)
 * that the theme and ~400 component styles reference directly, so runtime dynamic
 * colour would recolour almost nothing. This patch redefines the dark background /
 * surface greys instead.
 *
 * Text, icon and hint greys and white are left alone so contrast is preserved.
 * Higher `system_neutralN_M` numbers are darker tones.
 */
private val PALETTE = mapOf(
    "gray0D1012" to Tone("#FF0D1012", "@android:color/system_neutral1_1000", "#FF000000"),
    "gray14181C" to Tone("#FF14181C", "@android:color/system_neutral1_900", "#FF000000"),
    "gray181C20" to Tone("#FF181C20", "@android:color/system_neutral1_900", "#FF000000"), // colorBackground
    "windowBackground" to Tone("#FF181C20", "@android:color/system_neutral1_900", "#FF000000"),
    "gray1C242C" to Tone("#FF1C242C", "@android:color/system_neutral2_900", "#FF121212"),
    "gray202830" to Tone("#FF202830", "@android:color/system_neutral1_800", "#FF121212"),
    "gray283038" to Tone("#FF283038", "@android:color/system_neutral1_800", "#FF121212"),
    "gray223344" to Tone("#FF223344", "@android:color/system_neutral2_800", "#FF1C1C1C"),
    "gray2C3440" to Tone("#FF2C3440", "@android:color/system_neutral2_800", "#FF1C1C1C"),
    "gray303840" to Tone("#FF303840", "@android:color/system_neutral1_700", "#FF1C1C1C"),
    // colorPrimaryDark / colorPrimary — also the ratings-histogram bar colour, so the
    // OLED value is a visible dark grey rather than near-black.
    "gray334455" to Tone("#FF334455", "@android:color/system_neutral2_700", "#FF2E2E2E"),
    "gray445566" to Tone("#FF445566", "@android:color/system_neutral2_700", "#FF2E2E2E"),
)

/**
 * Indirection colours the patch creates and the chrome style-edits point at, so
 * the edits in the (unqualified) styles.xml always resolve — `@android:color/system_*`
 * would crash when inflated on Android 11 and below.
 */
private val CHROME = mapOf(
    "morphe_my_surface" to Tone("#FF181C20", "@android:color/system_neutral1_900", "#FF000000"),
    "morphe_my_surface_elevated" to Tone("#FF202830", "@android:color/system_neutral1_800", "#FF161616"),
    "morphe_my_divider" to Tone("#FF334455", "@android:color/system_neutral2_600", "#FF333333"),
)

/**
 * Applied to Letterboxd's green family: `green00A010/00B020/00C030` → [Accent.dim],
 * `colorAccent` + `green00E054` → [Accent.primary], `green0ADE53` → [Accent.bright].
 */
private val ACCENTS = mapOf(
    "green" to Accent("#FF0BA83E", "#FF1FE86A", "#FF4DF287"),
    "amber" to Accent("#FFB87400", "#FFFFC24B", "#FFFFD37A"),
    "orange" to Accent("#FFC24E12", "#FFFF8A3D", "#FFFFA968"),
    "coral" to Accent("#FFC23B3B", "#FFFF6B6B", "#FFFF9090"),
    "pink" to Accent("#FFC24C8E", "#FFFF7DC4", "#FFFFA6D6"),
    "violet" to Accent("#FF6B54C2", "#FFB69CFF", "#FFCFC0FF"),
    "blue" to Accent("#FF2E6FC2", "#FF5AA9FF", "#FF8AC4FF"),
    "teal" to Accent("#FF1F9E8F", "#FF3DD9C8", "#FF77E7DB"),
    "mono" to Accent("#FF9A9A9A", "#FFE6E6E6", "#FFFFFFFF"),
)

@Suppress("unused")
val materialYouThemePatch = resourcePatch(
    name = "Material You theme",
    description = "Repaints Letterboxd's dark chrome — window background, surfaces, cards, the top " +
        "bar, tab strip and bottom nav. 'Wallpaper tint' follows the device's Material You palette on " +
        "Android 12+ (no effect below). 'Pure black (OLED)' forces true black on any version. " +
        "Optional accent colour recolours Letterboxd's green; optional bottom-nav selected style " +
        "replaces the grey pill. No effect on Jetpack Compose screens. Overlaps \"Match bottom nav " +
        "to top bar color\" — enable one, not both.",
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

    val accent by stringOption(
        key = "accent",
        default = "green",
        values = mapOf(
            "Green (brighter on black)" to "green",
            "Amber" to "amber",
            "Orange" to "orange",
            "Coral" to "coral",
            "Pink" to "pink",
            "Violet" to "violet",
            "Blue" to "blue",
            "Teal" to "teal",
            "Mono (near-white)" to "mono",
        ),
        title = "Accent colour",
        description = "Recolours Letterboxd's green (stars, rating indicators, primary buttons). " +
            "'Green' is left untouched, except in OLED mode where it is brightened for contrast.",
    )

    val bottomNavIndicator by stringOption(
        key = "bottomNavIndicator",
        default = "stock",
        values = mapOf(
            "Stock (grey pill + blue icon)" to "stock",
            "No pill (keep blue icon)" to "nopill",
            "No pill, white selected icon" to "white",
            "No pill, accent selected icon" to "accent",
            "Accent pill + accent icon" to "accentPill",
        ),
        title = "Bottom nav selected style",
        description = "How the selected tab in the bottom navigation bar is shown. The green + " +
            "button is never affected.",
    )

    execute {
        val oled = surfaceStyle == "black"
        val accentKey = accent ?: "green"
        // "green" in wallpaper mode = leave Letterboxd's green exactly as-is.
        val accentRamp = if (accentKey != "green" || oled) ACCENTS.getValue(accentKey) else null

        // Bottom-nav selected-tab treatment.
        val navMode = bottomNavIndicator ?: "stock"
        val accentPrimary = ACCENTS.getValue(accentKey).primary
        // Selected icon fill; null = leave Letterboxd's blue. Falls back to white when the accent
        // is green so the selected tab stays distinct from the always-green + button.
        val navIconColor = when (navMode) {
            "white" -> "#FFF2F2F2"
            "accent" -> if (accentKey == "green") "#FFF2F2F2" else accentPrimary
            "accentPill" -> accentPrimary
            else -> null
        }
        // Active-indicator pill colour.
        val navPillColor = when (navMode) {
            "stock" -> null
            "accentPill" -> "#38" + accentPrimary.substring(3) // ~22% alpha
            else -> "@android:color/transparent"
        }

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

            // Accent recolour.
            accentRamp?.let { ramp ->
                listOf("green00A010", "green00B020", "green00C030").forEach {
                    upsertColor(document, resources, it, ramp.dim)
                }
                upsertColor(document, resources, "colorAccent", ramp.primary)
                upsertColor(document, resources, "green00E054", ramp.primary)
                upsertColor(document, resources, "green0ADE53", ramp.bright)
            }
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

            navPillColor?.let {
                setStyleItem(document, "Widget.Letterboxd.BottomNavigationView.ActiveIndicator", "android:color", it)
            }
        }

        // Selected bottom-nav icon fill — the *_filled vectors, never ic_log_filled (the +).
        navIconColor?.let { color ->
            listOf(
                "res/drawable/ic_popular_filled.xml",
                "res/drawable/ic_search_filled.xml",
                "res/drawable/ic_activity_filled.xml",
                "res/drawable/ic_profile_filled.xml",
            ).forEach { path ->
                document(path).use { doc ->
                    val paths = doc.getElementsByTagName("path")
                    if (paths.length == 0) throw PatchException("No <path> in $path")
                    for (i in 0 until paths.length) {
                        val el = paths.item(i) as Element
                        if (el.getAttribute("android:fillColor").isNotEmpty()) {
                            el.setAttribute("android:fillColor", color)
                        }
                    }
                }
            }
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
