package io.github.xwz.sbs.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.xwz.base.Utils;
import io.github.xwz.base.content.IContentManager;
import io.github.xwz.base.models.IEpisodeModel;

public class EpisodeModel implements IEpisodeModel {
    private static final String TAG = "EpisodeModel";

    private String seriesTitle;
    private String href;
    private String channel;
    private String thumbnail;
    private String livestream;
    private String episodeHouseNumber;
    private Set<String> categories = new HashSet<>();
    private String title;
    private int duration;
    private String rating;
    private int episodeCount;

    private String description;
    private String related;
    private String availability;
    private String stream;
    private String captions;
    private String share;

    private boolean extras = false;
    private boolean fetchedRelated = false;

    private Map<String, List<IEpisodeModel>> others = new HashMap<>();

    public static IEpisodeModel create(SBSApi.Entry data) {
        EpisodeModel ep = new EpisodeModel();
        ep.set(data);
        return ep;
    }

    private void set(SBSApi.Entry data) {
        seriesTitle = data.series;
        href = data.id;

        channel = data.getChannel();
        thumbnail = data.getThumbnail();
        livestream = null;
        episodeHouseNumber = data.guid;
        title = data.title;
        duration = data.getDuration();
        rating = data.getRating();
        episodeCount = 0;
        categories.addAll(data.getCategories());

        description = data.synopsis;
    }

    private void merge(EpisodeModel ep) {
        this.seriesTitle = ep.seriesTitle == null ? this.seriesTitle : ep.seriesTitle;
        this.href = ep.href == null ? this.href : ep.href;
        this.channel = ep.channel == null ? this.channel : ep.channel;
        this.thumbnail = ep.thumbnail == null ? this.thumbnail : ep.thumbnail;
        this.livestream = ep.livestream == null ? this.livestream : ep.livestream;
        this.episodeHouseNumber = ep.episodeHouseNumber == null ? this.episodeHouseNumber : ep.episodeHouseNumber;
        this.title = ep.title == null ? this.title : ep.title;
        this.duration = ep.duration;
        this.rating = ep.rating == null ? this.rating : ep.rating;
        this.episodeCount = ep.episodeCount;
        this.description = ep.description == null ? this.description : ep.description;
        this.related = ep.related == null ? this.related : ep.related;
        this.availability = ep.availability == null ? this.availability : ep.availability;
        this.stream = ep.stream == null ? this.stream : ep.stream;
        this.captions = ep.captions == null ? this.captions : ep.captions;
        this.share = ep.share == null ? this.share : ep.share;

        this.categories.addAll(ep.categories);
        this.others = ep.others.size() == 0 ? this.others : new LinkedHashMap<>(ep.others);

        this.extras = this.stream != null;
    }

    @Override
    public void merge(IEpisodeModel ep) {
        merge((EpisodeModel) ep);
    }

    public void setOtherEpisodes(Map<String, List<IEpisodeModel>> more) {
        others = more;
        if (more.containsKey(IContentManager.OTHER_EPISODES)) {
            if (more.get(IContentManager.OTHER_EPISODES).size() > 1) {
                episodeCount = more.get(IContentManager.OTHER_EPISODES).size() + 1;
                setHasExtra(true);
            }
        }
    }

    public void setOtherEpisodes(String cat, List<IEpisodeModel> more) {
        others.put(cat, more);
    }

    public void setHasExtra(boolean extra) {
        extras = extra;
    }

    public void setHasFetchedRelated(boolean fetched) {
        fetchedRelated = fetched;
    }

    @Override
    public void setEpisodeCount(int count) {
        episodeCount = count;
    }

    public Map<String, List<IEpisodeModel>> getOtherEpisodes() {
        return new LinkedHashMap<>(others);
    }

    private List<IEpisodeModel> getOtherEpisodes(String cat) {
        for (Map.Entry<String, List<IEpisodeModel>> episodes : getOtherEpisodes().entrySet()) {
            if (episodes.getKey().equals(cat)) {
                return episodes.getValue();
            }
        }
        return new ArrayList<>();
    }

    public List<String> getOtherEpisodeUrls(String cat) {
        List<String> urls = new ArrayList<>();
        for (IEpisodeModel ep : getOtherEpisodes(cat)) {
            urls.add(ep.getHref());
        }
        return urls;
    }

    public boolean matches(String query) {
        boolean found = false;
        if (getSeriesTitle() != null) {
            found = found || getSeriesTitle().toLowerCase().contains(query);
        }
        if (getTitle() != null) {
            found = found || getTitle().toLowerCase().contains(query);
        }
        return found;
    }

    private static boolean getBoolean(JSONObject data, String key, boolean fallback) {
        if (data != null && key != null && data.has(key)) {
            try {
                return data.getBoolean(key);
            } catch (JSONException e) {
                //Log.d(TAG, "No boolean value for: " + key);
            }
        }
        return fallback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EpisodeModel))
            return false;
        EpisodeModel other = (EpisodeModel) o;
        return other.getHref().equals(this.getHref());
    }

    public String getDurationText() {
        if (getRating() != null) {
            return getRating() + ", " + Utils.formatMillis(getDuration() * 1000);
        } else {
            return Utils.formatMillis(getDuration() * 1000);
        }
    }

    public void addCategory(String cat) {
        categories.add(cat);
    }

    public void setCategories(List<String> cats) {
        categories = new HashSet<>(cats);
    }

    public String toString() {
        return getHref() + ": '" + getSeriesTitle() + "' - '" + getTitle() + "'";
    }

    public String getSeriesTitle() {
        return seriesTitle;
    }

    public String getHref() {
        return href;
    }

    public String getChannel() {
        return channel;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getLivestream() {
        return livestream;
    }

    public String getEpisodeHouseNumber() {
        return episodeHouseNumber;
    }

    public List<String> getCategories() {
        return new ArrayList<>(categories);
    }

    public String getTitle() {
        return title;
    }

    public int getDuration() {
        return duration;
    }

    public String getRating() {
        return rating;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public String getDescription() {
        return description;
    }

    public String getRelated() {
        return related;
    }

    public String getAvailability() {
        return availability;
    }

    public String getStream() {
        return stream;
    }

    public String getCaptions() {
        return captions;
    }

    public String getShare() {
        return share;
    }

    public boolean hasExtras() {
        return extras;
    }

    public boolean hasOtherEpisodes() {
        return fetchedRelated || others.containsKey(IContentManager.MORE_LIKE_THIS);
    }
}
