package com.example.sauairinapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profile_picture")
public class ProfilePictureEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String imageBase64;

    public ProfilePictureEntity(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
