package com.vincios.musicstreamer2.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.connectors.Song;

import java.io.IOException;

public class PlayerActivity extends AppCompatActivity implements ExtractorMediaSource.EventListener, ExoPlayer.EventListener{
    public static final String SONG_TO_PLAY = "song";
    private static final String LOGTAG = "PlayerActivity";
    private Song playedSong;
    private SimpleExoPlayer player;
    private static final int MIN_BUFFER_MS = 3 * 60 * 1000;
    private static final int MAX_BUFFER_MS = 5 * 60 * 1000;

    private Bundle musixmatchBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        playedSong = (Song) intent.getSerializableExtra(SONG_TO_PLAY);
        String url = playedSong.getLink();

        musixmatchBundle = new Bundle();
        musixmatchBundle.putString("track", playedSong.getTitle());
        musixmatchBundle.putString("artist", playedSong.getArtist());
        musixmatchBundle.putString("album", playedSong.getAlbum());
        // put your application's package
        musixmatchBundle.putString("scrobbling_source", "com.vincios.musicstreamer2");


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


        SimpleExoPlayerView view = (SimpleExoPlayerView) findViewById(R.id.exoplayerView);
        view.setPlayer(player);

        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "MusicStreamer"), bandwidthMeter);

        Uri uri = Uri.parse(url);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, mainHandler, this); // Listener defined elsewhere

        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
        player.setMetadataOutput(new MetadataRenderer.Output() {
            @Override
            public void onMetadata(Metadata metadata) {
                Log.d(LOGTAG, "onMetadata");
                for(int i = 0; i<metadata.length(); i++){
                    Log.d(LOGTAG, metadata.get(i).toString());
                }
            }
        });
    }

    @Override
    public void onLoadError(IOException error) {
        Log.d(LOGTAG, error.getMessage());
        Toast.makeText(this, getResources().getString(R.string.song_connection_failed), Toast.LENGTH_LONG).show();
        error.printStackTrace();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        player.stop();
        player.release();
        Intent i = new Intent();
        i.setAction("com.android.music.playstatechanged");
        Bundle bundle = new Bundle(musixmatchBundle);

        // put the song's total duration (in ms)
        musixmatchBundle.putLong("duration", player.getDuration()); // 4:05
        // put the song's current position
        bundle.putLong("position", player.getCurrentPosition()); // beginning of the song

        // put the playback status
        bundle.putBoolean("playing", false); // currently playing

        i.putExtras(bundle);
        sendBroadcast(i);

        super.onDestroy();
    }

    /**************************
     * LISTENER METHODS FOR PLAYER STATE
     **************************/
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        Log.d(LOGTAG, "onTimelineChanged");
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d(LOGTAG, "onTracksChanged");

        Intent i = new Intent();
        i.setAction("com.android.music.metachanged");

        Bundle bundle = new Bundle(musixmatchBundle);

        // put the song's total duration (in ms)
        musixmatchBundle.putLong("duration", player.getDuration()); // 4:05
        // put the song's current position
        bundle.putLong("position", 0L); // beginning of the song

        // put the playback status
        bundle.putBoolean("playing", false); // currently playing

        i.putExtras(bundle);
        sendBroadcast(i);

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(LOGTAG, "onLoadingChanged. isLoading=" + isLoading);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(LOGTAG, "onPlayerStateChanged. playWhenReady=" +playWhenReady + " playbackState="+playbackState);
        if(playbackState == ExoPlayer.STATE_READY){
            Intent i = new Intent();
            i.setAction("com.android.music.playstatechanged");
            Bundle bundle = new Bundle(musixmatchBundle);

            // put the song's total duration (in ms)
            musixmatchBundle.putLong("duration", player.getDuration()); // 4:05
            // put the song's current position
            bundle.putLong("position", player.getCurrentPosition()); // beginning of the song

            // put the playback status
            bundle.putBoolean("playing", playWhenReady); // currently playing


            i.putExtras(bundle);
            sendBroadcast(i);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
            final String what;
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    what = error.getSourceException().getMessage();
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    what = error.getRendererException().getMessage();
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                    what = error.getUnexpectedException().getMessage();
                    break;
                default:
                    what = "Unknown: " + error;
            }
        Log.d(LOGTAG, "onPlayerError. Error: " + what);
    }

    @Override
    public void onPositionDiscontinuity() {
        Log.d(LOGTAG, "onPositionDiscontinuity");
    }

}
