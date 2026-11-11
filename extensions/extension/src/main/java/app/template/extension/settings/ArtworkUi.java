package app.template.extension.settings;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.template.extension.artwork.ArtworkHooks;
import app.template.extension.artwork.ArtworkImages;
import app.template.extension.artwork.ArtworkStore;
import app.template.extension.artwork.ArtworkStore.Film;
import app.template.extension.artwork.ArtworkStore.Type;
import app.template.extension.artwork.TmdbClient;
import app.template.extension.artwork.TmdbCredential;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Artwork screens use the same surfaces and accent selection as Letterboxd Mods. */
public final class ArtworkUi {
    private static final ExecutorService NETWORK = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String[] MODES = {"automatic", "local", "off"};
    private static final String[] MODE_LABELS = {"Automatic", "Always use local artwork", "Off"};

    private ArtworkUi() {}

    private interface Callback<T> {
        void accept(T value);
    }

    public static void openFilm(Context context, Film film) {
        new Picker(context, film, null, null).show();
    }

    public static void openProfile(Context context, String member) {
        openProfile(context, member, null);
    }

    private static void openProfile(Context context, String member, Runnable after) {
        if (member == null || !member.equals(ArtworkHooks.account())) return;
        Screen screen = new Screen(context, "Change profile backdrop", null);
        LinearLayout body = screen.body();
        ArtworkStore store = new ArtworkStore(context);
        Runnable redraw = () -> {
            body.removeAllViews();
            if (!member.equals(ArtworkHooks.account())) {
                screen.note(body, "Sign in to change your profile backdrop.");
                return;
            }
            String current = store.profile(member);
            screen.note(body, artworkStatus(current));
            screen.row(body, "Enter image URL", null, () -> enterUrl(context, url ->
                    preview(context, Type.BACKDROP, url, member, () -> store.saveProfile(member, url),
                            null, screen.redraw)));
            screen.row(body, "Choose a film backdrop", "Browse recently opened or customized films",
                    () -> profileFilms(context, member, screen.redraw));
            if (current != null) {
                screen.row(body, "Preview current backdrop", null, () ->
                        preview(context, Type.BACKDROP, current, member, null, null, null));
                screen.row(body, "Restore original profile backdrop", null, () -> {
                    store.saveProfile(member, null);
                    screen.redraw.run();
                });
            }
        };
        screen.redraw = redraw;
        if (after != null) screen.onDismiss = after;
        redraw.run();
        screen.show(false);
    }

    private static void profileFilms(Context context, String member, Runnable after) {
        Screen screen = new Screen(context, "Choose a film", null);
        LinearLayout body = screen.body();
        Map<String, Film> films = new LinkedHashMap<>();
        for (Film film : ArtworkHooks.recentFilms()) films.put(film.id, film);
        for (Film film : new ArtworkStore(context).films()) {
            if (!films.containsKey(film.id)) films.put(film.id, film);
        }
        if (films.isEmpty()) {
            screen.note(body, "Open a film first, then return here to browse its backdrops.");
        }
        for (Film film : films.values()) {
            screen.row(body, film.label(), null, () -> new Picker(context, film, member, after).show());
        }
        screen.show(true);
    }

