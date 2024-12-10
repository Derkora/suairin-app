package com.example.sauairinapp.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY date DESC")
    LiveData<List<RecordingEntity>> getAllRecordings();

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Tambahkan strategi untuk menangani konflik
    void insert(RecordingEntity recording);

    @Delete
    void delete(RecordingEntity recording);

    @Update
    void update(RecordingEntity recording);
}
