package com.example.sauairinapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;

import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sauairinapp.db.AppDatabase;
import com.example.sauairinapp.db.Converters;
import com.example.sauairinapp.db.RecordingEntity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private String currentFilePath;

    private Chronometer chronometer;
    private TextView dbLevelTextView;

    private ImageButton recordButton, stopButton, doneButton, historyButton, profileButton;
    private AppDatabase database;

    private List<String> recordingSegments = new ArrayList<>();
    private int segmentCount = 0;

    private Handler handler = new Handler();
    private Runnable dbLevelUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Periksa izin saat aplikasi dimulai
        if (!checkPermissions()) {
            requestPermissions();
        }

        database = AppDatabase.getInstance(this);

        profileButton = findViewById(R.id.profileButton);
        historyButton = findViewById(R.id.historyButton);
        recordButton = findViewById(R.id.recordButton);
        stopButton = findViewById(R.id.stopButton);
        doneButton = findViewById(R.id.doneButton);
        chronometer = findViewById(R.id.recordingTime);
        dbLevelTextView = findViewById(R.id.dbLevelTextView);

        loadProfilePicture();

        stopButton.setVisibility(View.GONE);
        doneButton.setVisibility(View.GONE);

        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                pauseRecording();
            } else {
                startRecording();
            }
        });

        stopButton.setOnClickListener(v -> cancelRecording());
        doneButton.setOnClickListener(v -> saveRecording());

        profileButton.setOnClickListener(v -> {
            Intent intentProfile = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intentProfile);
        });

        historyButton.setOnClickListener(v -> {
            Intent intentHistory = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intentHistory);
        });
    }

    // Memeriksa apakah izin yang diperlukan telah diberikan
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Meminta izin jika belum diberikan
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, PERMISSION_REQUEST_CODE);
    }

    // Menangani hasil permintaan izin
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_SHORT).show();
                finish(); // Tutup aplikasi jika izin tidak diberikan
            }
        }
    }

    private void startRecording() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        currentFilePath = dir.getAbsolutePath() + "/SEG_" + timeStamp + "_" + segmentCount + ".m4a"; // Gunakan ekstensi .m4a

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // Format MPEG_4 sesuai untuk .m4a
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);    // Encoder AAC untuk audio
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

            recordingSegments.add(currentFilePath);
            segmentCount++;
            updateUIForPause();
        }
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
        String outputFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/REC_" + timeStamp + ".wav"; // Target file WAV

        // Konversi file dari m4a ke wav
        convertToWav(currentFilePath, outputFilePath);

        // Simpan metadata ke database setelah konversi selesai
        RecordingEntity recording = new RecordingEntity();
        recording.name = "Recording " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        recording.date = new Date();
        recording.path = outputFilePath;

        Executors.newSingleThreadExecutor().execute(() -> database.recordingDao().insert(recording));

        resetToInitialState();
    }

    private void convertToWav(String inputFilePath, String outputFilePath) {
        String[] command = new String[] {
                "-i", inputFilePath,   // Input file path
                "-ac", "2",            // Set the number of audio channels (stereo)
                "-ar", "44100",        // Set the audio sampling rate (44.1kHz)
                "-acodec", "pcm_s16le",// Set audio codec to PCM signed 16-bit little-endian (WAV format)
                outputFilePath         // Output file path
        };

        FFmpeg.executeAsync(command, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                if (returnCode == 0) {
                    // Conversion successful
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "File converted to WAV", Toast.LENGTH_SHORT).show());
                } else {
                    // Conversion failed
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to convert file", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void startDbLevelUpdates() {
        dbLevelUpdater = () -> {
            if (isRecording && mediaRecorder != null) {
                int maxAmplitude = mediaRecorder.getMaxAmplitude();
                double dB = 20 * Math.log10(maxAmplitude == 0 ? 1 : maxAmplitude);
                dbLevelTextView.setText(String.format(Locale.getDefault(), "dB: %.2f", dB));
                handler.postDelayed(dbLevelUpdater, 200);
            }
        };
        handler.post(dbLevelUpdater);
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

    private void loadProfilePicture() {
        new Thread(() -> {
            String base64Image = database.profilePictureDao().getLastProfilePicture();
            if (base64Image != null) {
                Bitmap bitmap = Converters.base64ToBitmap(base64Image);
                runOnUiThread(() -> profileButton.setImageBitmap(bitmap));
            }
        }).start();
    }
}
