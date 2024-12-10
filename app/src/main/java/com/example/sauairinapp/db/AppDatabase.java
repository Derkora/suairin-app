package com.example.sauairinapp.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.Executors;

@Database(entities = {RecordingEntity.class, ProfilePictureEntity.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    // Akses DAO untuk rekaman
    public abstract RecordingDao recordingDao();

    // Akses DAO untuk profil
    public abstract ProfilePictureDao profilePictureDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "suairin_database")
                    .fallbackToDestructiveMigration()
                    .build();

            // Menjalankan clearAllTables di background thread
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    instance.clearAllTables();
                }
            });
        }
        return instance;
    }
}
