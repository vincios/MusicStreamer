package com.vincios.musicstreamer2.connectors;


import android.text.format.DateUtils;
import android.util.Log;

public class SongBase implements Song {

    private String id;
    private String artist;
    private String title;
    private int length;
    private String bitrate;
    private String size;
    private String album;
    private String link;
    private String host;
    private boolean saved;

    public SongBase(String id, String artist, String title, int length, String bitrate, String size, String album, String link, String host) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.length = length;
        this.bitrate = bitrate;
        this.size = size;
        this.album = album;
        this.link = link;
        this.host = host;
        this.saved = false;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLengthString() {
        return DateUtils.formatElapsedTime(length/1000);
    }

    @Override
    public int getLength() {
        return length;
    }

    public void setLenght(int lenght) {
        this.length = lenght;
    }

    @Override
    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    @Override
    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Override
    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }
}
