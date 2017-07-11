package com.vincios.musicstreamer2.songslistener;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vincios.musicstreamer2.Utils;

import java.util.List;


public class MediaListenerService extends Service {
    private static final String LOGTAG = "MediaListenerService";

    private MediaSessionManager mMediaSessionManager;

    private Context mContext;

    private MediaSessionManager.OnActiveSessionsChangedListener mASCL = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
            if (controllers == null){
                return;
            }
            Log.d(LOGTAG, "found " + controllers.size() + " controllers:");
            for (MediaController c : controllers) {
                PlaybackState controllerState = c.getPlaybackState();
                MediaMetadata controllerMetadata = c.getMetadata();
                String controllerName = c.getPackageName();
                int controllerStateInt = (controllerState == null ? -1 : controllerState.getState());
                boolean isControllerPlaying = controllerStateInt == PlaybackState.STATE_PLAYING;

                Log.d(LOGTAG, "Controller '"+ controllerName +"'. State="+controllerStateInt);

                if (!mContext.getPackageName().equals(controllerName)) {
                    if (isControllerPlaying && controllerMetadata != null) {
                        sendSearchQueryBroadcast(controllerName, controllerMetadata);
                    }
                }
            }
        }
    };


    private void sendSearchQueryBroadcast(String pkg, MediaMetadata metadata) {

        Log.d(LOGTAG, "Metadata from player '" + pkg + "':" + metadata.toString());

        String title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE).toString();
        String artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST).toString();
        Log.d(LOGTAG, title + " - " +artist);

        Intent notificationParams = new Intent(Utils.ACTION_SEARCH);
        notificationParams.putExtra(SearchNotificationManager.TITLE, title);
        notificationParams.putExtra(SearchNotificationManager.ARTIST, artist);
        sendBroadcast(notificationParams);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOGTAG, "onCreate");
        mContext = getApplicationContext();
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGTAG, "onStartCommand");

        mMediaSessionManager.addOnActiveSessionsChangedListener(mASCL, new ComponentName(this, NotificationListener.class));

        return START_STICKY;

    }


}
