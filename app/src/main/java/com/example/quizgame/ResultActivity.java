package com.example.quizgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    private MediaPlayer resultPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        MaterialCardView cardResult = findViewById(R.id.cardResult);
        TextView tvPlayerName = findViewById(R.id.tvPlayerNameResult);
        TextView tvScoreResult = findViewById(R.id.tvScoreResult);
        TextView tvComment = findViewById(R.id.tvComment);
        TextView tvRank = findViewById(R.id.tvRank);
        TextView tvHighScore = findViewById(R.id.tvHighScoreResult);
        TextView tvStar1 = findViewById(R.id.tvStar1);
        TextView tvStar2 = findViewById(R.id.tvStar2);
        TextView tvStar3 = findViewById(R.id.tvStar3);
        MaterialButton btnPlayAgain = findViewById(R.id.btnPlayAgain);
        MaterialButton btnMainMenu = findViewById(R.id.btnMainMenu);
        MaterialButton btnViewLeaderboard = findViewById(R.id.btnViewLeaderboard);

        String playerName = getIntent().getStringExtra("PLAYER_NAME");
        int score = getIntent().getIntExtra("SCORE", 0);
        int totalQuestions = getIntent().getIntExtra("TOTAL", 20);
        String category = getIntent().getStringExtra("CATEGORY");
        String rawGameMode = getIntent().getStringExtra("GAME_MODE");
        final String gameMode = (rawGameMode != null) ? rawGameMode : "CLASSIC";
        int maxStreak = getIntent().getIntExtra("MAX_STREAK", 0);

        tvPlayerName.setText("Player: " + playerName);
        tvScoreResult.setText("Score: " + score + "/" + totalQuestions + (maxStreak >= 2 ? " (Max Streak: " + maxStreak + "🔥)" : ""));

        double percentage = ((double) score / totalQuestions) * 100;
        
        SharedPreferences settings = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        boolean soundEnabled = settings.getBoolean("sound_enabled", true);

        if (percentage >= 90) {
            tvComment.setText("Genius");
            tvRank.setText("Rank: Quiz Master 🏆");
            tvStar1.setVisibility(View.VISIBLE);
            tvStar2.setVisibility(View.VISIBLE);
            tvStar3.setVisibility(View.VISIBLE);
            if (soundEnabled) playSound(R.raw.correct);
        } else if (percentage >= 70) {
            tvComment.setText("Smart");
            tvRank.setText("Rank: Expert 🎖️");
            tvStar1.setVisibility(View.VISIBLE);
            tvStar2.setVisibility(View.VISIBLE);
            tvStar3.setVisibility(View.INVISIBLE);
            if (soundEnabled) playSound(R.raw.correct);
        } else if (percentage >= 50) {
            tvComment.setText("Average");
            tvRank.setText("Rank: Intermediate 🥉");
            tvStar1.setVisibility(View.VISIBLE);
            tvStar2.setVisibility(View.INVISIBLE);
            tvStar3.setVisibility(View.INVISIBLE);
            if (soundEnabled) playSound(R.raw.correct);
        } else {
            tvComment.setText("Try Again");
            tvRank.setText("Rank: Beginner 👶");
            tvStar1.setVisibility(View.INVISIBLE);
            tvStar2.setVisibility(View.INVISIBLE);
            tvStar3.setVisibility(View.INVISIBLE);
            if (soundEnabled) playSound(R.raw.wronganswer);
        }

        // Logic to get category-specific high score
        SharedPreferences scoresPrefs = getSharedPreferences("QuizScores", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = scoresPrefs.getString("scores", null);
        Type type = new TypeToken<ArrayList<Score>>() {}.getType();
        List<Score> scoreList = gson.fromJson(json, type);
        int highScore = 0;
        if (scoreList != null) {
            for (Score s : scoreList) {
                if (s.getCategory() != null && s.getCategory().equalsIgnoreCase(category)) {
                    if (s.getScoreValue() > highScore) {
                        highScore = s.getScoreValue();
                    }
                }
            }
        }
        tvHighScore.setText("Best in " + category + ": " + highScore);

        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000);
        cardResult.startAnimation(fadeIn);

        btnPlayAgain.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.putExtra("PLAYER_NAME", playerName);
            intent.putExtra("CATEGORY", category);
            intent.putExtra("GAME_MODE", gameMode);
            startActivity(intent);
            finish();
        });

        btnMainMenu.setOnClickListener(v -> {
            startActivity(new Intent(ResultActivity.this, HomeActivity.class));
            finish();
        });

        if (btnViewLeaderboard != null) {
            btnViewLeaderboard.setOnClickListener(v -> {
                startActivity(new Intent(ResultActivity.this, ScoreActivity.class));
            });
        }
    }

    private void playSound(int resId) {
        if (resultPlayer != null) resultPlayer.release();
        resultPlayer = MediaPlayer.create(this, resId);
        if (resultPlayer != null) resultPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resultPlayer != null) resultPlayer.release();
    }
}