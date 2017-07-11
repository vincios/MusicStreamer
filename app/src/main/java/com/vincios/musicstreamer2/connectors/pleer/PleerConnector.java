package com.vincios.musicstreamer2.connectors.pleer;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.vincios.musicstreamer2.connectors.Connector;
import com.vincios.musicstreamer2.connectors.ConnectorException;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.SongLinkValue;
import com.vincios.musicstreamer2.connectors.pleer.pleermodel.PleerModelToken;
import com.vincios.musicstreamer2.connectors.pleer.pleermodel.PleerModelTrack;
import com.vincios.musicstreamer2.connectors.pleer.pleermodel.PleerModelTrackSearch;
import com.vincios.musicstreamer2.connectors.pleer.pleermodel.PleerSongInterfaceAdapter;
import com.vincios.musicstreamer2.connectors.pleer.token.ExpiredTokenException;
import com.vincios.musicstreamer2.connectors.pleer.token.PleerToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PleerConnector implements Connector{

    private PleerConnection connection;
    private Gson parser;

    public PleerConnector() {
        this.connection = new PleerConnection();
        this.parser = new Gson();
    }

    @Override
    public List<Song> search(String name, @Nullable String artist, @Nullable Bundle extras) throws ConnectorException {
        String a = (artist !=  null) ? artist : "";

        final Map<String, String> searchParams = new HashMap<>(3);

        searchParams.put(PleerConstants.POST_KEY_METHOD, PleerConstants.METHOD_FIND);
        searchParams.put(PleerConstants.POST_KEY_QUERY, name + " " +a);

        JSONObject response;

        try {
            response = startRequest(searchParams);
        } catch (ExpiredTokenException e) {
            PleerModelToken newToken = getNewToken();
            PleerToken.getInstance().setNewToken(newToken);
            response = startRequest(searchParams);
        }

        if(response == null)
            throw new ConnectorException("Response is null", PleerException.ERROR_RESPONSE_PARSING);

        PleerModelTrackSearch search;

        try {
            search = new PleerModelTrackSearch();
            search.setSuccess(response.getBoolean("success"));
            search.setCount(response.getInt("count"));
            if (search.getCount() > 0) {
                JSONObject results = response.getJSONObject("tracks");

                Iterator<String> tracks = results.keys();
                while (tracks.hasNext()) {
                    String key = tracks.next();
                    String trackStr = results.getString(key);

                    Log.d("TrackStr", trackStr);
                    search.addTrack(new Gson().fromJson(trackStr, PleerModelTrack.class));
                }
            }
        } catch (JSONException e) {
            throw new ConnectorException(e.getMessage(), e.getCause(), PleerException.ERROR_RESPONSE_PARSING);
        }

        return adaptPleerModelTrackList(search.getTracks());
    }

    @Override
    public SongLinkValue getLink(Song song, @Nullable Bundle extras) throws ConnectorException{
        String songId = song.getId();
        final Map<String, String> requestParams = new HashMap<>(4);
        requestParams.put(PleerConstants.POST_KEY_METHOD, PleerConstants.METHOD_GET_DOWNLOAD_LINK);
        requestParams.put(PleerConstants.POST_KEY_REASON, PleerConstants.POST_VALUE_REASON);
        requestParams.put(PleerConstants.POST_KEY_TRACK_ID, songId);

        JSONObject response;
        try {
            response = startRequest(requestParams);
        } catch (ExpiredTokenException e) {
            PleerModelToken newToken = getNewToken();
            PleerToken.getInstance().setNewToken(newToken);
            response = startRequest(requestParams);
        }

        if(response == null)
            throw new ConnectorException("Response is null", PleerException.ERROR_RESPONSE_PARSING);

        try {
            String link = response.getString("url");
            return new SongLinkValue(songId, link);
        } catch (JSONException e) {
            throw new ConnectorException(e.getMessage(), e.getCause(), PleerException.ERROR_RESPONSE_PARSING);
        }

    }

    @Override
    public Song getSong(String songId, @Nullable Bundle extras) {
        return null;
    }


    private JSONObject startRequest(final Map<String, String> params) throws ConnectorException {

        if (PleerToken.getInstance().isExpired()) {
            throw new ExpiredTokenException("Token is expired");
        }

        params.put(PleerConstants.POST_KEY_ACCESS_TOKEN, PleerToken.getInstance().getToken());

        try {
            return connection.executeRequest(params);
        } catch (PleerException e) {
            throw new ConnectorException(e.getMessage(), e.getCause(), e.getReasonCode());
        }
    }

    private PleerModelToken getNewToken() throws PleerException {
        Map<String, String> params = PleerConstants.tokenRequestParams;

        try {
            JSONObject response = connection.executeRequest(params);
            return parser.fromJson(response.toString(), PleerModelToken.class);

        } catch (PleerException e) {
            throw new PleerException("Error in request new token: " + e.getMessage(),
                    e.getCause(),
                    e.getReasonCode());
        }
    }

    private  List<Song> adaptPleerModelTrackList(List<PleerModelTrack> tracks){
        List<Song> songs = new ArrayList<>(tracks.size());

        for(PleerModelTrack track : tracks){
            Song song = new PleerSongInterfaceAdapter(track);
            songs.add(song);
        }

        return songs;
    }
}
