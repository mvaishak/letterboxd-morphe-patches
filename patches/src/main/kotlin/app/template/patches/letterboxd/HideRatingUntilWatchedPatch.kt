package app.template.patches.letterboxd

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * `FilmRatingsHistogramFragment` hosts the community-ratings section on a film page; its root
 * view is `@id/ratingsViewWrapper`, and it can reach `FilmViewModel` (which exposes the viewing
 * relationship). The extension attaches a layout listener that hides the rating content until the
 * film is marked watched, replacing it with the chosen reveal style.
 */
internal object HistogramOnViewCreatedFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/ui/fragments/film/FilmRatingsHistogramFragment;",
    name = "onViewCreated",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
)

@Suppress("unused")
val hideRatingUntilWatchedPatch = bytecodePatch(
    name = "Hide ratings until watched",
    description = "Hides the community rating (average + histogram) on a film's page until you have " +
        "marked that film as watched, covering it with a tap-to-reveal control. The reveal is per " +
        "visit — leave the film and come back and it is hidden again. Only the film page is " +
        "affected; ratings shown in lists, search and elsewhere are unchanged.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    extendWith("extensions/extension.mpe")

    val revealStyle by stringOption(
        key = "revealStyle",
        default = "panel",
        values = mapOf(
            "Frosted panel" to "panel",
            "Tap-to-show link" to "link",
            "Shimmer (Telegram-style)" to "shimmer",
            "Tap to burst" to "burst",
        ),
        title = "Reveal style",
        description = "How the hidden rating is covered. 'Frosted panel' is an opaque panel with a " +
            "label; 'Tap-to-show link' is a plain text link under the section title; 'Shimmer' is a " +
            "continuously animating particle field; 'Tap to burst' is a static particle field that " +
            "scatters when tapped.",
    )

    execute {
        val style = revealStyle ?: "panel"
        HistogramOnViewCreatedFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "$style"
                invoke-static { p0, v0 }, Lapp/template/extension/HideRatingUntilWatched;->enforce(Landroidx/fragment/app/Fragment;Ljava/lang/String;)V
            """,
        )
    }
}
