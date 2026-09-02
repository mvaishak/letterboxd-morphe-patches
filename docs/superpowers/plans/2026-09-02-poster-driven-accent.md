# Poster-driven Accent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an opt-in Letterboxd patch that recolours the "You've logged this film" bar and the ratings-histogram stars on a film page with a colour extracted from that film's poster.

**Architecture:** A Morphe **extension** (`PosterAccent`, plain Java, merged into the app as a DEX) holds a process-wide "current film accent" colour plus view-tinting helpers. A **bytecode patch** injects three calls: one in `FilmHeaderFragment.configurePoster` to hand the poster URL to the extension (which fetches it, runs `androidx.palette`, and publishes the colour), and one each at the top of `FilmRelationshipFragment.onViewCreated` and `FilmRatingsHistogramFragment.onViewCreated` to bind those views to the published colour. No resources are touched.

**Tech Stack:** Kotlin (patch), Java (extension), Morphe Patcher 1.11.0 (`bytecodePatch`, `Fingerprint`, `extendWith`, `InstructionExtensions`), `androidx.palette`, `androidx.core` `ColorUtils`, Android `View`/`ImageView`/`Palette` APIs, smali/dexlib2.

**Spec:** `docs/superpowers/specs/2026-09-02-poster-driven-accent-design.md`

## Global Constraints

- Target app: `com.letterboxd.letterboxd` **3.5.4 (496)**. Patch is pinned to this via `COMPATIBILITY_LETTERBOXD` (in `patches/src/main/kotlin/app/template/patches/shared/Constants.kt`).
- Patch is **opt-in**: `bytecodePatch(..., default = false)`.
- Extension package is **`app.template.extension`** (matches `extensions/extension/build.gradle.kts` `namespace`), so smali references it as `Lapp/template/extension/PosterAccent;`.
- Extension is **Java**, not Kotlin (the module has no Kotlin plugin; the removed example was `ExamplePatch.java`).
- Extension may only use `compileOnly` dependencies — everything it needs is already in the Letterboxd APK. Never bundle `androidx.*`.
- Build with `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` and `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`.
- Work on the **`dev`** branch. Conventional commits (`feat:` / `fix:` / `docs:` / `chore:`).
- No unit-test framework exists in this repo. "Verify" means: the Gradle build succeeds, the patch shows up in `patches-list.json` via `generatePatchesList`, and the named target methods exist in the decompiled app at `/Users/mvaishak/Downloads/letterboxd-decoded`. Runtime behaviour is verified manually on a device by the user (checklist in the spec).

## Colour math (used verbatim in Task 3)

- Swatch preference: `palette.getVibrantSwatch()` → `getLightVibrantSwatch()` → `getDominantSwatch()`; first non-null.
- Contrast guard: `double lum = ColorUtils.calculateLuminance(rgb);` if `lum < 0.20` set `rgb = ColorUtils.blendARGB(rgb, 0xFFFFFFFF, 0.55f);` recompute `lum`; if still `< 0.20` use `0xFF00E054` (Letterboxd green).
- Translucent bar fill: `ColorUtils.setAlphaComponent(accent, 0x38)` composited over the bar's current solid colour with `ColorUtils.compositeColors(translucentAccent, baseColor)` where `baseColor` is the bar's existing `ColorDrawable` colour or `0xFF14181C` if it has none.

---

## Task 1: Extension scaffold + dependencies

**Files:**
- Modify: `extensions/extension/build.gradle.kts`
- Create: `extensions/extension/src/main/java/app/template/extension/PosterAccent.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `app.template.extension.PosterAccent` with public static methods (bodies filled in Tasks 2–3):
    - `static void captureFrom(java.net.URL url)`
    - `static void bindLoggedBar(android.view.View fragmentRoot)`
    - `static void bindHistogram(android.view.View fragmentRoot)`

- [ ] **Step 1: Add compileOnly deps to the extension module**

Edit `extensions/extension/build.gradle.kts` to this exact content:

```kotlin
extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.template.extension"
}

dependencies {
    compileOnly("androidx.palette:palette:1.0.0")
    compileOnly("androidx.core:core:1.13.1")
    compileOnly("androidx.annotation:annotation:1.8.2")
}
```

- [ ] **Step 2: Create the extension class skeleton**

Create `extensions/extension/src/main/java/app/template/extension/PosterAccent.java`:

```java
package app.template.extension;

