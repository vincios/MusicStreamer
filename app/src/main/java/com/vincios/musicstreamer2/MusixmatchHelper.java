package com.vincios.musicstreamer2;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class MusixmatchHelper {
    private Bundle mCurrentPlayingBundle;
    private Context mContext;

    public MusixmatchHelper(Context ctx) {
        mCurrentPlayingBundle = new Bundle();
        mContext = ctx;
    }


    private void clearBundle(){
        mCurrentPlayingBundle.clear();
        mCurrentPlayingBundle.putString("scrobbling_source", "com.vincios.musicstreamer2");
    }

    private Intent createMetachangedIntent(){
        Intent intent = new Intent();
        intent.setAction("com.android.music.metachanged");
        return intent;
    }

    private Intent createStatechangedIntent(){
        Intent intent = new Intent();
        intent.setAction("com.android.music.playstatechanged");
        return intent;
    }

    public void sendMetadata(MediaMetadataCompat newSongMetadata){
        clearBundle();

        Intent metachangedIntent = createMetachangedIntent();

        String title = newSongMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        String artist = newSongMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        String album = newSongMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        Long duration = newSongMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);

        mCurrentPlayingBundle.putString("track", title);
        mCurrentPlayingBundle.putString("artist", artist);
        mCurrentPlayingBundle.putString("album", album);
        mCurrentPlayingBundle.putLong("duration", duration);

        mCurrentPlayingBundle.putBoolean("playing", false);
        mCurrentPlayingBundle.putLong("position", 1L); // beginning of the song


        metachangedIntent.putExtras(mCurrentPlayingBundle);

        mContext.sendBroadcast(metachangedIntent);
    }

    public void sendPlaybackState(PlaybackStateCompat playbackState){
        Intent statechangedIntent = createStatechangedIntent();

        boolean isPlaying = playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
        long position = playbackState.getPosition();

        mCurrentPlayingBundle.putBoolean("playing", isPlaying);
        mCurrentPlayingBundle.putLong("position", position);

        statechangedIntent.putExtras(mCurrentPlayingBundle);
        mContext.sendBroadcast(statechangedIntent);
    }
}
