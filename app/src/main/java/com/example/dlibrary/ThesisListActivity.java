package com.example.dlibrary;

import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ThesisListActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextInputEditText inputSearch;
    private ChipGroup chipGroupDept;
    private MaterialButton btnUploadThesis;
    private RecyclerView recyclerView;
    private TextView txtEmpty;

    private List<Resource> thesisList = new ArrayList<>();
    private ResourceAdapter adapter;
    private String selectedDeptFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thesis_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("🎓 DUET Research & Thesis Portal");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);

        inputSearch = findViewById(R.id.input_thesis_search);
        chipGroupDept = findViewById(R.id.chip_group_thesis_dept);
        btnUploadThesis = findViewById(R.id.btn_portal_upload_thesis);
        recyclerView = findViewById(R.id.recycler_thesis_list);
        txtEmpty = findViewById(R.id.txt_empty_thesis);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupListeners();
        filterThesisList();
    }

    private void setupListeners() {
        btnUploadThesis.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadResourceActivity.class);
            intent.putExtra("default_type", "Thesis");
            startActivity(intent);
        });

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterThesisList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupDept.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_thesis_cse) {
                selectedDeptFilter = "CSE";
            } else if (checkedId == R.id.chip_thesis_eee) {
                selectedDeptFilter = "EEE";
            } else if (checkedId == R.id.chip_thesis_me) {
                selectedDeptFilter = "ME";
            } else if (checkedId == R.id.chip_thesis_ce) {
                selectedDeptFilter = "CE";
            } else if (checkedId == R.id.chip_thesis_ipe) {
                selectedDeptFilter = "IPE";
            } else if (checkedId == R.id.chip_thesis_te) {
                selectedDeptFilter = "TE";
            } else if (checkedId == R.id.chip_thesis_arch) {
                selectedDeptFilter = "Arch";
            } else {
                selectedDeptFilter = "All";
            }
            filterThesisList();
        });
    }

    private void filterThesisList() {
        String searchQuery = inputSearch.getText() != null ? inputSearch.getText().toString().trim() : "";

        thesisList.clear();
        Cursor cursor = dbHelper.searchResources("Thesis", "All", selectedDeptFilter, searchQuery, "", "Approved");

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

                thesisList.add(new Resource(id, title, author, cat, type, file, code, dept, uploader, status, date, downloads, favorites));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (thesisList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new ResourceAdapter(this, thesisList, null);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterThesisList();
    }
}
