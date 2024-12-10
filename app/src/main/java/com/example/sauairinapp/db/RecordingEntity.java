package com.example.sauairinapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "recordings")
public class RecordingEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "date")
    public Date date;

    @ColumnInfo(name = "path")
    public String path;

    // Constructor tanpa parameter (default)
    public RecordingEntity() {}

    // Constructor baru untuk mendukung inisialisasi dengan parameter
    public RecordingEntity(String path, String name, long dateMillis) {
        this.path = path;
        this.name = name;
        this.date = new Date(dateMillis);
    }
}



