package com.example.sauairinapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String DB_NAME = "audio_records";
    private static final String TABLE_NAME = "recordings";
    private SQLiteDatabase db;

    private MediaRecorder mediaRecorder;
    private String filePath;
    private boolean isRecording = false;

    private ImageButton recordButton, historyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 1);

        // Initialize database
        db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        createTable();

        recordButton = findViewById(R.id.recordButton);
        historyButton = findViewById(R.id.historyButton);

        // Record button functionality
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        // History button functionality
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void createTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "date TEXT, " +
                "path TEXT)";
        db.execSQL(createTableQuery);
    }

    private void startRecording() {
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        filePath = directory.getAbsolutePath() + "/REC_" + timeStamp + ".mp3";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordButton.setImageResource(R.drawable.ic_mic_pause);
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        isRecording = false;
        recordButton.setImageResource(R.drawable.ic_mic_default);

        // Save recording to database
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        ContentValues values = new ContentValues();
        values.put("name", "Recording " + timeStamp);
        values.put("date", timeStamp);
        values.put("path", filePath);
        db.insert(TABLE_NAME, null, values);

        Toast.makeText(this, "Recording saved.", Toast.LENGTH_SHORT).show();
    }
}
