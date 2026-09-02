# Poster-driven accent — design

**Status:** approved design, pending implementation plan
**Date:** 2026-09-02
**Target:** Letterboxd `com.letterboxd.letterboxd` 3.5.4 (496)

## Goal

On a film page, derive an accent colour from that film's poster and apply it to
two elements that are otherwise a fixed grey/green:

1. The **"You've logged this film"** bar (`FilmRelationshipFragment`) — its
   background takes a **translucent** tint of the accent (~22% alpha over the
   existing dark surface); text, avatar and stars stay untouched and readable.
2. The **ratings histogram end stars** (`FilmRatingsHistogramFragment`) — the
   `1★` / `5★` icons and the `RatingView` star row switch from Letterboxd green
   to the accent. The grey distribution bars are **not** touched.

Nothing else on the film page changes in this version. The toolbar, the Compose
call-to-actions slot, buttons, links and section headers are out of scope.

This ships as one **opt-in** patch (`default = false`), separate from the
resource theming patches.

## Why it needs an extension

Letterboxd has no per-film colour concept. The green comes from the global
`@color/colorAccent` and the grey from `@color/colorPrimary`; both are ordinary
resources with no runtime override hook. `androidx.palette` is bundled in the
APK but unused by Letterboxd. So the accent must be computed and pushed onto
specific views at runtime — that is runtime code, i.e. a Morphe **extension**
(`.mpe`) merged into the app, driven by **bytecode patches** that call into it.

## Components

### 1. Extension — `app.template.extension.PosterAccent`

Lives in the repo's existing `extensions/extension/` module (namespace
`app.template.extension`), so injected smali references it as
`Lapp/template/extension/PosterAccent;`.

Responsibilities:

- **State.** A process-wide holder for the *current film page's* accent:
  `@Volatile Integer current` (null = no accent / not on a film page) plus a
  small `CopyOnWriteArrayList<Consumer<Integer>>` of listeners.
- **`captureFrom(Context, String posterUrl)`** — enqueues a Coil `ImageRequest`
  for `posterUrl` with `allowHardware(false)`; on success, converts the drawable
  to a bitmap, runs `Palette.from(bitmap).clearFilters().generate()`, picks a
  swatch (see *Colour choice*), stores it in `current`, and notifies listeners
  on the main thread. Coil's memory cache means this usually resolves without a
  network hit because the same poster is already being loaded by the page.
- **`observe(View anchor, Consumer<Integer> cb)`** — registers `cb`, invokes it
  immediately if `current != null`, and auto-unregisters when `anchor` detaches
  from the window (`View.addOnAttachStateChangeListener`). No `LifecycleOwner`
  dependency, so it is safe to call from any patched fragment.
- **`clear()`** — sets `current = null`, notifies listeners with `null` so views
  can restore their original colour, and is called when the film page goes away.
- **`bindLoggedBar(View fragmentRoot)`** — the entry point the relationship-bar
  patch injects. Finds the coloured bar (the `ConstraintLayout` child of the
  fragment root — it has no id, so `((ViewGroup) fragmentRoot).getChildAt(1)`,
  guarded by a `ConstraintLayout` instanceof plus a fallback descendant scan for
  the first `ViewGroup` whose background is a `ColorDrawable`/shape) and calls
  `observe(bar, c -> tintTranslucentBackground(bar, c))`.
- **`bindHistogram(View fragmentRoot)`** — the entry point the histogram patch
  injects. Calls `observe(fragmentRoot, c -> tintStars(fragmentRoot, c))`.
- **Tint helpers** (pure, no state), called by the two `bind*` methods:
  - `tintTranslucentBackground(View bar, Integer accent)` — on non-null,
    composites `accent` at alpha `0x38` (~22%) over the view's current solid
    background colour (read from its `ColorDrawable`; fall back to `#00000000`)
    and calls `bar.setBackgroundColor(...)`, after stashing the original
    `Drawable` in a view tag. On `null`, restores the stashed background.
  - `tintStars(View ratingRoot, Integer accent)` — on non-null, applies
    `ImageView.setColorFilter(accent, SRC_IN)` to `R.id.oneStarRating` and
    `RatingView.setStarColor(accent)` to `R.id.ratingView` (RatingView setter
    presence to be confirmed in the plan; else a colour filter on its star
    `ImageView` children). On `null`, clears the filters / restores.

Palette is on the app classpath already; the extension must **not** bundle its
own copy — reference `androidx.palette.graphics.Palette` as `provided`.

### 2. Bytecode patch — `posterDrivenAccentPatch`

`bytecodePatch(name = "Poster-driven accent", default = false)`, `compatibleWith(COMPATIBILITY_LETTERBOXD)`, `extendWith("extensions/extension.mpe")`.

