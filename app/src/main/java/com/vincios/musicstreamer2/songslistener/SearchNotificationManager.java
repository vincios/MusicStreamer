package com.vincios.musicstreamer2.songslistener;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.Utils;
import com.vincios.musicstreamer2.ui.activities.MainActivity;


public class SearchNotificationManager extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 789;
    private static final String LOGTAG = "SearchNotificationMan";
    public static final String TITLE = "com.vincios.musicstramer2.SearchNotificationManager.title";
    public static final String ARTIST = "com.vincios.musicstramer2.SearchNotificationManager.artist";

    @Override
    public void onReceive(Context context, Intent intent) {

        String title = intent.getStringExtra(TITLE);
        String artist = intent.getStringExtra(ARTIST);

        String query = title + " " + artist;

        Log.d(LOGTAG, query);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title + " - " + artist)
                        .setContentText(context.getString(R.string.search_from_notification));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent
                .setAction(Utils.ACTION_SEARCH)
                .putExtra(MainActivity.SEARCH_QUERY, query);


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
        mBuilder.setContentIntent(resultPendingIntent);

        mBuilder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
