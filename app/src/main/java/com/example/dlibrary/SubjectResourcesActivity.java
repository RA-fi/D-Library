package com.example.dlibrary;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SubjectResourcesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextInputEditText inputCode;
    private ChipGroup chipGroupType;
    private RecyclerView recyclerView;
    private TextView txtEmpty;

    private List<Resource> resourceList = new ArrayList<>();
    private ResourceAdapter adapter;
    private String selectedTypeFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_resources);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        inputCode = findViewById(R.id.input_subject_code);
        chipGroupType = findViewById(R.id.chip_group_subject_type);
        recyclerView = findViewById(R.id.recycler_subject_resources);
        txtEmpty = findViewById(R.id.txt_empty_subject);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupListeners();
        filterSubjectResources();
    }

    private void setupListeners() {
        inputCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSubjectResources();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_lectures) {
                selectedTypeFilter = "Lecture Sheet";
            } else if (checkedId == R.id.chip_manuals) {
                selectedTypeFilter = "Lab Manual";
            } else if (checkedId == R.id.chip_questions) {
                selectedTypeFilter = "Previous Question";
            } else {
                selectedTypeFilter = "All";
            }
            filterSubjectResources();
        });
    }

    private void filterSubjectResources() {
        String codeQuery = inputCode.getText() != null ? inputCode.getText().toString().trim() : "";

        resourceList.clear();
        Cursor cursor = dbHelper.searchResources(selectedTypeFilter, "All", "All", "", codeQuery, "Approved");

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
                String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Approved";
                String date = dateIdx != -1 ? cursor.getString(dateIdx) : "";
                int downloads = dlIdx != -1 ? cursor.getInt(dlIdx) : 0;
                int favorites = favIdx != -1 ? cursor.getInt(favIdx) : 0;

                resourceList.add(new Resource(id, title, author, cat, type, file, code, dept, uploader, status, date, downloads, favorites));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (resourceList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new ResourceAdapter(this, resourceList, null);
            recyclerView.setAdapter(adapter);
        }
    }
}
