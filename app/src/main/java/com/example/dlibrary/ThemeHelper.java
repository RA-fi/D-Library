package com.example.dlibrary;

import android.graphics.Color;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AppCompatActivity;

public class ThemeHelper {

    public static int getPrimaryColor(SessionManager session) {
        if (session == null) return Color.parseColor("#0288D1");
        if (session.isAdmin()) {
            return Color.parseColor("#0288D1"); // Sky Blue for Admin
        } else if (session.isTeacher()) {
            return Color.parseColor("#0097A7"); // Teal Sky Blue for Teacher
        } else {
            return Color.parseColor("#0288D1"); // Sky Blue for Student
        }
    }

    public static int getHeaderGradientDrawable(SessionManager session) {
        if (session == null) return R.drawable.bg_gradient_student;
        if (session.isAdmin()) {
            return R.drawable.bg_gradient_admin;
        } else if (session.isTeacher()) {
            return R.drawable.bg_gradient_teacher;
        } else {
            return R.drawable.bg_gradient_student;
        }
    }

    public static void applyRoleTheme(AppCompatActivity activity, SessionManager session) {
        if (activity == null || session == null) return;
        int primaryColor = getPrimaryColor(session);
        Window window = activity.getWindow();
        if (window != null) {
            window.setStatusBarColor(primaryColor);
        }
    }

    public static void applyHeaderTheme(View headerView, SessionManager session) {
        if (headerView == null || session == null) return;
        headerView.setBackgroundResource(getHeaderGradientDrawable(session));
    }
}
