# Letterboxd Morphe Patches

<div align="center">

Patches for the **Letterboxd** Android app (`com.letterboxd.letterboxd`),
built for [Morphe](https://morphe.software).

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0)
[![Built for Morphe](https://img.shields.io/badge/Built%20for-Morphe-1E5AA8?style=flat-square)](https://morphe.software)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)](https://android.com)


</div>

---

## About

A personal collection of patches for Letterboxd on Android — theming, layout
tweaks, and a handful of small conveniences. No unlocking, no ad or tracking
work, and nothing that talks to a new server: the two patches that open a
streaming app just hand off to whatever's already on your phone.

Almost everything is opt-in and controlled from one in-app **Letterboxd Mods**
screen rather than by re-patching — pick your patches once, then change your
mind about styling, ratings behaviour, or which sections show up, any time,
from inside the app.

Not affiliated with Letterboxd or the Morphe project.

---

## The Mods screen

With the **Mod settings** patch enabled (on by default), long-press the
**settings gear on your profile tab** to open **Letterboxd Mods**. A one-time
note also points this out the first time you launch after patching. Some
changes apply immediately; others need a restart — you're always prompted
either way.

### Theme

- **Pure black (OLED)** — true-black surfaces; elevated bits (cards, sheets)
  stay a faint grey so the ratings histogram and similar bars don't
  disappear. Needs Android 12+, and is locked off while the separate
  **Material You theme** patch is applied (see below) — the two systems
  would otherwise fight over the same colours; tap the row to see why.
- **Match bottom nav to top bar** — paints the bottom navigation bar black to
  match the top bar, instead of the default slate.
- **Accent colour** — a colour picker covering the stars, rating indicators,
  and the selected bottom-nav icon:
  - Nine presets (Letterboxd green, amber, orange, coral, pink, violet, blue,
    teal, mono), plus a full HSV/hex picker for anything else.
  - **Material You** (Android 12+, only offered once the separate **Material
    You theme** patch is applied): your device's own wallpaper-derived
    palette, as up to three distinct tones — Material You, Material You 2,
    Material You 3 — matching the tone Android itself uses for accents in
    dark contexts elsewhere on your phone (Quick Settings, toggles, the
    notification shade). Selected automatically as the default once available.
- **Bottom nav selected style** — *Stock*, *No pill*, *No pill + white icon*,
  *No pill + accent icon*, or *Accent pill*. The green **+** is never touched.

### Home

- **Hide Video Store** — removes the "Letterboxd Video Store" promo row from
  the Films tab. The Video Store itself and every other entry point are left
  alone. Off by default.
- **Hide Where to Watch** — removes the "Where to watch" section from a
  film's page. Off by default.

### Streaming

- **Open in player** — adds a small icon-only button beside Trailer on a
  film's page that opens the film directly in **Stremio** or **Nuvio**
  (your choice, in a second setting that appears once this is on). Off by
  default. The film's IMDb ID is read from Letterboxd's own data for the
  page you're on — nothing is looked up over the network by this patch.

### Ratings

- **Hide ratings until watched** — covers a film's community rating (average
  + histogram) until you've marked it watched. The reveal lasts for the
  current visit — leave the film and come back and it's hidden again. Only
  the film page is affected; ratings in lists, search, and "similar films"
  are unchanged. If the watched state can't be read for any reason, this
  fails open (ratings stay visible) rather than getting stuck hidden. On by
  default.
  - **Cover** — what the rating looks like while hidden: *Frosted panel* (an
    opaque panel with an eye glyph, default), *Tap-to-show link* (a plain
    text link under the section title, no cover animation), *Shimmer* (a
    continuously animating particle field), or *Tap to burst* (a static
    particle field).
  - **Reveal animation** — how tapping a cover disappears, independent of
    which one you picked (has no effect on *Tap-to-show link*, which has no
    cover to animate): *Pop* (that cover's own plain reveal, default),
    *Crumble* (dissolves in a staggered grid of shrinking, fading blocks),
    or *Confetti* (a real particle burst that flies out from the rating and
    falls, rendered as a brief full-screen overlay so it has room to move).
  - **Confetti color** — only shown once Confetti is selected: *Accent*
    (your current accent colour, plus lighter/darker tones), *Letterboxd
    colors* (the brand's orange/green/blue, default), or *Classic red*
    (a red/gold party-confetti palette).
  - **Reveal haptic feedback** — a short vibration when the rating is
    revealed, on any cover. On by default.

---

## Screenshots

Some of these predate the Mods-screen consolidation below and show a couple
of these as separate re-patch-time options rather than in-app toggles — the
visuals themselves are unchanged either way.

### Material You — surface style

The two "Wallpaper tint" columns are two different device wallpapers — the dark
chrome tracks whatever palette Android hands it.

| | Wallpaper tint A | Wallpaper tint B | Pure black (OLED) |
| :--- | :---: | :---: | :---: |
| **Film page** | <img src="docs/screenshots/materialyou-wallpaper-film.jpg" width="200"> | <img src="docs/screenshots/materialyou-wallpaper2-film.jpg" width="200"> | <img src="docs/screenshots/materialyou-oled-film.jpg" width="200"> |
| **Home** | <img src="docs/screenshots/materialyou-wallpaper-home.jpg" width="200"> | <img src="docs/screenshots/materialyou-wallpaper2-home.jpg" width="200"> | <img src="docs/screenshots/materialyou-oled-home.jpg" width="200"> |

### Accent colour

Same film page, OLED surface — a preset swatch or any hex. Visible on the ratings
histogram and the selected bottom-nav tab.

| Letterboxd green | Amber | Blue |
| :---: | :---: | :---: |
| <img src="docs/screenshots/accent-green.jpg" width="220"> | <img src="docs/screenshots/accent-amber.jpg" width="220"> | <img src="docs/screenshots/accent-blue.jpg" width="220"> |

### Bottom nav selected style

The green **+** button is untouched in every mode.

| Stock — grey pill, blue icon | No pill, white icon |
| :---: | :---: |
| <img src="docs/screenshots/bottomnav-stock.png" width="320"> | <img src="docs/screenshots/bottomnav-nopill-white.png" width="320"> |

| No pill, accent icon | Accent pill |
| :---: | :---: |
| <img src="docs/screenshots/bottomnav-nopill-accent.png" width="320"> | <img src="docs/screenshots/bottomnav-accent-pill.png" width="320"> |

### Denser poster grid

| Cozy | Dense |
| :---: | :---: |
| <img src="docs/screenshots/grid-default.jpg" width="220"> | <img src="docs/screenshots/grid-dense.jpg" width="220"> |

### Hide Video Store on home

| Before | After |
| :---: | :---: |
| <img src="docs/screenshots/videostore-before.jpg" width="220"> | <img src="docs/screenshots/videostore-after.jpg" width="220"> |

### Hide ratings until watched — cover

The film's community rating is fully covered until you tap.

| Shimmer | Frosted panel | Tap to burst | Tap-to-show link |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/spoiler-shimmer-updated.gif" width="220"> | <img src="docs/screenshots/spoiler-panel.jpg" width="220"> | <img src="docs/screenshots/spoiler-burst.jpg" width="220"> | <img src="docs/screenshots/spoiler-link.jpg" width="220"> |

Crumble and Confetti are reveal *animations* layered on top of these covers
(shown when you tap) rather than covers of their own — no static shot really
captures a burst of confetti flying off the screen.

---

## Install

1. On the device, open this link to add the source in Morphe Manager:
   <https://morphe.software/add-source?github=mvaishak/letterboxd-morphe-patches>
2. In the source settings, enable **pre-releases** for the newest (`dev`) builds,
   or leave it off for stable releases only.
3. Load a clean, unpatched Letterboxd APK from
   [APKMirror](https://www.apkmirror.com/apk/letterboxd/) or
   [Uptodown](https://letterboxd.en.uptodown.com/android) — not a file another
   tool has already patched or re-zipped.
4. Select the patches you want and patch.

> **Opening the Mods screen.** With the **Mod settings** patch enabled, almost
> everything configurable lives in an in-app screen — reach it by
> **long-pressing the settings gear on your profile tab**. A one-time note
> explaining this also shows on first launch after patching.

Any Letterboxd version should work — Morphe will warn you if something else
about your APK doesn't match (signature, etc.).

---

## Patches

<!-- PATCHES_START EXPANDED -->
> **[v1.6.0-dev.1](https://github.com/mvaishak/letterboxd-morphe-patches/releases/tag/v1.6.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;8 patches total
<details open>
<summary>Letterboxd&nbsp;&nbsp;•&nbsp;&nbsp;8 patches</summary>
<br>

**Supported versions:**

| 3.5.4 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Appearance](#appearance) | In-app appearance controls, adjustable from the Letterboxd Mods screen without re-patching: a true-black OLED surface, a custom accent colour (presets or any hex), and the bottom-navigation selected style. Applied at runtime via resource overlays on Android 12 and later. Needs the "Mod settings" patch. |  |
| [Brighter Watched-by stars](#brighter-watched-by-stars) | Other people's star ratings in a film's "Watched by" row use a very dark grey (#445566) that is hard to read, especially on a black theme. This switches them to the lighter grey (#99AABB) the rest of the app already uses for other people's ratings. A small legibility fix, on by default. |  |
| [Denser poster grid](#denser-poster-grid) | Tightens the spacing around posters in grids so they render larger and closer together. Does not change the number of columns. | • Grid density |
| [Hide Video Store on home](#hide-video-store-on-home) | Removes the "Letterboxd Video Store" promo row from the Films tab. The Video Store itself, its settings and every other entry point are left untouched. Can be toggled from the "Mod settings" screen if that patch is also enabled. |  |
| [Hide ratings until watched](#hide-ratings-until-watched) | Hides the community rating (average + histogram) on a film's page until you have marked that film as watched, covering it with a tap-to-reveal control. The reveal is per visit — leave the film and come back and it is hidden again. Only the film page is affected; ratings shown in lists, search and elsewhere are unchanged. | • Reveal style |
| [Match bottom nav to top bar color](#match-bottom-nav-to-top-bar-color) | Paints Letterboxd's bottom navigation bar black (#000000), matching the top bar, instead of the default slate. Can be toggled from the "Mod settings" screen if that patch is also enabled; the change applies on the next app start. With "Material You theme" also on, this wins — turn it off to keep the Material You nav tint. |  |
| [Material You theme](#material-you-theme) | Repaints Letterboxd's dark chrome — window background, surfaces, cards, the top bar, tab strip, bottom nav and sheets — from the device's Material You palette on Android 12+ (no effect below). No accent or OLED options here; those live in the "Mod settings" screen. No effect on Jetpack Compose screens. |  |
| [Mod settings](#mod-settings) | HOW TO OPEN: long-press the settings gear on your profile tab. — This adds a "Letterboxd Mods" screen that collects the other patches' options (theme, accent, hide ratings, hide video store, etc.) so you can change them inside the app instead of re-patching. Some changes apply immediately, others after a restart, and you'll be prompted either way. |  |

</details>

<!-- PATCHES_END -->

> The table above, between the `PATCHES_START` / `PATCHES_END` markers, is
> regenerated on every release and may lag one release behind everything
> described in [The Mods screen](#the-mods-screen) above — that section is
> the one to trust for what's actually configurable today. Don't hand-edit
> the table.

### Patch notes

**Mod settings** bundles several behaviours directly (not separate patches,
since each is fully controlled by its own toggle above anyway): hiding the
Video Store row, hiding Where to Watch, the "Open in player" button, hiding
ratings until watched, and matching the bottom nav to the top bar. See
[The Mods screen](#the-mods-screen) for the full walkthrough of every option.

**Appearance** — *on by default, Android 12+* — is the runtime half of
theming: OLED, accent colour (including Material You), and the bottom-nav
selected style, all applied via resource overlays with no restart-time
patch-file changes needed. Needs **Mod settings**.

**Material You theme** — *opt-in, Android 12+* — a separate, heavier
patch-time treatment: it flattens Letterboxd's app bar, tab strip, bottom
nav, cards and sheets onto the device's wallpaper palette directly in
`styles.xml`, rather than through a runtime overlay. This is *not* the same
thing as the Material You **accent** option above (which works without this
patch) — this one repaints the whole chrome, not just the accent, and can't
be toggled without re-patching. Turning it on locks out **Appearance**'s OLED
and bottom-nav-match switches, since the two would otherwise fight over the
same colours.

**Denser poster grid** — *opt-in* — tightens the spacing around posters so
they render larger and closer together. Does not change the column count.
Its density option (Cozy / Compact / Dense) is baked in at patch time, not
adjustable from Mod settings.

**Brighter Watched-by stars** — *on by default* — switches other people's
star ratings in a film's "Watched by" row from a near-unreadable dark grey
(`#445566`) to the lighter grey (`#99AABB`) the rest of the app uses for
other people's ratings. Always on, regardless of which theme patch you use.

## Building

Requires a JDK 21 and the Android SDK (platform 36, build-tools 36).

```bash
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`; apply it with
[Morphe Desktop](https://github.com/MorpheApp/morphe-desktop). See the
[Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more.

---

## Releasing

Handled by `release.yml` and semantic-release. Do not tag or upload releases by
hand, and do not edit the generated files (`patches-list.json`,
`patches-bundle.json`, `CHANGELOG.md`, or the patch table above).

- Work on the **`dev`** branch with
  [conventional commits](https://www.conventionalcommits.org): `feat:` and `fix:`
  cut a pre-release; `chore:` and `docs:` do not. A commit with a
  `BREAKING CHANGE:` footer (or a `!` after the type) cuts a **major** release
  regardless of type.
- Pushing to `dev` builds a pre-release and opens a `dev` to `main` pull request.
- Merge that pull request with a merge commit (not squash) to cut a stable
  release.

---

## Acknowledgements

The runtime `.arsc` resource-overlay theming technique behind **Appearance**
(Material You / OLED) is adapted from
[Piko](https://github.com/crimera/piko), a Morphe patches collection for
Twitter/X and Instagram. See [NOTICE](NOTICE) for the credit owed under
Piko's GPLv3 Section 7 terms.

## License

[GNU General Public License v3.0](LICENSE). See [NOTICE](NOTICE) for Morphe's
and Piko's additional conditions under GPLv3 Section 7.