    static void settings(ModSettingsActivity activity) {
        Screen screen = new Screen(activity, "Artwork & appearance", null);
        ArtworkStore store = new ArtworkStore(activity);
        LinearLayout body = screen.body();
        screen.redraw = () -> {
            body.removeAllViews();
            String account = ArtworkHooks.account();
            boolean connected = TmdbCredential.get(activity) != null;
            String mode = Prefs.getString(Prefs.KEY_ARTWORK_MODE, "automatic");
            int modeIndex = Math.max(0, java.util.Arrays.asList(MODES).indexOf(mode));
            screen.section(body, "Artwork mode");
            body.addView(screen.dropdown(MODE_LABELS[modeIndex], () -> chooseMode(screen, mode)));
            if (!ArtworkHooks.localEnabled()) {
                screen.note(body, "Using Letterboxd’s artwork. Your local choices are saved.");
            }
            screen.section(body, "TMDB connection");
            screen.row(body, connected ? "Change API key" : "Connect TMDB",
                    connected ? "Connected" : "Image URLs are available without a key",
                    () -> setup(activity, connected, screen.redraw, null));
            if (connected) {
                screen.row(body, "Remove API key", null, () -> {
                    if (TmdbCredential.remove(activity)) screen.redraw.run();
                    else screen.message("Couldn't remove the API key", "Try again.");
                });
            }
            screen.section(body, "Custom artwork");
            int count = store.films().size();
            screen.row(body, "Manage custom artwork", count + (count == 1 ? " customized film" : " customized films"),
                    () -> manage(activity, screen.redraw));
            screen.section(body, "Profile backdrop");
            if (account == null) screen.note(body, "Sign in to change your profile backdrop.");
            else {
                screen.row(body, "Change backdrop", null, () -> openProfile(activity, account, screen.redraw));
                if (store.profile(account) != null) {
                    screen.row(body, "Restore original profile backdrop", null, () -> {
                        store.saveProfile(account, null);
                        screen.redraw.run();
                    });
                }
            }
            screen.section(body, "Backup and restore");
            screen.row(body, "Export custom artwork", null, () -> activity.pickArtworkBackup(true));
            screen.row(body, "Import custom artwork", "Fills empty artwork slots", () -> {
                screen.dismiss();
                activity.pickArtworkBackup(false);
            });
            screen.section(body, "Reset artwork");
            screen.row(body, "Clear custom posters", null, () -> clear(screen,
                    "Clear custom posters?", "This restores the original poster for every customized film.",
                    () -> store.clear(Type.POSTER, true, null)));
            screen.row(body, "Clear custom film backdrops", null, () -> clear(screen,
                    "Clear custom film backdrops?", "This restores the original backdrop for every customized film.",
                    () -> store.clear(Type.BACKDROP, true, null)));
            if (account != null) {
                screen.row(body, "Clear profile backdrop", null, () -> clear(screen,
                        "Clear profile backdrop?", "This restores the original backdrop for your signed-in profile.",
                        () -> store.clear(null, false, account)));
            }
            screen.row(body, "Clear all custom artwork", null, () -> clear(screen,
                    "Clear all custom artwork?", account == null
                            ? "This clears all local film posters and backdrops."
                            : "This clears all local film posters and backdrops and your signed-in profile backdrop.",
                    () -> store.clear(null, true, account)));
            screen.section(body, "Credits");
            try (java.io.InputStream logo = activity.getAssets().open("morphe/tmdb.png")) {
                ImageView tmdb = new ImageView(activity);
                tmdb.setImageBitmap(android.graphics.BitmapFactory.decodeStream(logo));
                tmdb.setContentDescription("TMDB");
                tmdb.setScaleType(ImageView.ScaleType.FIT_START);
                LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(screen.dp(110), screen.dp(18));
                size.bottomMargin = screen.dp(6);
                body.addView(tmdb, size);
            } catch (java.io.IOException ignored) {
                // The textual credit is still shown.
            }
            screen.note(body, "This product uses the TMDB API but is not endorsed or certified by TMDB.");
            screen.row(body, "The Movie Database", null, () -> browser(activity, "https://www.themoviedb.org", body));
        };
        screen.show(true);
        screen.redraw.run();
    }

    private static void chooseMode(Screen settings, String current) {
        Screen chooser = new Screen(settings.context, "Artwork mode", null);
        String[] descriptions = {
                "Use local artwork when your account lacks native artwork controls",
                "Use the local picker and saved images on this device",
                "Use Letterboxd’s native artwork and controls",
        };
        for (int i = 0; i < MODES.length; i++) {
            String mode = MODES[i];
            String description = descriptions[i];
            if (mode.equals(current)) description += " · Selected";
            chooser.row(chooser.root, MODE_LABELS[i], description, () -> {
                Prefs.putString(Prefs.KEY_ARTWORK_MODE, mode);
                ArtworkHooks.refreshArtwork();
                chooser.dismiss();
                settings.redraw.run();
            });
        }
        chooser.show(false);
    }

    private static String artworkStatus(String saved) {
        if (!ArtworkHooks.localEnabled()) {
            return saved == null ? "Local artwork is inactive" : "Saved custom artwork is inactive";
        }
        return saved == null ? "Original artwork active" : "Custom artwork active";
    }

    private static void clear(Screen screen, String title, String message, Runnable action) {
        Screen confirm = new Screen(screen.context, title, message);
        confirm.root.addView(confirm.button("Clear", true, () -> {
            action.run();
            screen.redraw.run();
            confirm.dismiss();
        }));
        confirm.root.addView(confirm.outlineButton("Cancel", confirm::dismiss));
        confirm.show(false);
    }

    private static void manage(Context context, Runnable after) {
        Screen screen = new Screen(context, "Manage custom artwork", null);
        ArtworkStore store = new ArtworkStore(context);
        LinearLayout body = screen.body();
        screen.redraw = () -> {
            body.removeAllViews();
            List<Film> films = store.films();
            if (films.isEmpty()) screen.note(body, "No customized films yet.");
            for (Film film : films) {
                String poster = store.url(film.id, Type.POSTER);
                String backdrop = store.url(film.id, Type.BACKDROP);
                LinearLayout item = screen.horizontal();
                item.setPadding(0, screen.dp(6), 0, screen.dp(6));
                ImageView thumb = new ImageView(context);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setBackground(screen.shape(screen.surface, 8, 0));
                thumb.setClipToOutline(true);
                item.addView(thumb, new LinearLayout.LayoutParams(screen.dp(52), screen.dp(72)));
                ArtworkImages.load(thumb, poster != null ? poster : backdrop, null);
                LinearLayout name = screen.column();
                name.setPadding(screen.dp(12), 0, screen.dp(6), 0);
                name.addView(screen.text(film.label(), 16, Color.WHITE));
                name.addView(screen.text(poster != null && backdrop != null ? "Poster and backdrop"
                        : poster != null ? "Poster" : "Backdrop", 14, Screen.SECONDARY));
                item.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
                item.addView(screen.button("Manage", false, () -> new Picker(context, film, null, () -> {
                    screen.redraw.run();
                    after.run();
                }).show()));
                body.addView(item);
            }
        };
        screen.show(true);
        screen.redraw.run();
    }

