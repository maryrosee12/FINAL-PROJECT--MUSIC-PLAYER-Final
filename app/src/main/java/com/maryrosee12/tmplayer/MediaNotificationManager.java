package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class MediaNotificationManager {


    private static final String CHANNEL_ID = "media_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    private final Context context;
    private final NotificationManager notificationManager;


    private final MediaSessionCompat mediaSession;


    public MediaNotificationManager(Context context, MediaSessionCompat mediaSession) {
        this.context = context;
        this.mediaSession = mediaSession;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }


    public Notification showNotification(String title, String artist, Bitmap albumArt, boolean isPlaying) {
        Log.d("MediaNotificationManager","Notification updated");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mp_notification_logo)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(albumArt)
                .setDeleteIntent(getActionIntent("ACTION_STOP"))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(new NotificationCompat.Action(R.drawable.media_previous_small, "Previous", getActionIntent("ACTION_SKIP_TO_PREVIOUS")));


        if (isPlaying) {
            Log.d("MediaNotificationManager","Rendered pause button");
            builder.addAction(new NotificationCompat.Action(R.drawable.media_pause_small, "Pause", getActionIntent("ACTION_PAUSE")));
        } else {
            Log.d("MediaNotificationManager","Rendered play button");
            builder.addAction(new NotificationCompat.Action(R.drawable.media_play_small, "Play", getActionIntent("ACTION_PLAY")));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mediaSession.setMetadata(
                    new MediaMetadataCompat.Builder()
                            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                            .build()
            );
        }

        builder.addAction(new NotificationCompat.Action(R.drawable.media_next_small, "Next", getActionIntent("ACTION_SKIP_TO_NEXT")))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(getActionIntent("ACTION_STOP")));
        Log.d("MediaNotificationManager","Style set");

        return builder.build();
    }


    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(context, MediaPlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }


    @SuppressLint("MissingPermission")
    public void updateNotification(String title, String artist, Bitmap albumArt, boolean isPlaying) {
        Notification notification = showNotification(title, artist, albumArt, isPlaying);
        NotificationManagerCompat.from(this.context).notify(NOTIFICATION_ID, notification);
        Log.d("MediaSessionToken", "Token: " + mediaSession.getSessionToken().toString());
    }
}



