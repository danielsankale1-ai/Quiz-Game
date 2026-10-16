package com.example.quizgame;

public class Score implements Comparable<Score> {
    private String name;
    private int scoreValue;
    private String category;

    public Score(String name, int scoreValue, String category) {
        this.name = name;
        this.scoreValue = scoreValue;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public int getScoreValue() {
        return scoreValue;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public int compareTo(Score other) {
        // Sort descending
        return Integer.compare(other.scoreValue, this.scoreValue);
    }
}