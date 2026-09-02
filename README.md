# Letterboxd Morphe Patches

<div align="center">

Small, cosmetic patches for the **Letterboxd** Android app (`com.letterboxd.letterboxd`),
built for [Morphe](https://morphe.software).

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0)
[![Built for Morphe](https://img.shields.io/badge/Built%20for-Morphe-1E5AA8?style=flat-square)](https://morphe.software)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)](https://android.com)


</div>

---

## About

A personal collection of resource-only patches for Letterboxd on Android — theming
and layout tweaks, nothing more. No behaviour changes, no unlocking, no ad or
tracking work, no network changes.

Every patch is opt-in and independent. Three of them restyle the bottom navigation
bar (**Material You theme**, **Match bottom nav to top bar colour**), so enable
only one.

Not affiliated with Letterboxd or the Morphe project.

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

Current target: **Letterboxd 3.5.4 (496)**. Other versions may work; Morphe warns
on a mismatch.

---

## Patches

<!-- PATCHES_START EXPANDED -->
> **[v1.4.0-dev.2](https://github.com/mvaishak/letterboxd-morphe-patches/releases/tag/v1.4.0-dev.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;6 patches total
<details open>
<summary>Letterboxd&nbsp;&nbsp;•&nbsp;&nbsp;6 patches</summary>
<br>

**Supported versions:**

| 3.5.4 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Brighter Watched-by stars](#brighter-watched-by-stars) | Other people's star ratings in a film's "Watched by" row use a very dark grey (#445566) that is hard to read, especially on a black theme. This switches them to the lighter grey (#99AABB) the rest of the app already uses for other people's ratings. |  |
| [Denser poster grid](#denser-poster-grid) | Tightens the spacing around posters in grids so they render larger and closer together. Does not change the number of columns. | • Grid density |
| [Hide Video Store on home](#hide-video-store-on-home) | Removes the "Letterboxd Video Store" promo row from the Films tab. The Video Store itself, its settings and every other entry point are left untouched. |  |
| [Match bottom nav to top bar color](#match-bottom-nav-to-top-bar-color) | Sets Letterboxd's bottom navigation bar background to the same color as the top bar (@color/black100), so it blends into the app's dark chrome instead of showing the default slate bar. |  |
| [Material You theme](#material-you-theme) | Repaints Letterboxd's dark chrome — window background, surfaces, cards, the top bar, tab strip and bottom nav. 'Wallpaper tint' follows the device's Material You palette on Android 12+ (no effect below). 'Pure black (OLED)' forces true black on any version. Optional accent colour recolours Letterboxd's green; optional bottom-nav selected style replaces the grey pill. No effect on Jetpack Compose screens. Overlaps "Match bottom nav to top bar color" — enable one, not both. | • Surface style<br>• Accent colour<br>• Bottom nav selected style |
| [Poster corner shape](#poster-corner-shape) | Changes the corner radius of film posters across the app. 'Sharp' matches Letterboxd's classic look; the rounded options soften every poster in grids, lists and on film pages. | • Poster corner radius |

</details>

<!-- PATCHES_END -->

> The table above, between the `PATCHES_START` / `PATCHES_END` markers, is
> regenerated on every release. Don't edit it by hand.

### Details

**Material You theme** — *opt-in*

Recolours Letterboxd's dark chrome — window background, cards, top bar, tab strip,
bottom nav — into one flat surface, with sheets and dialogs a step above and
separators nudged to stay visible. White and the greys used for body text and
icons are left alone. No effect on the few Jetpack Compose screens.

- **Surface style** — *Wallpaper tint* (device Material You palette, Android 12+
  only) or *Pure black (OLED)* (true black on any version; elevated surfaces,
  including the ratings-histogram bars, stay a faint grey so they don't
  disappear).
- **Accent colour** — a colour picker (preset swatches or any hex) for
  Letterboxd's green: stars, rating indicators, primary buttons. Lighter and
  darker shades for gradients and pressed states are derived from your pick.
  Green is left untouched unless OLED, where it is brightened to read on black.
- **Bottom nav selected style** — *Stock* (grey pill + blue icon), *No pill*,
  *No pill + white icon*, *No pill + accent icon*, or *Accent pill*. The green
  "+" button is never touched.

**Match bottom nav to top bar colour** — *on by default*

Makes only the bottom navigation bar black, matching the top bar. A minimal
alternative to the Material You patch.

**Poster corner shape** — *opt-in*

Sets the corner radius on every film poster — grids, lists, film pages.

- **Poster corner radius** — Sharp (default), 4dp, 8dp, or 16dp.

**Denser poster grid** — *opt-in*

Tightens the spacing around posters so they render larger and closer together.
Does not change the column count.

- **Grid density** — Cozy, Compact (default), or Dense.

**Hide Video Store on home** — *opt-in*

Removes the "Letterboxd Video Store" promo row from the Films tab. The Video Store
and every other entry point (settings, film pages, search) are left alone.

**Brighter Watched-by stars** — *opt-in*

Switches other people's star ratings in a film's "Watched by" row from a
near-unreadable dark grey (`#445566`) to the lighter grey (`#99AABB`) the rest of
the app uses for other people's ratings.

---

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
  cut a pre-release; `chore:` and `docs:` do not.
- Pushing to `dev` builds a pre-release and opens a `dev` to `main` pull request.
- Merge that pull request with a merge commit (not squash) to cut a stable
  release.

---

## License

[GNU General Public License v3.0](LICENSE). See [NOTICE](NOTICE) for Morphe's
additional conditions under GPLv3 Section 7.
