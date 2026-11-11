package app.template.extension.artwork;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** One preference per film. Profile choices use a separate namespace keyed by member ID. */
public final class ArtworkStore {
    public enum Type {
        POSTER("poster"), BACKDROP("backdrop");

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }

    public static final class Film {
        public final String id;
        public final String title;
        public final int year;
        public final String imdb;
        public final String tmdb;
        public final String language;
        public final String originalBackdrop;

        public Film(String id, String title, int year, String imdb, String tmdb,
                    String language, String originalBackdrop) {
            this.id = id;
            this.title = title;
            this.year = year;
            this.imdb = imdb;
            this.tmdb = tmdb;
            this.language = language;
            this.originalBackdrop = originalBackdrop;
        }

        public String label() {
            if (title.isEmpty()) return "Film " + id;
            return year > 0 ? title + " (" + year + ")" : title;
        }
    }

    private static final String FILM = "film:";
    private static final String PROFILE = "profile:";
    private final SharedPreferences preferences;

    public ArtworkStore(Context context) {
        preferences = context.getSharedPreferences("morphe_custom_artwork", Context.MODE_PRIVATE);
    }

    public String url(String filmId, Type type) {
        if (!ArtworkRules.validId(filmId)) return null;
        return ArtworkRules.imageUrl(entry(filmId).optString(type.label, null));
    }

    public String profile(String account) {
        if (!ArtworkRules.validId(account)) return null;
        return ArtworkRules.imageUrl(preferences.getString(PROFILE + account, null));
    }

    public Film film(String id) {
        return film(id, entry(id));
    }

