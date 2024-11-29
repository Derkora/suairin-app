package com.example.sauairinapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.sauairinapp.db.AppDatabase;
import com.example.sauairinapp.db.RecordingEntity;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final LiveData<List<RecordingEntity>> recordings;
    private final AppDatabase db;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        recordings = db.recordingDao().getAllRecordings();
    }

    public LiveData<List<RecordingEntity>> getRecordings() {
        return recordings;
    }

    public void deleteRecording(RecordingEntity recording) {
        new Thread(() -> db.recordingDao().delete(recording)).start();
    }

    public void updateRecording(RecordingEntity recording) {
        new Thread(() -> db.recordingDao().update(recording)).start();
    }

}
