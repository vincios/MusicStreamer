package com.vincios.musicstreamer2.connectors.pleer.pleermodel;


import android.text.format.DateUtils;

import com.vincios.musicstreamer2.connectors.ConnectorConstants;
import com.vincios.musicstreamer2.connectors.Song;

import java.io.Serializable;
import java.sql.Time;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PleerSongInterfaceAdapter implements Song {

    private PleerModelTrack track;
    private boolean saved;

    public PleerSongInterfaceAdapter(PleerModelTrack track) {
        this.track = track;
    }

    @Override
    public String getId() {
        return track.getTrackId();
    }
    @Override
    public String getTitle() {
        return track.getTrack();
    }

    @Override
    public String getArtist() {
        return track.getArtist();
    }

    @Override
    public String getAlbum() {
        return "n/a";
    }

    @Override
    public String getBitrate() {
        return track.getBitrate();
    }

    @Override
    public String getLengthString() {
        int length = getLength();
        //long minutes = TimeUnit.MILLISECONDS.toMinutes(length);
        //long seconds = TimeUnit.MILLISECONDS.toSeconds(length);
        //int minutes = length / 60;
        //int seconds = length % 60;

        //return String.format(Locale.getDefault(), "%02d:%02d",minutes ,seconds);
        return DateUtils.formatElapsedTime(length / 1000);
    }

    @Override
    public int getLength() {
        return track.getLenght() * 1000; //Pleer.net gives length in seconds, but in Song Interface length must be in milliseconds
    }

    @Override
    public String getSize() {
        return track.getSize();
    }

    @Override
    public String getLink() {
        return track.getLink();
    }

    @Override
    public String getHost() {
        return ConnectorConstants.HOST_PLEER;
    }

    @Override
    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved){
        this.saved = saved;
    }

    @Override
    public void setLink(String link) {
        track.setLink(link);
    }

    @Override
    public void setSize(String size) {
        track.setSize(size);
    }

    @Override
    public String toString() {
        return "PleerSongInterfaceAdapter{" +
                "track=" + track +
                '}';
    }
}