    private static void setup(Context context, boolean changing, Runnable connected, Runnable manual) {
        Screen screen = new Screen(context, changing ? "Change TMDB API key" : "Set up custom artwork", null);
        LinearLayout body = screen.body();
        screen.note(body, "Choose custom posters and backdrops using images from TMDB. Your choices stay on this device.");
        screen.section(body, "1. Get your API key");
        screen.note(body, "Sign in to TMDB or create an account. Open API settings, complete API registration if required, then copy API Key (v3 auth).");
        body.addView(screen.outlineButton("Open TMDB API settings",
                () -> browser(context, TmdbClient.SETTINGS_URL, body)));
        screen.section(body, "2. Enter the key");
        screen.note(body, "Use API Key (v3 auth). The API Read Access Token is a different credential.");
        EditText input = screen.input("API Key (v3 auth)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(input);
        screen.row(body, "Paste", null, () -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = clipboard == null ? null : clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) input.setText(clip.getItemAt(0).getText());
        });
        TextView status = screen.note(body, "");
        Job job = new Job(screen);
        TextView connect = screen.button("Connect & continue", true, null);
        connect.setOnClickListener(view -> {
            String key = input.getText().toString().trim();
            String problem = TmdbClient.keyProblem(key);
            if (problem != null) {
                status.setText(problem);
                return;
            }
            status.setText("Connecting to TMDB…");
            connect.setEnabled(false);
            job.run(() -> {
                new TmdbClient(key).validate();
                return key;
            }, verified -> {
                try {
                    TmdbCredential.save(context, verified);
                    screen.dismiss();
                    connected.run();
                } catch (Exception e) {
                    connect.setEnabled(true);
                    status.setText("Couldn't save the key on this device.");
                }
            }, error -> {
                connect.setEnabled(true);
                status.setText(error);
            });
        });
        screen.root.addView(connect);
        if (manual != null) {
            screen.root.addView(screen.outlineButton("Use image URL", () -> {
                screen.dismiss();
                manual.run();
            }));
        }
        screen.onDismiss = job::cancel;
        screen.show(false);
    }

    private static void browser(Context context, String url, View anchor) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (android.content.ActivityNotFoundException e) {
            new Screen(anchor.getContext(), "Open in browser", url).show(false);
        }
    }

    private static void enterUrl(Context context, Callback<String> after) {
        Screen screen = new Screen(context, "Enter image URL", "Enter a direct HTTPS link to an image.");
        EditText field = screen.input("https://", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        screen.root.addView(field);
        TextView status = screen.note(screen.root, "");
        screen.root.addView(screen.button("Preview artwork", true, () -> {
            String url = TmdbClient.validateUrl(field.getText().toString());
            if (url == null) {
                status.setText("Enter a valid HTTPS image URL.");
                return;
            }
            screen.dismiss();
            after.accept(url);
        }));
        screen.show(false);
    }

    private static void preview(Context context, Type type, String url, String member,
                                Runnable apply, Runnable useOnProfile, Runnable after) {
        String title = member != null ? "Preview profile backdrop"
                : type == Type.POSTER ? "Preview poster" : "Preview film backdrop";
        Screen screen = new Screen(context, title, apply == null ? null : "Your choice stays on this device.");
        LinearLayout body = screen.body();
        ImageView image = new ImageView(context);
        image.setBackground(screen.shape(screen.surface, 10, 0));
        image.setClipToOutline(true);
        image.setScaleType(type == Type.POSTER ? ImageView.ScaleType.FIT_CENTER
                : member == null ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_START);
        image.setContentDescription(title);
        float aspect = type == Type.POSTER ? 1.5f : 9f / 16f;
        FrameLayout sample = new FrameLayout(context) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int width = Math.max(1, Math.min(MeasureSpec.getSize(widthSpec),
                        (int) (screen.panelHeight() * 0.65f / aspect)));
                int height = Math.max(1, Math.round(width * aspect));
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
        };
        sample.setBackground(screen.shape(screen.surface, 10, 0));
        sample.setClipToOutline(true);
        sample.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (type == Type.BACKDROP) {
            int gradient = context.getResources().getIdentifier("backdrop_gradient", "drawable", context.getPackageName());
            if (gradient != 0) {
                View overlay = new View(context);
                overlay.setBackgroundResource(gradient);
                sample.addView(overlay, new FrameLayout.LayoutParams(-1, screen.dp(member == null ? 80 : 60),
                        Gravity.BOTTOM));
            }
        }
        LinearLayout.LayoutParams sampleSize = new LinearLayout.LayoutParams(-2, -2);
        sampleSize.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(sample, sampleSize);
        TextView status = screen.note(body, "Loading image…");
        TextView confirm = screen.button("Apply artwork", true, () -> {
            if (member != null && !member.equals(ArtworkHooks.account())) {
                status.setText("The signed-in profile has changed.");
                return;
            }
            apply.run();
            screen.dismiss();
            if (after != null) after.run();
        });
        confirm.setEnabled(false);
        if (apply != null) {
            screen.root.addView(confirm);
            screen.root.addView(screen.outlineButton("Back to choices", screen::dismiss));
        }
        TextView shortcut = screen.outlineButton("Set as profile backdrop", () -> {
            screen.dismiss();
            useOnProfile.run();
        });
        shortcut.setEnabled(false);
        if (useOnProfile != null) screen.root.addView(shortcut);
        screen.onDismiss = () -> ArtworkImages.clear(image);
        screen.show(false);
        ArtworkImages.load(image, url, loaded -> {
            status.setText(loaded ? "" : "Couldn't load this image. Check the link or try again.");
            status.setVisibility(loaded ? View.GONE : View.VISIBLE);
            confirm.setEnabled(loaded);
            shortcut.setEnabled(loaded);
        });
    }

    private static final class Picker {
        final Film film;
        final String member;
        final Runnable after;
        final ArtworkStore store;
        final Screen screen;
        final Job job;
        final Map<Type, String> selection = new EnumMap<>(Type.class);
        final ListView gallery;
        final TextView status;
        final TextView browseAction;
        final TextView active;
        final TextView preview;
        final TextView restore;
        final TextView current;
        final TextView restoreBoth;
        final TextView filter;
        final TextView posters;
        final TextView backdrops;
        final GridAdapter adapter;
        Type type = Type.POSTER;
        TmdbClient.Images images;
        int languageFilter;
        String message;
        String browseLabel;
        Runnable browseClick;

        Picker(Context context, Film film, String member, Runnable after) {
            this.film = film;
            this.member = member;
            this.after = after;
            store = new ArtworkStore(context);
            screen = new Screen(context, member == null ? "Customize artwork" : "Choose profile backdrop", film.label());
            job = new Job(screen);

            if (member != null) {
                type = Type.BACKDROP;
                posters = null;
                backdrops = null;
            } else {
                LinearLayout tabs = screen.horizontal();
                posters = screen.button("Posters", false, () -> switchTo(Type.POSTER));
                backdrops = screen.button("Backdrops", false, () -> switchTo(Type.BACKDROP));
                screen.share(tabs, posters);
                screen.share(tabs, backdrops);
                screen.root.addView(tabs);
            }
            active = screen.note(screen.root, "");
            if (member != null) {
                String custom = store.url(film.id, Type.BACKDROP);
                if (custom != null) {
                    screen.root.addView(screen.outlineButton("Preview film’s custom backdrop", () -> selectAndPreview(custom)));
                }
                if (film.originalBackdrop != null) {
                    screen.root.addView(screen.outlineButton("Preview film’s original backdrop",
                            () -> selectAndPreview(film.originalBackdrop)));
                }
            }
            status = screen.note(screen.root, "");
            browseAction = screen.button("", true, () -> {
                if (browseClick != null) browseClick.run();
            });
            screen.root.addView(browseAction);
            filter = screen.dropdown("Language · All languages", this::chooseLanguage);
            screen.root.addView(filter);

            preview = screen.button("Preview selection", true, this::previewChoice);
            screen.root.addView(preview);
            screen.root.addView(screen.outlineButton("Use image URL", this::manual));

            LinearLayout secondary = screen.horizontal();
            current = screen.button("View saved artwork", false, () -> {
                String url = saved();
                if (url != null) ArtworkUi.preview(context, type, url, member, null, null, null);
            });
            restore = screen.button("", false, this::restore);
            screen.share(secondary, current);
            screen.share(secondary, restore);
            screen.root.addView(secondary);
            restoreBoth = screen.button("Restore both film images", false, () -> {
                store.restore(film.id, null);
                selection.clear();
                render();
                if (after != null) after.run();
            });
            screen.root.addView(restoreBoth);
            // One scrolling surface keeps both the controls and the recycled artwork reachable.
            adapter = new GridAdapter();
            gallery = new ListView(context) {
                @Override protected void onMeasure(int widthSpec, int heightSpec) {
                    super.onMeasure(widthSpec, screen.capHeight(heightSpec, !adapter.items.isEmpty()));
                }
            };
            gallery.setDivider(null);
            gallery.setDividerHeight(0);
            gallery.setItemsCanFocus(true);
            gallery.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            gallery.addHeaderView(screen.root, null, false);
            gallery.setAdapter(adapter);
            gallery.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (right - left != oldRight - oldLeft) {
                    adapter.resize(right - left);
                    adapter.notifyDataSetChanged();
                }
            });
            screen.onDismiss = () -> {
                job.cancel();
                gallery.setAdapter(null);
                if (after != null) after.run();
            };
        }

        void show() {
            if (TmdbCredential.get(screen.context) != null) {
                browse();
            } else {
                showBrowseState("Connect TMDB to browse posters and backdrops for this film.",
                        "Connect TMDB", () -> setup(screen.context, false, this::browse, this::manual));
            }
            screen.showPanel(gallery);
        }

        void switchTo(Type tab) {
            if (type == tab) return;
            type = tab;
            languageFilter = 0;
            render();
            gallery.setSelection(0);
        }

        void manual() {
            enterUrl(screen.context, this::selectAndPreview);
        }

        void selectAndPreview(String url) {
            if (url == null) return;
            selection.put(type, url);
            updateSelection();
            previewChoice();
        }

        void previewChoice() {
            Type choice = type;
            String url = selection.get(choice);
            if (url == null) return;
            String account = ArtworkHooks.account();
            Runnable shortcut = member == null && choice == Type.BACKDROP && account != null ? () ->
                    ArtworkUi.preview(screen.context, choice, url, account,
                            () -> store.saveProfile(account, url), null, screen::dismiss)
                    : null;
            ArtworkUi.preview(screen.context, choice, url, member, () -> {
                if (member == null) store.save(film, choice, url);
                else store.saveProfile(member, url);
            }, shortcut, screen::dismiss);
        }

        void restore() {
            if (member == null) store.restore(film.id, type);
            else store.saveProfile(member, null);
            selection.remove(type);
            render();
            if (after != null) after.run();
        }

        String saved() {
            if (member == null) return store.url(film.id, type);
            return member.equals(ArtworkHooks.account()) ? store.profile(member) : null;
        }

        void browse() {
            String key = TmdbCredential.get(screen.context);
            if (key == null) return;
            TmdbClient client = new TmdbClient(key);
            showBrowseState("Looking up film on TMDB…", null, null);
            job.run(() -> client.match(film), matches -> {
                if (matches.isEmpty()) {
                    showBrowseState("TMDB has no match for this film. You can still use an image URL.", null, null);
                } else if (matches.size() == 1 && matches.get(0).exact) {
                    fetch(client, matches.get(0));
                } else if (screen.isVisible()) {
                    chooseMatch(client, matches);
                } else {
                    offerFilmMatch(client, matches);
                }
            }, this::failed);
        }

        void offerFilmMatch(TmdbClient client, List<TmdbClient.Match> matches) {
            showBrowseState("Confirm the film to load its artwork.", "Choose film on TMDB",
                    () -> chooseMatch(client, matches));
        }

        void chooseMatch(TmdbClient client, List<TmdbClient.Match> matches) {
            offerFilmMatch(client, matches);
            Screen chooser = new Screen(screen.context, "Confirm film on TMDB", film.label());
            LinearLayout body = chooser.body();
            for (TmdbClient.Match match : matches) {
                chooser.row(body, match.label, null, () -> {
                    chooser.dismiss();
                    fetch(client, match);
                });
            }
            chooser.show(matches.size() > 5);
        }

        void fetch(TmdbClient client, TmdbClient.Match match) {
            showBrowseState("Loading artwork from TMDB…", null, null);
            job.run(() -> client.images(match.id), result -> {
                images = result;
                showBrowseState("", null, null);
            }, this::failed);
        }

        void failed(String error) {
            showBrowseState(error, "Retry TMDB", this::browse);
        }

        void showBrowseState(String message, String label, Runnable click) {
            this.message = message;
            browseLabel = label;
            browseClick = click;
            render();
        }

        void chooseLanguage() {
            Screen chooser = new Screen(screen.context, "Artwork languages", null);
            for (int i = 0; i < 4; i++) {
                if (i == 2 && film.language == null) continue;
                int index = i;
                chooser.row(chooser.root, languageLabel(index), index == languageFilter ? "Selected" : null, () -> {
                    languageFilter = index;
                    chooser.dismiss();
                    render();
                });
            }
            chooser.show(false);
        }

        String languageLabel(int index) {
            switch (index) {
                case 1: return "English";
                case 2: return "Original language";
                case 3: return "No text";
                default: return "All languages";
            }
        }

        void render() {
            if (posters != null) {
                screen.paint(posters, type == Type.POSTER);
                screen.paint(backdrops, type == Type.BACKDROP);
                posters.setSelected(type == Type.POSTER);
                backdrops.setSelected(type == Type.BACKDROP);
            }
            List<TmdbClient.Image> options = images == null ? Collections.emptyList()
                    : type == Type.POSTER ? images.posters : images.backdrops;
            List<TmdbClient.Image> filtered = new ArrayList<>();
            for (TmdbClient.Image image : options) {
                if (languageFilter == 0
                        || (languageFilter == 1 && "en".equals(image.language))
                        || (languageFilter == 2 && film.language.equals(image.language))
                        || (languageFilter == 3 && image.language == null)) filtered.add(image);
            }
            adapter.items = filtered;
            adapter.resize(gallery.getWidth() > 0 ? gallery.getWidth() : screen.width());
            filter.setVisibility(options.isEmpty() ? View.GONE : View.VISIBLE);
            filter.setText("Language · " + languageLabel(languageFilter));
            browseAction.setText(browseLabel);
            browseAction.setVisibility(browseLabel == null ? View.GONE : View.VISIBLE);
            String text = images == null ? message : options.isEmpty() ? "TMDB has no " + type.label + " artwork for this film."
                    : filtered.isEmpty() ? "No artwork matches this language."
                    : "Artwork from TMDB. Tap an image to preview it.";
            status.setText(text);
            status.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            String saved = saved();
            active.setText(artworkStatus(saved));
            current.setVisibility(saved == null ? View.GONE : View.VISIBLE);
            restore.setVisibility(saved == null ? View.GONE : View.VISIBLE);
            restoreBoth.setVisibility(member == null && after != null
                    && store.url(film.id, Type.POSTER) != null && store.url(film.id, Type.BACKDROP) != null
                    ? View.VISIBLE : View.GONE);
            restore.setText(member != null ? "Restore original profile backdrop" : "Restore original " + type.label);
            updateSelection();
        }

        void updateSelection() {
            preview.setVisibility(selection.get(type) == null ? View.GONE : View.VISIBLE);
            preview.setText(type == Type.POSTER ? "Preview selected poster" : "Preview selected backdrop");
            adapter.notifyDataSetChanged();
            gallery.requestLayout();
        }

        final class GridAdapter extends BaseAdapter {
            List<TmdbClient.Image> items = Collections.emptyList();
            int columns = 3;
            int cellWidth;
            float cellAspect = 1.5f;

            void resize(int width) {
                if (width <= 0) return;
                columns = type == Type.POSTER ? 3 : 2;
                if (width >= screen.dp(500)) columns++;
                cellWidth = Math.max(1, (width - screen.dp(40 + 8 * (columns - 1))) / columns);
                cellAspect = type == Type.POSTER ? 1.5f : 9f / 16f;
            }

            @Override public int getCount() { return (items.size() + columns - 1) / columns; }
            @Override public TmdbClient.Image getItem(int row) { return items.get(row * columns); }
            @Override public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View recycled, ViewGroup parent) {
                LinearLayout row = recycled instanceof LinearLayout
                        ? (LinearLayout) recycled : new LinearLayout(screen.context);
                row.setPadding(screen.dp(20), 0, screen.dp(20), screen.dp(8));
                while (row.getChildCount() < columns) row.addView(new ImageView(screen.context));
                while (row.getChildCount() > columns) row.removeViewAt(row.getChildCount() - 1);
                for (int col = 0; col < columns; col++) {
                    ImageView view = (ImageView) row.getChildAt(col);
                    int index = position * columns + col;
                    LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(
                            cellWidth, Math.round(cellWidth * cellAspect));
                    if (col > 0) cell.setMarginStart(screen.dp(8));
                    view.setLayoutParams(cell);
                    if (index >= items.size()) {
                        view.setVisibility(View.INVISIBLE);
                        ArtworkImages.clear(view);
                        view.setTag(null);
                    } else {
                        bind(view, items.get(index), index);
                    }
                }
                return row;
            }

            private void bind(ImageView view, TmdbClient.Image item, int index) {
                boolean selected = item.url.equals(selection.get(type));
                view.setVisibility(View.VISIBLE);
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                view.setBackground(screen.shape(screen.surface, 8, 0));
                view.setClipToOutline(true);
                view.setForeground(selected ? screen.shape(Color.TRANSPARENT, 8, screen.accent) : null);
                view.setSelected(selected);
                view.setContentDescription((type == Type.POSTER ? "Preview poster " : "Preview backdrop ") + (index + 1)
                        + (selected ? ", selected" : ""));
                view.setOnClickListener(v -> selectAndPreview(item.url));
                view.setFocusable(true);
                if (!item.thumbnail.equals(view.getTag())) {
                    view.setTag(item.thumbnail);
                    ArtworkImages.load(view, item.thumbnail, null);
                }
            }
        }
    }

    private static final class Job {
        final Screen screen;
        Future<?> request;
        int generation;

        Job(Screen screen) {
            this.screen = screen;
        }

        <T> void run(Callable<T> work, Callback<T> success, Callback<String> failure) {
            cancel();
            int ticket = generation;
            request = NETWORK.submit(() -> {
                try {
                    T result = work.call();
                    MAIN.post(() -> {
                        if (ticket == generation && screen.open) success.accept(result);
                    });
                } catch (Exception e) {
                    String message = e instanceof TmdbClient.Failure ? e.getMessage() : "Couldn't complete this request.";
                    MAIN.post(() -> {
                        if (ticket == generation && screen.open) failure.accept(message);
                    });
                }
            });
        }

        void cancel() {
            generation++;
            if (request != null) {
                request.cancel(true);
                request = null;
            }
        }
    }

    private static final class Screen {
        static final int SECONDARY = 0xFFC7C7C7;
        private static final Map<Context, WeakReference<Screen>> TOP = new WeakHashMap<>();
        final Context context;
        private final Dialog dialog;
        final LinearLayout root;
        final int accent;
        final int surface;
        Runnable redraw;
        Runnable onDismiss;
        private Screen parent;
        private boolean open;

        Screen(Context context, String title, String subtitle) {
            this.context = context;
            Prefs.load(context);
            accent = 0xFF000000 | AccentPresets.previewColor(context,
                    Prefs.getString(Prefs.KEY_THEME_ACCENT, AccentPresets.defaultAccent(context)),
                    Prefs.getString(Prefs.KEY_THEME_ACCENT_HEX, ""));
            surface = SurfaceColors.elevated(context);
            dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            root = column();
            root.setPadding(dp(20), dp(14), dp(20), dp(16));
            LinearLayout heading = horizontal();
            TextView titleView = text(title, 20, Color.WHITE);
            titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
            heading.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
            TextView close = button("×", false, this::dismiss);
            close.setContentDescription("Close");
            heading.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
            root.addView(heading);
            if (subtitle != null) note(root, subtitle);
        }

        void show(boolean tall) {
            ScrollView scroll = new ScrollView(context) {
                @Override protected void onMeasure(int widthSpec, int heightSpec) {
                    super.onMeasure(widthSpec, capHeight(heightSpec, tall));
                }
            };
            scroll.setFillViewport(tall);
            scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
            showPanel(scroll);
        }

        void showPanel(View panel) {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(shape(surface, 24, 0));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            dialog.setContentView(panel);
            dialog.setOnDismissListener(ignored -> closed());
            panel.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
                int width = width();
                if (window.getAttributes().width != width) window.setLayout(width, -2);
            });

            parent = nearestOpen(top());
            open = true;
            TOP.put(context, new WeakReference<>(this));
            if (parent != null) parent.dialog.hide();
            try {
                dialog.show();
                window.setLayout(width(), -2);
            } catch (WindowManager.BadTokenException ignored) {
                closed();
            }
        }

        boolean isVisible() {
            return open && top() == this && dialog.isShowing();
        }

        void dismiss() {
            if (!open) return;
            open = false;
            dialog.dismiss();
        }

        private void closed() {
            open = false;
            if (top() == this) {
                Screen previous = nearestOpen(parent);
                if (previous == null) {
                    TOP.remove(context);
                } else {
                    TOP.put(context, new WeakReference<>(previous));
                    // Hidden windows remain attached. A destroyed activity must not reopen them.
                    if (previous.dialog.getWindow().getDecorView().isAttachedToWindow()) {
                        previous.dialog.show();
                    }
                }
            }
            parent = null;
            if (onDismiss != null) onDismiss.run();
        }

        private Screen top() {
            WeakReference<Screen> ref = TOP.get(context);
            return ref == null ? null : ref.get();
        }

        private static Screen nearestOpen(Screen screen) {
            while (screen != null && !screen.open) screen = screen.parent;
            return screen;
        }

        int capHeight(int measureSpec, boolean tall) {
            int max = panelHeight();
            if (View.MeasureSpec.getMode(measureSpec) != View.MeasureSpec.UNSPECIFIED) {
                max = Math.min(max, View.MeasureSpec.getSize(measureSpec));
            }
            return View.MeasureSpec.makeMeasureSpec(max,
                    tall ? View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST);
        }

        LinearLayout body() {
            LinearLayout contents = column();
            root.addView(contents, new LinearLayout.LayoutParams(-1, -2));
            return contents;
        }

        LinearLayout column() {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            return layout;
        }

        LinearLayout horizontal() {
            LinearLayout layout = new LinearLayout(context);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            return layout;
        }

        void share(LinearLayout row, View view) {
            row.addView(view, new LinearLayout.LayoutParams(0, -2, 1));
        }

        TextView text(String value, int size, int color) {
            TextView text = new TextView(context);
            text.setText(value);
            text.setTextSize(size);
            text.setTextColor(color);
            return text;
        }

        TextView note(LinearLayout container, String value) {
            TextView note = text(value, 14, SECONDARY);
            note.setPadding(0, dp(3), 0, dp(8));
            note.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            container.addView(note);
            return note;
        }

        void section(LinearLayout container, String title) {
            TextView text = text(title.toUpperCase(java.util.Locale.ROOT), 12, accent);
            text.setTypeface(text.getTypeface(), Typeface.BOLD);
            text.setLetterSpacing(0.08f);
            text.setPadding(0, dp(16), 0, dp(4));
            container.addView(text);
        }

        void row(LinearLayout container, String title, String subtitle, Runnable action) {
            LinearLayout row = column();
            row.setMinimumHeight(dp(48));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(7), dp(6), dp(7));
            row.addView(text(title, 16, Color.WHITE));
            if (subtitle != null) row.addView(text(subtitle, 14, SECONDARY));
            row.setBackground(ripple(Color.TRANSPARENT, 10));
            row.setOnClickListener(view -> action.run());
            row.setFocusable(true);
            container.addView(row);
        }

        TextView button(String label, boolean primary, Runnable click) {
            android.widget.Button button = new android.widget.Button(context);
            button.setText(label);
            button.setTextSize(14);
            button.setTypeface(button.getTypeface(), Typeface.BOLD);
            button.setAllCaps(false);
            button.setGravity(Gravity.CENTER);
            button.setMinimumHeight(dp(48));
            button.setBackgroundTintList(null);
            button.setPadding(dp(8), dp(5), dp(8), dp(5));
            paint(button, primary);
            if (click != null) button.setOnClickListener(view -> click.run());
            return button;
        }

        TextView outlineButton(String label, Runnable click) {
            TextView button = button(label, false, click);
            GradientDrawable border = shape(surface, 12, 0);
            border.setStroke(dp(1), (accent & 0x00FFFFFF) | 0x90000000);
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x28FFFFFF), border,
                    shape(Color.WHITE, 12, 0)));
            LinearLayout.LayoutParams spacing = new LinearLayout.LayoutParams(-1, -2);
            spacing.setMargins(0, dp(4), 0, dp(8));
            button.setLayoutParams(spacing);
            return button;
        }

        TextView dropdown(String label, Runnable click) {
            TextView button = outlineButton(label, click);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setPaddingRelative(dp(12), dp(5), dp(10), dp(5));
            android.graphics.drawable.Drawable chevron = Glyphs.chevron(accent,
                    context.getResources().getDisplayMetrics().density, false);
            chevron.setBounds(0, 0, dp(20), dp(20));
            button.setCompoundDrawablePadding(dp(8));
            button.setCompoundDrawablesRelative(null, null, chevron, null);
            return button;
        }

        void paint(TextView button, boolean primary) {
            int enabled = primary ? (AccentPresets.isLight(accent) ? 0xFF141414 : Color.WHITE) : accent;
            button.setTextColor(new ColorStateList(
                    new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                    new int[]{0xFF999999, enabled}));
            android.graphics.drawable.StateListDrawable background = new android.graphics.drawable.StateListDrawable();
            background.addState(new int[]{-android.R.attr.state_enabled}, shape(surface, 14, 0));
            background.addState(new int[]{}, ripple(primary ? accent : Color.TRANSPARENT, 14));
            button.setBackground(background);
        }

        private RippleDrawable ripple(int color, int radius) {
            return new RippleDrawable(ColorStateList.valueOf(0x28FFFFFF), shape(color, radius, 0),
                    shape(Color.WHITE, radius, 0));
        }

        EditText input(String hint, int inputType) {
            EditText input = new EditText(context);
            input.setSingleLine(true);
            input.setInputType(inputType);
            input.setHint(hint);
            input.setContentDescription(hint);
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(SECONDARY);
            input.setBackgroundTintList(ColorStateList.valueOf(accent));
            input.setSaveEnabled(false);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            }
            return input;
        }

        void message(String title, String body) {
            Screen message = new Screen(context, title, body);
            message.root.addView(message.button("OK", true, message::dismiss));
            message.show(false);
        }

        GradientDrawable shape(int color, int radius, int stroke) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(color);
            drawable.setCornerRadius(dp(radius));
            if (stroke != 0) drawable.setStroke(dp(3), stroke);
            return drawable;
        }

        int dp(int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }

        int width() {
            return Math.max(1, Math.min(usableWindow().width() - dp(32), dp(600)));
        }

        int panelHeight() {
            return Math.max(1, usableWindow().height() * 86 / 100);
        }

        private android.graphics.Rect usableWindow() {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                WindowManager manager = context.getSystemService(WindowManager.class);
                if (manager != null) {
                    android.view.WindowMetrics metrics = manager.getCurrentWindowMetrics();
                    android.graphics.Rect bounds = new android.graphics.Rect(metrics.getBounds());
                    android.graphics.Insets bars = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                            android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                    bounds.inset(bars.left, bars.top, bars.right, bars.bottom);
                    return bounds;
                }
            }
            android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            return new android.graphics.Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
        }
    }
}
