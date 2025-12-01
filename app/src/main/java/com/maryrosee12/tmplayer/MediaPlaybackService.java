package com.example.musicplayer;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.os.Binder;
import android.util.Log;

import androidx.media.MediaBrowserServiceCompat;

import com.example.musicplayer.ui.library.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;




public class MediaPlaybackService extends MediaBrowserServiceCompat {


    private List<Song> songList;
    private int currentSongIndex;

    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;
    private MediaNotificationManager mediaNotificationManager;


    private String currentTitle;
    private String currentArtist;
    private Bitmap currentAlbumArt;
    private Song currentSong;
    private static final int NOTIFICATION_ID = 1;
    private OnSongChangedListener onSongChangedListener;


    private boolean isRepeat = false;

    private boolean isShuffle = false;
    boolean isSwitchingSongs = false;


    private List<Integer> shuffledIndices;
    private int currentIndex = 0;
    private boolean userHasSkipped = false;


    private static final String PREFS_NAME = "com.adepge.tmplayer";
    private static final String PREF_CURRENT_SONG_INDEX = "currentSongIndex";
    private static final String PREF_SONG_POSITION = "songPosition";


    public interface OnSongChangedListener {
        void onSongChanged(Song song);
    }


    private void saveSongInfo() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_CURRENT_SONG_INDEX, currentSongIndex);
        editor.putInt(PREF_SONG_POSITION, mediaPlayer.getCurrentPosition());
        editor.apply();
    }


    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }


    public void setOnSongChangedListener(OnSongChangedListener listener) {
        this.onSongChangedListener = listener;
    }


    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }


    public void setRepeat(boolean isRepeat) {
        this.isRepeat = isRepeat;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSessionCompat(this, "MusicPlayer");
        mediaPlayer = new MediaPlayer();


        mediaSession.setCallback(new MediaSessionCompat.Callback() {

            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                switch (command) {
                    case "ACTION_PLAY":
                        onPlay();
                        break;
                    case "ACTION_PAUSE":
                        onPause();
                        break;
                    case "ACTION_SKIP_TO_NEXT":
                        onSkipToNext();
                        break;
                    case "ACTION_SKIP_TO_PREVIOUS":
                        onSkipToPrevious();
                        break;
                    case "ACTION_STOP":
                        onStop();
                        break;
                    default:
                        super.onCommand(command, extras, cb);
                }
            }


            @Override
            public void onPlay() {
                super.onPlay();
                mediaPlayer.start();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                        .build());
                mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, true);
            }


            @Override
            public void onPause() {
                super.onPause();
                mediaPlayer.pause();
                saveSongInfo();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                        .build());
                mediaNotificationManager.updateNotification(currentTitle, currentArtist, currentAlbumArt, false);
            }


            @Override
            public void onStop() {
                super.onStop();
                mediaPlayer.stop();
                saveSongInfo();
                mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_STOPPED, mediaPlayer.getCurrentPosition(), 1)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                        .build());
                stopForeground(true);
            }


            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                userHasSkipped = true;
                if (currentSongIndex < songList.size() - 1) {
                    if (isRepeat) {
                        playSong(songList.get(currentSongIndex));
                    } else {
                        if (isShuffle) {
                            currentIndex = (currentIndex + 1) % shuffledIndices.size();
                            currentSongIndex = shuffledIndices.get(currentIndex);
                        } else {
                            currentSongIndex++;
                        }
                        playSong(songList.get(currentSongIndex));
                    }
                }
            }


            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                userHasSkipped = true;
                if (currentSongIndex > 0) {
                    if (isRepeat) {
                        playSong(songList.get(currentSongIndex));
                    } else {
                        if (isShuffle) {
                            currentIndex = (currentIndex - 1) % shuffledIndices.size();
                            currentSongIndex = shuffledIndices.get(currentIndex);
                        } else {
                            currentSongIndex--;
                        }
                        playSong(songList.get(currentSongIndex));
                    }
                }
            }
        });


        mediaSession.setActive(true);
        mediaNotificationManager = new MediaNotificationManager(this, mediaSession);


        setSessionToken(mediaSession.getSessionToken());


        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(PREF_CURRENT_SONG_INDEX) && prefs.contains(PREF_SONG_POSITION)) {
            currentSongIndex = prefs.getInt(PREF_CURRENT_SONG_INDEX, 0);
            int songPosition = prefs.getInt(PREF_SONG_POSITION, 0);

            if (songList != null && !songList.isEmpty()) {
                playSong(songList.get(currentSongIndex));
                mediaPlayer.seekTo(songPosition);
            }
        }
    }


    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot("root", null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(new ArrayList<>());
    }


    public void playSong(Song song) {
        currentTitle = song.getTitle();
        currentArtist = song.getArtist();
        currentAlbumArt = getAlbumArt(song.getAlbumCover());
        currentSong = song;
        try {

            isSwitchingSongs = true;
            mediaPlayer.reset();
            Log.d("MediaPlaybackService", "Song Uri: " + song.getSongUri());
            mediaPlayer.setDataSource(this, song.getSongUri());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1)
                            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                            .build());
                    Notification notification = mediaNotificationManager.showNotification(currentTitle, currentArtist, currentAlbumArt, mp.isPlaying());
                    startForeground(NOTIFICATION_ID, notification);
                    isSwitchingSongs = false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!isSwitchingSongs && !userHasSkipped) {
                        if (isRepeat) {
                            playSong(song);
                        } else {
                            if (isShuffle) {
                                currentIndex = (currentIndex + 1) % shuffledIndices.size();
                                playSong(songList.get(shuffledIndices.get(currentIndex)));
                            } else {
                                if (currentSongIndex < songList.size() - 1) {
                                    currentSongIndex++;
                                    playSong(songList.get(currentSongIndex));
                                } else {
                                    mediaPlayer.stop();
                                }
                            }
                        }
                    }
                    userHasSkipped = false;
                }
            });
            if (onSongChangedListener != null) {
                onSongChangedListener.onSongChanged(song);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void playSongs(List<Song> songs, int startIndex) {
        songList = songs;
        currentSongIndex = startIndex;
        playSong(songList.get(currentSongIndex));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaSession.setActive(false);
        mediaSession.release();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "ACTION_PLAY":
                        mediaSession.getController().getTransportControls().play();
                        break;
                    case "ACTION_PAUSE":
                        mediaSession.getController().getTransportControls().pause();
                        break;
                    case "ACTION_SKIP_TO_NEXT":
                        mediaSession.getController().getTransportControls().skipToNext();
                        break;
                    case "ACTION_SKIP_TO_PREVIOUS":
                        mediaSession.getController().getTransportControls().skipToPrevious();
                        break;
                    case "ACTION_STOP":
                        mediaSession.getController().getTransportControls().stop();
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }


    public Bitmap getAlbumArt(String albumArtUri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(albumArtUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    public Song getCurrentSong() {
        return currentSong;
    }


    public void setShuffle(boolean isShuffle) {
        this.isShuffle = isShuffle;
        if (isShuffle) {
            shuffleIndices();
        } else {
            currentIndex = shuffledIndices.indexOf(currentSongIndex);
        }
    }


    private void shuffleIndices() {
        shuffledIndices = new ArrayList<>();
        for (int i = 0; i < songList.size(); i++) {
            shuffledIndices.add(i);
        }
        Collections.shuffle(shuffledIndices);
        currentIndex = shuffledIndices.indexOf(currentSongIndex);
    }


    public int getSongDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

}

