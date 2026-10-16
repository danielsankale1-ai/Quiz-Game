package com.example.quizgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvBrain = findViewById(R.id.tvBrainEmoji);
        TextView tvTitle = findViewById(R.id.tvQuizMaster);
        TextView tvSubtitle = findViewById(R.id.tvTestKnowledge);
        TextView tvDeveloped = findViewById(R.id.tvDevelopedBy);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1500);
        
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);

        // Apply animations
        tvBrain.startAnimation(fadeIn);
        tvTitle.startAnimation(fadeIn);
        tvSubtitle.startAnimation(fadeIn);
        tvDeveloped.startAnimation(fadeIn);

        // Transition logic
        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            
            if (isLoggedIn) {
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 3000);
    }
}