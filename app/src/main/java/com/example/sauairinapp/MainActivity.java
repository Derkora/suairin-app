package com.example.sauairinapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private ImageButton micButton, historyTitle;
    private TextView recordingTime, dbReading;
    private MediaRecorder mediaRecorder;
    private Handler handler;
    private Runnable timerRunnable;
    private long startTime = 0L;
    private boolean isMicActive = false;
    private boolean isRecording = false;
    private String audioFilePath;

    private SQLiteDatabase db;
    private static final String DB_NAME = "audio_records";
    private static final String TABLE_NAME = "recordings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        micButton = findViewById(R.id.micButton);
        historyTitle = findViewById(R.id.historyTitle);
        recordingTime = findViewById(R.id.recordingTime);
        dbReading = findViewById(R.id.dbReading);

        mediaRecorder = new MediaRecorder();
        handler = new Handler();

        // Initialize database
        db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, date TEXT, path TEXT)");

        setupMicButton();
        setupHistoryButton();
    }

    private void setupMicButton() {
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMicActive) {
                    // Jika mic sedang aktif, hentikan perekaman
                    stopRecording();
                    micButton.setImageResource(R.drawable.ic_mic_default); // Ubah ikon mic menjadi default
                } else {
                    // Jika mic tidak aktif, mulai perekaman
                    startRecording();
                    micButton.setImageResource(R.drawable.ic_mic_active); // Ubah ikon mic menjadi aktif
                }
                isMicActive = !isMicActive; // Toggle status
            }
        });
    }

    private void setupHistoryButton() {
        historyTitle.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void startRecording() {
        try {
            File audioFile = new File(getFilesDir(), "recording_" + System.currentTimeMillis() + ".3gp");
            audioFilePath = audioFile.getAbsolutePath();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            startTimer(); // Mulai timer untuk menunjukkan waktu perekaman
            startDbMeter(); // Mulai menunjukkan pembacaan dB
        } catch (Exception e) {
            Log.e("Recorder", "Failed to start recording", e);
        }
    }

    private void stopRecording() {
        try {
            if (isRecording) {
                mediaRecorder.stop();
                mediaRecorder.reset();

                isRecording = false;
                stopTimer(); // Hentikan timer
                stopDbMeter(); // Hentikan pembacaan dB

                // Simpan rekaman ke SQLite
                ContentValues values = new ContentValues();
                values.put("name", "Recording " + System.currentTimeMillis());
                values.put("date", android.text.format.DateFormat.format("dd MMM yyyy HH:mm", System.currentTimeMillis()).toString());
                values.put("path", audioFilePath);
                db.insert(TABLE_NAME, null, values);
            }
        } catch (Exception e) {
            Log.e("Recorder", "Failed to stop recording", e);
        }
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsed / 1000) % 60;
                int minutes = (int) (elapsed / (1000 * 60)) % 60;
                int hours = (int) (elapsed / (1000 * 60 * 60));
                recordingTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }

    private void startDbMeter() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    // Ini simulasi dB. Dalam implementasi nyata, gunakan AudioRecord untuk membaca amplitudo
                    int simulatedDb = (int) (Math.random() * 100); // Hanya untuk simulasi
                    dbReading.setText(simulatedDb + "dB");
                    handler.postDelayed(this, 500); // Perbarui setiap 500ms
                }
            }
        });
    }

    private void stopDbMeter() {
        handler.removeCallbacksAndMessages(null);
        dbReading.setText("0dB"); // Reset dB ke 0
    }
}
