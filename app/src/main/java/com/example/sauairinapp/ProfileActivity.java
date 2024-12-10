package com.example.sauairinapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sauairinapp.db.AppDatabase;
import com.example.sauairinapp.db.Converters;
import com.example.sauairinapp.db.ProfilePictureEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private ImageButton profileIcon, backButton;;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        backButton = findViewById(R.id.backButton);
        profileIcon = findViewById(R.id.profileIcon);

        database = AppDatabase.getInstance(this);

        // Memuat gambar terakhir
        loadProfilePicture();

        // Menangkap gambar dari kamera
        profileIcon.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        // Tombol back
        backButton.setOnClickListener(v -> navigateToMain());
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            // Koreksi rotasi gambar
            Bitmap correctedBitmap = fixImageRotation(imageBitmap);

            // Simpan gambar ke database
            saveProfilePicture(correctedBitmap);
        }
    }

    private Bitmap fixImageRotation(Bitmap bitmap) {
        try {
            // Path sementara untuk membaca data Exif
            File tempFile = File.createTempFile("temp_image", ".jpg", getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

            ExifInterface exif = new ExifInterface(tempFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotationDegrees = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotationDegrees = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotationDegrees = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotationDegrees = 270;
            }

            // Rotasi bitmap jika diperlukan
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap; // Kembalikan bitmap asli jika gagal
        }
    }


    private void saveProfilePicture(Bitmap bitmap) {
        String base64Image = Converters.bitmapToBase64(bitmap);
        ProfilePictureEntity profilePicture = new ProfilePictureEntity(base64Image);

        new Thread(() -> {
            database.profilePictureDao().insert(profilePicture);
            runOnUiThread(this::loadProfilePicture);
        }).start();
    }

    private void loadProfilePicture() {
        new Thread(() -> {
            String base64Image = database.profilePictureDao().getLastProfilePicture();
            if (base64Image != null) {
                Bitmap bitmap = Converters.base64ToBitmap(base64Image);
                runOnUiThread(() -> profileIcon.setImageBitmap(bitmap));
            }
        }).start();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

}
