package com.vincios.musicstreamer2.connectors.pleer.pleermodel;


import java.io.Serializable;

public class PleerModelTrack implements Serializable{

    private String id;
    private String artist;
    private String track;
    private int lenght;
    private String bitrate;
    private String size;
    private String link;

    public String getTrackId() {
        return id;
    }

    public String getArtist() {
        return artist;
    }

    public String getTrack() {
        return track;
    }

    public int getLenght() {
        return lenght;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return "PleerModelTrack{" +
                "id='" + id + '\'' +
                ", artist='" + artist + '\'' +
                ", track='" + track + '\'' +
                ", lenght=" + lenght +
                ", bitrate='" + bitrate + '\'' +
                ", size=" + size +
                '}';
    }
}
