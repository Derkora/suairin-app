package com.example.sauairinapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sauairinapp.adapter.RecordingAdapter;
import com.example.sauairinapp.item.Recording;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private static final String DB_NAME = "audio_records";
    private static final String TABLE_NAME = "recordings";
    private RecyclerView recyclerView;
    private RecordingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        List<Recording> recordings = getAllRecordings();
        adapter = new RecordingAdapter(recordings);
        recyclerView.setAdapter(adapter);
    }

    private List<Recording> getAllRecordings() {
        List<Recording> recordings = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        while (cursor.moveToNext()) {
            recordings.add(new Recording(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3)
            ));
        }
        cursor.close();
        return recordings;
    }
}
