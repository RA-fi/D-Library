package com.example.dlibrary;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logoCard = findViewById(R.id.splash_logo_card);
        TextView txtTitle = findViewById(R.id.splash_title);
        TextView txtSubtitle = findViewById(R.id.splash_subtitle);

        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_anim);
        Animation titleAnim = AnimationUtils.loadAnimation(this, R.anim.splash_title_anim);
        Animation subtitleAnim = AnimationUtils.loadAnimation(this, R.anim.splash_subtitle_anim);

        if (logoCard != null) logoCard.startAnimation(logoAnim);
        if (txtTitle != null) txtTitle.startAnimation(titleAnim);
        if (txtSubtitle != null) txtSubtitle.startAnimation(subtitleAnim);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(SplashActivity.this);
            Intent intent;
            if (sessionManager.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, RegistrationActivity.class);
            }

            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
