package com.example.quizgame;

public class Question {
    private String question;
    private String[] options;
    private int correctAnswerIndex;
    private String category;
    private int difficulty; // 1: Easy, 2: Medium, 3: Hard, 4: Expert
    private String clue;

    public Question(String question, String[] options, int correctAnswerIndex, String category, int difficulty, String clue) {
        this.question = question;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.category = category;
        this.difficulty = difficulty;
        this.clue = clue;
    }

    public String getQuestion() {
        return question;
    }

    public String[] getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public String getCategory() {
        return category;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getClue() {
        return clue;
    }
}