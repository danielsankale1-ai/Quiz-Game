package com.example.quizgame;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreActivity extends AppCompatActivity {

    private TextView tvScoresList;
    private MaterialButton btnBack, btnClear;
    private TabLayout tabLayout;
    private List<Score> allScores;
    private final String[] categories = {"All", "ICT", "Science", "Geography", "Sports"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        tvScoresList = findViewById(R.id.tvScoresList);
        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);
        tabLayout = findViewById(R.id.tabLayout);

        setupTabs();
        loadScores();
        displayScores("All");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                displayScores(categories[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnBack.setOnClickListener(v -> finish());
        btnClear.setOnClickListener(v -> clearScores());
    }

    private void setupTabs() {
        for (String category : categories) {
            tabLayout.addTab(tabLayout.newTab().setText(category));
        }
    }

    private void loadScores() {
        SharedPreferences sharedPreferences = getSharedPreferences("QuizScores", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("scores", null);
        Type type = new TypeToken<ArrayList<Score>>() {}.getType();
        allScores = gson.fromJson(json, type);

        if (allScores == null || allScores.isEmpty()) {
            allScores = new ArrayList<>();
            allScores.add(new Score("Quiz Master 🏆", 20, "ICT"));
            allScores.add(new Score("Brainiac 🧠", 18, "Science"));
            allScores.add(new Score("Champion 🥇", 16, "Sports"));
            allScores.add(new Score("Explorer 🌍", 15, "Geography"));
            allScores.add(new Score("Scholar 📖", 12, "ICT"));

            String defaultJson = gson.toJson(allScores);
            sharedPreferences.edit().putString("scores", defaultJson).apply();
        }
        Collections.sort(allScores);
    }

    private void displayScores(String categoryFilter) {
        if (allScores == null || allScores.isEmpty()) {
            tvScoresList.setText("No scores recorded yet!");
            return;
        }

        List<Score> filteredList = new ArrayList<>();
        if (categoryFilter.equals("All")) {
            filteredList.addAll(allScores);
        } else {
            for (Score s : allScores) {
                if (categoryFilter.equalsIgnoreCase(s.getCategory())) {
                    filteredList.add(s);
                }
            }
        }

        Collections.sort(filteredList);

        if (filteredList.isEmpty()) {
            tvScoresList.setText("No scores for " + categoryFilter + " yet!");
            return;
        }

        StringBuilder builder = new StringBuilder();
        int displayCount = Math.min(filteredList.size(), 10);
        for (int i = 0; i < displayCount; i++) {
            Score s = filteredList.get(i);
            String medal;
            if (i == 0) medal = "🥇 ";
            else if (i == 1) medal = "🥈 ";
            else if (i == 2) medal = "🥉 ";
            else if (i < 5) medal = "🎖️ ";
            else medal = "   ";

            builder.append(medal)
                    .append(i + 1).append(". ")
                    .append(s.getName())
                    .append(" — ")
                    .append(s.getScoreValue()).append(" pts");

            if (categoryFilter.equals("All") && s.getCategory() != null) {
                builder.append(" (").append(s.getCategory()).append(")");
            }
            builder.append("\n\n");
        }
        tvScoresList.setText(builder.toString().trim());
    }

    private void clearScores() {
        SharedPreferences sharedPreferences = getSharedPreferences("QuizScores", MODE_PRIVATE);
        sharedPreferences.edit().remove("scores").apply();
        allScores.clear();
        displayScores(categories[tabLayout.getSelectedTabPosition()]);
        Toast.makeText(this, "Scores cleared!", Toast.LENGTH_SHORT).show();
    }
}