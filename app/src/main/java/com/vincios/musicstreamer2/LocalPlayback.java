package com.vincios.musicstreamer2;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

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
import com.google.android.exoplayer2.metadata.emsg.EventMessage;
import com.google.android.exoplayer2.metadata.id3.ApicFrame;
import com.google.android.exoplayer2.metadata.id3.CommentFrame;
import com.google.android.exoplayer2.metadata.id3.GeobFrame;
import com.google.android.exoplayer2.metadata.id3.Id3Frame;
import com.google.android.exoplayer2.metadata.id3.PrivFrame;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.metadata.id3.UrlLinkFrame;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.Queue;

public class LocalPlayback implements ExoPlayer.EventListener, ExtractorMediaSource.EventListener {
    private static final int MIN_BUFFER_MS = 3 * 60 * 1000;
    private static final int MAX_BUFFER_MS = 5 * 60 * 1000;

    private static final int AUDIO_FOCUSED = 0;
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 1;
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 2;

    private static final float VOLUME_NORMAL = 1.0f;
    private static final float VOLUME_DUCK = 0.2f;
    private static final String LOGTAG = "LocalPlayback";

    private Context mContext;
    private PlaybackCallback mCallback;
    private PlayerOnAudioFocusChangeListener mAudioFocusChangeListener;
    private AudioManager mAudioManager;
    private final WifiManager.WifiLock mWifiLock;


    private int mAudioFocusState;
    private boolean mPlayOnFocus;

    //private String mCurrentPlayingMediaId;
    private long mCurrentPlayingQueueId = MediaSessionCompat.QueueItem.UNKNOWN_ID;
    private MediaDescriptionCompat mCurrentPlayingDescription;

