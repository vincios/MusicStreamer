package com.vincios.musicstreamer2.songslistener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vincios.musicstreamer2.Utils;

import java.util.List;

public class NotificationListener extends NotificationListenerService {

    private Context mContext;
    private static final String LOGTAG = "NotificationListener";
    private MediaSessionManager mMediaSessionManager;

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
    @Override
    public void onCreate() {
        Log.d(LOGTAG, "onCreate");
        super.onCreate();
        mContext = getApplicationContext();
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);


        mMediaSessionManager.addOnActiveSessionsChangedListener(mASCL, new ComponentName(this, NotificationListener.class));

    }

    private void sendSearchQueryBroadcast(String pkg, MediaMetadata metadata) {

        Log.d(LOGTAG, "Metadata from player '" + pkg + "':" + metadata.toString());

        String title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE).toString();
        String artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST).toString();
        Log.d(LOGTAG, title + " - " +artist);

        Intent notificationParams = new Intent(Utils.CONSTANTS.ACTION_SEARCH);
        notificationParams.putExtra(SearchNotificationManager.TITLE, title);
        notificationParams.putExtra(SearchNotificationManager.ARTIST, artist);
        sendBroadcast(notificationParams);
    }

    /*
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Log.d(LOGTAG, "onNotificationPosted");
        String pack = sbn.getPackageName();


        if(NOTIFICATION_PACKAGE.equals(pack)) {
            String ticker = "";
            if (sbn.getNotification().tickerText != null) {
                ticker = sbn.getNotification().tickerText.toString();
            }
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString("android.title");
            String text = extras.getCharSequence("android.text").toString();

            Log.d(LOGTAG, title);

            Intent msgrcv = new Intent(Utils.ACTION_SEARCH);
            msgrcv.putExtra("package", pack);
            msgrcv.putExtra("ticker", ticker);
            msgrcv.putExtra("title", title);
            msgrcv.putExtra("text", text);

            mContext.sendBroadcast(msgrcv);
            //LocalBroadcastManager.getInstance(mContext).sendBroadcast(msgrcv);

        }

    }*/

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(LOGTAG, "onDestroy");
        mMediaSessionManager.removeOnActiveSessionsChangedListener(mASCL);
    }


}
