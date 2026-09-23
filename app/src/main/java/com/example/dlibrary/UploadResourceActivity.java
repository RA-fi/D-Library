package com.example.dlibrary;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class UploadResourceActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_PICK = 101;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private FirebaseRepository firebaseRepo;

    private Spinner spinnerType, spinnerCategory;
    private TextInputEditText inputTitle, inputAuthor, inputCourseCode, inputDept;
    private TextView txtFileSelected;
    private MaterialButton btnSubmit;

    private String selectedFileUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_resource);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        firebaseRepo = FirebaseRepository.getInstance();

        initViews();
        setupSpinners();

        String defaultType = getIntent().getStringExtra("default_type");
        if (defaultType != null) {
            setSpinnerToValue(spinnerType, defaultType);
        }

        if (sessionManager.getDepartment() != null && !sessionManager.getDepartment().isEmpty()) {
            inputDept.setText(sessionManager.getDepartment());
        }
    }

    private void initViews() {
        spinnerType = findViewById(R.id.spinner_type);
        spinnerCategory = findViewById(R.id.spinner_category);

        inputTitle = findViewById(R.id.input_res_title);
        inputAuthor = findViewById(R.id.input_res_author);
        inputCourseCode = findViewById(R.id.input_course_code);
        inputDept = findViewById(R.id.input_res_dept);

        txtFileSelected = findViewById(R.id.txt_file_selected);
        btnSubmit = findViewById(R.id.btn_upload_submit);

        if (!sessionManager.isAdmin()) {
            inputAuthor.setText(sessionManager.getName());
        }

        findViewById(R.id.card_select_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.slideshow", "application/vnd.openxmlformats-officedocument.presentationml.presentation"});
            startActivityForResult(Intent.createChooser(intent, "Select PDF / PPTX File"), REQUEST_FILE_PICK);
        });

        btnSubmit.setOnClickListener(v -> handleUpload());
    }

    private void setupSpinners() {
        String[] types = {"Book", "Lecture Sheet", "Lab Manual", "Previous Question", "Thesis", "Notice"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);

        List<String> categories = dbHelper.getAllCategories();
        if (categories.isEmpty()) {
            categories.add("Computer Science & Engineering (CSE)");
            categories.add("Electrical & Electronic Engineering (EEE)");
            categories.add("Mechanical Engineering (ME)");
            categories.add("Civil Engineering (CE)");
            categories.add("Industrial & Production Engineering (IPE)");
            categories.add("Textile Engineering (TE)");
            categories.add("Architecture (Arch)");
            categories.add("Chemical & Food Process Engineering (CFPE)");
            categories.add("Materials & Metallurgical Engineering (MME)");
            categories.add("Mathematics (Math)");
            categories.add("Physics (Phy)");
            categories.add("Chemistry (Chem)");
            categories.add("Humanities & Social Sciences (Hum)");
        }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);
    }

    private void setSpinnerToValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_PICK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            txtFileSelected.setText("Caching file...");
            new Thread(() -> {
                try {
                    // Determine file extension
                    String extension = ".pdf";
                    String type = getContentResolver().getType(uri);
                    if (type != null && type.contains("presentation")) {
                        extension = ".pptx";
                    } else if (uri.toString().toLowerCase().contains(".pptx")) {
                        extension = ".pptx";
                    }

                    java.io.File cacheFile = new java.io.File(getCacheDir(), "upload_" + System.currentTimeMillis() + extension);
                    try (java.io.InputStream is = getContentResolver().openInputStream(uri);
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile)) {
                        if (is != null) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, read);
                            }
                        }
                    }
                    final String savedUri = Uri.fromFile(cacheFile).toString();
                    runOnUiThread(() -> {
                        selectedFileUri = savedUri;
                        txtFileSelected.setText("File Selected: Ready to Upload");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(UploadResourceActivity.this, "Failed to cache file for upload", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void handleUpload() {
        String title = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
        String author = inputAuthor.getText() != null ? inputAuthor.getText().toString().trim() : "";
        String courseCode = inputCourseCode.getText() != null ? inputCourseCode.getText().toString().trim() : "";
        String dept = inputDept.getText() != null ? inputDept.getText().toString().trim() : "";

        String type = spinnerType.getSelectedItem().toString();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(this, "Title and Author/Instructor are required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading File to Cloud...");

        String status = sessionManager.isAdmin() ? "Approved" : "Pending";

        if (!selectedFileUri.isEmpty()) {
            try {
                Uri uri = Uri.parse(selectedFileUri);
                firebaseRepo.uploadPdfFile(uri, "resources_pdf", new FirebaseRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        saveResourceToDatabase(title, author, category, type, downloadUrl, courseCode, dept, status);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Upload Resource");
                            Toast.makeText(UploadResourceActivity.this, "File upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } catch (Exception e) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Upload Resource");
                Toast.makeText(this, "File upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            saveResourceToDatabase(title, author, category, type, "", courseCode, dept, status);
        }
    }

    private void saveResourceToDatabase(String title, String author, String category, String type, String filePath, String courseCode, String dept, String status) {
        firebaseRepo.publishResourceToFirebase(title, author, category, type, filePath, courseCode, dept, sessionManager.getEmail(), status, new FirebaseRepository.UploadCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Upload Resource");
                    dbHelper.addResource(title, author, category, type, filePath, "", courseCode, dept, sessionManager.getEmail(), status);
                    
                    if (sessionManager.isAdmin()) {
                        Toast.makeText(UploadResourceActivity.this, "Resource uploaded and published to Firebase!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UploadResourceActivity.this, "Resource submitted successfully! Awaiting Librarian approval.", Toast.LENGTH_LONG).show();
                    }
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Upload Resource");
                    Toast.makeText(UploadResourceActivity.this, "Database sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
