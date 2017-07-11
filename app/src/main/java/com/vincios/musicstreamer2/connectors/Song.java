package com.vincios.musicstreamer2.connectors;

import android.os.Bundle;

import java.io.Serializable;

public interface Song extends Serializable{

    String _ID = "id";
    String TITLE = "title";
    String ARTIST = "artist";
    String ALBUM = "album";
    String BITRATE = "bitrate";
    String LENGTH = "length";
    String LENGTH_STRING = "length_string";
    String SIZE = "size";
    String LINK = "link";
    String HOST = "host";
    String SAVED = "saved";
    String EXTRAS = "extras";

    enum SongSaved{
        YES,NO
    }

    String getId();
    String getTitle();
    String getArtist();
    String getAlbum();
    String getBitrate();
    String getLengthString();
    /**@return Length of song in MILLISECONDS**/
    int getLength();
    String getSize();
    String getLink();
    String getHost();
    boolean isSaved();

    void setSaved(boolean saved);
    void setLink(String link);
    void setSize(String size);
}
