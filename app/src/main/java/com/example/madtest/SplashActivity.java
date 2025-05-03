package com.example.tasktodo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SplashActivity onCreate started");

        // Set window to full screen as in reference code
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);
        Log.d(TAG, "Layout set: activity_splash.xml");

        LottieAnimationView animationView = findViewById(R.id.lottieAnimationView);
        if (animationView == null) {
            Log.e(TAG, "LottieAnimationView is null");
            proceedToMainActivity();
            return;
        }

        Log.d(TAG, "LottieAnimationView found");

        try {
            // Animation is set via XML (app:lottie_rawRes)
            // Make sure it's playing
            animationView.playAnimation();
            Log.d(TAG, "Animation started playing");
        } catch (Exception e) {
            Log.e(TAG, "Failed to play animation", e);
            proceedToMainActivity();
            return;
        }

        // Use Handler with 3-second delay to ensure animation is visible
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Handler delay finished, proceeding to MainActivity");
                proceedToMainActivity();
            }
        }, 3000); // 3000ms = 3 seconds
    }

    private void proceedToMainActivity() {
        Log.d(TAG, "Proceeding to MainActivity");
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}