Three fingerprints, three injections:

**a. Capture — `FilmHeaderFragmentAccentFingerprint`**
Match a method in
`com.letterboxd.letterboxd.ui.fragments.film.FilmHeaderFragment` that runs once
per film with the film model in hand and a `Context` available — the method that
binds the header (sets title / loads the poster). Inject, after the poster URL
is known:
```
invoke-static {<context>, <posterUrlString>}, Lapp/template/extension/PosterAccent;->captureFrom(Landroid/content/Context;Ljava/lang/String;)V
```
The exact method + register plumbing (how to obtain the `Context` and the
poster/backdrop URL string at that point) is a plan task. Preference order for
the source image: poster URL → backdrop URL → give up (leave `current` null).

**b. Apply — `FilmRelationshipOnViewCreatedFingerprint`**
`FilmRelationshipFragment.onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V`.
Inject at the end of the method:
```
invoke-static {p1}, Lapp/template/extension/PosterAccent;->bindLoggedBar(Landroid/view/View;)V
```
`bindLoggedBar(root)` finds the coloured bar (the `ConstraintLayout` child of
the fragment root — it has no id, so `((ViewGroup) root).getChildAt(1)`, guarded
by a `ConstraintLayout` instanceof + fallback scan) and calls
`observe(bar, c -> tintTranslucentBackground(bar, c))`.

**c. Apply — `FilmRatingsHistogramOnViewCreatedFingerprint`**
`FilmRatingsHistogramFragment.onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V`.
Inject at the end:
```
invoke-static {p1}, Lapp/template/extension/PosterAccent;->bindHistogram(Landroid/view/View;)V
```
`bindHistogram(root)` calls `observe(root, c -> tintStars(root, c))`.

**d. Lifecycle — clear on new film**
No separate fingerprint. `captureFrom` begins with `clear()` (`current = null`,
notify listeners with `null` so views restore), then loads asynchronously. This
covers the common flow — opening film B runs its `configurePoster`, which clears
A's accent before B's resolves. Views that outlive their film page are cleaned
up by `bindView`'s attach-state listener, which unregisters on detach.
(There is no `FilmActivity` class to hook; the page is a `FilmFragment`.)

## Colour choice

`Palette` swatch selection, in order, first non-null wins:
1. `vibrantSwatch`
2. `lightVibrantSwatch`
3. `dominantSwatch`

Then a **contrast guard** against the dark UI: if the chosen colour's relative
luminance is below `0.20`, blend it 55% toward white; if it is still below
`0.20`, fall back to Letterboxd green (`0xFF00E054`) so the UI never renders an
invisible accent. The blended colour is what both apply sites receive.

No user option in this version — vibrant-with-guard only. A `stringOption`
(`vibrant` / `muted` / `dominant`) can be added later without touching the
patch structure.

## Failure behaviour

- Any of the three apply fingerprints failing to match ⇒ the patch fails at
  patch time with the fingerprint name in the trace (standard Morphe behaviour).
  Acceptable: this patch is opt-in and pinned to 3.5.4.
- `captureFrom` failing at runtime (bad URL, Coil error, Palette returns
  nothing) ⇒ `current` stays null, listeners keep the original colours, no
  crash. All extension entry points are wrapped in `try/catch` that logs and
  returns.
- `getGraphView`/`RatingView.setStarColor` absent ⇒ `tintStars` catches and
  no-ops that element; the other element still works.

## Out of scope (candidates for later versions)

- The distribution bars in `FilmRatingsGraphView` (needs a colour setter added
  to that custom view).
- The Compose `film_call_to_actions_compose` slot.
- Toolbar / nav icons, the `TRAILER` button, cast & crew links, section headers.
- A per-film cache so re-opening a film is instant (currently re-derived each
  visit; cheap because of Coil's cache).
- User-selectable swatch type.

## Testing

No unit harness in this repo; verification is manual on device against a clean
Letterboxd 3.5.4 APK:

1. Open a film with a strongly coloured poster (e.g. a red/orange one). The
   "You've logged this film" bar shows a faint coloured wash; the histogram
   `1★`/`5★` and the star row are that colour, not green.
2. Open a film with a muted/grey poster. Accent falls back sensibly (light
   greyish tint or green), nothing invisible, no crash.
3. Rapidly open film A then film B before A's poster loads — B never shows A's
   accent; worst case B is briefly green/grey then updates.
4. Scroll the film page, rotate, background/foreground the app — no flicker to
   stale colours, no leaked listeners (the attach-state cleanup covers this).
5. A film you have **not** logged — the relationship bar is absent; no crash,
   histogram still accents.
