package com.vincios.musicstreamer2.connectors.tasks;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.vincios.musicstreamer2.connectors.Connector;
import com.vincios.musicstreamer2.connectors.ConnectorConstants;
import com.vincios.musicstreamer2.connectors.ConnectorException;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.SongLinkValue;
import com.vincios.musicstreamer2.connectors.chiasenhac.ChiasenhacConnector;
import com.vincios.musicstreamer2.connectors.pleer.PleerConnector;

public class SongLinkRequestTask  extends AsyncTask<Song, Void, SongLinkValue>{

    private ConnectorsResultListener listener;
    private Connector connector;
    private Exception exception;

    public SongLinkRequestTask(ConnectorsResultListener listener) {
        this.listener = listener;
        this.exception = null;
        this.connector = null;
    }

    @Override
    protected SongLinkValue doInBackground(Song... params) {
        if(params.length != 1) {
            //exception = new IllegalArgumentException("SongLinkRequestTask's parameters must be 2: (host, songID)");
            exception = new IllegalArgumentException("SongLinkRequestTask's parameters must be exactly ONE Song");

            return null;
        }

        Song song = params[0];

        String host = song.getHost();
        String songId = song.getId();
        //Bundle extras = song.getExtras();

        switch (host){
            case ConnectorConstants.HOST_PLEER:
                connector = new PleerConnector(); break;
            case ConnectorConstants.HOST_CHIASENHAC:
                connector = new ChiasenhacConnector(); break;
            default:
                exception = new IllegalArgumentException("SongLinkRequestTask's first parameter must be a valid host"); break;
        }

        if(exception != null)
            return null;


        try {
            return connector.getLink(song, null);
        } catch (ConnectorException e) {
            exception = e;
            return null;
        }

    }

    @Override
    protected void onPostExecute(SongLinkValue value) {
        if(exception != null)
            listener.onFail(exception);
        else
            listener.onLinkRequest(value);
    }
}
