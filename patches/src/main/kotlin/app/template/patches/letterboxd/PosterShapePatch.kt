package app.template.patches.letterboxd

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import org.w3c.dom.Element

@Suppress("unused")
val posterShapePatch = resourcePatch(
    name = "Poster corner shape",
    description = "Changes the corner radius of film posters across the app. 'Sharp' matches " +
        "Letterboxd's classic look; the rounded options soften every poster in grids, lists and " +
        "on film pages.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    val cornerRadius by intOption(
        key = "cornerRadius",
        default = 0,
        values = mapOf(
            "Sharp" to 0,
            "Slight (4dp)" to 4,
            "Rounded (8dp)" to 8,
            "Very rounded (16dp)" to 16,
        ),
        title = "Poster corner radius",
        description = "Corner radius applied to poster shapes, in dp.",
    )

    execute {
        val radius = "${cornerRadius ?: 0}.0dp"

        // The two shape drawables every poster ImageView is drawn on / masked with.
        listOf("res/drawable/poster_background.xml", "res/drawable/poster_border.xml").forEach { path ->
            document(path).use { document ->
                val corners = document.getElementsByTagName("corners")
                if (corners.length == 0) throw PatchException("No <corners> element in $path")
                (corners.item(0) as Element).setAttribute("android:radius", radius)
            }
        }

        // Keep the list-summary poster (rounded outer edge) consistent.
        document("res/values/dimens.xml").use { document ->
            val dimens = document.getElementsByTagName("dimen")
            val target = (0 until dimens.length)
                .map { dimens.item(it) as Element }
                .firstOrNull { it.getAttribute("name") == "list_summary_poster_corner_radius" }
                ?: throw PatchException("dimen list_summary_poster_corner_radius not found in res/values/dimens.xml")
            target.textContent = radius
        }
    }
}
