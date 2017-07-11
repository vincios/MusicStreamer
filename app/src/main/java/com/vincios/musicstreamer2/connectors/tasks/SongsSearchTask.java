package com.vincios.musicstreamer2.connectors.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.vincios.musicstreamer2.connectors.Connector;
import com.vincios.musicstreamer2.connectors.ConnectorException;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.chiasenhac.ChiasenhacConnector;
import com.vincios.musicstreamer2.connectors.pleer.PleerConnector;

import java.util.ArrayList;
import java.util.List;

public class SongsSearchTask extends AsyncTask<String, Void, List<Song>> {

    private List<Connector> connectors;
    private ConnectorsResultListener listener;
    private ConnectorException exception;


    public SongsSearchTask(ConnectorsResultListener listener) {
        super();
        this.connectors = new ArrayList<>(1);
        this.connectors.add(new PleerConnector());
        this.connectors.add(new ChiasenhacConnector());
        this.listener = listener;
        this.exception = null;
    }

    @Override
    protected List<Song> doInBackground(String... params) {
        String name = params[0];

        String artist = params.length > 1 ? params[0] : null;

        List<Song> resultList = new ArrayList<>();

        for(Connector connector : connectors) {
            try {
                List<Song> search = connector.search(name, artist, null);
                resultList.addAll(search);
            } catch (ConnectorException e) {
                this.exception = e;
            }
        }

        return resultList;
    }

    @Override
    protected void onPostExecute(List<Song> songs) {
        if (exception != null)
            listener.onFail(exception);
        else
            listener.onSearchResult(songs);
    }


}
