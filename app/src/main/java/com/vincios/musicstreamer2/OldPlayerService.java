package com.vincios.musicstreamer2;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.vincios.musicstreamer2.connectors.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OldPlayerService extends Service implements ExtractorMediaSource.EventListener, ExoPlayer.EventListener {

    private IBinder mBinder = new PlayerBinder();
    public static final String SONG_TO_PLAY = "song";
    private static final String LOGTAG = "OldPlayerService";
    private static final int MIN_BUFFER_MS = 3 * 60 * 1000;
    private static final int MAX_BUFFER_MS = 5 * 60 * 1000;
    private static final int NOTIFICATION_ID = 666;

    private Song playedSong;
    private SimpleExoPlayer player;
    private Bundle musixmatchBundle;
    private List<PlayerListener> listeners;
    private ProgressUpdateTask updateTask;
    private long currentPositionOnPause;

    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    private MediaSessionCompat mMediaSession;

    public OldPlayerService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class PlayerBinder extends Binder{
        public OldPlayerService getService(){
            return OldPlayerService.this;
        }
    }

    public interface PlayerListener{
        void updateTrack(Song newSong);
        void updateProgress(long duration, long currentPosition, long bufferedPosition);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //Intent intent = getIntent();
        //playedSong = (Song) intent.getSerializableExtra(SONG_TO_PLAY);
        //String url = playedSong.getLink();
        musixmatchBundle = new Bundle();


        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        final DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl(
                new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);


        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        player.addListener(this);
        extractorsFactory = new DefaultExtractorsFactory();
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "MusicStreamer"), bandwidthMeter);

        listeners = new ArrayList<>();
        currentPositionOnPause = 0;

        mMediaSession = new MediaSessionCompat(this, LOGTAG);

    }

    public void addListener(PlayerListener listener){
        listeners.add(listener);
    }

    public void removeListener(PlayerListener listener){
        listeners.remove(listener);
    }

    public void startPlaying(Song s){
        playedSong = s;
        String url = s.getLink();
        Uri uri = Uri.parse(url);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, mainHandler, this);

        musixmatchBundle.clear();
        musixmatchBundle.putString("track", playedSong.getTitle());
        musixmatchBundle.putString("artist", playedSong.getArtist());
        musixmatchBundle.putString("album", playedSong.getAlbum());
        // put your application's package
        musixmatchBundle.putString("scrobbling_source", "com.vincios.musicstreamer2");

        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
        startUpdateTask();

    }

    public void stopPlaying(){
        player.stop();
        stopUpdateTask();
    }

    public void pause(){
        player.setPlayWhenReady(false);
    }

    public void resume(){
        player.setPlayWhenReady(true);
    }

    public void seekTo(long position){
        long seek;
        if(position < 0)
            seek = 0;
        else if(position > player.getDuration())
            seek = player.getDuration();
        else
            seek = position;

        player.seekTo(seek);
    }


    public void startForeground(){

    }


    private void startUpdateTask(){
        if(updateTask == null){
            updateTask = new ProgressUpdateTask();
            updateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void stopUpdateTask(){
        if(updateTask != null){
            updateTask.cancel(true);
            updateTask = null;
        }
    }


    /*************************************
     * LISTENER METHODS FOR PLAYER STATE *
     *************************************/

    @Override
    public void onLoadError(IOException error) {
        Log.d(LOGTAG, error.getMessage());
        Toast.makeText(this, getResources().getString(R.string.song_connection_failed), Toast.LENGTH_LONG).show();
        error.printStackTrace();

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        for(PlayerListener l : listeners)
            l.updateTrack(playedSong);
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }


    class ProgressUpdateTask extends AsyncTask<Void,Void,Void>{

        final static int UPDATE_INTERVAL_MILLIS = 1000;
        @Override
        protected Void doInBackground(Void... params) {
            while(!isCancelled()){
                try{

                    for(PlayerListener l : listeners)
                        l.updateProgress(player.getDuration(), player.getCurrentPosition(), player.getBufferedPosition());

                    Thread.sleep(UPDATE_INTERVAL_MILLIS);
                }catch (InterruptedException e){
                    Log.d(LOGTAG, "Interrupted update task");

                }
            }
            return null;
        }
    }
}
