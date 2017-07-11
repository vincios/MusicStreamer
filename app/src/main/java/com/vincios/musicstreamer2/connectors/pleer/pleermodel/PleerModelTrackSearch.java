package com.vincios.musicstreamer2.connectors.pleer.pleermodel;

import java.util.ArrayList;
import java.util.List;

public class PleerModelTrackSearch {
    private boolean success;
    private int count;
    private List<PleerModelTrack> tracks;

    public PleerModelTrackSearch() {
        this.success = false;
        this.count = 0;
        this.tracks = new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public int getCount() {
        return count;
    }

    public List<PleerModelTrack> getTracks() {
        return tracks;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setTracks(List<PleerModelTrack> tracks) {
        this.tracks = tracks;
    }

    public void addTrack(PleerModelTrack track){
        this.tracks.add(track);
    }
}
