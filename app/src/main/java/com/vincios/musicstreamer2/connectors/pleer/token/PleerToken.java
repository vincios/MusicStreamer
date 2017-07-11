package com.vincios.musicstreamer2.connectors.pleer.token;

import com.vincios.musicstreamer2.connectors.pleer.pleermodel.PleerModelToken;

public class PleerToken {
    private static final PleerToken ourInstance = new PleerToken();

    private long crationTime;
    private int duration;

    private String token;

    public static PleerToken getInstance() {
        return ourInstance;
    }

    private PleerToken() {
        token = null;
        duration = 0;
        crationTime = 0;
    }

    /**
     * Return Pleer Token string
     * @return Pleer Token string
     * @throws ExpiredTokenException if token is expired
     */
    public String getToken(){
        if(isExpired())
            throw new ExpiredTokenException("Token expired");

        return token;
    }

    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        return token == null || currentTime > crationTime + duration;

    }

    public void setNewToken(PleerModelToken newToken) {
        this.token = newToken.getAccessToken();
        this.duration = newToken.getExpiresIn() * 1000;
        this.crationTime = System.currentTimeMillis();
    }
}
