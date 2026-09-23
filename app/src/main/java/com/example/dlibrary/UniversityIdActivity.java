package com.example.dlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class UniversityIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_university_id);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Digital Library ID Card");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SessionManager sessionManager = new SessionManager(this);

        TextView txtName = findViewById(R.id.id_user_name);
        TextView txtNumber = findViewById(R.id.id_user_number);
        TextView txtDept = findViewById(R.id.id_user_dept);
        TextView txtRoleBadge = findViewById(R.id.id_role_badge);
        ImageView imgUserPhoto = findViewById(R.id.user_photo);

        txtName.setText(sessionManager.getName().toUpperCase());
        txtRoleBadge.setText(sessionManager.getRole().toUpperCase());

        String idVal = sessionManager.getStudentId();
        txtNumber.setText("ID: " + (idVal.isEmpty() ? "N/A" : idVal));
        txtDept.setText("Dept: " + sessionManager.getDepartment());

        loadProfilePicture(sessionManager, imgUserPhoto);
    }

    private void loadProfilePicture(SessionManager sessionManager, ImageView userPhoto) {
        String email = sessionManager.getEmail();
        File localCache = new File(getFilesDir(), "profile_" + email.replace("@", "_").replace(".", "_") + ".jpg");

        if (localCache.exists() && localCache.length() > 0) {
            Bitmap bitmap = BitmapFactory.decodeFile(localCache.getAbsolutePath());
            if (bitmap != null) {
                userPhoto.clearColorFilter();
                userPhoto.setImageBitmap(bitmap);
                return;
            }
        }

        String url = sessionManager.getProfileImageUrl();
        if (url.isEmpty()) {
            DatabaseHelper db = new DatabaseHelper(this);
            url = db.getUserProfileImage(email);
            db.close();
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            String finalUrl = url;
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    URL u = new URL(finalUrl);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(10000);
                    try (InputStream in = conn.getInputStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                        if (bitmap != null) {
                            try (FileOutputStream out = new FileOutputStream(localCache)) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                            } catch (Exception ignored) {}

                            runOnUiThread(() -> {
                                userPhoto.clearColorFilter();
                                userPhoto.setImageBitmap(bitmap);
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("UniversityIdActivity", "Error loading photo: " + e.getMessage());
                }
            });
        } else {
            userPhoto.setImageResource(R.drawable.ic_profile);
            userPhoto.setColorFilter(Color.parseColor("#1A237E"));
        }
    }
}