import android.view.View;
import java.net.URL;

/**
 * Runtime helper merged into Letterboxd by the "Poster-driven accent" patch.
 * Holds the current film page's poster-derived accent colour and tints a
 * curated set of views with it. All entry points are exception-safe: on any
 * failure the app keeps its original colours.
 */
public final class PosterAccent {

    private PosterAccent() {}

    /** Called from FilmHeaderFragment.configurePoster with the chosen poster URL. */
    public static void captureFrom(URL url) {
        // Task 2
    }

    /** Called from the top of FilmRelationshipFragment.onViewCreated. */
    public static void bindLoggedBar(View fragmentRoot) {
        // Task 3
    }

    /** Called from the top of FilmRatingsHistogramFragment.onViewCreated. */
    public static void bindHistogram(View fragmentRoot) {
        // Task 3
    }
}
```

- [ ] **Step 3: Build the extension**

Run:
```bash
cd /Users/mvaishak/Downloads/letterboxd-patches
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew :extensions:extension:assembleRelease --console=plain
```
Expected: `BUILD SUCCESSFUL`. If it fails to resolve `androidx.palette:palette:1.0.0`, confirm `google()` is in the plugin/dependency repositories (it is, via `settings.gradle.kts`).

- [ ] **Step 4: Commit**

```bash
git add extensions/extension/build.gradle.kts extensions/extension/src/main/java/app/template/extension/PosterAccent.java
git commit -m "feat: Scaffold PosterAccent extension for poster-driven accent

