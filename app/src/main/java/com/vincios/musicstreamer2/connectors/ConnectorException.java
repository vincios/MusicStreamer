package com.vincios.musicstreamer2.connectors;


public class ConnectorException extends Exception{
    public static final int ERROR_REQUEST_PREPARATION = 1;
    public static final int ERROR_SERVER_CONNECTION = 2;
    public static final int ERROR_SEND = 3 ;
    public static final int ERROR_RESPONSE_RECEIVE = 4;
    public static final int ERROR_RESPONSE_PARSING = 5;
    public static final int RESPONSE_INVALID_TOKEN = 6;
    public static final int RESPONSE_INSUCCESS = 7;

    private int reasonCode;

    public ConnectorException(int reasonCode) {
        this.reasonCode = reasonCode;
    }

    public ConnectorException(String message, int reasonCode) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public ConnectorException(String message, Throwable cause, int reasonCode) {
        super(message, cause);
        this.reasonCode = reasonCode;
    }

    public int getReasonCode() {
        return reasonCode;
    }
}
