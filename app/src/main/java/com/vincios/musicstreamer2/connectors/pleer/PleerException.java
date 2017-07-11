package com.vincios.musicstreamer2.connectors.pleer;


import com.vincios.musicstreamer2.connectors.ConnectorException;

public class PleerException extends ConnectorException{
    public PleerException(int reasonCode) {
        super(reasonCode);
    }

    public PleerException(String message, int reasonCode) {
        super(message, reasonCode);
    }

    public PleerException(String message, Throwable cause, int reasonCode) {
        super(message, cause, reasonCode);
    }
}
