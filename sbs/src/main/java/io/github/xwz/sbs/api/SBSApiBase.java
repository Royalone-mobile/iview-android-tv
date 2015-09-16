package io.github.xwz.sbs.api;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.xwz.base.api.HttpApiBase;
import io.github.xwz.base.models.IEpisodeModel;
import io.github.xwz.sbs.BuildConfig;

abstract class SBSApiBase extends HttpApiBase {

    private static final String API_URL = BuildConfig.API_URL;
    private static final String RELATED_URL = BuildConfig.RELATED_URL;
    private static final String SMIL_URL = BuildConfig.SMIL_URL;
    private static final String AUTH_TOKEN = BuildConfig.AUTH_TOKEN;

    // http://www.sbs.com.au/api/video_feed/related?context=android&form=json&id=517455939941
    // http://www.sbs.com.au/api/video_feed/smil?context=android&form=xml&id=517455939941

    // Basic YW5kcm9pZDliYTNmMTgzYmM1MjY5MzI4YTM1YTBlNmQzYmJmMGYwMmY5ZTFhZWE6YW5kcm9pZA==

    private static final String CACHE_PATH = "sbs-api";

    private int lastEntryCount = 0;

    public SBSApiBase(Context context) {
        super(context);
    }

    protected String getCachePath() {
        return CACHE_PATH;
    }

    Uri buildApiUrl(Map<String, String> params) {
        return buildUrl(API_URL, params);
    }

    Uri buildRelatedUrl(Map<String, String> params) {
        return buildUrl(RELATED_URL, params);
    }

    Uri buildVideoUrl(Map<String, String> params) {
        return buildUrl(SMIL_URL, params);
    }

    private Uri buildUrl(String path, Map<String, String> params) {
        Uri.Builder uri = Uri.parse(path).buildUpon();
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                uri.appendQueryParameter(param.getKey(), param.getValue());
            }
        }
        return uri.build();
    }

    protected int getLastEntryCount() {
        return lastEntryCount;
    }

    List<IEpisodeModel> fetchContent(Uri url, int staleness) {
        InputStream response = fetchStream(url, staleness);
        List<IEpisodeModel> all = new ArrayList<>();
        if (response != null) {
            try {
                JsonReader reader = new JsonReader(new InputStreamReader(response, "UTF-8"));
                reader.beginObject();
                while (reader.hasNext()) {
                    JsonToken token = reader.peek();
                    if (token == JsonToken.NAME) {
                        String name = reader.nextName();
                        if (name.equals("entryCount")) {
                            lastEntryCount = reader.nextInt();
                        } else if (name.equals("entries")) {
                            all.addAll(readEntriesArray(reader));
                        } else {
                            reader.skipValue();
                        }
                    }
                }
                reader.endObject();
                reader.close();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return all;
    }

    private List<IEpisodeModel> readEntriesArray(JsonReader reader) throws IOException {
        reader.beginArray();
        Gson gson = new GsonBuilder().create();
        List<IEpisodeModel> all = new ArrayList<>();
        while (reader.hasNext()) {
            try {
                Entry entry = gson.fromJson(reader, Entry.class);
                IEpisodeModel ep = EpisodeModel.create(entry);
                all.add(ep);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        reader.endArray();
        return all;
    }

    static class Category {
        @SerializedName("media$name")
        private String name;

        @SerializedName("media$scheme")
        private String scheme;

        @SerializedName("media$label")
        private String label;

        public String toString() {
            return name + " : " + scheme + " : " + label;
        }

        public String getName() {
            if (name != null) {
                String[] parts = name.split("/");
                return parts.length > 1 ? parts[1] : name;
            }
            return name;
        }

        public String getScheme() {
            if (scheme != null && scheme.length() > 0) {
                return scheme;
            }
            if (name != null) {
                String[] parts = name.split("/");
                return parts.length > 1 ? parts[0] : scheme;
            }
            return scheme;
        }
    }

    static class Thumbnail {
        @SerializedName("plfile$height")
        private int height;

        @SerializedName("plfile$width")
        private int width;

        @SerializedName("plfile$downloadUrl")
        private String url;

        public String toString() {
            return width + "x" + height + ":" + url;
        }
    }

    static class Content {
        @SerializedName("plfile$duration")
        private String duration;

        @SerializedName("plfile$bitrate")
        private int bitrate;

        @SerializedName("plfile$downloadUrl")
        private String url;
    }

    static class Rating {
        private String rating;
    }

    public static class Entry {
        String id;
        String guid;
        String title;
        String author;
        String description;

        @SerializedName("plmedia$defaultThumbnailUrl")
        String thumbnail;

        @SerializedName("pl1$programName")
        String series;

        @SerializedName("pl1$seriesId")
        String seriesId;

        @SerializedName("pl1$shortSynopsis")
        String synopsis;

        @SerializedName("media$categories")
        private List<Category> categories;

        @SerializedName("media$thumbnails")
        private List<Thumbnail> thumbnails;

        @SerializedName("media$content")
        private List<Content> contents;

        @SerializedName("media$ratings")
        private List<Rating> ratings;

        public String toString() {
            return id + " : " + title + " : " + series;
        }

        public String getThumbnail() {
            Thumbnail largest = null;
            if (thumbnails != null) {
                for (Thumbnail t : thumbnails) {
                    if (largest == null || t.width > largest.width) {
                        largest = t;
                    }
                }
            }
            if (largest != null) {
                return largest.url;
            }
            return thumbnail;
        }

        public List<String> getCategories() {
            List<String> cats = new ArrayList<>();
            if (categories != null) {
                for (Category c : categories) {
                    String scheme = c.getScheme();
                    if ("Channel".equals(scheme) || "Genre".equals(scheme) || "Film".equals(scheme)) {
                        String name = c.getName();
                        if (name != null && !scheme.equals(name)) {
                            cats.add(scheme + "/" + name);
                        }
                    }
                }
            }
            return cats;
        }

        public String getChannel() {
            if (categories != null) {
                for (Category c : categories) {
                    if ("Channel".equals(c.scheme)) {
                        String name = c.getName();
                        if (name != null) {
                            return name;
                        }
                    }
                }
            }
            return "";
        }

        public String getRating() {
            if (ratings != null && ratings.size() > 0) {
                String details = ratings.get(0).rating;
                return details != null ? details.toUpperCase() : details;
            }
            return "";
        }

        public int getDuration() {
            if (contents != null && contents.size() > 0) {
                try {
                    return Math.round(Float.parseFloat(contents.get(0).duration));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    }
}
