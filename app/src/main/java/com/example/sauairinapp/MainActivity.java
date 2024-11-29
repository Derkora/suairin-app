package com.example.sauairinapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;

import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.example.sauairinapp.db.AppDatabase;
import com.example.sauairinapp.db.RecordingEntity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private String currentFilePath;

    private Chronometer chronometer;
    private TextView dbLevelTextView;

    private ImageButton recordButton, stopButton, doneButton, historyButton;

    private List<String> recordingSegments = new ArrayList<>();
    private int segmentCount = 0;

    private Handler handler = new Handler();
    private Runnable dbLevelUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 1);

        historyButton = findViewById(R.id.historyButton);
        recordButton = findViewById(R.id.recordButton);
        stopButton = findViewById(R.id.stopButton);
        doneButton = findViewById(R.id.doneButton);
        chronometer = findViewById(R.id.recordingTime);
        dbLevelTextView = findViewById(R.id.dbLevelTextView);

        stopButton.setVisibility(View.GONE);
        doneButton.setVisibility(View.GONE);

        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                pauseRecording();
            } else {
                startRecording(); // Nonaktifkan resume dengan hanya memulai rekaman baru
            }
        });

        stopButton.setOnClickListener(v -> cancelRecording());
        doneButton.setOnClickListener(v -> saveRecording());

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void startRecording() {
        if (!checkPermissions()) return;

        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        currentFilePath = dir.getAbsolutePath() + "/SEG_" + timeStamp + "_" + segmentCount + ".mp3";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(currentFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            isPaused = false;

            if (segmentCount == 0) {
                chronometer.setBase(SystemClock.elapsedRealtime());
            } else {
                long pausedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                chronometer.setBase(SystemClock.elapsedRealtime() - pausedTime);
            }
            chronometer.start();
            startDbLevelUpdates();
            updateUIForRecording();
            Toast.makeText(this, "Recording ...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseRecording() {
        if (isRecording && mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            isPaused = true;
            chronometer.stop();
            handler.removeCallbacks(dbLevelUpdater);

            // Tambahkan file ke daftar segmen
            recordingSegments.add(currentFilePath);
            segmentCount++;
            updateUIForPause();
        }
    }

    private void resumeRecording() {
        Toast.makeText(this, "Resume recording is disabled", Toast.LENGTH_SHORT).show();
    }

    private void cancelRecording() {
        if (isRecording) {
            pauseRecording();
        }
        for (String segment : recordingSegments) {
            File file = new File(segment);
            if (file.exists()) {
                file.delete();
            }
        }
        resetToInitialState();
        Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show();
    }

    private void saveRecording() {
        if (isRecording) {
            // Hentikan rekaman jika sedang berlangsung
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            chronometer.stop();
            handler.removeCallbacks(dbLevelUpdater);
        }

        if (currentFilePath == null || currentFilePath.isEmpty()) {
            Toast.makeText(this, "No recording to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String outputFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/REC_" + timeStamp + ".mp3";

        File currentFile = new File(currentFilePath);
        File outputFile = new File(outputFilePath);

        if (!currentFile.exists()) {
            Toast.makeText(this, "Recording file does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Rename or move the recorded file to the final output path
        if (currentFile.renameTo(outputFile)) {
            runOnUiThread(() -> {
                RecordingEntity recording = new RecordingEntity();
                recording.name = "Recording " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                recording.date = new Date();
                recording.path = outputFilePath;

                AppDatabase db = AppDatabase.getInstance(this);
                Executors.newSingleThreadExecutor().execute(() -> db.recordingDao().insert(recording));

                Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show();
                resetToInitialState();
            });
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Failed to save recording", Toast.LENGTH_SHORT).show());
        }
    }


    private void startDbLevelUpdates() {
        dbLevelUpdater = new Runnable() {
            @Override
            public void run() {
                if (isRecording && mediaRecorder != null) {
                    int maxAmplitude = mediaRecorder.getMaxAmplitude();
                    double dB = 20 * Math.log10(maxAmplitude == 0 ? 1 : maxAmplitude);
                    dbLevelTextView.setText(String.format(Locale.getDefault(), "dB: %.2f", dB));
                    handler.postDelayed(this, 200);
                }
            }
        };
        handler.post(dbLevelUpdater);
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateUIForRecording() {
        recordButton.setImageResource(R.drawable.ic_pause);
        stopButton.setVisibility(View.GONE);
        doneButton.setVisibility(View.GONE);
    }

    private void updateUIForPause() {
        recordButton.setImageResource(R.drawable.ic_mic_default);
        stopButton.setVisibility(View.VISIBLE);
        doneButton.setVisibility(View.VISIBLE);
        dbLevelTextView.setText("paused");
    }

    private void resetToInitialState() {
        recordingSegments.clear();
        segmentCount = 0;
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        recordButton.setImageResource(R.drawable.ic_mic_default);
        stopButton.setVisibility(View.GONE);
        doneButton.setVisibility(View.GONE);
        dbLevelTextView.setText("dB: 0.00");
        currentFilePath = null;
    }
}
