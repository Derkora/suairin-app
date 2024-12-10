package com.example.sauairinapp;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sauairinapp.adapter.HistoryAdapter;
import com.example.sauairinapp.db.RecordingEntity;
import com.example.sauairinapp.viewmodel.HistoryViewModel;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryViewModel viewModel;
    private MediaPlayer mediaPlayer;
    private ImageButton playPauseButton, backButton;
    private SeekBar seekBar;
    private TextView currentTitle, playbackTime;
    private boolean isPlaying = false;
    private Timer progressTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Inisialisasi UI
        recyclerView = findViewById(R.id.recyclerView);
        playPauseButton = findViewById(R.id.playButton);
        backButton = findViewById(R.id.backButton);
        currentTitle = findViewById(R.id.audioName);
        playbackTime = findViewById(R.id.playbackTime);
        seekBar = findViewById(R.id.seekBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mediaPlayer = new MediaPlayer();
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // Muat data rekaman
        viewModel.getRecordings().observe(this, this::updateRecordings);

        // Tombol play/pause
        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) pauseAudio();
            else resumeAudio();
        });

        // Tombol back
        backButton.setOnClickListener(v -> navigateToMain());

        // Atur interaksi pengguna dengan SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress); // Atur posisi audio
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressUpdate(); // Hentikan timer sementara
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startProgressUpdate(); // Lanjutkan timer setelah pengguna berhenti menyeret
            }
        });

    }

    private void updateRecordings(List<RecordingEntity> recordings) {
        if (recordings == null || recordings.isEmpty()) {
            Toast.makeText(this, "No recordings available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Urutkan rekaman dari yang terbaru ke yang lama
        recordings.sort((r1, r2) -> r2.date.compareTo(r1.date));

        // Pasang adapter ke RecyclerView
        HistoryAdapter adapter = new HistoryAdapter(recordings, (filePath, title) -> {
            playRecording(filePath, title); // Langsung memutar ketika item ditekan
        }, viewModel, this::showMoreOptions);

        recyclerView.setAdapter(adapter);

        // Atur audio terbaru sebagai default tanpa memutar
        RecordingEntity latestRecording = recordings.get(0);
        updateAudioDetails(latestRecording.path, latestRecording.name);
    }


    private void updateAudioDetails(String filePath, String title) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare(); // Siapkan audio untuk membaca durasi

            // Perbarui UI tanpa memutar
            currentTitle.setText(title);
            seekBar.setMax(mediaPlayer.getDuration());
            playbackTime.setText(String.format("00:00 / %02d:%02d",
                    mediaPlayer.getDuration() / 60000, (mediaPlayer.getDuration() % 60000) / 1000));
            Log.d("HistoryActivity", "Audio details updated: " + title);
        } catch (Exception e) {
            Log.e("HistoryActivity", "Error updating audio details", e);
            Toast.makeText(this, "Error loading audio details", Toast.LENGTH_SHORT).show();
        }
    }

    private void playRecording(String filePath, String title) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show();
                Log.e("HistoryActivity", "File not found: " + filePath);
                return;
            }

            Log.d("HistoryActivity", "Playing file: " + filePath);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(filePath);

            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mediaPlayer.getDuration());
                mediaPlayer.start();
                Log.d("HistoryActivity", "Playback started for: " + title);
            });

            mediaPlayer.prepare(); // Memulai persiapan audio
            currentTitle.setText(title);
            playPauseButton.setImageResource(R.drawable.ic_pause);
            isPlaying = true;

            startProgressUpdate(); // Memperbarui SeekBar selama pemutaran

            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                stopProgressUpdate();
            });
        } catch (Exception e) {
            Log.e("HistoryActivity", "Error playing recording", e);
            Toast.makeText(this, "Unable to play the recording", Toast.LENGTH_SHORT).show();
        }
    }


    private void pauseAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setImageResource(R.drawable.ic_play);
            isPlaying = false;
        }
    }

    private void resumeAudio() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            playPauseButton.setImageResource(R.drawable.ic_pause);
            isPlaying = true;
        }
    }

    private void startProgressUpdate() {
        progressTimer = new Timer();
        progressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (mediaPlayer != null && isPlaying) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(currentPosition);

                        playbackTime.setText(String.format("%02d:%02d / %02d:%02d",
                                currentPosition / 60000, (currentPosition % 60000) / 1000,
                                mediaPlayer.getDuration() / 60000, (mediaPlayer.getDuration() % 60000) / 1000));
                    }
                });
            }
        }, 0, 1000);
    }


    private void stopProgressUpdate() {
        if (progressTimer != null) {
            progressTimer.cancel();
            progressTimer = null;
        }
    }

    private void showMoreOptions(RecordingEntity recording) {
        String[] options = {"Rename", "Enhance", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle("Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showRenameDialog(recording); break;
                        case 1: enhanceRecording(recording); break;
                        case 2: viewModel.deleteRecording(recording); break;
                    }
                }).show();
    }

    private void enhanceRecording(RecordingEntity recording) {
        String filePath = recording.path;
        String originalFileName = recording.name;

        // Format nama file enhanced dengan "Enhanced_" di depan nama file asli
        String enhancedFileName = "Enhanced_" + originalFileName;

        // Tentukan direktori tujuan untuk menyimpan file yang ditingkatkan (langsung di /Music)
        File musicDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "");
        if (!musicDir.exists()) {
            musicDir.mkdirs(); // Membuat folder Music jika belum ada
        }

        // Menggunakan sistem waktu untuk nama file, disimpan langsung di direktori /Music
        String timeStampedFileName = "REC_" + System.currentTimeMillis() + ".wav";
        File enhancedFile = new File(musicDir, timeStampedFileName);

        Log.d("HistoryActivity", "Enhancing file: " + filePath);

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .build();

                File file = new File(filePath);
                RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/wav"));

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("audio", file.getName(), fileBody)
                        .build();

                Request request = new Request.Builder()
                        .url("http://159.65.143.238:5000/enhance-audio")
                        .post(requestBody)
                        .build();

                Log.d("HistoryActivity", "File path to upload: " + filePath);
                Log.d("HistoryActivity", "File exists: " + file.exists());

                Response response = client.newCall(request).execute();
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        // Menyimpan file hasil enhancement dengan nama yang ditentukan
                        try (BufferedSink sink = Okio.buffer(Okio.sink(enhancedFile))) {
                            sink.writeAll(response.body().source());
                        }

                        // Setelah file selesai diproses, tambahkan rekaman enhanced ke database
                        runOnUiThread(() -> {
                            RecordingEntity enhancedRecording = new RecordingEntity(
                                    enhancedFile.getAbsolutePath(),
                                    enhancedFileName, // Nama file enhanced tetap yang original
                                    System.currentTimeMillis()
                            );
                            viewModel.addRecording(enhancedRecording);
                            Toast.makeText(this, "Enhanced audio saved", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.e("HistoryActivity", "Server response: " + response.code() + " " + response.message());
                        if (response.body() != null) {
                            Log.e("HistoryActivity", "Response body: " + response.body().string());
                        }
                        runOnUiThread(() -> Toast.makeText(this, "Failed to enhance audio", Toast.LENGTH_SHORT).show());
                    }
                } finally {
                    response.close(); // Pastikan response ditutup
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error enhancing audio", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void showRenameDialog(RecordingEntity recording) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(recording.name);

        new AlertDialog.Builder(this)
                .setTitle("Rename Recording")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        recording.name = newName;
                        viewModel.updateRecording(recording);
                        Toast.makeText(this, "Recording renamed", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopProgressUpdate();
    }
}
