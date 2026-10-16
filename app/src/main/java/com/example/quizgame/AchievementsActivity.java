package com.example.quizgame;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private TextView tvPlayerTitle, tvXpSummary, tvBadgesCount, tvStatGamesPlayed, tvStatMaxStreak, tvStatAccuracy;
    private ProgressBar pbXpProgress;
    private RecyclerView rvAchievements;
    private MaterialButton btnBack;

    public static class AchievementItem {
        String icon;
        String title;
        String description;
        int currentProgress;
        int maxProgress;
        boolean isUnlocked;

        public AchievementItem(String icon, String title, String description, int currentProgress, int maxProgress, boolean isUnlocked) {
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.currentProgress = currentProgress;
            this.maxProgress = maxProgress;
            this.isUnlocked = isUnlocked;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvXpSummary = findViewById(R.id.tvXpSummary);
        tvBadgesCount = findViewById(R.id.tvBadgesCount);
        tvStatGamesPlayed = findViewById(R.id.tvStatGamesPlayed);
        tvStatMaxStreak = findViewById(R.id.tvStatMaxStreak);
        tvStatAccuracy = findViewById(R.id.tvStatAccuracy);
        pbXpProgress = findViewById(R.id.pbXpProgress);
        rvAchievements = findViewById(R.id.rvAchievements);
        btnBack = findViewById(R.id.btnBackAch);

        btnBack.setOnClickListener(v -> finish());

        loadPlayerStatsAndAchievements();
    }

    private void loadPlayerStatsAndAchievements() {
        SharedPreferences settingsPrefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        SharedPreferences achPrefs = getSharedPreferences("QuizAchievements", MODE_PRIVATE);

        int xp = settingsPrefs.getInt("player_xp", 0);
        int level = (xp / 300) + 1;
        int xpInCurrentLevel = xp % 300;
        int xpForNextLevel = 300;

        String[] titles = {"Novice Scholar", "Quiz Apprentice", "Knowledge Seeker", "Mind Master", "Grand Polymath"};
        String title = titles[Math.min(level - 1, titles.length - 1)];

        tvPlayerTitle.setText("Level " + level + " • " + title);
        tvXpSummary.setText("XP: " + xpInCurrentLevel + " / " + xpForNextLevel + " (Total: " + xp + ")");
        pbXpProgress.setMax(xpForNextLevel);
        pbXpProgress.setProgress(xpInCurrentLevel);

        int totalQuestions = achPrefs.getInt("total_questions", 0);
        int totalGames = achPrefs.getInt("total_games", 0);
        int maxStreak = achPrefs.getInt("max_streak", 0);
        int totalCorrect = achPrefs.getInt("total_correct", 0);

        int accuracy = (totalQuestions > 0) ? (int) (((double) totalCorrect / totalQuestions) * 100) : 0;

        tvStatGamesPlayed.setText(String.valueOf(totalGames));
        tvStatMaxStreak.setText(String.valueOf(maxStreak));
        tvStatAccuracy.setText(accuracy + "%");

        List<AchievementItem> itemList = new ArrayList<>();

        boolean bBeginner = achPrefs.getBoolean("badge_beginner", totalQuestions >= 10);
        itemList.add(new AchievementItem("🧠", "Quiz Beginner", "Answer 10 questions total", Math.min(totalQuestions, 10), 10, bBeginner));

        boolean bOnFire = achPrefs.getBoolean("badge_on_fire", maxStreak >= 5);
        itemList.add(new AchievementItem("🔥", "On Fire!", "Reach a 5x answer streak", Math.min(maxStreak, 5), 5, bOnFire));

        boolean bMaster = achPrefs.getBoolean("badge_master", false);
        itemList.add(new AchievementItem("🏆", "Quiz Master", "Score 100% in any quiz category", bMaster ? 1 : 0, 1, bMaster));

        boolean bDaily = achPrefs.getBoolean("badge_daily", false);
        itemList.add(new AchievementItem("📅", "Daily Challenger", "Complete a Daily Challenge", bDaily ? 1 : 0, 1, bDaily));

        boolean bSurvivor = achPrefs.getBoolean("badge_survivor", false);
        itemList.add(new AchievementItem("💣", "Survivor", "Score 10+ in Survival Mode", bSurvivor ? 1 : 0, 1, bSurvivor));

        boolean bSpeed = achPrefs.getBoolean("badge_speed", totalGames >= 5);
        itemList.add(new AchievementItem("⚡", "Quiz Veteran", "Play 5 quiz games", Math.min(totalGames, 5), 5, bSpeed));

        int unlockedCount = 0;
        for (AchievementItem item : itemList) {
            if (item.isUnlocked) unlockedCount++;
        }
        tvBadgesCount.setText("Badges: " + unlockedCount + " / " + itemList.size());

        rvAchievements.setLayoutManager(new LinearLayoutManager(this));
        rvAchievements.setAdapter(new AchievementsAdapter(itemList));
    }

    private static class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ViewHolder> {
        private final List<AchievementItem> items;

        public AchievementsAdapter(List<AchievementItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AchievementItem item = items.get(position);
            holder.tvIcon.setText(item.icon);
            holder.tvTitle.setText(item.title);
            holder.tvDesc.setText(item.description);

            holder.pbProgress.setMax(item.maxProgress);
            holder.pbProgress.setProgress(item.currentProgress);

            if (item.isUnlocked) {
                holder.tvStatus.setText("✅");
                holder.tvTitle.setAlpha(1.0f);
                holder.tvDesc.setAlpha(1.0f);
            } else {
                holder.tvStatus.setText("🔒");
                holder.tvTitle.setAlpha(0.6f);
                holder.tvDesc.setAlpha(0.6f);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvTitle, tvDesc, tvStatus;
            ProgressBar pbProgress;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tvBadgeIcon);
                tvTitle = itemView.findViewById(R.id.tvBadgeTitle);
                tvDesc = itemView.findViewById(R.id.tvBadgeDesc);
                tvStatus = itemView.findViewById(R.id.tvBadgeStatus);
                pbProgress = itemView.findViewById(R.id.pbBadgeProgress);
            }
        }
    }
}