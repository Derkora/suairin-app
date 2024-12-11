package com.example.sauairinapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.sauairinapp.db.AppDatabase;
import com.example.sauairinapp.db.Converters;
import com.example.sauairinapp.db.ProfilePictureEntity;

import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import java.io.File;
import java.io.FileOutputStream;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private Uri photoUri;
    private File photoFile;

    private ImageButton profileIcon, backButton, settingsButton;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        backButton = findViewById(R.id.backButton);
        profileIcon = findViewById(R.id.profileIcon);
        settingsButton = findViewById(R.id.settingsButton);

        database = AppDatabase.getInstance(this);

        // Memuat gambar terakhir
        loadProfilePictureRealtime();

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

        // Tombol settings
        settingsButton.setOnClickListener(v -> {
            Intent intentSettings = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intentSettings);
        });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            try {
                // Buat file sementara untuk menyimpan gambar
                photoFile = File.createTempFile("profile_image", ".jpg", getCacheDir());
                photoUri = FileProvider.getUriForFile(this, "com.example.sauairinapp.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                // Baca bitmap dari file yang disimpan
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

                // Koreksi rotasi gambar
                Bitmap correctedBitmap = fixImageRotation(imageBitmap, photoFile.getAbsolutePath());

                // Simpan gambar ke database
                saveProfilePicture(correctedBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap fixImageRotation(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap; // Tidak perlu rotasi
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap; // Kembalikan gambar asli jika gagal
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private void saveProfilePicture(Bitmap bitmap) {
        // Resize gambar sebelum mengonversi ke base64
        Bitmap resizedBitmap = resizeBitmap(bitmap, 512, 512);
        String base64Image = Converters.bitmapToBase64(resizedBitmap);

        ProfilePictureEntity profilePicture = new ProfilePictureEntity(base64Image);

        new Thread(() -> {
            database.profilePictureDao().insert(profilePicture);
            runOnUiThread(this::loadProfilePictureRealtime);
        }).start();
    }

    private void loadProfilePictureRealtime() {
        database.profilePictureDao().getLastProfilePicture().observe(this, base64Image -> {
            if (base64Image != null) {
                Bitmap bitmap = Converters.base64ToBitmap(base64Image);
                profileIcon.setImageBitmap(bitmap);
            }
        });
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
