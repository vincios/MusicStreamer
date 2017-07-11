package com.vincios.musicstreamer2.connectors.chiasenhac.model;

import android.os.Bundle;

import com.vincios.musicstreamer2.connectors.ConnectorConstants;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.chiasenhac.ChiasenhacConnector;

public class ChiasenhacSong implements Song {
    private String id;
    private String title;
    private String artist;
    private int lenght;
    private String bitrate;
    private String size;
    private String link;
    private String album;
    private String lenghtString;
    private boolean saved;
    private Bundle extras;

    private int downloads;

    public ChiasenhacSong(String id, String title, String artist, String album, String bitrate, String lenghtString, int downloads, String infoPageLink) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.bitrate = bitrate;
        this.lenghtString = lenghtString;
        this.saved = false;
        this.downloads = downloads;

        if(album.isEmpty()){
            album = "n/a";
        }
        this.album = album;

        extras = new Bundle();
        extras.putString(ChiasenhacConnector.EXTRAS_INFORMATION_PAGE_LINK, infoPageLink);

        String[] lSplit = lenghtString.split(":");
        int minutes = Integer.parseInt(lSplit[0]);
        int seconds = Integer.parseInt(lSplit[1]);

        this.lenght = ((minutes * 60) + seconds) * 1000;
    }

    public int getDownloads() {
        return downloads;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getArtist() {
        return artist;
    }

    @Override
    public String getAlbum() {
        return album;
    }

    @Override
    public String getBitrate() {
        return bitrate;
    }

    @Override
    public String getLengthString() {
        return lenghtString;
    }

    @Override
    public int getLength() {
        return lenght;
    }

    @Override
    public String getSize() {
        return size;
    }

    @Override
    public String getLink() {
        return link;
    }

    @Override
    public String getHost() {
        return ConnectorConstants.HOST_CHIASENHAC;
    }

    @Override
    public boolean isSaved() {
        return saved;
    }

    @Override
    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    @Override
    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public void setSize(String size) {
        this.size = size;
    }

    public Bundle getExtras() {
        return extras;
    }

    public void setExtras(Bundle extras) {
        this.extras = extras;
    }

    @Override
    public String toString() {
        return "ChiasenhacSong{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", lenght=" + lenght +
                ", bitrate='" + bitrate + '\'' +
                ", size='" + size + '\'' +
                ", link='" + link + '\'' +
                ", album='" + album + '\'' +
                ", lenghtString='" + lenghtString + '\'' +
                ", saved=" + saved +
                ", downloads=" + downloads +
                '}';
    }
}
