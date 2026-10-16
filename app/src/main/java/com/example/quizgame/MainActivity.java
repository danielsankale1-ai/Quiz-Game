package com.example.quizgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvQuestion, tvScore, tvTimer, tvResultPlaceholder, tvStreak, tvLives;
    private MaterialButton[] btnOptions = new MaterialButton[4];
    private MaterialButton btnNext, btn5050, btnHint, btnSkip, btnPoll;
    private MaterialCardView cardQuestion;

    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private CountDownTimer countDownTimer;
    private static final long COUNTDOWN_IN_MILLIS = 20000;
    private long timeLeftInMillis;

    private MediaPlayer correctPlayer;
    private MediaPlayer wrongPlayer;
    private MediaPlayer tickPlayer;
    private MediaPlayer backgroundMusic;

    private String gameMode = "CLASSIC";
    private int lives = 3;
    private int currentStreak = 0;
    private int maxStreakInSession = 0;
    private boolean isPollUsed = false;
    
    private String category;
    private String playerName;
    private int level = 1;
    
    private Animation fadeIn, scaleUp;

    private boolean is5050Used = false;
    private boolean isHintUsed = false;
    private boolean isSkipUsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        category = getIntent().getStringExtra("CATEGORY");
        playerName = getIntent().getStringExtra("PLAYER_NAME");
        gameMode = getIntent().getStringExtra("GAME_MODE");
        if (gameMode == null) gameMode = "CLASSIC";
        level = getIntent().getIntExtra("LEVEL", 1);
        if (category == null) category = "ICT";

        tvQuestion = findViewById(R.id.tvQuestion);
        tvScore = findViewById(R.id.tvScore);
        tvTimer = findViewById(R.id.tvTimer);
        tvResultPlaceholder = findViewById(R.id.tvResultPlaceholder);
        tvStreak = findViewById(R.id.tvStreak);
        tvLives = findViewById(R.id.tvLives);
        cardQuestion = findViewById(R.id.cardQuestion);
        btnNext = findViewById(R.id.btnNext);

        btnOptions[0] = findViewById(R.id.btnOption1);
        btnOptions[1] = findViewById(R.id.btnOption2);
        btnOptions[2] = findViewById(R.id.btnOption3);
        btnOptions[3] = findViewById(R.id.btnOption4);

        btn5050 = findViewById(R.id.btn5050);
        btnHint = findViewById(R.id.btnHint);
        btnSkip = findViewById(R.id.btnSkip);
        btnPoll = findViewById(R.id.btnPoll);

        if ("SURVIVAL".equals(gameMode)) {
            tvLives.setVisibility(View.VISIBLE);
            updateLivesDisplay();
        } else {
            tvLives.setVisibility(View.GONE);
        }
        
        fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        scaleUp = AnimationUtils.loadAnimation(this, R.anim.bounce); 

        SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        boolean musicEnabled = prefs.getBoolean("music_enabled", true);

        if (soundEnabled) {
            correctPlayer = MediaPlayer.create(this, R.raw.correct);
            wrongPlayer = MediaPlayer.create(this, R.raw.wronganswer);
            int tickResId = getResources().getIdentifier("timercountdown", "raw", getPackageName());
            if (tickResId != 0) {
                tickPlayer = MediaPlayer.create(this, tickResId);
                tickPlayer.setLooping(true);
            }
        }

        if (musicEnabled) {
            backgroundMusic = MediaPlayer.create(this, R.raw.backgroundmusic);
            if (backgroundMusic != null) {
                backgroundMusic.setLooping(true);
                backgroundMusic.start();
            }
        }

        loadQuestions();
        showNextQuestion();

        for (int i = 0; i < 4; i++) {
            final int index = i;
            btnOptions[i].setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                checkAnswer(index);
            });
        }

        btnNext.setOnClickListener(v -> {
            if (currentQuestionIndex < questionList.size()) {
                showNextQuestion();
            } else {
                finishQuiz();
            }
        });

        btn5050.setOnClickListener(v -> use5050());
        btnHint.setOnClickListener(v -> useHint());
        btnSkip.setOnClickListener(v -> useSkip());
        btnPoll.setOnClickListener(v -> useAudiencePoll());
    }

    private void use5050() {
        if (is5050Used) {
            Toast.makeText(this, "50/50 already used!", Toast.LENGTH_SHORT).show();
            return;
        }
        is5050Used = true;
        btn5050.setEnabled(false);
        btn5050.setAlpha(0.5f);

        Question currentQuestion = questionList.get(currentQuestionIndex - 1);
        int correctIndex = currentQuestion.getCorrectAnswerIndex();
        
        List<Integer> incorrectIndices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i != correctIndex) incorrectIndices.add(i);
        }
        Collections.shuffle(incorrectIndices);
        
        // Hide two incorrect options
        btnOptions[incorrectIndices.get(0)].setVisibility(View.INVISIBLE);
        btnOptions[incorrectIndices.get(1)].setVisibility(View.INVISIBLE);
    }

    private void useHint() {
        if (isHintUsed) {
            Toast.makeText(this, "Hint already used!", Toast.LENGTH_SHORT).show();
            return;
        }
        isHintUsed = true;
        btnHint.setEnabled(false);
        btnHint.setAlpha(0.5f);

        Question currentQuestion = questionList.get(currentQuestionIndex - 1);
        new AlertDialog.Builder(this)
                .setTitle("Hint")
                .setMessage(currentQuestion.getClue())
                .setPositiveButton("OK", null)
                .show();
    }

    private void useSkip() {
        if (isSkipUsed) {
            Toast.makeText(this, "Skip already used!", Toast.LENGTH_SHORT).show();
            return;
        }
        isSkipUsed = true;
        btnSkip.setEnabled(false);
        btnSkip.setAlpha(0.5f);
        
        if (countDownTimer != null) countDownTimer.cancel();
        if (tickPlayer != null && tickPlayer.isPlaying()) tickPlayer.pause();
        if (currentQuestionIndex < questionList.size()) {
            showNextQuestion();
        } else {
            finishQuiz();
        }
    }

    private void useAudiencePoll() {
        if (isPollUsed) {
            Toast.makeText(this, "Audience Poll already used!", Toast.LENGTH_SHORT).show();
            return;
        }
        isPollUsed = true;
        btnPoll.setEnabled(false);
        btnPoll.setAlpha(0.5f);

        Question currentQuestion = questionList.get(currentQuestionIndex - 1);
        int correctIndex = currentQuestion.getCorrectAnswerIndex();

        View pollView = LayoutInflater.from(this).inflate(R.layout.dialog_audience_poll, null);
        TextView tvOpt1 = pollView.findViewById(R.id.tvPollOption1);
        TextView tvOpt2 = pollView.findViewById(R.id.tvPollOption2);
        TextView tvOpt3 = pollView.findViewById(R.id.tvPollOption3);
        TextView tvOpt4 = pollView.findViewById(R.id.tvPollOption4);

        TextView tvPerc1 = pollView.findViewById(R.id.tvPollPercent1);
        TextView tvPerc2 = pollView.findViewById(R.id.tvPollPercent2);
        TextView tvPerc3 = pollView.findViewById(R.id.tvPollPercent3);
        TextView tvPerc4 = pollView.findViewById(R.id.tvPollPercent4);

        ProgressBar pb1 = pollView.findViewById(R.id.pbPoll1);
        ProgressBar pb2 = pollView.findViewById(R.id.pbPoll2);
        ProgressBar pb3 = pollView.findViewById(R.id.pbPoll3);
        ProgressBar pb4 = pollView.findViewById(R.id.pbPoll4);

        String[] options = currentQuestion.getOptions();
        tvOpt1.setText("A: " + options[0]);
        tvOpt2.setText("B: " + options[1]);
        tvOpt3.setText("C: " + options[2]);
        tvOpt4.setText("D: " + options[3]);

        int[] percents = new int[4];
        int correctVotes = 55 + new Random().nextInt(25);
        percents[correctIndex] = correctVotes;

        int remaining = 100 - correctVotes;
        List<Integer> otherIndices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i != correctIndex) otherIndices.add(i);
        }

        int p1 = new Random().nextInt(Math.max(1, remaining / 2));
        int p2 = new Random().nextInt(Math.max(1, remaining - p1));
        int p3 = remaining - p1 - p2;

        percents[otherIndices.get(0)] = p1;
        percents[otherIndices.get(1)] = p2;
        percents[otherIndices.get(2)] = Math.max(0, p3);

        tvPerc1.setText(percents[0] + "%");
        tvPerc2.setText(percents[1] + "%");
        tvPerc3.setText(percents[2] + "%");
        tvPerc4.setText(percents[3] + "%");

        pb1.setProgress(percents[0]);
        pb2.setProgress(percents[1]);
        pb3.setProgress(percents[2]);
        pb4.setProgress(percents[3]);

        new AlertDialog.Builder(this)
                .setView(pollView)
                .setPositiveButton("Got it!", null)
                .show();
    }

    private void updateLivesDisplay() {
        if (tvLives == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i < lives) {
                sb.append("❤️ ");
            } else {
                sb.append("🖤 ");
            }
        }
        tvLives.setText(sb.toString().trim());
    }

    private void loadQuestions() {
        List<Question> allQuestions = new ArrayList<>();
        // ICT (30 Questions)
        allQuestions.add(new Question("What does CPU stand for?", new String[]{"Central Processing Unit", "Computer Personal Unit", "Central Process Unit", "Central Peripheral Unit"}, 0, "ICT", 1, "It is the brain of the computer."));
        allQuestions.add(new Question("Which protocol is used to send emails?", new String[]{"HTTP", "FTP", "SMTP", "SSH"}, 2, "ICT", 1, "Simple Mail Transfer Protocol."));
        allQuestions.add(new Question("What is the main language for Android development?", new String[]{"Swift", "Kotlin", "PHP", "C#"}, 1, "ICT", 1, "A modern language from JetBrains."));
        allQuestions.add(new Question("How many bits are in a single byte?", new String[]{"4", "8", "16", "32"}, 1, "ICT", 1, "The power of 2 cubed."));
        allQuestions.add(new Question("Who is known as the father of Computers?", new String[]{"Alan Turing", "Charles Babbage", "Bill Gates", "Steve Jobs"}, 1, "ICT", 1, "British mathematician and inventor."));
        allQuestions.add(new Question("What does RAM stand for?", new String[]{"Random Access Memory", "Read Access Memory", "Run Active Module", "Random Active Memory"}, 0, "ICT", 1, "Temporary working memory."));
        allQuestions.add(new Question("What does HTTP stand for?", new String[]{"Hypertext Transfer Protocol", "High Text Transfer Process", "Hyperlink Text Test Plan", "Hyper Transfer Text Protocol"}, 0, "ICT", 2, "Protocol for web pages."));
        allQuestions.add(new Question("Who is considered the first computer programmer?", new String[]{"Charles Babbage", "Alan Turing", "Ada Lovelace", "Grace Hopper"}, 2, "ICT", 2, "A woman who worked with Babbage."));
        allQuestions.add(new Question("Who created the Linux kernel?", new String[]{"Bill Gates", "Linus Torvalds", "Steve Jobs", "Richard Stallman"}, 1, "ICT", 2, "A Finnish-American software engineer."));
        allQuestions.add(new Question("What does SQL stand for?", new String[]{"Structured Query Language", "Simple Queue List", "Sample Query Logic", "System Query Language"}, 0, "ICT", 2, "Standard for relational databases."));
        allQuestions.add(new Question("Who invented the World Wide Web?", new String[]{"Steve Jobs", "Tim Berners-Lee", "Bill Gates", "Vint Cerf"}, 1, "ICT", 2, "He worked at CERN."));
        allQuestions.add(new Question("What is the binary representation of the decimal number 10?", new String[]{"1001", "1110", "1010", "1100"}, 2, "ICT", 3, "Contains two 1s and two 0s."));
        allQuestions.add(new Question("Who created Python?", new String[]{"Brendan Eich", "Guido van Rossum", "James Gosling", "Bjarne Stroustrup"}, 1, "ICT", 2, "Dutch programmer."));
        allQuestions.add(new Question("What is the core of an OS called?", new String[]{"Kernel", "Shell", "Script", "Command"}, 0, "ICT", 3, "A nut shares this name."));
        allQuestions.add(new Question("Which type of memory is non-volatile?", new String[]{"RAM", "Cache", "ROM", "Registers"}, 2, "ICT", 2, "Read Only Memory."));
        allQuestions.add(new Question("What is the size of an IPv6 address?", new String[]{"32-bit", "64-bit", "48-bit", "128-bit"}, 3, "ICT", 3, "Much larger than 32-bit IPv4."));
        allQuestions.add(new Question("What was the first electronic computer?", new String[]{"ENIAC", "UNIVAC", "EDVAC", "IBM PC"}, 0, "ICT", 3, "Built during WWII."));
        allQuestions.add(new Question("Who created JavaScript?", new String[]{"Brendan Eich", "James Gosling", "Douglas Crockford", "John Resig"}, 0, "ICT", 3, "Created it in 10 days."));
        allQuestions.add(new Question("What does DNS stand for?", new String[]{"Domain Name System", "Digital Network Service", "Direct Node Server", "Data Name Serial"}, 0, "ICT", 2, "Phonebook of the internet."));
        allQuestions.add(new Question("What does GUI stand for?", new String[]{"General User Interface", "Graphical User Interface", "Global User Index", "Group Unit Image"}, 1, "ICT", 1, "Uses visual icons."));
        allQuestions.add(new Question("Which company developed the Java programming language?", new String[]{"Microsoft", "Apple", "Sun Microsystems", "Oracle"}, 2, "ICT", 2, "Later acquired by Oracle."));
        allQuestions.add(new Question("What is the file extension for a Java source file?", new String[]{".class", ".java", ".txt", ".exe"}, 1, "ICT", 1, "Standard naming convention."));
        allQuestions.add(new Question("Which of these is a widely used VCS?", new String[]{"Git", "Grit", "Gat", "Got"}, 0, "ICT", 1, "Created by Linus Torvalds."));
        allQuestions.add(new Question("What does BIOS stand for?", new String[]{"Basic Input Output System", "Binary Input Output Service", "Basic Interior OS", "Binary Internal OS"}, 0, "ICT", 2, "Executes when computer starts."));
        allQuestions.add(new Question("What is the main function of a router?", new String[]{"Data storage", "Forwarding data packets", "Printing documents", "Executing code"}, 1, "ICT", 1, "Directs network traffic."));
        allQuestions.add(new Question("Which layer of the OSI model is responsible for routing?", new String[]{"Physical", "Data Link", "Network", "Transport"}, 2, "ICT", 3, "Layer 3."));
        allQuestions.add(new Question("What does XML stand for?", new String[]{"Extensible Markup Language", "Extra Modern Link", "Extended Modal Logic", "External Module Link"}, 0, "ICT", 2, "Used for structured data."));
        allQuestions.add(new Question("Which part of the computer is responsible for calculations?", new String[]{"RAM", "ALU", "SSD", "GPU"}, 1, "ICT", 2, "Arithmetic Logic Unit."));
        allQuestions.add(new Question("What was the original name of the Internet?", new String[]{"ARPANET", "Intranet", "Webnet", "Ethernet"}, 0, "ICT", 3, "Developed by the US DoD."));
        allQuestions.add(new Question("Who is the CEO of Meta (as of 2024)?", new String[]{"Elon Musk", "Mark Zuckerberg", "Jeff Bezos", "Sundar Pichai"}, 1, "ICT", 1, "Founder of Facebook."));
        allQuestions.add(new Question("What does SSD stand for in computer hardware?", new String[]{"Solid State Drive", "System Storage Device", "Super Speed Disk", "Serial Storage Drive"}, 0, "ICT", 1, "Non-mechanical storage drive."));
        allQuestions.add(new Question("Which company developed the Windows OS?", new String[]{"Apple", "IBM", "Microsoft", "Google"}, 2, "ICT", 1, "Founded by Bill Gates and Paul Allen."));
        allQuestions.add(new Question("What does VPN stand for?", new String[]{"Virtual Private Network", "Visual Process Node", "Verified Public Network", "Virtual Protected Node"}, 0, "ICT", 1, "Encrypts online internet connection."));
        allQuestions.add(new Question("Which language is primarily used for styling web pages?", new String[]{"HTML", "CSS", "Java", "C++"}, 1, "ICT", 1, "Cascading Style Sheets."));
        allQuestions.add(new Question("What is the main function of an IP address?", new String[]{"Encrypt data", "Identify devices on a network", "Speed up internet", "Store passcodes"}, 1, "ICT", 1, "Unique numerical identifier."));
        allQuestions.add(new Question("Which organization defines web standards like HTML and CSS?", new String[]{"IEEE", "W3C", "ISO", "IETF"}, 1, "ICT", 2, "World Wide Web Consortium."));
        allQuestions.add(new Question("What does GPU stand for?", new String[]{"Graphics Processing Unit", "General Process Utility", "Global Power Unit", "Graphical Personal Unit"}, 0, "ICT", 1, "Renders images and video."));
        allQuestions.add(new Question("What type of software license is Linux?", new String[]{"Proprietary", "Open Source", "Shareware", "Adware"}, 1, "ICT", 1, "Free to modify and distribute."));
        allQuestions.add(new Question("Which port is standard for HTTPS web traffic?", new String[]{"80", "21", "443", "22"}, 2, "ICT", 2, "Secure web protocol port."));
        allQuestions.add(new Question("What does USB stand for?", new String[]{"Universal Serial Bus", "United System Base", "Universal Storage Board", "United Serial Bit"}, 0, "ICT", 1, "Common cable connection standard."));
        allQuestions.add(new Question("Which data structure operates on LIFO (Last In, First Out)?", new String[]{"Queue", "Stack", "Array", "Tree"}, 1, "ICT", 2, "Like a stack of plates."));
        allQuestions.add(new Question("What does URL stand for?", new String[]{"Uniform Resource Locator", "Universal Reference Link", "Unified Resource Line", "Universal Record Locator"}, 0, "ICT", 1, "Web address format."));
        allQuestions.add(new Question("Which company originally created Android before Google acquired it?", new String[]{"Google", "Android Inc.", "Nokia", "Samsung"}, 1, "ICT", 2, "Co-founded by Andy Rubin."));
        allQuestions.add(new Question("What is 'phishing' in cybersecurity?", new String[]{"Hardware repair", "Social engineering attack to steal credentials", "Speeding up CPU", "Formatting hard drive"}, 1, "ICT", 1, "Deceptive emails or sites."));
        allQuestions.add(new Question("Which programming language uses the file extension .py?", new String[]{"Python", "PHP", "Perl", "Pascal"}, 0, "ICT", 1, "Named after Monty Python."));

        // Science (45 Questions)
        allQuestions.add(new Question("What is the chemical symbol for Gold?", new String[]{"Au", "Ag", "Gd", "Go"}, 0, "Science", 1, "Latin name is Aurum."));
        allQuestions.add(new Question("What is the boiling point of water at sea level?", new String[]{"90°C", "100°C", "110°C", "120°C"}, 1, "Science", 1, "A nice round number in Celsius."));
        allQuestions.add(new Question("Which element does 'O' represent on the periodic table?", new String[]{"Osmium", "Oxygen", "Gold", "Silver"}, 1, "Science", 1, "Essential for human breathing."));
        allQuestions.add(new Question("Which planet is known as the Red Planet?", new String[]{"Venus", "Mars", "Jupiter", "Saturn"}, 1, "Science", 1, "Named after the Roman god of war."));
        allQuestions.add(new Question("Which gas do plants absorb from the atmosphere?", new String[]{"Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"}, 2, "Science", 1, "Often abbreviated as CO2."));
        allQuestions.add(new Question("What is the approximate speed of light?", new String[]{"300,000 km/s", "150,000 km/s", "450,000 km/s", "1,000,000 km/s"}, 0, "Science", 3, "Fastest thing in the universe."));
        allQuestions.add(new Question("What is the chemical formula for water?", new String[]{"H2O", "CO2", "NaCl", "O2"}, 0, "Science", 1, "Two hydrogen, one oxygen."));
        allQuestions.add(new Question("What is the closest star to Earth?", new String[]{"Sirius", "Proxima Centauri", "Alpha Centauri", "Sun"}, 3, "Science", 2, "It provides us with light and heat."));
        allQuestions.add(new Question("What is the outermost layer of the Earth called?", new String[]{"Crust", "Mantle", "Core", "Atmosphere"}, 0, "Science", 2, "Like the outer part of bread."));
        allQuestions.add(new Question("Which galaxy is Earth located in?", new String[]{"Andromeda", "Milky Way", "Sombrero", "Triangulum"}, 1, "Science", 1, "Named after a spilled dairy product."));
        allQuestions.add(new Question("How many bones are in the adult human body?", new String[]{"186", "206", "226", "246"}, 1, "Science", 2, "Starts with 20."));
        allQuestions.add(new Question("Which vitamin is produced when skin is exposed to sunlight?", new String[]{"Vitamin A", "Vitamin B", "Vitamin D", "Vitamin C"}, 2, "Science", 1, "Crucial for bone health."));
        allQuestions.add(new Question("Who discovered the law of universal gravitation?", new String[]{"Isaac Newton", "Albert Einstein", "Galileo Galilei", "Nikola Tesla"}, 0, "Science", 1, "An apple supposedly fell on his head."));
        allQuestions.add(new Question("Who is known as the father of the Periodic Table?", new String[]{"John Dalton", "Dmitri Mendeleev", "Marie Curie", "Antoine Lavoisier"}, 1, "Science", 2, "A Russian chemist."));
        allQuestions.add(new Question("Which of these is a noble gas?", new String[]{"Nitrogen", "Oxygen", "Helium", "Hydrogen"}, 2, "Science", 2, "Used to make balloons float."));
        allQuestions.add(new Question("What is the 'powerhouse' of the cell?", new String[]{"Nucleus", "Ribosome", "Mitochondria", "Cytoplasm"}, 2, "Science", 2, "Provides energy for cellular functions."));
        allQuestions.add(new Question("What is the hardest natural substance on Earth?", new String[]{"Gold", "Diamond", "Iron", "Quartz"}, 1, "Science", 1, "Used in engagement rings."));
        allQuestions.add(new Question("The speed of sound is measured in which units?", new String[]{"Mach", "Knots", "Light Years", "Decibels"}, 0, "Science", 3, "Breaking this causes a sonic boom."));
        allQuestions.add(new Question("What is the pH value of pure water?", new String[]{"5", "7", "9", "11"}, 1, "Science", 2, "It's neutral on the scale."));
        allQuestions.add(new Question("Who developed the theory of general relativity?", new String[]{"Isaac Newton", "Stephen Hawking", "Albert Einstein", "Niels Bohr"}, 2, "Science", 1, "E=mc²"));
        allQuestions.add(new Question("What is the largest planet in our solar system?", new String[]{"Earth", "Jupiter", "Saturn", "Neptune"}, 1, "Science", 1, "Famous for its Great Red Spot."));
        allQuestions.add(new Question("Which element has the atomic number 1?", new String[]{"Oxygen", "Helium", "Hydrogen", "Lithium"}, 2, "Science", 1, "Most abundant element in the universe."));
        allQuestions.add(new Question("What is the study of living organisms called?", new String[]{"Physics", "Chemistry", "Biology", "Geology"}, 2, "Science", 1, "From Greek 'bios'."));
        allQuestions.add(new Question("Who is the scientist who proposed the theory of evolution?", new String[]{"Gregor Mendel", "Charles Darwin", "Louis Pasteur", "Marie Curie"}, 1, "Science", 2, "Author of 'On the Origin of Species'."));
        allQuestions.add(new Question("What is the most common gas in Earth's atmosphere?", new String[]{"Oxygen", "Nitrogen", "Argon", "Carbon Dioxide"}, 1, "Science", 2, "Makes up about 78%."));
        allQuestions.add(new Question("Which blood type is known as the universal donor?", new String[]{"A", "B", "AB", "O"}, 3, "Science", 2, "Can be given to any other blood type."));
        allQuestions.add(new Question("How many planets are in our solar system?", new String[]{"7", "8", "9", "10"}, 1, "Science", 1, "Pluto is no longer one."));
        allQuestions.add(new Question("What is the freezing point of water in Fahrenheit?", new String[]{"0°F", "32°F", "100°F", "212°F"}, 1, "Science", 2, "Equivalent to 0°C."));
        allQuestions.add(new Question("What is the unit of electric current?", new String[]{"Volt", "Watt", "Ampere", "Ohm"}, 2, "Science", 3, "Named after André-Marie Ampère."));
        allQuestions.add(new Question("What is the largest organ in the human body?", new String[]{"Heart", "Liver", "Skin", "Lungs"}, 3, "Science", 1, "Protects the body from the environment."));
        allQuestions.add(new Question("What is the chemical symbol for Sodium?", new String[]{"So", "Na", "Sd", "Sm"}, 1, "Science", 1, "Latin name Natrium."));
        allQuestions.add(new Question("What is the approximate speed of sound in air?", new String[]{"343 m/s", "100 m/s", "1000 m/s", "1500 m/s"}, 0, "Science", 2, "Around 1235 km/h."));
        allQuestions.add(new Question("Which organ produces insulin in the human body?", new String[]{"Liver", "Kidney", "Pancreas", "Stomach"}, 2, "Science", 1, "Regulates blood sugar."));
        allQuestions.add(new Question("What is the central part of an atom called?", new String[]{"Electron", "Proton", "Nucleus", "Neutron"}, 2, "Science", 1, "Contains protons and neutrons."));
        allQuestions.add(new Question("Which gas makes fizzy carbonated drinks bubbly?", new String[]{"Oxygen", "Carbon Dioxide", "Nitrogen", "Helium"}, 1, "Science", 1, "CO2 gas under pressure."));
        allQuestions.add(new Question("What type of rock forms from cooled lava or magma?", new String[]{"Igneous", "Sedimentary", "Metamorphic", "Limestone"}, 0, "Science", 1, "From Latin ignis (fire)."));
        allQuestions.add(new Question("Which scientist formulated the three laws of motion?", new String[]{"Galileo", "Isaac Newton", "Tesla", "Edison"}, 1, "Science", 1, "Classical mechanics pioneer."));
        allQuestions.add(new Question("What is the main hydrocarbon component of natural gas?", new String[]{"Methane", "Ethane", "Propane", "Butane"}, 0, "Science", 2, "Chemical formula CH4."));
        allQuestions.add(new Question("Which planet has the most confirmed moons in our solar system?", new String[]{"Jupiter", "Saturn", "Uranus", "Neptune"}, 1, "Science", 2, "Has over 140 moons."));
        allQuestions.add(new Question("By what process do green plants convert light into chemical energy?", new String[]{"Respiration", "Photosynthesis", "Transpiration", "Osmosis"}, 1, "Science", 1, "Uses chlorophyll."));
        allQuestions.add(new Question("What is the SI unit of Force in physics?", new String[]{"Joule", "Pascal", "Newton", "Watt"}, 2, "Science", 1, "Symbol N."));
        allQuestions.add(new Question("Which is the rarest ABO blood type in humans?", new String[]{"O negative", "AB negative", "A positive", "B negative"}, 1, "Science", 2, "Under 1% of population."));
        allQuestions.add(new Question("Which metallic element is liquid at standard room temperature?", new String[]{"Mercury", "Lead", "Tin", "Copper"}, 0, "Science", 1, "Also known as quicksilver."));
        allQuestions.add(new Question("What is the chemical symbol for Iron?", new String[]{"Ir", "Fe", "In", "Fr"}, 1, "Science", 1, "From Latin Ferrum."));
        allQuestions.add(new Question("Which atmospheric layer contains Earth's protective ozone layer?", new String[]{"Troposphere", "Stratosphere", "Mesosphere", "Thermosphere"}, 1, "Science", 2, "Second layer of atmosphere."));

        // Geography (45 Questions)
        allQuestions.add(new Question("Which is the largest continent by area?", new String[]{"Africa", "Europe", "Asia", "North America"}, 2, "Geography", 1, "Home to China and India."));
        allQuestions.add(new Question("What is the capital of Japan?", new String[]{"Seoul", "Beijing", "Tokyo", "Bangkok"}, 2, "Geography", 1, "The most populous metropolitan area."));
        allQuestions.add(new Question("Which river is the longest in the world?", new String[]{"Amazon", "Nile", "Yangtze", "Mississippi"}, 1, "Geography", 1, "Flows through Northeastern Africa."));
        allQuestions.add(new Question("Which ocean is the largest?", new String[]{"Atlantic", "Indian", "Arctic", "Pacific"}, 3, "Geography", 1, "Name means 'peaceful'."));
        allQuestions.add(new Question("Where is the Eiffel Tower located?", new String[]{"Berlin", "London", "Paris", "Rome"}, 2, "Geography", 1, "The City of Light."));
        allQuestions.add(new Question("What is the smallest country in the world?", new String[]{"Monaco", "Vatican City", "San Marino", "Liechtenstein"}, 1, "Geography", 2, "An enclave within Rome."));
        allQuestions.add(new Question("What is the height of Mount Everest?", new String[]{"7,848m", "8,848m", "9,848m", "10,848m"}, 1, "Geography", 2, "Highest peak above sea level."));
        allQuestions.add(new Question("Which continent is the Amazon rainforest located on?", new String[]{"Africa", "Asia", "South America", "North America"}, 2, "Geography", 1, "Home to Brazil."));
        allQuestions.add(new Question("Where is the Sahara Desert located?", new String[]{"Africa", "Australia", "Asia", "South America"}, 0, "Geography", 1, "World's largest hot desert."));
        allQuestions.add(new Question("In which country is the Great Wall located?", new String[]{"Japan", "China", "India", "Korea"}, 1, "Geography", 1, "Visible from space (debated)."));
        allQuestions.add(new Question("What is the capital of Australia?", new String[]{"Sydney", "Melbourne", "Canberra", "Perth"}, 2, "Geography", 2, "Not Sydney or Melbourne."));
        allQuestions.add(new Question("What is the deepest point in the world's oceans?", new String[]{"Java Trench", "Mariana Trench", "Tonga Trench", "Puerto Rico Trench"}, 1, "Geography", 3, "Located in the Pacific."));
        allQuestions.add(new Question("Which is the largest country by land area?", new String[]{"Russia", "Canada", "China", "USA"}, 0, "Geography", 1, "Spans 11 time zones."));
        allQuestions.add(new Question("Which country is known as the Land of the Rising Sun?", new String[]{"China", "Japan", "Thailand", "Vietnam"}, 1, "Geography", 1, "Nippon."));
        allQuestions.add(new Question("Which canal connects the Atlantic and Pacific?", new String[]{"Suez Canal", "Erie Canal", "Panama Canal", "Kiel Canal"}, 2, "Geography", 2, "Located in Central America."));
        allQuestions.add(new Question("What is the capital city of Canada?", new String[]{"Toronto", "Ottawa", "Vancouver", "Montreal"}, 1, "Geography", 2, "Located in Ontario."));
        allQuestions.add(new Question("Where is the Dead Sea located?", new String[]{"Greece", "Egypt", "Jordan/Israel", "Italy"}, 2, "Geography", 2, "Lowest point on Earth's surface."));
        allQuestions.add(new Question("What is the capital of Iceland?", new String[]{"Oslo", "Reykjavik", "Helsinki", "Copenhagen"}, 1, "Geography", 3, "Smoky Bay."));
        allQuestions.add(new Question("What is the largest island in the world?", new String[]{"Greenland", "New Guinea", "Borneo", "Madagascar"}, 0, "Geography", 3, "An autonomous territory of Denmark."));
        allQuestions.add(new Question("What is the source of the River Nile?", new String[]{"Lake Tanganyika", "Lake Malawi", "Lake Victoria", "Lake Tana"}, 2, "Geography", 3, "Africa's largest lake."));
        allQuestions.add(new Question("Which country has the largest population in the world (as of 2024)?", new String[]{"China", "India", "USA", "Indonesia"}, 1, "Geography", 1, "Surpassed China in 2023."));
        allQuestions.add(new Question("What is the capital of Italy?", new String[]{"Milan", "Venice", "Florence", "Rome"}, 3, "Geography", 1, "The Eternal City."));
        allQuestions.add(new Question("Which sea is located between Europe and Africa?", new String[]{"Red Sea", "Mediterranean Sea", "Caspian Sea", "Black Sea"}, 1, "Geography", 1, "Central to ancient civilizations."));
        allQuestions.add(new Question("Which US state is the largest by area?", new String[]{"Texas", "California", "Alaska", "Montana"}, 2, "Geography", 2, "The Last Frontier."));
        allQuestions.add(new Question("What is the capital of Brazil?", new String[]{"Rio de Janeiro", "São Paulo", "Brasília", "Salvador"}, 2, "Geography", 2, "Inland planned city."));
        allQuestions.add(new Question("Which mountain range runs through South America?", new String[]{"Alps", "Himalayas", "Rockies", "Andes"}, 2, "Geography", 1, "Longest continental mountain range."));
        allQuestions.add(new Question("Which country is also a continent?", new String[]{"New Zealand", "Australia", "Madagascar", "Iceland"}, 1, "Geography", 1, "Land Down Under."));
        allQuestions.add(new Question("What is the currency of the UK?", new String[]{"Euro", "Pound Sterling", "Dollar", "Franc"}, 1, "Geography", 1, "Symbol is £."));
        allQuestions.add(new Question("Which desert is the largest cold desert in the world?", new String[]{"Gobi", "Antarctica", "Sahara", "Arctic"}, 1, "Geography", 3, "Covers the South Pole."));
        allQuestions.add(new Question("What is the capital of Egypt?", new String[]{"Alexandria", "Luxor", "Cairo", "Giza"}, 2, "Geography", 1, "City of a Thousand Minarets."));
        allQuestions.add(new Question("What is the capital city of South Korea?", new String[]{"Tokyo", "Seoul", "Busan", "Pyongyang"}, 1, "Geography", 1, "Major Asian metropolitan area."));
        allQuestions.add(new Question("Which country has the highest number of time zones including overseas territories?", new String[]{"Russia", "France", "USA", "China"}, 1, "Geography", 3, "Has 12 time zones due to territories."));
        allQuestions.add(new Question("What is the longest continental mountain range above sea level?", new String[]{"Rockies", "Andes", "Himalayas", "Alps"}, 1, "Geography", 1, "Spans western South America."));
        allQuestions.add(new Question("Which famous river flows through Paris?", new String[]{"Thames", "Seine", "Danube", "Rhine"}, 1, "Geography", 1, "Flows into the English Channel."));
        allQuestions.add(new Question("What is the capital city of Spain?", new String[]{"Barcelona", "Madrid", "Seville", "Valencia"}, 1, "Geography", 1, "Located in central Spain."));
        allQuestions.add(new Question("Which US state is nicknamed 'The Sunshine State'?", new String[]{"California", "Florida", "Texas", "Hawaii"}, 1, "Geography", 1, "Home to Miami and Orlando."));
        allQuestions.add(new Question("What is the largest lake in Africa by surface area?", new String[]{"Lake Victoria", "Lake Tanganyika", "Lake Chad", "Lake Malawi"}, 0, "Geography", 1, "Named after Queen Victoria."));
        allQuestions.add(new Question("Which country is home to Mount Kilimanjaro?", new String[]{"Kenya", "Tanzania", "Uganda", "Ethiopia"}, 1, "Geography", 2, "Africa's highest peak."));
        allQuestions.add(new Question("What is the capital city of Germany?", new String[]{"Munich", "Frankfurt", "Berlin", "Hamburg"}, 2, "Geography", 1, "Reunified capital city."));
        allQuestions.add(new Question("Which extremely dry desert is located in northern Chile?", new String[]{"Atacama", "Sahara", "Gobi", "Kalahari"}, 0, "Geography", 2, "Driest non-polar desert."));
        allQuestions.add(new Question("Which large island nation lies off the southeastern coast of Africa?", new String[]{"Madagascar", "Sri Lanka", "Fiji", "Maldives"}, 0, "Geography", 1, "Fourth largest island in world."));
        allQuestions.add(new Question("What is the capital city of Argentina?", new String[]{"Buenos Aires", "Santiago", "Lima", "Bogota"}, 0, "Geography", 1, "Located on Rio de la Plata."));
        allQuestions.add(new Question("Which European country is divided into 26 federal cantons?", new String[]{"Austria", "Switzerland", "Belgium", "Norway"}, 1, "Geography", 2, "Famous for neutrality and Alps."));
        allQuestions.add(new Question("What is the capital city of Turkey?", new String[]{"Istanbul", "Ankara", "Izmir", "Antalya"}, 1, "Geography", 2, "Not Istanbul."));
        allQuestions.add(new Question("Which ocean lies between Africa, Asia, and Australia?", new String[]{"Atlantic", "Pacific", "Indian", "Arctic"}, 2, "Geography", 1, "Third largest ocean."));

        // Sports (55 Questions)
        allQuestions.add(new Question("How many players are on a soccer team on the field?", new String[]{"9", "10", "11", "12"}, 2, "Sports", 1, "10 outfielders + 1 goalkeeper."));
        allQuestions.add(new Question("In which sport do you use a shuttlecock?", new String[]{"Tennis", "Badminton", "Squash", "Golf"}, 1, "Sports", 1, "Uses rackets and a net."));
        allQuestions.add(new Question("How long is a marathon?", new String[]{"21 km", "42.195 km", "10 km", "50 km"}, 1, "Sports", 1, "About 26.2 miles."));
        allQuestions.add(new Question("Who is often called the 'G.O.A.T' in basketball?", new String[]{"LeBron James", "Michael Jordan", "Kobe Bryant", "Stephen Curry"}, 1, "Sports", 1, "Played for the Chicago Bulls."));
        allQuestions.add(new Question("How many rings are on the Olympic flag?", new String[]{"3", "4", "5", "6"}, 2, "Sports", 2, "Represents the five continents."));
        allQuestions.add(new Question("Who has won the most Ballon d'Or awards?", new String[]{"Cristiano Ronaldo", "Lionel Messi", "Michel Platini", "Johan Cruyff"}, 1, "Sports", 1, "An Argentine magician."));
        allQuestions.add(new Question("Which country has won the most FIFA World Cups?", new String[]{"Germany", "Italy", "Brazil", "Argentina"}, 2, "Sports", 1, "The land of Samba."));
        allQuestions.add(new Question("In which city is the final of the UEFA Champions League 2024 held?", new String[]{"London", "Paris", "Madrid", "Munich"}, 0, "Sports", 2, "Wembley Stadium is here."));
        allQuestions.add(new Question("Who is the all-time top scorer in the Premier League?", new String[]{"Wayne Rooney", "Alan Shearer", "Harry Kane", "Thierry Henry"}, 1, "Sports", 2, "Scored 260 goals."));
        allQuestions.add(new Question("Which club has won the most Champions League titles?", new String[]{"AC Milan", "Liverpool", "Bayern Munich", "Real Madrid"}, 3, "Sports", 1, "The Kings of Europe."));
        allQuestions.add(new Question("What is the nickname of the Egyptian national soccer team?", new String[]{"The Lions", "The Pharaohs", "The Eagles", "The Warriors"}, 1, "Sports", 1, "Related to ancient history."));
        allQuestions.add(new Question("Who was the first player to win the World Cup as both a player and a manager?", new String[]{"Franz Beckenbauer", "Mario Zagallo", "Didier Deschamps", "Zinedine Zidane"}, 1, "Sports", 3, "A Brazilian legend."));
        allQuestions.add(new Question("Which country hosted the first-ever FIFA World Cup in 1930?", new String[]{"Brazil", "Uruguay", "Italy", "France"}, 1, "Sports", 2, "They also won it."));
        allQuestions.add(new Question("What is the maximum number of substitutions allowed in a standard competitive match (since 2020)?", new String[]{"3", "4", "5", "6"}, 2, "Sports", 1, "Increased due to the pandemic."));
        allQuestions.add(new Question("Who is known as the 'Special One'?", new String[]{"Pep Guardiola", "Jurgen Klopp", "Jose Mourinho", "Carlo Ancelotti"}, 2, "Sports", 1, "Portuguese manager."));
        allQuestions.add(new Question("Which player is known as 'The Phenomenon' (O Fenômeno)?", new String[]{"Ronaldinho", "Ronaldo Nazário", "Neymar Jr", "Romário"}, 1, "Sports", 2, "Legendary Brazilian striker."));
        allQuestions.add(new Question("What is the capacity of the Maracanã Stadium (approx)?", new String[]{"50,000", "78,000", "100,000", "120,000"}, 1, "Sports", 3, "Located in Rio de Janeiro."));
        allQuestions.add(new Question("Who won the Golden Boot in the 2022 World Cup?", new String[]{"Lionel Messi", "Kylian Mbappé", "Julián Álvarez", "Olivier Giroud"}, 1, "Sports", 2, "Scored a hat-trick in the final."));
        allQuestions.add(new Question("Which team is known as 'The Red Devils'?", new String[]{"Manchester United", "Liverpool", "Arsenal", "Chelsea"}, 0, "Sports", 1, "A famous English club."));
        allQuestions.add(new Question("How many minutes is a standard soccer match (excluding stoppage time)?", new String[]{"80", "90", "100", "120"}, 1, "Sports", 1, "Two halves of 45."));
        allQuestions.add(new Question("Which country won the Euro 2020 (held in 2021)?", new String[]{"England", "Italy", "Spain", "France"}, 1, "Sports", 2, "Beat England on penalties."));
        allQuestions.add(new Question("Who is the youngest player to ever score in a World Cup final?", new String[]{"Pelé", "Kylian Mbappé", "Lionel Messi", "Michael Owen"}, 0, "Sports", 2, "Did it at age 17."));
        allQuestions.add(new Question("Which club does Erling Haaland play for (as of 2024)?", new String[]{"Real Madrid", "Paris Saint-Germain", "Manchester City", "Barcelona"}, 2, "Sports", 1, "Plays in the Premier League."));
        allQuestions.add(new Question("What color card does a referee show to send a player off?", new String[]{"Yellow", "Blue", "Green", "Red"}, 3, "Sports", 1, "The color of danger."));
        allQuestions.add(new Question("Who won the first ever Premier League title in the 1992-93 season?", new String[]{"Blackburn Rovers", "Manchester United", "Arsenal", "Leeds United"}, 1, "Sports", 3, "Led by Sir Alex Ferguson."));
        allQuestions.add(new Question("Which African country was the first to reach a World Cup semi-final?", new String[]{"Cameroon", "Senegal", "Ghana", "Morocco"}, 3, "Sports", 2, "Did it in 2022."));
        allQuestions.add(new Question("Who is the all-time top scorer for the Brazil national team?", new String[]{"Pelé", "Ronaldo", "Neymar Jr", "Romário"}, 2, "Sports", 2, "Surpassed Pelé in 2023."));
        allQuestions.add(new Question("Which city is home to the club Benfica?", new String[]{"Porto", "Lisbon", "Madrid", "Barcelona"}, 1, "Sports", 2, "The capital of Portugal."));
        allQuestions.add(new Question("What is the standard distance of a penalty kick from the goal line?", new String[]{"10 yards", "11 meters", "12 yards", "15 meters"}, 1, "Sports", 2, "Usually converted into 12 yards."));
        allQuestions.add(new Question("Who managed Leicester City to their miracle Premier League win in 2016?", new String[]{"Claudio Ranieri", "Nigel Pearson", "Brendan Rodgers", "Craig Shakespeare"}, 0, "Sports", 3, "Dilly ding, dilly dong!"));
        allQuestions.add(new Question("Who is the all-time top scorer in FIFA World Cup history?", new String[]{"Miroslav Klose", "Ronaldo", "Gerd Müller", "Just Fontaine"}, 0, "Sports", 2, "Scored 16 goals for Germany."));
        allQuestions.add(new Question("Which country won the first-ever UEFA European Championship in 1960?", new String[]{"Soviet Union", "Yugoslavia", "Czechoslovakia", "France"}, 0, "Sports", 3, "Lev Yashin was their keeper."));
        allQuestions.add(new Question("How many minutes are played in extra time in a soccer knockout match?", new String[]{"15", "20", "30", "45"}, 2, "Sports", 1, "Two periods of 15."));
        allQuestions.add(new Question("Which club is known as 'The Old Lady'?", new String[]{"Juventus", "Inter Milan", "Roma", "Lazio"}, 0, "Sports", 2, "Italian giants from Turin."));
        allQuestions.add(new Question("Who was the first player to reach 100 goals in the UEFA Champions League?", new String[]{"Lionel Messi", "Cristiano Ronaldo", "Raúl", "Robert Lewandowski"}, 1, "Sports", 2, "Mr. Champions League."));
        allQuestions.add(new Question("What is the nickname of the Arsenal soccer club?", new String[]{"The Gunners", "The Blues", "The Spurs", "The Hammers"}, 0, "Sports", 1, "Founded by workers at a Royal Arsenal."));
        allQuestions.add(new Question("Which country hosted the 2010 FIFA World Cup?", new String[]{"South Africa", "Brazil", "Germany", "South Korea"}, 0, "Sports", 1, "The first time in Africa."));
        allQuestions.add(new Question("Who is the most expensive soccer player of all time (transfer fee)?", new String[]{"Kylian Mbappé", "Neymar Jr", "Philippe Coutinho", "Jack Grealish"}, 1, "Sports", 2, "Transferred to PSG in 2017."));
        allQuestions.add(new Question("Which team won the 2023 Women's World Cup?", new String[]{"England", "USA", "Spain", "Sweden"}, 2, "Sports", 2, "Beat England in the final."));
        allQuestions.add(new Question("In soccer, what does VAR stand for?", new String[]{"Video Assistant Referee", "Visual Aid Review", "Variable Action Result", "Video Answer Referee"}, 0, "Sports", 1, "Technology used to review decisions."));
        allQuestions.add(new Question("Which country won the FIFA World Cup in 2018 in Russia?", new String[]{"Croatia", "France", "Brazil", "Germany"}, 1, "Sports", 1, "Mbappé starred in the final."));
        allQuestions.add(new Question("Who has won the most Wimbledon men's singles titles (8 titles)?", new String[]{"Rafael Nadal", "Roger Federer", "Novak Djokovic", "Pete Sampras"}, 1, "Sports", 1, "Swiss tennis legend."));
        allQuestions.add(new Question("How many points is a touchdown worth in American Football?", new String[]{"3", "6", "7", "8"}, 1, "Sports", 1, "Excludes extra point kick."));
        allQuestions.add(new Question("In which strategic board game is the term 'checkmate' used?", new String[]{"Chess", "Polo", "Fencing", "Cricket"}, 0, "Sports", 1, "Ancient board game."));
        allQuestions.add(new Question("Which NBA franchise is tied with Celtics for most titles (17 championships)?", new String[]{"Chicago Bulls", "LA Lakers", "Golden State Warriors", "Miami Heat"}, 1, "Sports", 1, "Based in Los Angeles."));
        allQuestions.add(new Question("What is the maximum possible score in a single game of ten-pin bowling?", new String[]{"200", "300", "400", "500"}, 1, "Sports", 2, "12 consecutive strikes."));
        allQuestions.add(new Question("Which city hosted the 2016 Summer Olympic Games?", new String[]{"London", "Beijing", "Rio de Janeiro", "Tokyo"}, 2, "Sports", 1, "First time in South America."));
        allQuestions.add(new Question("In golf, what is the term for scoring one stroke under par on a hole?", new String[]{"Eagle", "Birdie", "Bogey", "Albatross"}, 1, "Sports", 1, "Feathered score term."));
        allQuestions.add(new Question("Who won the FIFA Men's Best Player award for 2023?", new String[]{"Kylian Mbappé", "Lionel Messi", "Erling Haaland", "Karim Benzema"}, 1, "Sports", 1, "Won World Cup 2022."));
        allQuestions.add(new Question("How many dimples are typically on a standard golf ball?", new String[]{"100-200", "300-500", "600-800", "1000+"}, 1, "Sports", 2, "Helps aerodynamics."));
        allQuestions.add(new Question("Which country won the ICC Men's Cricket World Cup 2023?", new String[]{"India", "Australia", "England", "South Africa"}, 1, "Sports", 1, "Beat India in final."));
        allQuestions.add(new Question("In Formula 1 racing, which team is famously known as the 'Prancing Horse'?", new String[]{"Mercedes", "Ferrari", "Red Bull", "McLaren"}, 1, "Sports", 1, "Iconic Italian red team."));
        allQuestions.add(new Question("How many major Grand Slam tournaments are held in tennis each year?", new String[]{"2", "3", "4", "5"}, 2, "Sports", 1, "Australian, French, Wimbledon, US."));
        allQuestions.add(new Question("Which legendary heavyweight boxer was nicknamed 'The Greatest'?", new String[]{"Mike Tyson", "Muhammad Ali", "Joe Frazier", "George Foreman"}, 1, "Sports", 1, "Float like a butterfly, sting like a bee."));
        allQuestions.add(new Question("What is the traditional national sport of Japan?", new String[]{"Judo", "Sumo Wrestling", "Karate", "Kendo"}, 1, "Sports", 1, "Ancient heavy wrestling."));

        questionList = new ArrayList<>();
        for (Question q : allQuestions) {
            if (q.getCategory().equalsIgnoreCase(category)) {
                questionList.add(q);
            }
        }
        Collections.shuffle(questionList);

        // Pick 20 questions for each quiz round
        int limit = Math.min(questionList.size(), 20);
        questionList = new ArrayList<>(questionList.subList(0, limit));
    }

    private void showNextQuestion() {
        btnNext.setVisibility(View.GONE);
        btnNext.setEnabled(true);
        tvResultPlaceholder.setText("");
        resetOptionButtons();

        for (MaterialButton btn : btnOptions) {
            btn.setVisibility(View.VISIBLE);
            btn.setAlpha(1.0f);
        }

        Question currentQuestion = questionList.get(currentQuestionIndex);
        tvQuestion.setText(currentQuestion.getQuestion());
        
        cardQuestion.startAnimation(fadeIn);
        
        String[] options = currentQuestion.getOptions();
        for (int i = 0; i < 4; i++) {
            btnOptions[i].setText(options[i]);
            btnOptions[i].setEnabled(true);
            btnOptions[i].startAnimation(fadeIn);
        }

        currentQuestionIndex++;
        timeLeftInMillis = COUNTDOWN_IN_MILLIS;
        startCountDown();
    }

    private void startCountDown() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                
                if (timeLeftInMillis <= 6000) { // Starting from 5 seconds remaining (approx)
                    if (tickPlayer != null && !tickPlayer.isPlaying()) {
                        tickPlayer.start();
                    }
                }
            }
            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountDownText();
                if (tickPlayer != null && tickPlayer.isPlaying()) tickPlayer.pause();
                checkAnswer(-1);
            }
        }.start();
    }

    private void updateCountDownText() {
        int seconds = (int) (timeLeftInMillis / 1000);
        tvTimer.setText(getString(R.string.timer_format, seconds));
        tvTimer.setTextColor(timeLeftInMillis < 10000 ? Color.RED : ContextCompat.getColor(this, R.color.secondaryColor));
    }

    private void checkAnswer(int selectedOptionIndex) {
        if (countDownTimer != null) countDownTimer.cancel();
        if (tickPlayer != null && tickPlayer.isPlaying()) tickPlayer.pause();
        
        Question currentQuestion = questionList.get(currentQuestionIndex - 1);
        int correctIndex = currentQuestion.getCorrectAnswerIndex();

        for (MaterialButton btn : btnOptions) btn.setEnabled(false);

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if (selectedOptionIndex == correctIndex) {
            currentStreak++;
            if (currentStreak > maxStreakInSession) {
                maxStreakInSession = currentStreak;
            }
            if (tvStreak != null) {
                tvStreak.setText("🔥 " + currentStreak + "x");
                tvStreak.startAnimation(scaleUp);
            }

            int streakBonus = currentStreak >= 3 ? 2 : 1;
            score += streakBonus;

            tvScore.setText(getString(R.string.score_format, score));
            if (correctPlayer != null) correctPlayer.start();
            if (vibrator != null) vibrator.vibrate(50);

            highlightButton(selectedOptionIndex, R.color.correct);
            tvResultPlaceholder.setText(currentStreak >= 3 ? "Combo! +" + streakBonus + " pts" : "Correct!");
            tvResultPlaceholder.setTextColor(ContextCompat.getColor(this, R.color.correct));
            tvScore.startAnimation(scaleUp);

            if ("TIME_ATTACK".equals(gameMode)) {
                timeLeftInMillis += 3000;
                Toast.makeText(this, "+3s Time Bonus!", Toast.LENGTH_SHORT).show();
            }
        } else {
            currentStreak = 0;
            if (tvStreak != null) tvStreak.setText("🔥 0x");

            if (wrongPlayer != null) wrongPlayer.start();
            if (vibrator != null) vibrator.vibrate(200);

            if (selectedOptionIndex != -1) {
                highlightButton(selectedOptionIndex, R.color.wrong);
                tvResultPlaceholder.setText("Wrong!");
            } else {
                tvResultPlaceholder.setText("Time's Up!");
            }
            highlightButton(correctIndex, R.color.correct);
            tvResultPlaceholder.setTextColor(ContextCompat.getColor(this, R.color.wrong));

            if ("SURVIVAL".equals(gameMode)) {
                lives--;
                updateLivesDisplay();
                if (lives <= 0) {
                    tvResultPlaceholder.setText("Out of Lives! Game Over!");
                    new Handler().postDelayed(this::finishQuiz, 1500);
                    return;
                }
            }
        }

        btnNext.setVisibility(View.VISIBLE);
        btnNext.setText(currentQuestionIndex == questionList.size() ? getString(R.string.finish) : getString(R.string.next_question));

        if (selectedOptionIndex == -1 && !"SURVIVAL".equals(gameMode)) {
            btnNext.setEnabled(false);
            new Handler().postDelayed(() -> {
                if (currentQuestionIndex < questionList.size()) {
                    showNextQuestion();
                } else {
                    finishQuiz();
                }
            }, 2000);
        }
    }

    private void finishQuiz() {
        updateAchievements(playerName, score);
        saveScore(playerName, score, category);
        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putExtra("PLAYER_NAME", playerName);
        intent.putExtra("SCORE", score);
        intent.putExtra("TOTAL", questionList.size());
        intent.putExtra("CATEGORY", category);
        intent.putExtra("GAME_MODE", gameMode);
        intent.putExtra("MAX_STREAK", maxStreakInSession);
        startActivity(intent);
        finish();
    }

    private void updateAchievements(String name, int score) {
        SharedPreferences prefs = getSharedPreferences("QuizAchievements", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int totalQuestionsAnswered = prefs.getInt("total_questions", 0) + questionList.size();
        int totalGames = prefs.getInt("total_games", 0) + 1;
        int prevMaxStreak = prefs.getInt("max_streak", 0);
        int totalCorrect = prefs.getInt("total_correct", 0) + score;

        editor.putInt("total_questions", totalQuestionsAnswered);
        editor.putInt("total_games", totalGames);
        editor.putInt("total_correct", totalCorrect);
        if (maxStreakInSession > prevMaxStreak) {
            editor.putInt("max_streak", maxStreakInSession);
        }

        if (totalQuestionsAnswered >= 10 && !prefs.getBoolean("badge_beginner", false)) {
            editor.putBoolean("badge_beginner", true);
            Toast.makeText(this, "Badge Earned: Quiz Beginner! 🧠", Toast.LENGTH_LONG).show();
        }

        if (score >= 20 && !prefs.getBoolean("badge_master", false)) {
            editor.putBoolean("badge_master", true);
            Toast.makeText(this, "Badge Earned: Quiz Master! 🏆", Toast.LENGTH_LONG).show();
        }

        if (maxStreakInSession >= 5 && !prefs.getBoolean("badge_on_fire", false)) {
            editor.putBoolean("badge_on_fire", true);
            Toast.makeText(this, "Badge Earned: On Fire! 🔥", Toast.LENGTH_LONG).show();
        }

        if ("SURVIVAL".equals(gameMode) && score >= 10 && !prefs.getBoolean("badge_survivor", false)) {
            editor.putBoolean("badge_survivor", true);
            Toast.makeText(this, "Badge Earned: Survivor! 💣", Toast.LENGTH_LONG).show();
        }

        // Award XP
        SharedPreferences settings = getSharedPreferences("QuizSettings", MODE_PRIVATE);
        int currentXp = settings.getInt("player_xp", 0);
        int earnedXp = (score * 15) + (maxStreakInSession * 10);
        settings.edit().putInt("player_xp", currentXp + earnedXp).apply();

        // Check for level unlock
        if (score >= 14 && level == 1) { // 70% to unlock next level
            settings.edit().putInt("unlocked_level", 2).apply();
            Toast.makeText(this, "Level 2 Unlocked! 🌟", Toast.LENGTH_LONG).show();
        }

        editor.apply();
    }

    private void highlightButton(int index, int colorResId) {
        btnOptions[index].setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));
        btnOptions[index].setTextColor(Color.WHITE);
    }

    private void resetOptionButtons() {
        for (MaterialButton btn : btnOptions) {
            btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.optionColor)));
            btn.setTextColor(Color.WHITE);
        }
    }

    private void saveScore(String name, int scoreValue, String category) {
        SharedPreferences sharedPreferences = getSharedPreferences("QuizScores", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("scores", null);
        Type type = new TypeToken<ArrayList<Score>>() {}.getType();
        List<Score> scoreList = gson.fromJson(json, type);

        if (scoreList == null) scoreList = new ArrayList<>();
        
        scoreList.add(new Score(name, scoreValue, category));
        Collections.sort(scoreList);

        if (scoreList.size() > 50) { // Keep more records since they are categorized
            scoreList = scoreList.subList(0, 50);
        }

        String newJson = gson.toJson(scoreList);
        sharedPreferences.edit().putString("scores", newJson).apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
        if (tickPlayer != null && tickPlayer.isPlaying()) {
            tickPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            SharedPreferences prefs = getSharedPreferences("QuizSettings", MODE_PRIVATE);
            if (prefs.getBoolean("music_enabled", true)) {
                backgroundMusic.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (correctPlayer != null) correctPlayer.release();
        if (wrongPlayer != null) wrongPlayer.release();
        if (tickPlayer != null) tickPlayer.release();
        if (backgroundMusic != null) {
            backgroundMusic.release();
        }
    }
}