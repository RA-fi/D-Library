package com.example.dlibrary;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryManagementActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextInputEditText inputCategory;
    private MaterialButton btnAdd;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        inputCategory = findViewById(R.id.input_category_name);
        btnAdd = findViewById(R.id.btn_add_category);
        listView = findViewById(R.id.list_categories);

        loadCategories();

        btnAdd.setOnClickListener(v -> {
            String name = inputCategory.getText() != null ? inputCategory.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter category name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.addCategory(name)) {
                FirebaseRepository.getInstance().addCategoryToFirebase(name, null);
                inputCategory.setText("");
                loadCategories();
                Toast.makeText(this, "Category added & synced to Firebase!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed or category already exists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategories() {
        categoryList.clear();
        categoryList.addAll(dbHelper.getAllCategories());

        FirebaseRepository.getInstance().fetchCategoriesFromFirebase(new FirebaseRepository.UserListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> userList) {
                if (userList != null && !userList.isEmpty()) {
                    for (Map<String, Object> map : userList) {
                        String name = (String) map.get("name");
                        if (name != null && !name.isEmpty()) {
                            dbHelper.addCategory(name);
                            if (!categoryList.contains(name)) {
                                categoryList.add(name);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Exception e) {}
        });

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categoryList);
        listView.setAdapter(adapter);
    }
}
