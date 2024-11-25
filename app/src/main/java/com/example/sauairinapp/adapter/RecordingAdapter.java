package com.example.sauairinapp.adapter;

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sauairinapp.R;
import com.example.sauairinapp.item.Recording;

import java.io.IOException;
import java.util.List;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {
    private List<Recording> recordings;

    public RecordingAdapter(List<Recording> recordings) {
        this.recordings = recordings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recording recording = recordings.get(position);
        holder.title.setText(recording.getName());
        holder.date.setText(recording.getDate());

        holder.playButton.setOnClickListener(v -> {
            MediaPlayer mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(recording.getPath());
                mediaPlayer.prepare();
                mediaPlayer.start();
                holder.playButton.setImageResource(R.drawable.ic_mic_pause);

                mediaPlayer.setOnCompletionListener(mp -> {
                    holder.playButton.setImageResource(R.drawable.ic_play);
                    mediaPlayer.release();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;
        ImageButton playButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recordingTitle);
            date = itemView.findViewById(R.id.recordingDate);
            playButton = itemView.findViewById(R.id.playButton);
        }
    }
}
