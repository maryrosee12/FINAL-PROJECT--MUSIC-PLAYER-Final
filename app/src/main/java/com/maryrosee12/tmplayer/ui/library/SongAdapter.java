package com.example.musicplayer.ui.library;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.bumptech.glide.Glide;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView albumCover;
        TextView title;
        TextView artist;

        public SongViewHolder(View v) {
            super(v);
            albumCover = v.findViewById(R.id.album_thumbnail);
            title = v.findViewById(R.id.song_list_title);
            artist = v.findViewById(R.id.song_list_artist);
        }
    }

    private List<Song> songs;
    private OnSongClickListener listener;

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(v);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, int position) {
        Song song = songs.get(position);
        Log.d("SongAdapter", "Binding song: " + song.getTitle());
        Glide.with(holder.albumCover.getContext()).load(song.getAlbumCover()).error(R.drawable.album_empty).into(holder.albumCover);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Song song = songs.get(position);
                    Log.d("SongAdapter", "Song clicked: " + song.getTitle());
                    listener.onSongClick(song);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}

