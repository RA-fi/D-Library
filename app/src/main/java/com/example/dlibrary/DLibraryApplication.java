package com.example.dlibrary;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;

public class DLibraryApplication extends Application {

    private static final String TAG = "DLibraryApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully in Application class");
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase auto-init notice: " + e.getMessage());
        }
    }
}
