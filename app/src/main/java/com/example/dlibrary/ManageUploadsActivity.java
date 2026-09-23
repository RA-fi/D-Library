package com.example.dlibrary;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ManageUploadsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private List<Resource> myUploadsList = new ArrayList<>();
    private ResourceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_uploads);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        recyclerView = findViewById(R.id.recycler_my_uploads);
        txtEmpty = findViewById(R.id.txt_empty_uploads);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadMyUploads();
    }

    private void loadMyUploads() {
        myUploadsList.clear();
        Cursor cursor = dbHelper.getResourcesByUploader(sessionManager.getEmail());

        if (cursor != null && cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
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

            do {
                int id = idIdx != -1 ? cursor.getInt(idIdx) : 0;
                String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                String author = authorIdx != -1 ? cursor.getString(authorIdx) : "";
                String cat = catIdx != -1 ? cursor.getString(catIdx) : "";
                String type = typeIdx != -1 ? cursor.getString(typeIdx) : "";
                String file = fileIdx != -1 ? cursor.getString(fileIdx) : "";
                String code = codeIdx != -1 ? cursor.getString(codeIdx) : "";
                String dept = deptIdx != -1 ? cursor.getString(deptIdx) : "";
                String uploader = byIdx != -1 ? cursor.getString(byIdx) : "";
                String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Pending";
                String date = dateIdx != -1 ? cursor.getString(dateIdx) : "";
                int downloads = dlIdx != -1 ? cursor.getInt(dlIdx) : 0;
                int favorites = favIdx != -1 ? cursor.getInt(favIdx) : 0;

                myUploadsList.add(new Resource(id, title, author, cat, type, file, code, dept, uploader, status, date, downloads, favorites));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (myUploadsList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            adapter = new ResourceAdapter(this, myUploadsList, true, true, new ResourceAdapter.OnResourceActionListener() {
                @Override
                public void onItemClick(Resource resource) {
                    // Open ResourceDetailsActivity
                }

                @Override
                public void onDownloadClick(Resource resource) {
                    Toast.makeText(ManageUploadsActivity.this, "Approval Status: " + resource.getApprovalStatus(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onDeleteClick(Resource resource) {
                    new AlertDialog.Builder(ManageUploadsActivity.this)
                            .setTitle("Delete Upload")
                            .setMessage("Are you sure you want to delete " + resource.getTitle() + "?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                if (dbHelper.deleteResource(resource.getId())) {
                                    myUploadsList.remove(resource);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(ManageUploadsActivity.this, "Resource deleted", Toast.LENGTH_SHORT).show();
                                    if (myUploadsList.isEmpty()) {
                                        txtEmpty.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

            recyclerView.setAdapter(adapter);
        }
    }
}
