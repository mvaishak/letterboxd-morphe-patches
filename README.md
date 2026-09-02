# 🎬 Letterboxd Morphe Patches

Personal [Morphe](https://morphe.software) patches for the **Letterboxd** Android app
(`com.letterboxd.letterboxd`). Small cosmetic tweaks to the app's dark theme.

Not affiliated with Letterboxd or the Morphe open source project.

## ❓ About

Each patch is an independent, opt-in modification you enable per app in Morphe
Manager. Patches here only touch resources (colors, styles) — no behaviour,
tracking, or network changes.

Current target: **Letterboxd 3.5.4 (496)**. Other versions may work but are
untested; Morphe will warn if the APK doesn't match.

### How to use these patches

1. Open this link on your phone to add the source to Morphe Manager:
   <https://morphe.software/add-source?github=mvaishak/letterboxd-morphe-patches>
2. In the source settings, enable **pre-releases** if you want builds from the
   `dev` branch (newest, less tested). Leave it off for stable `main` releases.
3. Load a clean, unpatched Letterboxd APK (from APKMirror / Uptodown — not an
   already-patched or re-zipped file), pick the patches you want, and patch.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.1.0-dev.1](https://github.com/mvaishak/letterboxd-morphe-patches/releases/tag/v1.1.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 XYZ app&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 2.0.0 | 1.0.2 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Example Patch](#example-patch) | Example patch to start with. |  |

</details>

<details open>
<summary>📦 Letterboxd&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 3.5.4 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Match bottom nav to top bar color](#match-bottom-nav-to-top-bar-color) | Sets Letterboxd's bottom navigation bar background to the same color as the top bar (@color/black100), so it blends into the app's dark chrome instead of showing the default slate bar. |  |
| [Material You theme](#material-you-theme) | Tints Letterboxd's dark chrome (backgrounds, surfaces, top and bottom bars) with the device's wallpaper palette on Android 12+. Letterboxd's green (ratings, stars, primary actions) is kept. No effect on Android 11 and below or on Jetpack Compose screens. Overlaps "Match bottom nav to top bar color" on one style item — enable one, not both. |  |

</details>

<!-- PATCHES_END -->

> The table above between the `PATCHES_START` / `PATCHES_END` markers is
> regenerated automatically on every release — don't edit it by hand.
> "XYZ app / Example Patch" is the leftover template sample and can be deleted
> from `patches/src/main/kotlin/app/template/patches/example/` whenever.

## 📖 Patch details

### Match bottom nav to top bar color

Rewrites the `android:background` item of the
`Widget.Letterboxd.BottomNavigationView` style in `res/values/styles.xml` from
`@color/gray445566` (#445566) to `@color/black100` — the color Letterboxd uses
for the top app bar. The bottom navigation bar then blends into the app's dark
chrome instead of showing the default slate bar.

- Enabled by default.
- `@color/gray445566` is also `colorPrimary`, so it can't be swapped in
  `colors.xml` without recolouring the whole app; the patch targets the style
  item directly.

### Material You theme

Tints Letterboxd's dark chrome — window background, surfaces, cards, and the top
and bottom bars — with the device's wallpaper palette on Android 12+.

Letterboxd hard-codes its palette as named colours (`@color/gray181C20`, …) that
the theme and component styles reference directly, so runtime dynamic colour
would recolour almost nothing. Instead the patch redefines the dark surface
greys under `res/values-v31` as references to `@android:color/system_neutral*`,
and points the app-bar / bottom-nav backgrounds at the same tone. Android 11 and
below are untouched.

Left alone on purpose: the Letterboxd green (ratings, stars, primary buttons),
white, black, and every grey used for text, icons, dividers or hints — so
contrast and the brand accent survive.

- Opt-in (off by default).
- No effect on the handful of Jetpack Compose screens (their colours are set in
  Kotlin, not resources).
- Overlaps "Match bottom nav to top bar color" on one style item — enable one or
  the other, not both.

## 🛠️ Building locally

Requires a JDK 21 and the Android SDK (platform 36, build-tools 36).

```sh
./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`. Apply it with
[Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) like any other
patch bundle. See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation)
for more.

## 🚢 Releasing

Releases are handled entirely by `release.yml` + semantic-release. Don't tag or
upload releases by hand, and don't hand-edit the generated files
(`patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`, the patch table
above).

- Work on the **`dev`** branch. Use [conventional commits](https://www.conventionalcommits.org):
  `feat:` and `fix:` cut a new pre-release; `chore:` / `docs:` don't.
- Pushing to `dev` builds a pre-release `.mpp` and opens a `dev → main` PR.
- Merge that PR **with a merge commit (not squash)** to cut a stable release.

## 📜 License

Licensed under the [GNU General Public License v3.0](LICENSE).
