package com.example.dlibrary;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private TextInputEditText inputSearch;
    private Spinner spinnerType, spinnerDept;
    private RecyclerView recyclerView;
    private TextView txtEmpty;

    private List<Resource> searchResults = new ArrayList<>();
    private ResourceAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        dbHelper = new DatabaseHelper(requireContext());

        inputSearch = view.findViewById(R.id.input_search_query);
        spinnerType = view.findViewById(R.id.search_spinner_type);
        spinnerDept = view.findViewById(R.id.search_spinner_dept);
        recyclerView = view.findViewById(R.id.recycler_search_results);
        txtEmpty = view.findViewById(R.id.txt_empty_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        setupSpinners();
        setupListeners();
        performSearch();

        return view;
    }

    private void setupSpinners() {
        String[] types = {"All", "Book", "Lecture Sheet", "Lab Manual", "Previous Question", "Thesis", "Notice"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);

        String[] depts = {"All", "CSE", "EEE", "ME", "CE", "IPE", "TE", "Arch", "CFPE", "MME", "Math", "Phy", "Chem", "Hum", "Library"};
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, depts);
        spinnerDept.setAdapter(deptAdapter);
    }

    private void setupListeners() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerType.setOnItemSelectedListener(spinnerListener);
        spinnerDept.setOnItemSelectedListener(spinnerListener);
    }

    private void performSearch() {
        String query = inputSearch.getText() != null ? inputSearch.getText().toString().trim() : "";
        String selectedType = spinnerType.getSelectedItem() != null ? spinnerType.getSelectedItem().toString() : "All";
        String selectedDept = spinnerDept.getSelectedItem() != null ? spinnerDept.getSelectedItem().toString() : "All";

        FirebaseRepository.getInstance().fetchApprovedResourcesFromFirebase(new FirebaseRepository.ResourceListCallback() {
            @Override
            public void onSuccess(List<java.util.Map<String, Object>> resources) {
                if (resources != null && !resources.isEmpty()) {
                    new Thread(() -> {
                        for (java.util.Map<String, Object> map : resources) {
                            String title = (String) map.get("title");
                            String author = (String) map.get("author");
                            String category = (String) map.get("category");
                            String type = (String) map.get("type");
                            String filePath = (String) map.get("file_path");
                            String courseCode = (String) map.get("course_code");
                            String dept = (String) map.get("res_department");
                            String uploadedBy = (String) map.get("uploaded_by");
                            String status = (String) map.get("approval_status");

                            if (title != null && !title.isEmpty()) {
                                dbHelper.addResource(title, author, category, type, filePath, "", courseCode, dept, uploadedBy, status);
                            }
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Exception e) {}
        });

        searchResults.clear();
        Cursor cursor = dbHelper.searchResources(selectedType, "All", selectedDept, query, "", "Approved");

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

                searchResults.add(new Resource(id, title, author, cat, type, file, code, dept, uploader, status, date, downloads, favorites));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (searchResults.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new ResourceAdapter(requireContext(), searchResults, null);
            recyclerView.setAdapter(adapter);
        }
    }
}