    private SimpleExoPlayer mPlayer = null;
    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    private boolean mIsBecomingNoiseReceiverRegistered = false;
    private IntentFilter myBecomingNoiseFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BroadcastReceiver myBecomingNoiseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
                Log.d(LOGTAG, "Headphone disconnected");
                pause();
            }
        }
    };


    //private             LocalPlaybackLogger eventLogger;
    private DefaultTrackSelector mTrackSelector;
    private boolean mIsLoading;


    public LocalPlayback(Context context, PlaybackCallback callback) {

        this.mContext = context;
        this.mCallback = callback;


        mAudioFocusChangeListener = new PlayerOnAudioFocusChangeListener();

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mWifiLock =
                ((WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "musicStreamer_lock");

    }

    private void instantiatePlayer(){
        if(mPlayer == null) {
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            final DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            mTrackSelector = trackSelector;
            LoadControl loadControl = new DefaultLoadControl(
                    new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);


            //eventLogger = new LocalPlaybackLogger(trackSelector);

            mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector, loadControl);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.addListener(this);
            /*mPlayer.addListener(eventLogger);
            mPlayer.setAudioDebugListener(eventLogger);
            mPlayer.setVideoDebugListener(eventLogger);
            mPlayer.setMetadataOutput(eventLogger);*/

            extractorsFactory = new DefaultExtractorsFactory();
            dataSourceFactory = new DefaultDataSourceFactory(
                    mContext, Util.getUserAgent(mContext, "MusicStreamer"), bandwidthMeter
            );
        }
    }

    public int getState() {
        if(mPlayer == null)
            return PlaybackStateCompat.STATE_NONE;

        int playerState = mPlayer.getPlaybackState();

        switch (playerState){
            case ExoPlayer.STATE_IDLE:
                return PlaybackStateCompat.STATE_PAUSED;
            case ExoPlayer.STATE_BUFFERING:
                return PlaybackStateCompat.STATE_BUFFERING;
            case ExoPlayer.STATE_READY:
                return mPlayer.getPlayWhenReady()
                        ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    public void play(MediaSessionCompat.QueueItem song){
        Log.d(LOGTAG, "play. song="+song);
        mPlayOnFocus = true;

        requestAudioFocus();
        registerBecomingNoiseReceiver();
        instantiatePlayer();

        String newMediaId = song.getDescription().getMediaId();
        boolean mediaHasChanged = mCurrentPlayingDescription == null || !(newMediaId.equals(mCurrentPlayingDescription.getMediaId()));

        if(mediaHasChanged) {
            mCurrentPlayingDescription = song.getDescription();
            mCurrentPlayingQueueId = song.getQueueId();
        }
        if(mediaHasChanged || mPlayer == null) {
            Uri uri = song.getDescription().getMediaUri();

            Handler mainHandler = new Handler(Looper.getMainLooper());
            MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, mainHandler, this); // Listener defined elsewhere
            //MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, mainHandler, eventLogger); // Listener defined elsewhere
            mPlayer.prepare(mediaSource);


            if(mCallback != null)
                mCallback.onMetadataUpdated(song.getDescription());

            mWifiLock.acquire();
        }

        configurePlayer();
    }


    private void configurePlayer() {
        if(mAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            pause();
        }else if(mPlayer != null){
            if (mAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                mPlayer.setVolume(VOLUME_DUCK);
            }else{
                mPlayer.setVolume(VOLUME_NORMAL);
            }

            if(mPlayOnFocus && mPlayer != null) {
                mPlayer.setPlayWhenReady(true);
            }
        }
    }


    public void pause(){
        if(mPlayer != null)
            mPlayer.setPlayWhenReady(false);
        releaseResource(false);
        unregisterBecomingNoiseReceiver();
    }

    public void stop(){
        if(mPlayer != null)
            mPlayer.stop();
        releaseAudioFocus();
        releaseResource(true);
        unregisterBecomingNoiseReceiver();
    }

    public long getCurrentPosition(){
        return (mPlayer == null) ? 0 : mPlayer.getCurrentPosition();
    }

    public long getBufferedPosition(){
        return (mPlayer == null) ? 0 : mPlayer.getBufferedPosition();
    }

    public void seekTo(long value){
        if(mPlayer != null){
            mPlayer.seekTo(value);
        }
    }

    public long getCurrentPlayingQueueId(){
        return mCurrentPlayingQueueId;
    }

    public boolean isPlaying(){
        return mPlayOnFocus || (mPlayer != null && mPlayer.getPlayWhenReady());
    }

    public boolean isLoading(){
        return mIsLoading;
    }

    private void releaseResource(boolean releasePlayer) {
        if(releasePlayer && mPlayer != null) {
            mPlayer.release();
            mPlayer.removeListener(this);
            mPlayer = null;
        }

        if(mWifiLock.isHeld())
            mWifiLock.release();
    }

    private void requestAudioFocus(){
        if(mAudioFocusState != AUDIO_FOCUSED) {
            int focus = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            if (focus == AudioManager.AUDIOFOCUS_GAIN)
                mAudioFocusState = AUDIO_FOCUSED;
            else
                mAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }

    }

    private void releaseAudioFocus(){
        int result = mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            mAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    }

    private void registerBecomingNoiseReceiver(){
        if(!mIsBecomingNoiseReceiverRegistered){
            mContext.registerReceiver(myBecomingNoiseReceiver, myBecomingNoiseFilter);
            mIsBecomingNoiseReceiverRegistered = true;
        }
    }

    private void unregisterBecomingNoiseReceiver(){
        if(mIsBecomingNoiseReceiverRegistered){
            mContext.unregisterReceiver(myBecomingNoiseReceiver);
            mIsBecomingNoiseReceiverRegistered = false;
        }
    }


    private void sendArtworkToService(Bitmap art) {
        MediaDescriptionCompat.Builder b = mdcBuilderFrom(mCurrentPlayingDescription);
        b.setIconBitmap(art);
        mCallback.onMetadataUpdated(b.build());
    }

    private MediaDescriptionCompat.Builder mdcBuilderFrom(MediaDescriptionCompat source){
        MediaDescriptionCompat.Builder b = new MediaDescriptionCompat.Builder();
        b.setTitle(source.getTitle())
                .setSubtitle(source.getSubtitle())
                .setIconBitmap(source.getIconBitmap())
                .setExtras(source.getExtras())
                .setDescription(source.getDescription())
                .setMediaUri(source.getMediaUri())
                .setIconUri(source.getIconUri())
                .setMediaId(source.getMediaId());

        return b;
    }


    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo != null) {
            for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.length; rendererIndex++) {
                TrackSelection trackSelection = trackSelections.get(rendererIndex);
                if (trackSelection != null) {
                    for (int selectionIndex = 0; selectionIndex < trackSelection.length(); selectionIndex++) {
                        Metadata metadata = trackSelection.getFormat(selectionIndex).metadata;
                        if (metadata != null) {
                            printMetadata(metadata, "->");
                            Bitmap art = findArtwork(metadata);
                            if(art != null)
                                sendArtworkToService(art);
                        }
                    }
                }
            }
        }
    }


    private Bitmap findArtwork(Metadata metadata) {
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry metadataEntry = metadata.get(i);
            if (metadataEntry instanceof ApicFrame) {
                byte[] bitmapData = ((ApicFrame) metadataEntry).pictureData;
                return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
            }
        }
        return null;
    }

    private void printMetadata(Metadata metadata, String prefix) {
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry entry = metadata.get(i);
            if (entry instanceof TextInformationFrame) {
                TextInformationFrame textInformationFrame = (TextInformationFrame) entry;
                Log.d(LOGTAG, prefix + String.format("%s: value=%s", textInformationFrame.id,
                        textInformationFrame.value));
            } else if (entry instanceof UrlLinkFrame) {
                UrlLinkFrame urlLinkFrame = (UrlLinkFrame) entry;
                Log.d(LOGTAG, prefix + String.format("%s: url=%s", urlLinkFrame.id, urlLinkFrame.url));
            } else if (entry instanceof PrivFrame) {
                PrivFrame privFrame = (PrivFrame) entry;
                Log.d(LOGTAG, prefix + String.format("%s: owner=%s", privFrame.id, privFrame.owner));
            } else if (entry instanceof GeobFrame) {
                GeobFrame geobFrame = (GeobFrame) entry;
                Log.d(LOGTAG, prefix + String.format("%s: mimeType=%s, filename=%s, description=%s",
                        geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
            } else if (entry instanceof ApicFrame) {
                ApicFrame apicFrame = (ApicFrame) entry;
                Log.d(LOGTAG, prefix + String.format("%s: mimeType=%s, description=%s",
                        apicFrame.id, apicFrame.mimeType, apicFrame.description));
            } else if (entry instanceof CommentFrame) {
                CommentFrame commentFrame = (CommentFrame) entry;
                Log.d(LOGTAG, prefix + String.format("%s: language=%s, description=%s", commentFrame.id,
                        commentFrame.language, commentFrame.description));
            } else if (entry instanceof Id3Frame) {
                Id3Frame id3Frame = (Id3Frame) entry;
                Log.d(LOGTAG, prefix + String.format("%s", id3Frame.id));
            } else if (entry instanceof EventMessage) {
                EventMessage eventMessage = (EventMessage) entry;
                Log.d(LOGTAG, prefix + String.format("EMSG: scheme=%s, id=%d, value=%s",
                        eventMessage.schemeIdUri, eventMessage.id, eventMessage.value));
            }
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(LOGTAG, "isLoading=" + isLoading);
        mIsLoading = isLoading;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(LOGTAG, "onPlayerStateChanged");
        if (playbackState == ExoPlayer.STATE_ENDED) {
            if (mCallback != null)
                mCallback.onCompletion();
        }
        mCallback.onPlaybackStatusChanged(getState());
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

        Log.d(LOGTAG, "Player Error: " + what);
        if(mCallback != null)
            mCallback.onError(what);
    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onLoadError(IOException error) {
        mCallback.onError(error.getLocalizedMessage());
    }


    private class PlayerOnAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener{

        @Override
        public void onAudioFocusChange(int focusChanged) {
            switch (focusChanged){
                case AudioManager.AUDIOFOCUS_GAIN:
                    mAudioFocusState = AUDIO_FOCUSED;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    mAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    mAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                    mPlayOnFocus = mPlayer != null && mPlayer.getPlayWhenReady();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    mAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                    break;
            }

            configurePlayer();
        }
    }

    public interface PlaybackCallback{
        void onMetadataUpdated(MediaDescriptionCompat metadata);
        void onPlaybackStatusChanged(int newState);
        void onCompletion();
        void onError(String cause);
    }
}