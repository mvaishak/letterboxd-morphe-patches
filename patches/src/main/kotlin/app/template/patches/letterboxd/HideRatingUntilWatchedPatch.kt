package app.template.patches.letterboxd

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * `FilmRatingsHistogramFragment` hosts the community-ratings section on a film page; its root
 * view is `@id/ratingsViewWrapper`, and it can reach `FilmViewModel` (which exposes the viewing
 * relationship). The extension attaches a layout listener that hides the section until the film
 * is marked watched.
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
        "marked that film as watched. Only the film page is affected — ratings shown in lists, " +
        "search and elsewhere are unchanged.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    extendWith("extensions/extension.mpe")

    execute {
        HistogramOnViewCreatedFingerprint.method.addInstructions(
            0,
            "invoke-static { p0 }, Lapp/template/extension/HideRatingUntilWatched;->enforce(Landroidx/fragment/app/Fragment;)V",
        )
    }
}
