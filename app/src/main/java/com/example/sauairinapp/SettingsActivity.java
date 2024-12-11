package com.example.sauairinapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton backButton;
    private String source; // Menyimpan asal Intent

    public static final String SOURCE_MAIN = "MainActivity";
    public static final String SOURCE_HISTORY = "HistoryActivity";
    public static final String SOURCE_PROFILE = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        backButton = findViewById(R.id.backButton);

        // Ambil data "source" dari Intent dengan default nilai "MainActivity"
        source = getIntent().getStringExtra("source");
        if (source == null) {
            source = "MainActivity"; // Default nilai jika tidak ada sumber
        }

        // Tombol back
        backButton.setOnClickListener(v -> navigateBack());
    }


    private void navigateBack() {
        Intent intent;
        if (SOURCE_MAIN.equals(source)) {
            intent = new Intent(this, MainActivity.class);
        } else if (SOURCE_HISTORY.equals(source)) {
            intent = new Intent(this, HistoryActivity.class);
        } else if (SOURCE_PROFILE.equals(source)) {
            intent = new Intent(this, ProfileActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class); // Default fallback
        }
        startActivity(intent);
        finish();
    }


}
