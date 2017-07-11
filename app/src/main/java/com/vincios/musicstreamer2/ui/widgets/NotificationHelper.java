package com.vincios.musicstreamer2.ui.widgets;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.ui.activities.MainActivity;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
public class NotificationHelper {

    private static final String LOGTAG = "NotificationHelper";

    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     * @param context Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    public static NotificationCompat.Builder createNotification(Context context,
                                                                MediaSessionCompat mediaSession) {

        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mMetadata = controller.getMetadata();
        PlaybackStateCompat mPlaybackState = controller.getPlaybackState();


        if (mMetadata == null || mPlaybackState == null) {
            return null;
        }

        boolean isPlaying = (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING)
                || (mPlaybackState.getState() == PlaybackStateCompat.STATE_BUFFERING);

        //NOTIFICATION ACTIONS
        NotificationCompat.Action action = isPlaying
                ? new NotificationCompat.Action(R.drawable.ic_pause_black_24dp,
                context.getString(R.string.label_pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE))
                : new NotificationCompat.Action(R.drawable.ic_play_arrow_black_24dp,
                context.getString(R.string.label_play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE));
        PendingIntent stopAction = MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP);
        //Log.d(LOGTAG, "stopAction=" + stopAction);

        MediaDescriptionCompat description = mMetadata.getDescription();

        //NOTIFICATION IMAGE
        Bitmap art = description.getIconBitmap();
        // use a placeholder art while the remote art is being downloaded.
        if(art == null) {
            art = BitmapFactory.decodeResource(context.getResources(),
                    R.mipmap.ic_note);
        }

        ////NOTIFICATION ACTIVITY ON CLICK
        //PendingIntent sessionActivity = controller.getSessionActivity();


        Intent resultIntent = new Intent(context, MainActivity.class);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


       //mBuilder.setContentIntent(resultPendingIntent);

        int color;
        if(Build.VERSION.SDK_INT >= 23)
            color = context.getColor(R.color.colorAccent);
        else
            color = context.getResources().getColor(R.color.colorAccent);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder
                .setStyle(
                        new NotificationCompat.MediaStyle()
                                // show only play/pause in compact view.
                                .setShowActionsInCompactView(new int[]{0})
                                .setMediaSession(mediaSession.getSessionToken())
                                //show x button on the top corner of notification
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(stopAction)
                )
                .addAction(action)
                .setDeleteIntent(stopAction)
                .setShowWhen(false)
                .setContentIntent(resultPendingIntent)
                .setContentTitle(description.getTitle())
                .setContentText(mMetadata.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR))
                .setLargeIcon(art)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(color)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return notificationBuilder;
    }

    public static void updateNotification(Context context, MediaSessionCompat mediaSession, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = createNotification(context, mediaSession);
        if(notification != null)
            notificationManager.notify(notificationId, notification.build());
    }
}
