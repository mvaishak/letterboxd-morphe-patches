# 🎬 Letterboxd Morphe Patches

Personal [Morphe](https://morphe.software) patches for the **Letterboxd** Android
app (`com.letterboxd.letterboxd`) — small, cosmetic tweaks to the app's dark
theme. Nothing else is touched: no behaviour changes, no unlocking, no ads/tracking
work, no network changes.

Not affiliated with Letterboxd or the Morphe open source project.

## ✨ What the patches do

Every patch is opt-in and independent. The theming patches (🎨 / 🖤 / 📍) all
restyle the app's chrome — **pick one**.

### 🎨 Material You theme  ·  _opt-in, has options_

Repaints Letterboxd's dark chrome — window background, cards, the top bar, the
tab strip and the bottom nav — as one flat, unified surface instead of
Letterboxd's fixed slate-and-black. Bottom sheets and dialogs sit one gentle
step above; separators are nudged so they stay visible.

**Option — Surface style:**
- **Wallpaper tint (Android 12+)** — follows your device's Material You palette.
  No effect on Android 11 and below.
- **Pure black (OLED)** — true black on any Android version, for AMOLED screens.
  Elevated bits (cards, sheets, the ratings-histogram bars) stay a faint grey so
  they don't vanish into the black.

**Option — Accent colour:** recolours Letterboxd's green (stars, rating
indicators, primary buttons). **Green** / **Amber** / **Orange** / **Coral** /
**Pink** / **Violet** / **Blue** / **Teal** / **Mono**. "Green" is left
untouched, except in OLED mode where it's brightened so it reads on black.

**Option — Bottom nav selected style:** how the selected tab in the bottom bar
is shown. **Stock** (grey pill + blue icon) / **No pill** / **No pill, white
icon** / **No pill, accent icon** / **Accent pill**. The green **+** button is
never touched (accent modes fall back to white if you also picked a green accent,
so the selected tab stays distinct from **+**).

**Kept as-is:** white and every grey used for body text and icons, so contrast
is never harmed. No effect on the few Jetpack Compose screens (their colours are
baked into code).

### 📍 Match bottom nav to top bar colour  ·  _on by default_

Makes just the **bottom navigation bar black**, matching Letterboxd's top bar.
A tiny alternative to the theming patch above.

### 🖼️ Poster corner shape  ·  _opt-in, has options_

Sets the corner radius of every film poster — grids, lists, film pages.

**Option — Poster corner radius:** Sharp (0dp, default) · Slight (4dp) ·
Rounded (8dp) · Very rounded (16dp).

### 🔲 Denser poster grid  ·  _opt-in, has options_

Tightens the spacing around posters in grids so they render larger and closer
together. Does **not** change the number of columns.

**Option — Grid density:** Cozy (near default) · Compact (default) · Dense.

### 🚫 Hide Video Store on home  ·  _opt-in_

Removes the "Letterboxd Video Store" promo row from the Films tab. The Video
Store itself and every other entry point (settings, film pages, search) are
left alone.

### ⭐ Brighter Watched-by stars  ·  _opt-in_

Other people's star ratings in a film's "Watched by" row use a very dark grey
(`#445566`) that's hard to read, especially on a black theme. This switches them
to the lighter grey (`#99AABB`) the rest of the app uses for other people's
ratings.

## 📲 How to use

1. On your phone, open this link to add the source to Morphe Manager:
   <https://morphe.software/add-source?github=mvaishak/letterboxd-morphe-patches>
2. In the source's settings, turn on **pre-releases** to get `dev`-branch builds
   (newest, less tested). Leave it off for stable releases only.
3. Feed Morphe a **clean, unpatched** Letterboxd APK from
   [APKMirror](https://www.apkmirror.com/apk/letterboxd/) or
   [Uptodown](https://letterboxd.en.uptodown.com/android) — not a file that's
   already been patched or re-zipped by another tool.
4. Pick the patches you want and patch.

Current target: **Letterboxd 3.5.4 (496)**. Other versions may work; Morphe warns
if the APK doesn't match.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.4.0-dev.1](https://github.com/mvaishak/letterboxd-morphe-patches/releases/tag/v1.4.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;6 patches total
<details open>
<summary>📦 Letterboxd&nbsp;&nbsp;•&nbsp;&nbsp;6 patches</summary>
<br>

**🎯 Supported versions:**

| 3.5.4 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Brighter Watched-by stars](#brighter-watched-by-stars) | Other people's star ratings in a film's "Watched by" row use a very dark grey (#445566) that is hard to read, especially on a black theme. This switches them to the lighter grey (#99AABB) the rest of the app already uses for other people's ratings. |  |
| [Denser poster grid](#denser-poster-grid) | Tightens the spacing around posters in grids so they render larger and closer together. Does not change the number of columns. | • Grid density |
| [Hide Video Store on home](#hide-video-store-on-home) | Removes the "Letterboxd Video Store" promo row from the Films tab. The Video Store itself, its settings and every other entry point are left untouched. |  |
| [Match bottom nav to top bar color](#match-bottom-nav-to-top-bar-color) | Sets Letterboxd's bottom navigation bar background to the same color as the top bar (@color/black100), so it blends into the app's dark chrome instead of showing the default slate bar. |  |
| [Material You theme](#material-you-theme) | Repaints Letterboxd's dark chrome — window background, surfaces, cards, the top bar, tab strip and bottom nav. 'Wallpaper tint' follows the device's Material You palette on Android 12+ (no effect below). 'Pure black (OLED)' forces true black on any version. Optional accent colour recolours Letterboxd's green; optional bottom-nav selected style replaces the grey pill. No effect on Jetpack Compose screens. Overlaps "Match bottom nav to top bar color" — enable one, not both. | • Surface style<br>• Accent colour<br>• Bottom nav selected style |
| [Poster corner shape](#poster-corner-shape) | Changes the corner radius of film posters across the app. 'Sharp' matches Letterboxd's classic look; the rounded options soften every poster in grids, lists and on film pages. | • Poster corner radius |

</details>

<!-- PATCHES_END -->

> The table between the `PATCHES_START` / `PATCHES_END` markers is regenerated on
> every release — don't edit it by hand. It refreshes on the next release.

## 🛠️ Building locally

Needs a JDK 21 and the Android SDK (platform 36, build-tools 36).

```sh
./gradlew buildAndroid
```

The bundle lands at `patches/build/libs/patches-*.mpp`; apply it with
[Morphe Desktop](https://github.com/MorpheApp/morphe-desktop). See the
[Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more.

## 🚢 Releasing

`release.yml` + semantic-release handle everything. Don't tag or upload releases
by hand, and don't edit the generated files (`patches-list.json`,
`patches-bundle.json`, `CHANGELOG.md`, the patch table above).

- Work on **`dev`** with [conventional commits](https://www.conventionalcommits.org):
  `feat:` / `fix:` cut a pre-release, `chore:` / `docs:` don't.
- Pushing to `dev` builds a pre-release `.mpp` and opens a `dev → main` PR.
- Merge that PR **with a merge commit (not squash)** for a stable release.

## 📜 License

[GNU General Public License v3.0](LICENSE).
