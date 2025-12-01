package com.example.musicplayer.ui.playing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlaybackService;
import com.example.musicplayer.R;
import com.example.musicplayer.ui.library.Song;
import com.bumptech.glide.Glide;

public class PlayingFragment extends Fragment {

    private MediaPlaybackService mediaPlaybackService;

    private TextView songTitle;
    private TextView songArtist;
    private ImageButton playButton;
    private ImageButton skipForward;
    private ImageButton skipBackward;
    private ImageButton repeatButton;
    private ImageButton shuffleButton;
    private ImageView albumCover;

    private boolean isBound = false;

    private boolean isRepeat = false;
    private boolean isShuffle = false;

    private SeekBar seekBar;
    private Handler handler = new Handler();

    private Runnable seekBarUpdater;

    @Override
    public void onStart() {
        super.onStart();
        startSeekBarUpdater();
        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (mediaPlaybackService != null) {
            if (mediaPlaybackService.isPlaying()) {
                playButton.setBackgroundResource(R.drawable.media_pause);
            } else {
                playButton.setBackgroundResource(R.drawable.media_play);
            }
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newPosition = (int) (((float) progress / 100) * mediaPlaybackService.getSongDuration());
                    mediaPlaybackService.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing, container, false);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("serviceConnection", "Service has successfully connected");
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaPlaybackService = binder.getService();
            mediaPlaybackService.setOnSongChangedListener(new MediaPlaybackService.OnSongChangedListener() {
                @Override
                public void onSongChanged(Song song) {
                    Song currentSong = mediaPlaybackService.getCurrentSong();
                    if (currentSong != null) {
                        songTitle.setText(currentSong.getTitle());
                        songArtist.setText(currentSong.getArtist());

                        String albumArtUri = currentSong.getAlbumCover();
                        if (albumArtUri != null) {
                            Glide.with(getActivity())
                                    .load(albumArtUri)
                                    .error(R.drawable.album_empty)
                                    .into(albumCover);
                        }
                    }
                    seekBar.setMax(100);
                    updateSeekBar();
                }
            });
            isBound = true;

            if (mediaPlaybackService.isPlaying()) {
                playButton.setBackgroundResource(R.drawable.media_pause);
            } else {
                playButton.setBackgroundResource(R.drawable.media_play);
            }

            Song currentSong = mediaPlaybackService.getCurrentSong();
            if (currentSong != null) {
                songTitle.setText(currentSong.getTitle());
                songArtist.setText(currentSong.getArtist());

                String albumArtUri = currentSong.getAlbumCover();
                if (albumArtUri != null) {
                    Glide.with(getActivity())
                            .load(albumArtUri)
                            .error(R.drawable.album_empty)
                            .into(albumCover);
                } else {
                    albumCover.setImageResource(R.drawable.album_empty);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        albumCover = view.findViewById(R.id.album_cover);
        songTitle = view.findViewById(R.id.song_title);
        songArtist = view.findViewById(R.id.song_album);
        playButton = view.findViewById(R.id.play_button);
        skipForward = view.findViewById(R.id.skip_forward);
        skipBackward = view.findViewById(R.id.skip_backward);
        repeatButton = view.findViewById(R.id.track_repeat);
        shuffleButton = view.findViewById(R.id.shuffle);
        seekBar = view.findViewById(R.id.song_seek_bar);

        Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                if (mediaPlaybackService.isPlaying()) {
                    intent.setAction("ACTION_PAUSE");
                    playButton.setBackgroundResource(R.drawable.media_play);
                } else {
                    intent.setAction("ACTION_PLAY");
                    playButton.setBackgroundResource(R.drawable.media_pause);
                }
                getActivity().startService(intent);
            }
        });

        skipForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                intent.setAction("ACTION_SKIP_TO_NEXT");
                getActivity().startService(intent);
            }
        });

        skipBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MediaPlaybackService.class);
                intent.setAction("ACTION_SKIP_TO_PREVIOUS");
                getActivity().startService(intent);
            }
        });

        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    isRepeat = !isRepeat;
                    mediaPlaybackService.setRepeat(isRepeat);
                    if (isRepeat) {
                        ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#34C95C"));
                        repeatButton.setBackgroundTintList(colorStateList);
                    } else {
                        ColorStateList inactiveStateList = ColorStateList.valueOf(Color.GRAY);
                        repeatButton.setBackgroundTintList(inactiveStateList);
                    }
                }
            }
        });


        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBound) {
                    isShuffle = !isShuffle;
                    mediaPlaybackService.setShuffle(isShuffle);
                    if (isShuffle) {
                        ColorStateList colorStateList = ColorStateList.valueOf(Color.parseColor("#34C95C"));
                        shuffleButton.setBackgroundTintList(colorStateList);
                    } else {
                        ColorStateList inactiveStateList = ColorStateList.valueOf(Color.GRAY);
                        shuffleButton.setBackgroundTintList(inactiveStateList);
                    }
                }
            }
        });
    }


    @Override
    public void onStop() {
        super.onStop();
        stopSeekBarUpdater();
        if (isBound) {
            mediaPlaybackService.setOnSongChangedListener(null);
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (isBound) {
            getActivity().unbindService(serviceConnection);
            isBound = false;
        }
    }


    private void updateSeekBar() {
        if (mediaPlaybackService != null) {
            int progress = (int) (((float) mediaPlaybackService.getCurrentPosition() / mediaPlaybackService.getSongDuration()) * 100);
            seekBar.setProgress(progress);
            if (mediaPlaybackService.isPlaying()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateSeekBar();
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        }
    }


    private void startSeekBarUpdater() {
        stopSeekBarUpdater();
        seekBarUpdater = new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(seekBarUpdater, 1000);
    }

    private void stopSeekBarUpdater() {
        if (seekBarUpdater != null) {
            handler.removeCallbacks(seekBarUpdater);
            seekBarUpdater = null;
        }
    }
}
