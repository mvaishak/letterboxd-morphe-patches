package app.template.patches.letterboxd

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * `PopularViewModel.UIState.MainView.getVideoStoreHomeData()` is read in exactly one
 * place — `SignedInPopularFilmsFragment$onViewCreated$3$1`, which shows the "Video
 * Store" row on the Films tab only when the value is non-null. Returning null there
 * takes the branch that hides the row (`videoStoreProductsLayout` → GONE) without
 * touching the adapter, so nothing else about the Video Store changes.
 */
internal object VideoStoreHomeDataFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/ui/fragments/popular/PopularViewModel\$UIState\$MainView;",
    name = "getVideoStoreHomeData",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lcom/letterboxd/api/model/VideoStoreHomeResponse;",
    parameters = emptyList(),
)

@Suppress("unused")
val hideVideoStoreHomeSectionPatch = bytecodePatch(
    name = "Hide Video Store on home",
    description = "Removes the \"Letterboxd Video Store\" promo row from the Films tab. The Video " +
        "Store itself, its settings and every other entry point are left untouched. Can be toggled " +
        "from the \"Mod settings\" screen if that patch is also enabled.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    extendWith("extensions/extension.mpe")

    execute {
        // Return null (→ row hidden) only while Prefs says so. With the "Mod settings" patch off,
        // Prefs has no store and hideVideoStore() defaults to true, i.e. unchanged behaviour.
        VideoStoreHomeDataFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, Lapp/template/extension/settings/Prefs;->hideVideoStore()Z
                move-result v0
                if-eqz v0, :keep
                const/4 v0, 0x0
                return-object v0
                :keep
                nop
            """,
        )
    }
}
