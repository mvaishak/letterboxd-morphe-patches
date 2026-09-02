# 🎬 Letterboxd Morphe Patches

Personal [Morphe](https://morphe.software) patches for the **Letterboxd** Android
app (`com.letterboxd.letterboxd`) — small, cosmetic tweaks to the app's dark
theme. Nothing else is touched: no behaviour changes, no unlocking, no ads/tracking
work, no network changes.

Not affiliated with Letterboxd or the Morphe open source project.

## ✨ What the patches do

### 🎨 Material You theme  ·  _opt-in_

Repaints Letterboxd's dark chrome with **your phone's wallpaper colours** on
Android 12 and newer.

- The window background, cards, the top bar, the tab strip and the bottom
  navigation bar all take on a dark, wallpaper-tinted neutral — one flat, unified
  surface instead of Letterboxd's fixed slate-and-black.
- Bottom sheets and dialogs (the log / rate / film-actions popups) sit one gentle
  step above that, so they read as raised surfaces rather than a different colour
  scheme.
- Separators and hairlines are nudged so they stay visible on both.
- **Kept exactly as-is:** the Letterboxd green (star ratings, rating graphs,
  primary buttons, the logo), white, black, and every grey used for text and
  icons — so contrast and the brand accent are never harmed.
- **No effect** on Android 11 and below, or on the few screens Letterboxd builds
  with Jetpack Compose (their colours are baked in and can't be reached from a
  resource patch).

### 📍 Match bottom nav to top bar colour  ·  _on by default_

Makes the **bottom navigation bar black**, the same as Letterboxd's top bar, so
it blends into the app's chrome instead of showing the lighter slate colour.

A small, self-contained alternative to the Material You patch. If you run
**Material You theme**, leave this one **off** — they both restyle the bottom
bar and the last one applied wins.

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
> **[v1.1.0](https://github.com/mvaishak/letterboxd-morphe-patches/releases/tag/v1.1.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
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
