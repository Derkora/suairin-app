package com.example.sauairinapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface ProfilePictureDao {
    @Insert
    void insert(ProfilePictureEntity profilePicture);

    @Query("SELECT imageBase64 FROM profile_picture ORDER BY id DESC LIMIT 1")
    String getLastProfilePicture();
}
