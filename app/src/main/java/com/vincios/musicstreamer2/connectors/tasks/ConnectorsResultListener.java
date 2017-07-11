package com.vincios.musicstreamer2.connectors.tasks;


import com.vincios.musicstreamer2.connectors.ConnectorException;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.SongLinkValue;

import java.util.List;

public interface ConnectorsResultListener {
    void onSearchResult(List<Song> songs);
    void onFail(Exception exception);
    void onLinkRequest(SongLinkValue value);
}
