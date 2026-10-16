package com.example.quizgame;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private MediaPlayer menuMusic;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private TextView tvPlayerLevelHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvPlayerLevelHome = findViewById(R.id.tvPlayerLevelHome);
        MaterialButton btnDailyChallenge = findViewById(R.id.btnDailyChallenge);
        MaterialButton btnStartGame = findViewById(R.id.btnStartGame);
        MaterialButton btnCategories = findViewById(R.id.btnCategories);
        MaterialButton btnLeaderboard = findViewById(R.id.btnLeaderboard);
        MaterialButton btnAchievements = findViewById(R.id.btnAchievements);
        MaterialButton btnHowToPlay = findViewById(R.id.btnHowToPlay);
        MaterialButton btnSettings = findViewById(R.id.btnSettings);
        MaterialButton btnExit = findViewById(R.id.btnExit);

        SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        boolean musicEnabled = prefs.getBoolean("music_enabled", true);

        if (musicEnabled) {
            menuMusic = MediaPlayer.create(this, R.raw.backgroundmusic);
            if (menuMusic != null) {
                menuMusic.setLooping(true);
                menuMusic.start();
            }
        }

        btnDailyChallenge.setOnClickListener(v -> handleDailyChallenge());
        btnStartGame.setOnClickListener(v -> showPlayerNameDialog(null));
        btnCategories.setOnClickListener(v -> showCategoriesDialog());
        btnLeaderboard.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ScoreActivity.class)));
        btnAchievements.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AchievementsActivity.class)));
        btnHowToPlay.setOnClickListener(v -> showHowToPlayDialog());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnExit.setOnClickListener(v -> finishAffinity());

        checkNotificationPermission();
        scheduleDailyNotification();
        updatePlayerLevelHeader();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void scheduleDailyNotification() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10); // 10:00 AM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (Calendar.getInstance().after(calendar)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleDailyChallenge() {
        SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        String lastDate = prefs.getString("last_daily_challenge", "");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (lastDate.equals(currentDate)) {
            Toast.makeText(this, "You've already completed today's challenge! Come back tomorrow.", Toast.LENGTH_LONG).show();
        } else {
            prefs.edit().putString("last_daily_challenge", currentDate).apply();
            String[] categories = {"ICT", "Science", "Geography", "Sports"};
            String randomCategory = categories[(int) (Math.random() * categories.length)];
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_user_name, null);
            EditText etName = view.findViewById(R.id.etName);
            
            builder.setView(view)
                    .setTitle("Daily Challenge: " + randomCategory)
                    .setCancelable(true)
                    .setPositiveButton("Start Challenge", (dialog, which) -> {
                        String name = etName.getText().toString().trim();
                        if (name.isEmpty()) name = "Challenger";
                        
                        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                        intent.putExtra("PLAYER_NAME", name);
                        intent.putExtra("CATEGORY", randomCategory);
                        intent.putExtra("IS_DAILY", true);
                        startActivity(intent);
                    })
                    .show();
        }
    }

    private void updatePlayerLevelHeader() {
        if (tvPlayerLevelHome == null) return;
        SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        int xp = prefs.getInt("player_xp", 0);
        int level = (xp / 300) + 1;
        tvPlayerLevelHome.setText("Level " + level + " Scholar • XP: " + xp);
    }

    private void showPlayerNameDialog(String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_user_name, null);
        EditText etName = view.findViewById(R.id.etName);
        
        builder.setView(view)
                .setCancelable(true)
                .setPositiveButton("Next", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) name = "Player";
                    showGameModeDialog(name, category != null ? category : "ICT");
                })
                .show();
    }

    private void showGameModeDialog(String playerName, String category) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_game_mode, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        View cardClassic = view.findViewById(R.id.cardModeClassic);
        View cardSurvival = view.findViewById(R.id.cardModeSurvival);
        View cardTimeAttack = view.findViewById(R.id.cardModeTimeAttack);

        cardClassic.setOnClickListener(v -> {
            dialog.dismiss();
            startGameWithMode(playerName, category, "CLASSIC");
        });

        cardSurvival.setOnClickListener(v -> {
            dialog.dismiss();
            startGameWithMode(playerName, category, "SURVIVAL");
        });

        cardTimeAttack.setOnClickListener(v -> {
            dialog.dismiss();
            startGameWithMode(playerName, category, "TIME_ATTACK");
        });

        dialog.show();
    }

    private void startGameWithMode(String playerName, String category, String gameMode) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra("PLAYER_NAME", playerName);
        intent.putExtra("CATEGORY", category);
        intent.putExtra("GAME_MODE", gameMode);
        startActivity(intent);
    }

    private void showCategoriesDialog() {
        String[] categories = {"ICT", "Science", "Geography", "Sports"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Category")
                .setItems(categories, (dialog, which) -> {
                    showPlayerNameDialog(categories[which]);
                })
                .show();
    }

    private void showHowToPlayDialog() {
        new AlertDialog.Builder(this)
                .setTitle("How to Play")
                .setMessage("1. You will face 20 questions.\n" +
                        "2. You have 20 seconds per question.\n" +
                        "3. A ticking sound warns you as time runs out.\n" +
                        "4. Correct answers earn points.\n" +
                        "5. If time runs out, it counts as a fail and moves to the next question automatically.\n" +
                        "6. Reach the end to see your rank and stars!")
                .setPositiveButton("Got it!", null)
                .show();
    }

    private void showSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        Switch swSound = view.findViewById(R.id.swSound);
        Switch swMusic = view.findViewById(R.id.swMusic);
        MaterialButton btnReset = view.findViewById(R.id.btnResetScores);

        SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        swSound.setChecked(prefs.getBoolean("sound_enabled", true));
        swMusic.setChecked(prefs.getBoolean("music_enabled", true));

        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    boolean sound = swSound.isChecked();
                    boolean music = swMusic.isChecked();
                    prefs.edit()
                            .putBoolean("sound_enabled", sound)
                            .putBoolean("music_enabled", music)
                            .apply();
                    
                    if (!music && menuMusic != null && menuMusic.isPlaying()) {
                        menuMusic.stop();
                    } else if (music && (menuMusic == null || !menuMusic.isPlaying())) {
                        recreate();
                    }
                })
                .show();

        btnReset.setOnClickListener(v -> {
            getSharedPreferences("QuizScores", MODE_PRIVATE).edit().clear().apply();
            Toast.makeText(this, "Leaderboard Reset!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePlayerLevelHeader();
        if (menuMusic != null && !menuMusic.isPlaying()) {
            SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
            if (prefs.getBoolean("music_enabled", true)) {
                menuMusic.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuMusic != null) {
            menuMusic.release();
        }
    }
}