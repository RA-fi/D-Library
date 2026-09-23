package com.example.dlibrary;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Redirect to the unified Auth page (RegistrationActivity)
        startActivity(new Intent(this, RegistrationActivity.class));
        finish();
    }
}
