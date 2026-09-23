package com.example.dlibrary;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "DLibrarySession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_STUDENT_ID = "studentId";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_STATUS = "status";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String name, String email, String role, String studentId, String department, String status) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_STUDENT_ID, studentId);
        editor.putString(KEY_DEPARTMENT, department != null ? department : "Library");
        editor.putString(KEY_STATUS, status != null ? status : "Active");
        editor.commit();
    }

    public void updateSession(String name, String department) {
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_DEPARTMENT, department);
        editor.apply();
    }

    public void setProfileImageUrl(String url) {
        editor.putString("profileImageUrl", url);
        editor.apply();
    }

    public String getProfileImageUrl() {
        return pref.getString("profileImageUrl", "");
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }

    public String getName() { return pref.getString(KEY_NAME, ""); }
    public String getEmail() { return pref.getString(KEY_EMAIL, ""); }
    public String getRole() { return pref.getString(KEY_ROLE, "Student"); }
    public String getStudentId() { return pref.getString(KEY_STUDENT_ID, ""); }
    public String getDepartment() { return pref.getString(KEY_DEPARTMENT, "Library"); }
    public String getStatus() { return pref.getString(KEY_STATUS, "Active"); }

    public boolean isStudent() {
        return "Student".equalsIgnoreCase(getRole());
    }

    public boolean isTeacher() {
        return "Teacher".equalsIgnoreCase(getRole());
    }

    public boolean isAdmin() {
        String role = getRole();
        if (role == null) return false;
        return role.contains("Admin") || role.contains("Staff") || role.contains("Librarian");
    }
}
