package com.example.dlibrary;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;

public class ResourceDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private int resourceId;
    private Resource currentResource;

    private TextView txtType, txtCode, txtTitle, txtAuthor, txtCategory, txtDept, txtUploader, txtDate, txtStats;
    private ImageButton btnFavorite;
    private MaterialButton btnReadOnline, btnDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        resourceId = getIntent().getIntExtra("resource_id", -1);
        if (resourceId == -1) {
            Toast.makeText(this, "Invalid Resource", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadResourceDetails();
    }

    private void initViews() {
        txtType = findViewById(R.id.detail_type);
        txtCode = findViewById(R.id.detail_code);
        txtTitle = findViewById(R.id.detail_title);
        txtAuthor = findViewById(R.id.detail_author);
        txtCategory = findViewById(R.id.detail_category);
        txtDept = findViewById(R.id.detail_dept);
        txtUploader = findViewById(R.id.detail_uploader);
        txtDate = findViewById(R.id.detail_date);
        txtStats = findViewById(R.id.detail_stats);

        btnFavorite = findViewById(R.id.btn_favorite);
        btnReadOnline = findViewById(R.id.btn_read_online);
        btnDownload = findViewById(R.id.btn_download_file);

        btnFavorite.setOnClickListener(v -> {
            boolean isFav = dbHelper.toggleFavorite(sessionManager.getEmail(), resourceId);
            updateFavoriteUI(isFav);
            loadResourceDetails(); // Refresh stats
            Toast.makeText(this, isFav ? "Added to Favorites!" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
        });

        btnReadOnline.setOnClickListener(v -> showOnlineReaderDialog());

        btnDownload.setOnClickListener(v -> {
            dbHelper.incrementDownloadCount(resourceId, sessionManager.getEmail());
            loadResourceDetails(); // Refresh stats
            Toast.makeText(this, "Downloading " + currentResource.getTitle() + "...", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadResourceDetails() {
        Cursor cursor = dbHelper.getResourceById(resourceId);
        if (cursor != null && cursor.moveToFirst()) {
            int titleIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE);
            int authorIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_AUTHOR);
            int catIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY);
            int typeIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE);
            int fileIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_FILE_PATH);
            int codeIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_CODE);
            int deptIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_RES_DEPARTMENT);
            int byIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_UPLOADED_BY);
            int statusIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_APPROVAL_STATUS);
            int dateIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_UPLOAD_DATE);
            int dlIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_DOWNLOAD_COUNT);
            int favIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_FAVORITE_COUNT);

            String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
            String author = authorIdx != -1 ? cursor.getString(authorIdx) : "";
            String cat = catIdx != -1 ? cursor.getString(catIdx) : "";
            String type = typeIdx != -1 ? cursor.getString(typeIdx) : "";
            String file = fileIdx != -1 ? cursor.getString(fileIdx) : "";
            String code = codeIdx != -1 ? cursor.getString(codeIdx) : "";
            String dept = deptIdx != -1 ? cursor.getString(deptIdx) : "";
            String uploader = byIdx != -1 ? cursor.getString(byIdx) : "";
            String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Approved";
            String date = dateIdx != -1 ? cursor.getString(dateIdx) : "";
            int downloads = dlIdx != -1 ? cursor.getInt(dlIdx) : 0;
            int favorites = favIdx != -1 ? cursor.getInt(favIdx) : 0;

            currentResource = new Resource(resourceId, title, author, cat, type, file, code, dept, uploader, status, date, downloads, favorites);
            cursor.close();

            txtTitle.setText(currentResource.getTitle());
            txtAuthor.setText("By " + currentResource.getAuthor());
            txtType.setText(currentResource.getType());
            txtCode.setText(currentResource.getCourseCode().isEmpty() ? currentResource.getCategory() : currentResource.getCourseCode());
            txtCategory.setText("Category: " + currentResource.getCategory());
            txtDept.setText("Department: " + currentResource.getDepartment());
            txtUploader.setText("Uploaded By: " + currentResource.getUploadedBy());
            txtDate.setText("Upload Date: " + currentResource.getUploadDate());
            txtStats.setText("Downloads: " + currentResource.getDownloadCount() + " • Favorites: " + currentResource.getFavoriteCount());

            boolean isFav = dbHelper.isFavorite(sessionManager.getEmail(), resourceId);
            updateFavoriteUI(isFav);
        } else {
            if (cursor != null) cursor.close();
            Toast.makeText(this, "Resource not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateFavoriteUI(boolean isFav) {
        if (isFav) {
            btnFavorite.setColorFilter(Color.RED);
        } else {
            btnFavorite.setColorFilter(Color.GRAY);
        }
    }

    private void showOnlineReaderDialog() {
        if (currentResource == null) return;

        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra("is_online_read", true);
        intent.putExtra("pdf_path", currentResource.getFilePath() != null ? currentResource.getFilePath() : "");
        intent.putExtra("pdf_title", currentResource.getTitle());
        intent.putExtra("pdf_content", "D Library Digital Reference Document\n\n" +
                "Resource: " + currentResource.getTitle() + "\n" +
                "Author: " + currentResource.getAuthor() + "\n" +
                "Category: " + currentResource.getCategory() + "\n" +
                "Type: " + currentResource.getType() + " (" + currentResource.getCourseCode() + ")\n" +
                "Department: " + currentResource.getDepartment() + "\n" +
                "Uploaded By: " + currentResource.getUploadedBy() + "\n\n" +
                "Complete reference materials, lecture notes, and guidelines provided by the DUET Central Library Network.");
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
