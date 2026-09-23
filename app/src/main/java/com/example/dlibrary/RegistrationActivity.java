package com.example.dlibrary;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegistrationActivity extends AppCompatActivity {

    private boolean isLoginMode = true;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private FirebaseRepository firebaseRepo;

    private TextView authTitle, toggleLink, lblSelectRole;
    private MaterialButton authButton;
    private RadioGroup roleGroup;
    private TextInputLayout layoutName, layoutStudentId, layoutDepartment;
    private TextInputEditText inputName, inputStudentId, inputDepartment, inputEmail, inputPassword;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        firebaseRepo = FirebaseRepository.getInstance();

        executor.execute(() -> dbHelper.ensureSuperAdmin());

        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        setupListeners();

        // Default to Login Mode first when opening app
        isLoginMode = true;
        updateUI();
    }

    private void initViews() {
        authTitle = findViewById(R.id.auth_title);
        toggleLink = findViewById(R.id.toggle_link);
        authButton = findViewById(R.id.auth_button);
        roleGroup = findViewById(R.id.role_group);
        lblSelectRole = findViewById(R.id.lbl_select_role);

        layoutName = findViewById(R.id.layout_name);
        layoutStudentId = findViewById(R.id.layout_student_id);
        layoutDepartment = findViewById(R.id.layout_department);

        inputName = findViewById(R.id.input_name);
        inputStudentId = findViewById(R.id.input_student_id);
        inputDepartment = findViewById(R.id.input_department);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
    }

    private void setupListeners() {
        toggleLink.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isLoginMode) {
                updateFieldVisibility(checkedId);
            }
        });

        authButton.setOnClickListener(v -> {
            if (isLoginMode) {
                handleLogin();
            } else {
                handleRegistration();
            }
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            authTitle.setText("Welcome Back");
            authButton.setText("Sign In");
            toggleLink.setText("New to D Library? Create Account");

            lblSelectRole.setVisibility(View.GONE);
            roleGroup.setVisibility(View.GONE);
            layoutName.setVisibility(View.GONE);
            layoutStudentId.setVisibility(View.GONE);
            layoutDepartment.setVisibility(View.GONE);
        } else {
            authTitle.setText("Join D Library");
            authButton.setText("Create Account");
            toggleLink.setText("Already have an account? Sign In");

            lblSelectRole.setVisibility(View.VISIBLE);
            roleGroup.setVisibility(View.VISIBLE);
            layoutName.setVisibility(View.VISIBLE);
            updateFieldVisibility(roleGroup.getCheckedRadioButtonId());
        }
    }

    private void updateFieldVisibility(int checkedId) {
        if (checkedId == R.id.role_student) {
            layoutStudentId.setVisibility(View.VISIBLE);
            layoutStudentId.setHint("Student ID / Roll No");
            layoutDepartment.setVisibility(View.VISIBLE);
        } else if (checkedId == R.id.role_teacher) {
            layoutStudentId.setVisibility(View.VISIBLE);
            layoutStudentId.setHint("Designation (e.g. Professor)");
            layoutDepartment.setVisibility(View.VISIBLE);
        } else { // Librarian / Admin
            layoutStudentId.setVisibility(View.GONE);
            layoutDepartment.setVisibility(View.GONE);
        }
    }

    private void handleRegistration() {
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String password = inputPassword.getText() != null ? inputPassword.getText().toString().trim() : "";
        String idOrDesignationInput = inputStudentId.getText() != null ? inputStudentId.getText().toString().trim() : "";
        String department = inputDepartment.getText() != null ? inputDepartment.getText().toString().trim() : "";

        int selectedRoleId = roleGroup.getCheckedRadioButtonId();
        RadioButton selectedRoleBtn = findViewById(selectedRoleId);
        String role = selectedRoleBtn != null ? selectedRoleBtn.getText().toString() : "Student";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill required fields (Name, Email, Password)", Toast.LENGTH_SHORT).show();
            return;
        }

        String idOrDesignation = idOrDesignationInput;
        if (selectedRoleId == R.id.role_teacher || "Teacher".equalsIgnoreCase(role)) {
            if (idOrDesignation.isEmpty()) idOrDesignation = "Teacher";
            role = "Teacher";
        } else if (selectedRoleId == R.id.role_admin || "Librarian".equalsIgnoreCase(role) || role.contains("Admin")) {
            idOrDesignation = "Librarian Staff";
            if (department.isEmpty()) department = "Library";
            role = "Admin";
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please connect to the internet to register.", Toast.LENGTH_LONG).show();
            return;
        }

        final String finalRole = role;
        final String finalIdOrDesignation = idOrDesignation;
        final String finalDepartment = department;

        authButton.setEnabled(false);
        authButton.setText("Creating Account in Cloud...");

        // Save in Firebase Firestore
        firebaseRepo.saveUserToFirebase(name, finalIdOrDesignation, finalDepartment, email, password, finalRole, "Active", new FirebaseRepository.UploadCallback() {
            @Override
            public void onSuccess(String result) {
                dbHelper.registerUser(name, finalIdOrDesignation, finalDepartment, email, password, finalRole);
                sessionManager.createLoginSession(name, email, finalRole, finalIdOrDesignation, finalDepartment, "Active");
                Toast.makeText(RegistrationActivity.this, "Registration Successful (Firebase Cloud Sync)!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                authButton.setEnabled(true);
                authButton.setText("Create Account");
                Toast.makeText(RegistrationActivity.this, "Cloud Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLogin() {
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String password = inputPassword.getText() != null ? inputPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Direct Super Admin Instant Bypass Check
        if ("admin@library.duet.ac.bd".equalsIgnoreCase(email) && "qwer1234@".equals(password)) {
            dbHelper.ensureSuperAdmin();
            sessionManager.createLoginSession("DUET Chief Librarian", email, "Admin", "Chief Librarian", "Library", "Active");
            Toast.makeText(RegistrationActivity.this, "Welcome Super Admin!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        authButton.setEnabled(false);
        authButton.setText("Verifying Credentials...");

        // Fetch from Firebase Firestore
        firebaseRepo.fetchUserFromFirebase(email, new FirebaseRepository.UserCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                authButton.setEnabled(true);
                authButton.setText("Sign In");

                String dbPass = (String) userData.get("password");
                String status = (String) userData.get("status");

                if (Objects.equals(dbPass, password)) {
                    if ("Blocked".equalsIgnoreCase(status)) {
                        Toast.makeText(RegistrationActivity.this, "Account is blocked by Librarian. Please contact library staff.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String name = (String) userData.get("name");
                    String role = (String) userData.get("role");
                    String studentId = (String) userData.get("student_id");
                    String department = (String) userData.get("department");

                    dbHelper.registerUser(name, studentId, department, email, password, role);
                    sessionManager.createLoginSession(name, email, role, studentId, department, status);

                    Toast.makeText(RegistrationActivity.this, "Welcome " + name + " (Firebase Cloud Authenticated)", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                } else {
                    Toast.makeText(RegistrationActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Fallback to local DB check
                authButton.setEnabled(true);
                authButton.setText("Sign In");

                Cursor cursor = dbHelper.checkUser(email, password);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME);
                    int roleIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE);
                    int idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_STUDENT_ID);
                    int deptIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DEPARTMENT);
                    int statusIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS);

                    String name = nameIndex != -1 ? cursor.getString(nameIndex) : "User";
                    String role = roleIndex != -1 ? cursor.getString(roleIndex) : "Student";
                    String studentId = idIndex != -1 ? cursor.getString(idIndex) : "";
                    String department = deptIndex != -1 ? cursor.getString(deptIndex) : "Library";
                    String status = statusIndex != -1 ? cursor.getString(statusIndex) : "Active";

                    cursor.close();

                    if ("Blocked".equalsIgnoreCase(status)) {
                        Toast.makeText(RegistrationActivity.this, "Account is blocked by Librarian.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    sessionManager.createLoginSession(name, email, role, studentId, department, status);
                    Toast.makeText(RegistrationActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                } else {
                    if (cursor != null) cursor.close();
                    Toast.makeText(RegistrationActivity.this, "Invalid Email or Password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
