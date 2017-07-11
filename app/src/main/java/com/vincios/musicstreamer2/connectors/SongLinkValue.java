package com.vincios.musicstreamer2.connectors;


public class SongLinkValue {

    String songId;
    String link;

    public SongLinkValue(String songId, String link) {
        this.songId = songId;
        this.link = link;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
