package com.example.sauairinapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sauairinapp.adapter.HistoryAdapter;
import com.example.sauairinapp.item.HistoryItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyList;
    private ImageButton micButton;
    private boolean isMicActive = false; // Menyimpan status aktif dari micButton

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Initialize data
        historyList = new ArrayList<>();
        historyList.add(new HistoryItem("50 dB", "30 Okt 2024", "19.14 PM"));
        historyList.add(new HistoryItem("45 dB", "29 Okt 2024", "18.10 PM"));
        historyList.add(new HistoryItem("60 dB", "28 Okt 2024", "20.00 PM"));
        historyList.add(new HistoryItem("55 dB", "27 Okt 2024", "21.30 PM"));

        Log.d("MainActivity", "History List Size: " + historyList.size());

        // Set adapter
        historyAdapter = new HistoryAdapter(historyList);
        recyclerViewHistory.setAdapter(historyAdapter);

        // Setup mic button
        micButton = findViewById(R.id.micButton);
        micButton.setImageResource(R.drawable.ic_mic_default); // Set drawable awal

        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMicActive) {
                    micButton.setImageResource(R.drawable.ic_mic_default);
                } else {
                    micButton.setImageResource(R.drawable.ic_mic_active);
                }
                isMicActive = !isMicActive; // Toggle status
            }
        });
    }
}
