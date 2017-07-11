package com.vincios.musicstreamer2.connectors;

import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.List;

public interface Connector {

    List<Song> search(String name, @Nullable String artist, @Nullable Bundle extras) throws ConnectorException;
    SongLinkValue getLink(Song song, @Nullable Bundle extras) throws ConnectorException;
    Song getSong(String songId, @Nullable Bundle extras);
}
