package com.example.dlibrary;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, RegistrationActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        ThemeHelper.applyRoleTheme(this, sessionManager);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Role-Based Theme Color for Bottom Navigation
        int primaryColor = ThemeHelper.getPrimaryColor(sessionManager);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                primaryColor,
                Color.parseColor("#757575")
        };
        ColorStateList colorStateList = new ColorStateList(states, colors);
        navView.setItemIconTintList(colorStateList);
        navView.setItemTextColor(colorStateList);

        // Customize Bottom Navigation Item & Icon based on User Role
        MenuItem navItem = navView.getMenu().findItem(R.id.navigation_librarian);
        if (sessionManager.isAdmin()) {
            navItem.setTitle("Approvals");
            navItem.setIcon(R.drawable.ic_approval);
        } else if (sessionManager.isTeacher()) {
            navItem.setTitle("My Uploads");
            navItem.setIcon(R.drawable.ic_lecture);
        } else {
            navItem.setTitle("Library");
            navItem.setIcon(R.drawable.ic_book);
        }

        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.navigation_librarian) {
                if (sessionManager.isAdmin()) {
                    startActivity(new Intent(MainActivity.this, ResourceApprovalActivity.class));
                    return false;
                } else if (sessionManager.isTeacher()) {
                    startActivity(new Intent(MainActivity.this, ManageUploadsActivity.class));
                    return false;
                } else {
                    selectedFragment = new SearchFragment();
                }
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            } else {
                return false;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        }
    }
}
