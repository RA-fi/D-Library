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

public class ResourceListActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private TextInputEditText inputSearch;
    private ChipGroup chipGroupSection;
    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private ResourceAdapter adapter;
    private List<Resource> resourceList = new ArrayList<>();

    private String listType = "";
    private String filterValue = "";
    private String pageTitle = "Resource Library";
    private boolean showOnlyDownloaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_list);

        listType = getIntent().getStringExtra("list_type");
        if (listType == null) listType = "";
        filterValue = getIntent().getStringExtra("filter_value");
        if (filterValue == null) filterValue = "";
        pageTitle = getIntent().getStringExtra("title");
        if (pageTitle == null || pageTitle.isEmpty()) pageTitle = "Resource Library";

        if ("DOWNLOAD_HISTORY".equalsIgnoreCase(listType)) {
            showOnlyDownloaded = true;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(pageTitle);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        inputSearch = findViewById(R.id.input_resource_search);
        chipGroupSection = findViewById(R.id.chip_group_section);
        recyclerView = findViewById(R.id.recycler_resource_list);
        txtEmpty = findViewById(R.id.txt_empty_list);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (showOnlyDownloaded) {
            chipGroupSection.check(R.id.chip_section_downloaded);
        }

        setupListeners();
        loadResources();
    }

    private void setupListeners() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadResources();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupSection.setOnCheckedChangeListener((group, checkedId) -> {
            showOnlyDownloaded = (checkedId == R.id.chip_section_downloaded);
            loadResources();
        });
    }

    private void loadResources() {
        String searchQuery = inputSearch.getText() != null ? inputSearch.getText().toString().trim() : "";

        resourceList.clear();
        Cursor cursor = null;

        if (showOnlyDownloaded) {
            cursor = dbHelper.getUserDownloadHistory(sessionManager.getEmail());
        } else if ("FAVORITES".equalsIgnoreCase(listType)) {
            cursor = dbHelper.getUserFavorites(sessionManager.getEmail());
        } else if ("TYPE_FILTER".equalsIgnoreCase(listType)) {
            cursor = dbHelper.searchResources(filterValue, "All", "All", searchQuery, "", "Approved");
        } else {
            cursor = dbHelper.searchResources("All", "All", "All", searchQuery, "", "Approved");
        }

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

                // Apply type filter and search query filter if viewing downloaded list
                if (showOnlyDownloaded && !filterValue.isEmpty() && !"All".equalsIgnoreCase(filterValue)) {
                    if (!filterValue.equalsIgnoreCase(type)) {
                        continue;
                    }
                }

                if (!searchQuery.isEmpty() && showOnlyDownloaded) {
                    String matchTarget = (title + " " + author + " " + code + " " + cat).toLowerCase();
                    if (!matchTarget.contains(searchQuery.toLowerCase())) {
                        continue;
                    }
                }

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

    @Override
    protected void onResume() {
        super.onResume();
        loadResources();
    }
}
