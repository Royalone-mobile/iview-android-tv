package io.github.xwz.sbs.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.xwz.base.ImmutableMap;
import io.github.xwz.base.content.IContentManager;
import io.github.xwz.base.models.IEpisodeModel;
import io.github.xwz.sbs.content.ContentManager;

public class SBSRelatedApi extends SBSApiBase {
    private static final String TAG = "SBSRelatedApi";
    private static final int CACHE_EXPIRY = 3600; // 1h
    private static final Pattern ID_PATTERN = Pattern.compile("/(\\d+)$");

    private final String id;
    private boolean success = false;

    public SBSRelatedApi(Context context, String id) {
        super(context);
        this.id = id;
    }

    @Override
    protected Void doInBackground(String... urls) {
        if (urls.length > 0) {
            updateEpisode(urls[0]);
        }
        return null;
    }

    private boolean updateEpisode(String url) {
        EpisodeModel current = (EpisodeModel) ContentManager.getInstance().getEpisode(url);
        if (current != null) {
            Log.d(TAG, "Fetched related info for: " + current);
            String series = current.getSeriesTitle();
            List<IEpisodeModel> related = fetchRelated(url);
            if (related != null) {
                List<IEpisodeModel> more = new ArrayList<>();
                for (IEpisodeModel ep : related) {
                    if (series != null && !series.equals(ep.getSeriesTitle())) {
                        more.add(ep);
                    }
                }
                if (more.size() > 0) {
                    current.setOtherEpisodes(IContentManager.MORE_LIKE_THIS, more);
                }
                current.setHasExtra(true);
                current.setHasFetchedRelated(true);
                success = true;
            }
        } else {
            Log.d(TAG, "Unable to find current episode");
        }
        return false;
    }

    private List<IEpisodeModel> fetchRelated(String url) {
        Matcher m = ID_PATTERN.matcher(url);
        if (m.find()) {
            String id = m.group(1);
            return fetchContent(getRelatedUrl(id), CACHE_EXPIRY);
        }
        return null;
    }

    private Uri getRelatedUrl(String id) {
        Map<String, String> params = ImmutableMap.of("context", "android", "form", "json", "id", id);
        return buildRelatedUrl(params);
    }

    protected void onPreExecute() {
        ContentManager.getInstance().broadcastChange(ContentManager.CONTENT_EPISODE_START, id);
    }

    protected void onPostExecute(Void v) {
        if (success) {
            ContentManager.getInstance().broadcastChange(ContentManager.CONTENT_EPISODE_DONE, id);
        } else {
            ContentManager.getInstance().broadcastChange(ContentManager.CONTENT_EPISODE_ERROR, id);
        }
    }
}