    public List<Film> films() {
        List<Film> films = new ArrayList<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (!entry.getKey().startsWith(FILM) || !(entry.getValue() instanceof String)) continue;
            String id = entry.getKey().substring(FILM.length());
            JSONObject data = parse((String) entry.getValue());
            if (ArtworkRules.validId(id) && hasArtwork(data)) films.add(film(id, data));
        }
        Collections.sort(films, (a, b) -> a.label().compareToIgnoreCase(b.label()));
        return films;
    }

    public void save(Film film, Type type, String url) {
        String image = ArtworkRules.imageUrl(url);
        if (!ArtworkRules.validId(film.id) || image == null) return;
        JSONObject data = entry(film.id);
        put(data, type.label, image);
        if (!film.title.isEmpty()) put(data, "title", film.title);
        if (film.year > 0) put(data, "year", film.year);
        if (film.imdb != null) put(data, "imdb", film.imdb);
        if (film.tmdb != null) put(data, "tmdb", film.tmdb);
        if (film.language != null) put(data, "language", film.language);
        if (film.originalBackdrop != null) put(data, "originalBackdrop", film.originalBackdrop);
        preferences.edit().putString(FILM + film.id, data.toString()).apply();
        ArtworkHooks.refreshFilm(film.id, type);
    }

    public void restore(String id, Type type) {
        if (!ArtworkRules.validId(id)) return;
        JSONObject data = entry(id);
        if (type == null) {
            data.remove(Type.POSTER.label);
            data.remove(Type.BACKDROP.label);
        } else {
            data.remove(type.label);
        }
        write(preferences.edit(), id, data).apply();
        ArtworkHooks.refreshFilm(id, type);
    }

    public void saveProfile(String account, String url) {
        if (!ArtworkRules.ownsProfile(account, ArtworkHooks.account())) return;
        String image = ArtworkRules.imageUrl(url);
        if (url != null && image == null) return;
        SharedPreferences.Editor editor = preferences.edit();
        if (image == null) editor.remove(PROFILE + account);
        else editor.putString(PROFILE + account, image);
        editor.apply();
        ArtworkHooks.refreshProfiles();
    }

    /** A null type clears both film image types. Profile removal is current-account only. */
    public void clear(Type type, boolean films, String account) {
        SharedPreferences.Editor editor = preferences.edit();
        if (films) {
            for (Film film : films()) {
                JSONObject data = entry(film.id);
                if (type == null) {
                    data.remove(Type.POSTER.label);
                    data.remove(Type.BACKDROP.label);
                } else {
                    data.remove(type.label);
                }
                write(editor, film.id, data);
            }
        }
        if (ArtworkRules.ownsProfile(account, ArtworkHooks.account())) {
            editor.remove(PROFILE + account);
        }
        editor.apply();
        if (films) ArtworkHooks.refreshFilm(null, type);
        if (account != null) ArtworkHooks.refreshProfiles();
    }

    public String exportJson(String account) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", "letterboxd-morphe-artwork");
        root.put("format", 1);
        JSONArray films = new JSONArray();
        for (Film film : films()) {
            JSONObject data = entry(film.id);
            data.put("id", film.id);
            films.put(data);
        }
        root.put("films", films);
        String profile = profile(account);
        if (profile != null && ArtworkRules.ownsProfile(account, ArtworkHooks.account())) {
            JSONObject data = new JSONObject();
            data.put("member", account);
            data.put("backdrop", profile);
            root.put("profile", data);
        }
        return root.toString(2);
    }

    /** Merges valid entries into unused slots. Explicit replacement stays in the picker. */
    public int importJson(String text, String account) throws JSONException {
        JSONObject root = new JSONObject(text);
        if (!"letterboxd-morphe-artwork".equals(root.optString("app"))
                || root.optInt("format") != 1) throw new JSONException("Unknown artwork format");
        JSONArray rows = root.getJSONArray("films");
        SharedPreferences.Editor editor = preferences.edit();
        int count = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject source = rows.optJSONObject(i);
            if (source == null) continue;
            String id = source.optString("id");
            if (!ArtworkRules.validId(id) || !seen.add(id)) continue;
            JSONObject target = entry(id);
            boolean changed = false;
            for (Type type : Type.values()) {
                String url = ArtworkRules.imageUrl(source.optString(type.label, null));
                if (url == null || target.has(type.label)) continue;
                target.put(type.label, url);
                changed = true;
                count++;
            }
            if (changed) {
                Film film = film(id, source);
                if (!target.has("title") && !film.title.isEmpty()) target.put("title", film.title);
                if (!target.has("year") && film.year > 0) target.put("year", film.year);
                if (!target.has("imdb") && film.imdb != null) target.put("imdb", film.imdb);
                if (!target.has("tmdb") && film.tmdb != null) target.put("tmdb", film.tmdb);
                if (!target.has("language") && film.language != null) target.put("language", film.language);
                if (!target.has("originalBackdrop") && film.originalBackdrop != null) {
                    target.put("originalBackdrop", film.originalBackdrop);
                }
                write(editor, id, target);
            }
        }
        JSONObject profile = root.optJSONObject("profile");
        if (profile != null && ArtworkRules.ownsProfile(profile.optString("member"), account)
                && ArtworkRules.ownsProfile(account, ArtworkHooks.account()) && profile(account) == null) {
            String url = ArtworkRules.imageUrl(profile.optString("backdrop"));
            if (url != null) {
                editor.putString(PROFILE + account, url);
                count++;
            }
        }
        editor.apply();
        if (count > 0) {
            ArtworkHooks.refreshFilm(null, null);
            ArtworkHooks.refreshProfiles();
        }
        return count;
    }

    private JSONObject entry(String id) {
        return parse(preferences.getString(FILM + id, null));
    }

    private static JSONObject parse(String data) {
        if (data != null) {
            try {
                return new JSONObject(data);
            } catch (JSONException ignored) {
            }
        }
        return new JSONObject();
    }

    private static Film film(String id, JSONObject data) {
        String title = data.optString("title");
        return new Film(id, title.length() <= 500 ? title : "", data.optInt("year", 0),
                match(data, "imdb", "tt[0-9]{5,15}"), match(data, "tmdb", "[0-9]{1,10}"),
                match(data, "language", "[a-z]{2}"),
                ArtworkRules.imageUrl(data.optString("originalBackdrop", null)));
    }

    private static String match(JSONObject data, String key, String pattern) {
        String value = data.optString(key);
        return value.matches(pattern) ? value : null;
    }

    private static boolean hasArtwork(JSONObject data) {
        return data.has(Type.POSTER.label) || data.has(Type.BACKDROP.label);
    }

    private static SharedPreferences.Editor write(SharedPreferences.Editor editor, String id, JSONObject data) {
        return hasArtwork(data) ? editor.putString(FILM + id, data.toString()) : editor.remove(FILM + id);
    }

    private static void put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
