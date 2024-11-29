package com.example.sauairinapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sauairinapp.R;
import com.example.sauairinapp.db.RecordingEntity;
import com.example.sauairinapp.viewmodel.HistoryViewModel;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<RecordingEntity> recordings;
    private final OnRecordingClickListener listener;
    private final OnMoreOptionsClickListener moreOptionsClickListener;

    public HistoryAdapter(List<RecordingEntity> recordings, OnRecordingClickListener listener,
                          HistoryViewModel viewModel, OnMoreOptionsClickListener moreOptionsClickListener) {
        this.recordings = recordings;
        this.listener = listener;
        this.moreOptionsClickListener = moreOptionsClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecordingEntity recording = recordings.get(position);
        holder.recordingTitle.setText(recording.name);
        holder.recordingDate.setText(recording.date.toString());

        holder.itemView.setOnClickListener(v -> listener.onRecordingClick(recording.path, recording.name));
        holder.moreButton.setOnClickListener(v -> moreOptionsClickListener.onMoreOptionsClick(recording));
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public interface OnRecordingClickListener {
        void onRecordingClick(String filePath, String title);
    }

    public interface OnMoreOptionsClickListener {
        void onMoreOptionsClick(RecordingEntity recording);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recordingTitle, recordingDate;
        ImageButton moreButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recordingTitle = itemView.findViewById(R.id.recordingTitle);
            recordingDate = itemView.findViewById(R.id.recordingDate);
            moreButton = itemView.findViewById(R.id.moreButton);
        }
    }
}