Claude-Session: https://claude.ai/code/session_01EipCEkd6H2BxUgVFpMTyvx"
```

---

## Task 2: Accent capture (URL → Palette → published colour)

**Files:**
- Modify: `extensions/extension/src/main/java/app/template/extension/PosterAccent.java`

**Interfaces:**
- Consumes: `PosterAccent` skeleton from Task 1.
- Produces (used by Task 3):
  - `static volatile Integer current` — the published accent ARGB, or `null` when none.
  - `static final java.util.List<Runnable> ...` — internal; not referenced elsewhere.
  - `static void addListener(Runnable r)` / `static void removeListener(Runnable r)`
  - `static void clear()` — sets `current = null` and notifies listeners.

- [ ] **Step 1: Implement capture + state**

Replace the whole file body of `PosterAccent.java` with:

```java
package app.template.extension;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PosterAccent {

    private static final int FALLBACK_GREEN = 0xFF00E054;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    /** Published accent ARGB for the film page currently on screen, or null. */
    public static volatile Integer current = null;

    private static volatile String loadingUrl = null;

    private PosterAccent() {}

    public static void addListener(Runnable r) {
        LISTENERS.addIfAbsent(r);
    }

    public static void removeListener(Runnable r) {
        LISTENERS.remove(r);
    }

    public static void clear() {
        current = null;
        loadingUrl = null;
        notifyListeners();
    }

    private static void notifyListeners() {
        MAIN.post(() -> {
            for (Runnable r : LISTENERS) {
                try {
                    r.run();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    /** Called from FilmHeaderFragment.configurePoster. url may be null. */
    public static void captureFrom(URL url) {
        try {
            clear();
            if (url == null) {
                return;
            }
            final String urlString = url.toString();
            loadingUrl = urlString;
            new Thread(() -> {
                Integer accent = null;
                try {
                    accent = deriveAccent(url);
                } catch (Throwable ignored) {
                }
                final Integer result = accent;
                MAIN.post(() -> {
                    // Ignore if the user has since navigated to a different film.
                    if (urlString.equals(loadingUrl)) {
                        current = result;
                        notifyListeners();
                    }
                });
            }, "PosterAccent").start();
        } catch (Throwable ignored) {
        }
    }

    private static Integer deriveAccent(URL url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setInstanceFollowRedirects(true);
        Bitmap bitmap;
        try (InputStream in = conn.getInputStream()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2; // posters are ~300px wide; half is plenty for Palette
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        } finally {
            conn.disconnect();
        }
        if (bitmap == null) {
            return null;
        }
        Palette palette = Palette.from(bitmap).clearFilters().generate();
        bitmap.recycle();

        Palette.Swatch swatch = palette.getVibrantSwatch();
        if (swatch == null) swatch = palette.getLightVibrantSwatch();
        if (swatch == null) swatch = palette.getDominantSwatch();
        if (swatch == null) {
            return FALLBACK_GREEN;
        }
        return guardContrast(swatch.getRgb());
    }

    private static int guardContrast(int rgb) {
        int argb = 0xFF000000 | (rgb & 0x00FFFFFF);
        double lum = ColorUtils.calculateLuminance(argb);
        if (lum < 0.20) {
            argb = ColorUtils.blendARGB(argb, 0xFFFFFFFF, 0.55f);
            lum = ColorUtils.calculateLuminance(argb);
        }
        if (lum < 0.20) {
            return FALLBACK_GREEN;
        }
        return argb;
    }

    public static void bindLoggedBar(View fragmentRoot) {
        // Task 3
    }

    public static void bindHistogram(View fragmentRoot) {
        // Task 3
    }
}
```

- [ ] **Step 2: Build the extension**

Run:
```bash
cd /Users/mvaishak/Downloads/letterboxd-patches
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew :extensions:extension:assembleRelease --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add extensions/extension/src/main/java/app/template/extension/PosterAccent.java
git commit -m "feat: Extract and publish a poster accent colour in PosterAccent

Claude-Session: https://claude.ai/code/session_01EipCEkd6H2BxUgVFpMTyvx"
```

---

## Task 3: View tinting (bindLoggedBar, bindHistogram)

**Files:**
- Modify: `extensions/extension/src/main/java/app/template/extension/PosterAccent.java`

**Interfaces:**
- Consumes: `current`, `addListener`, `removeListener` from Task 2.
- Produces: fully-implemented `bindLoggedBar(View)` and `bindHistogram(View)` (already declared in Task 1; still the only smali entry points besides `captureFrom`).

- [ ] **Step 1: Replace the two stub methods**

Add these two fields to the top of the class (next to `LISTENERS`):

```java
    private static final java.util.WeakHashMap<View, android.graphics.drawable.Drawable> ORIG_BG =
            new java.util.WeakHashMap<>();
    private static final java.util.WeakHashMap<android.widget.ImageView, Boolean> STAR_TINTED =
            new java.util.WeakHashMap<>();
```

In `PosterAccent.java`, replace the two `// Task 3` stub methods with:

```java
    public static void bindLoggedBar(View fragmentRoot) {
        try {
            final View bar = firstColouredViewGroup(fragmentRoot);
            if (bar == null) return;
            bindView(bar, () -> tintTranslucentBackground(bar));
        } catch (Throwable ignored) {
        }
    }

    public static void bindHistogram(View fragmentRoot) {
        try {
            bindView(fragmentRoot, () -> tintStars(fragmentRoot));
        } catch (Throwable ignored) {
        }
    }

    /** Runs apply now and on every accent change while `anchor` is attached. */
    private static void bindView(View anchor, Runnable apply) {
        final Runnable listener = () -> {
            try {
                apply.run();
            } catch (Throwable ignored) {
            }
        };
        addListener(listener);
        listener.run();
        anchor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) {
                removeListener(listener);
                v.removeOnAttachStateChangeListener(this);
            }
        });
    }

    /** The film-relationship bar: a ConstraintLayout child of the fragment root. */
    private static View firstColouredViewGroup(View root) {
        if (!(root instanceof android.view.ViewGroup)) return null;
        android.view.ViewGroup vg = (android.view.ViewGroup) root;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View c = vg.getChildAt(i);
            if (c instanceof androidx.constraintlayout.widget.ConstraintLayout) {
                return c;
            }
        }
        for (int i = 0; i < vg.getChildCount(); i++) {
            View found = firstColouredViewGroup(vg.getChildAt(i));
            if (found != null) return found;
        }
        // Fall back to the first child ViewGroup that actually has a background.
        for (int i = 0; i < vg.getChildCount(); i++) {
            View c = vg.getChildAt(i);
            if (c instanceof android.view.ViewGroup && c.getBackground() != null) return c;
        }
        return null;
    }

    private static void tintTranslucentBackground(View bar) {
        Integer accent = current;
        if (accent == null) {
            if (ORIG_BG.containsKey(bar)) {
                bar.setBackground(ORIG_BG.remove(bar));
            }
            return;
        }
        if (!ORIG_BG.containsKey(bar)) {
            ORIG_BG.put(bar, bar.getBackground()); // may be null; that's fine
        }
        int base = 0xFF14181C;
        android.graphics.drawable.Drawable bg = ORIG_BG.get(bar);
        if (bg instanceof android.graphics.drawable.ColorDrawable) {
            base = ((android.graphics.drawable.ColorDrawable) bg).getColor();
        }
        int wash = ColorUtils.compositeColors(ColorUtils.setAlphaComponent(accent, 0x38), base);
        bar.setBackgroundColor(wash);
    }

    private static void tintStars(View histogramRoot) {
        applyStarFilter(histogramRoot, current);
    }

    private static void applyStarFilter(View v, Integer accent) {
        if (v instanceof android.widget.ImageView) {
            android.widget.ImageView iv = (android.widget.ImageView) v;
            boolean wasTinted = Boolean.TRUE.equals(STAR_TINTED.get(iv));
            if (accent == null) {
                if (wasTinted) {
                    iv.clearColorFilter();
                    STAR_TINTED.remove(iv);
                }
            } else if (wasTinted || isSmallSquare(iv)) {
                iv.setColorFilter(accent, android.graphics.PorterDuff.Mode.SRC_IN);
                STAR_TINTED.put(iv, Boolean.TRUE);
            }
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyStarFilter(vg.getChildAt(i), accent);
            }
        }
    }

    /** Heuristic: histogram stars are tiny (<= ~24dp) roughly-square ImageViews. */
    private static boolean isSmallSquare(android.widget.ImageView iv) {
        int w = iv.getWidth(), h = iv.getHeight();
        if (w == 0 || h == 0) return true; // not laid out yet — allow, filter is idempotent
        float density = iv.getResources().getDisplayMetrics().density;
        return w <= 28 * density && h <= 28 * density && Math.abs(w - h) <= 6 * density;
    }
```

Note: `androidx.constraintlayout.widget.ConstraintLayout` and `androidx.core.graphics.ColorUtils` are on the app classpath. Add `compileOnly("androidx.constraintlayout:constraintlayout:2.1.4")` to `extensions/extension/build.gradle.kts` `dependencies { }` for this task.

- [ ] **Step 2: Build the extension**

Run:
```bash
cd /Users/mvaishak/Downloads/letterboxd-patches
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew :extensions:extension:assembleRelease --console=plain
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add extensions/extension/build.gradle.kts extensions/extension/src/main/java/app/template/extension/PosterAccent.java
git commit -m "feat: Tint the logged bar and histogram stars from the poster accent

Claude-Session: https://claude.ai/code/session_01EipCEkd6H2BxUgVFpMTyvx"
```

---

## Task 4: Bytecode patch (fingerprints + injections)

**Files:**
- Create: `patches/src/main/kotlin/app/template/patches/letterboxd/PosterDrivenAccentPatch.kt`
- Create: `patches/src/main/kotlin/app/template/patches/letterboxd/PosterAccentFingerprints.kt`

**Interfaces:**
- Consumes: `app.template.extension.PosterAccent` (from the `.mpe`, referenced as smali `Lapp/template/extension/PosterAccent;`); `COMPATIBILITY_LETTERBOXD` from `Constants.kt`.
- Produces: `val posterDrivenAccentPatch` — a `bytecodePatch` registered in the bundle.

- [ ] **Step 1: Pre-check the three target methods still exist**

Run:
```bash
cd /Users/mvaishak/Downloads/letterboxd-decoded
grep -n 'configurePoster(Lcom/letterboxd/api/model/Film;Lcom/letterboxd/letterboxd/databinding/FragmentFilmHeaderBinding;)V' smali_classes7/com/letterboxd/letterboxd/ui/fragments/film/FilmHeaderFragment.smali
grep -n 'ImageSize;->getUrl()Ljava/net/URL;' smali_classes7/com/letterboxd/letterboxd/ui/fragments/film/FilmHeaderFragment.smali
grep -n '\.method public onViewCreated' smali_classes2/com/letterboxd/letterboxd/ui/fragments/film/FilmRelationshipFragment.smali
grep -rn '\.method public onViewCreated' smali*/com/letterboxd/letterboxd/ui/fragments/film/FilmRatingsHistogramFragment.smali
```
Expected: each prints a match. If `configurePoster` has changed signature, update the fingerprint in Step 2 accordingly (still: name `configurePoster`, defining class `FilmHeaderFragment`, contains a call to `Lcom/letterboxd/api/model/ImageSize;->getUrl()Ljava/net/URL;` followed by `move-result-object`).

- [ ] **Step 2: Write the fingerprints file**

Create `patches/src/main/kotlin/app/template/patches/letterboxd/PosterAccentFingerprints.kt`:

```kotlin
package app.template.patches.letterboxd

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/** FilmHeaderFragment.configurePoster(Film, FragmentFilmHeaderBinding) — capture the poster URL. */
internal object ConfigurePosterFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/ui/fragments/film/FilmHeaderFragment;",
    name = "configurePoster",
    returnType = "V",
    parameters = listOf(
        "Lcom/letterboxd/api/model/Film;",
        "Lcom/letterboxd/letterboxd/databinding/FragmentFilmHeaderBinding;",
    ),
    filters = listOf(
        // ImageSize.getUrl() -> URL
        methodCall(
            definingClass = "Lcom/letterboxd/api/model/ImageSize;",
            name = "getUrl",
            returnType = "Ljava/net/URL;",
        ),
        // its move-result-object holds the URL register we hand to the extension
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
    ),
)

/** FilmRelationshipFragment.onViewCreated — the "You've logged this film" bar. */
internal object FilmRelationshipOnViewCreatedFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/ui/fragments/film/FilmRelationshipFragment;",
    name = "onViewCreated",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
)

/** FilmRatingsHistogramFragment.onViewCreated — the ratings histogram stars. */
internal object FilmRatingsHistogramOnViewCreatedFingerprint : Fingerprint(
    definingClass = "Lcom/letterboxd/letterboxd/ui/fragments/film/FilmRatingsHistogramFragment;",
    name = "onViewCreated",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
)
```

If `methodCall` / `opcode` / `MatchAfterImmediately` do not resolve, check imports against `patches/src/main/kotlin/app/template/patches/example/Fingerprints.kt` history (`git show e3983ac:patches/src/main/kotlin/app/template/patches/example/Fingerprints.kt`) — that file used exactly these APIs.

- [ ] **Step 3: Write the patch**

Create `patches/src/main/kotlin/app/template/patches/letterboxd/PosterDrivenAccentPatch.kt`:

```kotlin
package app.template.patches.letterboxd

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_LETTERBOXD
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/template/extension/PosterAccent;"

@Suppress("unused")
val posterDrivenAccentPatch = bytecodePatch(
    name = "Poster-driven accent",
    description = "On a film page, tints the \"You've logged this film\" bar and the ratings " +
        "histogram stars with a colour taken from that film's poster. Experimental; other accents " +
        "on the page are unchanged.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_LETTERBOXD)

    extendWith("extensions/extension.mpe")

    execute {
        // 1. Capture: hand the poster URL to the extension, right after
        //    ImageSize.getUrl() resolves it.
        ConfigurePosterFingerprint.let { fp ->
            val match = fp.match ?: error("ConfigurePosterFingerprint did not match")
            val moveResultIndex = match.instructionMatches[1].index
            val urlRegister = fp.method
                .getInstruction<OneRegisterInstruction>(moveResultIndex).registerA
            fp.method.addInstructions(
                moveResultIndex + 1,
                "invoke-static { v$urlRegister }, $EXTENSION_CLASS->captureFrom(Ljava/net/URL;)V",
            )
        }

        // 2. Apply: bind the logged bar.
        FilmRelationshipOnViewCreatedFingerprint.method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->bindLoggedBar(Landroid/view/View;)V",
        )

        // 3. Apply: bind the histogram stars.
        FilmRatingsHistogramOnViewCreatedFingerprint.method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->bindHistogram(Landroid/view/View;)V",
        )
    }
}
```

If `fp.match`/`instructionMatches` are named differently, consult the jar:
`javap -classpath <morphe-patcher-1.11.0.jar> -p app.morphe.patcher.Fingerprint app.morphe.patcher.Match`
(`Match.getMethod()` and `Match.getInstructionMatches()` are confirmed to exist; the Kotlin property forms are `match.method` / `match.instructionMatches`). `Fingerprint.method` is used the same way as in the old `examplePatch`.

If `invoke-static { p1 }` fails to assemble because `onViewCreated`'s `.locals` pushes `p1` past `v15`, change those two injections to `invoke-static/range { p1 .. p1 }, ...`.

- [ ] **Step 4: Build the whole bundle**

Run:
```bash
cd /Users/mvaishak/Downloads/letterboxd-patches
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew clean buildAndroid :patches:generatePatchesList --console=plain
```
Expected: `BUILD SUCCESSFUL`, and `patches/build/libs/patches-*.mpp` exists.

- [ ] **Step 5: Verify the patch registered**

Run:
```bash
cd /Users/mvaishak/Downloads/letterboxd-patches
python3 -c "import json;d=json.load(open('patches-list.json'));print([p['name'] for p in d['patches']])"
git checkout -- patches-list.json
```
Expected: the list includes `'Poster-driven accent'`.

- [ ] **Step 6: Commit**

```bash
git add patches/src/main/kotlin/app/template/patches/letterboxd/PosterDrivenAccentPatch.kt \
        patches/src/main/kotlin/app/template/patches/letterboxd/PosterAccentFingerprints.kt
git commit -m "feat: Add Poster-driven accent bytecode patch

Claude-Session: https://claude.ai/code/session_01EipCEkd6H2BxUgVFpMTyvx"
```

---

## Task 5: Document the patch

**Files:**
- Modify: `README.md` (the `## ✨ What the patches do` section)

**Interfaces:**
- Consumes: nothing.
- Produces: nothing (docs only).

- [ ] **Step 1: Add a subsection**

In `README.md`, after the `### 🚫 Hide Video Store on home` subsection and before `## 📲 How to use`, add:

```markdown
### 🎨 Poster-driven accent  ·  _opt-in, experimental_

On a film page, pulls a vibrant colour from that film's poster and uses it for
the "You've logged this film" bar (a soft translucent wash) and the ratings
histogram's end stars (instead of Letterboxd green). If the poster colour is too
dark to read against the dark UI it's lightened, and it falls back to green if
there's still no usable colour.

Only those two elements change in this version. It's the first patch here that
runs code at runtime (a small merged helper), so treat it as experimental and
report anything odd.
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: Describe the Poster-driven accent patch

Claude-Session: https://claude.ai/code/session_01EipCEkd6H2BxUgVFpMTyvx"
```

---

## Task 6: Ship a pre-release and verify on device

**Files:** none (release + manual QA).

- [ ] **Step 1: Push and let CI build the pre-release**

```bash
cd /Users/mvaishak/Downloads/letterboxd-patches
git push origin dev
```
Wait for the `Release` action to publish `vX.Y.0-dev.1` with `patches-*.mpp`.

- [ ] **Step 2: Manual device checklist (from the spec)**

On a clean Letterboxd 3.5.4 APK with only **Poster-driven accent** enabled:

1. Film with a strongly-coloured poster → logged bar shows a faint colour wash; histogram `1★`/`5★` and star row are that colour, not green.
2. Film with a muted/grey poster → sensible fallback (light greyish or green); nothing invisible; no crash.
3. Open film A then film B fast, before A's poster loads → B never shows A's colour; worst case B is briefly green then updates.
4. Scroll / rotate / background+foreground the film page → no flicker to stale colours.
5. A film you have **not** logged → relationship bar absent, no crash, histogram still accents.

- [ ] **Step 3: If all pass, merge to main for a stable release**

```bash
cd /Users/mvaishak/Downloads/letterboxd-patches
git checkout main && git pull --ff-only origin main
git merge --no-ff dev -m "chore: Merge branch \`dev\` to \`main\` — poster-driven accent

https://claude.ai/code/session_01EipCEkd6H2BxUgVFpMTyvx"
git push origin main
```
