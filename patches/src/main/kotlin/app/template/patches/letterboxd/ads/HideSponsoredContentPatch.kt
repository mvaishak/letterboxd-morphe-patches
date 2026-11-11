package app.template.patches.letterboxd.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import com.android.tools.smali.dexlib2.AccessFlags

internal object HomeFeaturedTrailerFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/ui/fragments/popular/PopularViewModel\$UIState\$MainView;",
    name = "getFeaturedTrailer",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/letterboxd/api/model/FeaturedContentItem\$FeaturedTrailer;",
    parameters = emptyList(),
)

/**
 * The Films home observer treats a null featured trailer as no sponsored placement and sets
 * its entire adComposeView to GONE. Taking that branch also skips the sponsored composable's
 * impression tracking, without changing regular film trailers or other home sections.
 */
@Suppress("unused")
val hideSponsoredContentPatch = bytecodePatch(
    name = "Hide sponsored content",
    description = "Hides Letterboxd's sponsored Spotlight / featured-trailer card on the Films " +
        "home tab, including its space. Separate from Google AdMob banners. On by default.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    execute {
        HomeFeaturedTrailerFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )
    }
}
