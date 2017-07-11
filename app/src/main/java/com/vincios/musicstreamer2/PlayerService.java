package com.vincios.musicstreamer2;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.ui.activities.MainActivity;
import com.vincios.musicstreamer2.ui.widgets.NotificationHelper;

import java.util.List;

public class PlayerService extends MediaBrowserServiceCompat implements LocalPlayback.PlaybackCallback, PlayingSongsQueue.QueueListener{

    private static final java.lang.String LOGTAG = "PlayerService";
    private static final java.lang.String LOGTAG_MEDIASESSION = "MediaSession";
    private static final String MEDIA_ROOT_ID = "mediaRootId";
    private static final int NOTIFICATION_ID = 426;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mPlayBackBuilder;
    private MediaMetadataCompat.Builder mMetaMetadataBuilder;

    private LocalPlayback mPlayer;

    public boolean mIsServiceStarted;
    private PlayingSongsQueue mQueue;
    private android.support.v4.media.session.MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCallback();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOGTAG, "onCreate");

        //adding media button handling on android N
        /*ComponentName mbrCN = new ComponentName(this, MediaButtonReceiver.class);

        mMediaSession = new MediaSessionCompat(this, LOGTAG_MEDIASESSION, mbrCN, null);*/
        mMediaSession = new MediaSessionCompat(this, LOGTAG_MEDIASESSION);
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
              | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );


        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);

        mMediaSession.setMediaButtonReceiver(mbrIntent);

        mPlayer = new LocalPlayback(this, this);
        //PlayBackBuilder costruisce lo stato corrente della Mediasession
        mPlayBackBuilder = new PlaybackStateCompat.Builder();
        mMetaMetadataBuilder = new MediaMetadataCompat.Builder();

        updatePlaybackState(PlaybackStateCompat.STATE_NONE);

        mMediaSession.setCallback(mMediaSessionCallback);
        setSessionToken(mMediaSession.getSessionToken());

        //Create a pending intent to launch the main activity on notification click
        Context context = getApplicationContext();
        Intent launchActivity = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99, launchActivity, PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setSessionActivity(pi);


        mIsServiceStarted = false;

        mQueue = PlayingSongsQueue.getInstance();
        mQueue.setListener(this);
    }


    private void handlePlayRequest() {
        String playerIstance = mPlayer != null ? mPlayer.toString() : "null";
        Log.d(LOGTAG, "handlePlayRequest. mIsServiceStarted="+mIsServiceStarted + " playerIstance="+playerIstance);
        MediaSessionCompat.QueueItem item = PlayingSongsQueue.getInstance().getCurrentPlaying();

        Log.d(LOGTAG, "handlePlayRequest. CurrentPlayingFromQueue="+item);


        if(!mIsServiceStarted) {
            startService(new Intent(getApplicationContext(), PlayerService.class));
            mIsServiceStarted = true;
        }
        if(mPlayer != null)
            mPlayer.play(item);

        startForegroundPlay();

        if(!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }
    }


    private void handlePauseRequest() {
        if(mPlayer != null)
            mPlayer.pause();

        stopForegroundPlay(false);
        NotificationHelper.updateNotification(this, mMediaSession, NOTIFICATION_ID);
    }


    private void handleStopRequest() {

        mPlayer.stop();
        if(mIsServiceStarted){
            stopSelf();
            mIsServiceStarted = false;
        }
        if(mMediaSession.isActive()) {
            mMediaSession.setActive(false);
        }
        stopForegroundPlay(true);
    }

    private void seekPlayer(long position){
        mPlayer.seekTo(position);
    }

    private void startForegroundPlay() {
        NotificationCompat.Builder notification = NotificationHelper.createNotification(getApplicationContext(), mMediaSession);

        if(notification != null) {
            startForeground(NOTIFICATION_ID, notification.build());
        }
    }

    private void stopForegroundPlay(boolean removeNotification){
        stopForeground(removeNotification);
    }

    private void updatePlaybackState(int state) {
        Log.d(LOGTAG, "updatePlaybackState. state ="+state);
        PlaybackStateCompat newState;
        switch (state){
            case PlaybackStateCompat.STATE_NONE:
                newState = mPlayBackBuilder
                        .setActions(getAvailableActions())
                        .setState(state, 0, 1.0f, android.os.SystemClock.elapsedRealtime())
                        .build();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                newState = mPlayBackBuilder
                        .setActions(getAvailableActions())
                        .setBufferedPosition(mPlayer.getBufferedPosition())
                        .setState(PlaybackStateCompat.STATE_BUFFERING, mPlayer.getCurrentPosition(), 1.0f, android.os.SystemClock.elapsedRealtime())
                        .setBufferedPosition(mPlayer.getBufferedPosition())
                        .setActiveQueueItemId(mPlayer.getCurrentPlayingQueueId())
                        .build();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                newState = mPlayBackBuilder
                        .setActions(getAvailableActions())
                        .setBufferedPosition(mPlayer.getBufferedPosition())
                        .setState(PlaybackStateCompat.STATE_PLAYING, mPlayer.getCurrentPosition(), 1.0f, android.os.SystemClock.elapsedRealtime())
                        .setBufferedPosition(mPlayer.getBufferedPosition())
                        .setActiveQueueItemId(mPlayer.getCurrentPlayingQueueId())
                        .build();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                newState = mPlayBackBuilder
                        .setActions(getAvailableActions())
                        .setBufferedPosition(mPlayer.getBufferedPosition())
                        .setState(PlaybackStateCompat.STATE_PAUSED, mPlayer.getCurrentPosition(), 1.0f, android.os.SystemClock.elapsedRealtime())
                        .setActiveQueueItemId(mPlayer.getCurrentPlayingQueueId())
                        .build();
                break;
            default:
                newState = mPlayBackBuilder
                        .setActions(getAvailableActions())
                        .setState(state, 0, 1.0f, android.os.SystemClock.elapsedRealtime())
                        .build();
                break;
        }

        mMediaSession.setPlaybackState(newState);
    }

    private void updateMetadata(MediaDescriptionCompat metadata){
        String current = metadata != null ? metadata.toString() : "null";
        Log.d(LOGTAG, "updateMetadata. currentPlaying=" +current);
        if(metadata != null){
            Bundle extras = metadata.getExtras();
            mMetaMetadataBuilder
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, metadata.getMediaId())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, ""+metadata.getTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, ""+metadata.getSubtitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, ""+metadata.getMediaUri())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, metadata.getIconBitmap());

            if(extras != null){
                String desc= getResources().getString(R.string.song_list_item_bitrate) + ": " + extras.getString(Song.BITRATE)
                        + getResources().getString(R.string.song_list_item_host) + ": " + extras.getString(Song.HOST);
                mMetaMetadataBuilder
                        //.putString(MediaMetadataCompat.METADATA_KEY_DURATION, extras.getString(Song.LENGTH))
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, desc);

                if(extras.getString(Song.ALBUM) != null)
                    mMetaMetadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, extras.getString(Song.ALBUM));

                if(extras.getInt(Song.LENGTH) != 0)
                    mMetaMetadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, extras.getInt(Song.LENGTH));
            }

            mMediaSession.setMetadata(mMetaMetadataBuilder.build());
        }
    }

    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (mPlayer.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG,"onDestroy");
        mQueue.removeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGTAG, "onStartCommand. Intent = " + intent);
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOGTAG, "onBind");
        return super.onBind(intent);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(LOGTAG, "onGetRoot. clientPackageName="+clientPackageName+" clientUid="+clientUid);
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(LOGTAG, "onLoadChildren. parentId="+parentId);
        result.detach();
    }

    /**
     *CALLBACK METHODS FOR PLAYBACK PLAYER STATE
     */
    @Override
    public void onMetadataUpdated(MediaDescriptionCompat metadata) {
        updateMetadata(metadata);
        NotificationHelper.updateNotification(this, mMediaSession, NOTIFICATION_ID);
    }

    @Override
    public void onPlaybackStatusChanged(int newState) {
        updatePlaybackState(newState);
    }

    @Override
    public void onCompletion() {
        int count = mQueue.nextItemsCount();
        Log.d(LOGTAG, "Current queue nextItemsCount="+count);
        if(count > 0) {
            mQueue.incrementCurrentPlaying(1);
        }else {
            this.mMediaSessionCallback.onStop();
        }
    }


    @Override
    public void onError(String cause) {
        Log.d(LOGTAG, "Song play error: "+ cause);
        Toast.makeText(this, getResources().getText(R.string.song_connection_failed), Toast.LENGTH_LONG).show();
        this.onCompletion();
    }


    /**
     * LISTENER FOR QUEUE CHANGE
     */
    @Override
    public void onQueueChanged() {
        mMediaSession.setQueue(mQueue.getCurrentQueue());
        mMediaSession.setQueueTitle(mQueue.getQueueTitle());
    }

    @Override
    public void onCurrentIndexChanged(int index) {
        handlePlayRequest();
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback{

        @Override
        public void onPlay() {
            super.onPlay();
            Log.d(LOGTAG, "onPlay callback");

            handlePlayRequest();
        }

        @Override
        public void onPause() {
            super.onPause();
            handlePauseRequest();

        }

        @Override
        public void onStop() {
            super.onStop();
            handleStopRequest();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
            Log.d(LOGTAG, "onPrepareFromMediaId. mediaId="+mediaId);
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            seekPlayer(pos);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            if(Utils.ACTION_UPDATE_BUFFERED_POSITION.equals(action) && mPlayer.isLoading()){
                updatePlaybackState(mPlayer.getState());
            }
        }


        /*@Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(LOGTAG, "onPlayFromMediaId. mediaId="+mediaId);
            MediaBrowserCompat.MediaItem song = PlayingSongsQueue.getInstance().getItemById(mediaId);
            if(song != null){
                playSong(song);
            }
        }*/

    }


}
