package com.example.quizit;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuizFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "QUIZ_FRAGMENT_LOG";

    private NavController navController;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private String quizName = "Space Science";
    private String quizId;

    //UI elements
    private TextView quizTitle;
    private Button optionOneBtn;
    private Button optionTwoBtn;
    private Button optionThreeBtn;
    private Button nextBtn;
    private ImageButton closeBtn;
    private TextView questionFeedback;
    private TextView questionText;
    private TextView questionTime;
    private ProgressBar questionProgress;
    private TextView questionNumber;

    //Firebase data
    private List<QuestionsModel> allQuestionsList = new ArrayList<>();
    private long totalQuestionsToAnswer = 0L;
    private List<QuestionsModel> questionsToAnswer = new ArrayList<>();
    private CountDownTimer countDownTimer;

    private boolean canAnswer = false;
    private int currentQuestion = 0;
    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private int notAnswered = 0;

    public QuizFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() != null){
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            //go back to home page
        }

        //Initialize firestore
        firebaseFirestore = FirebaseFirestore.getInstance();

        //UI initialize
        quizTitle = view.findViewById(R.id.quiz_title);
        optionOneBtn = view.findViewById(R.id.quiz_option_one);
        optionTwoBtn = view.findViewById(R.id.quiz_option_two);
        optionThreeBtn = view.findViewById(R.id.quiz_option_three);
        nextBtn = view.findViewById(R.id.quiz_next_btn);
        questionFeedback = view.findViewById(R.id.quiz_question_feedback);
        questionText = view.findViewById(R.id.quiz_question);
        questionTime = view.findViewById(R.id.quiz_question_time);
        questionProgress = view.findViewById(R.id.quiz_question_progress);
        questionNumber = view.findViewById(R.id.quiz_question_number);

        //get quizId
        quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizId();
        //quizName = QuizFragmentArgs.fromBundle(getArguments()).getQuizName();
        totalQuestionsToAnswer = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();

        // Get all questions from the quizList in firestore
        firebaseFirestore.collection("QuizList").document(quizId)
                .collection("Questions").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    //Add all questions to list
                    allQuestionsList = task.getResult().toObjects(QuestionsModel.class);
                    pickQuestions();
                    loadUI();
                }else {
                    //Error message
                    quizTitle.setText("Error : " + task.getException().getMessage());
                }
            }
        });
        optionOneBtn.setOnClickListener(this);
        optionTwoBtn.setOnClickListener(this);
        optionThreeBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);

    }

    private void loadUI() {
        quizTitle.setText(quizName);
        questionText.setText("Load first question");

        enableOptions();

        loadQuestion(1);
    }

    private void loadQuestion(int questionNum) {
        questionNumber.setText(questionNum);

        questionText.setText(questionsToAnswer.get(questionNum-1).getQuestion());

        optionOneBtn.setText(questionsToAnswer.get(questionNum-1).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questionNum-1).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questionNum-1).getOption_c());

        canAnswer = true;
        currentQuestion = questionNum;
        startTimer(questionNum);
    }

    private void startTimer(int questionNumber) {
        //Set timer
        Long timeToAnswer = questionsToAnswer.get(questionNumber-1).getTimer();
        questionTime.setText(timeToAnswer.toString());

        questionProgress.setVisibility(View.VISIBLE);

        //Start timer
        countDownTimer = new CountDownTimer(timeToAnswer*1000, 10){
            @Override
            public void onTick(long millisUntilFinished) {
                //update timer
                questionTime.setText(millisUntilFinished/1000 + "");

                //progress in percent
                Long percent = millisUntilFinished/(timeToAnswer*10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                //Time up, cannot answer anymore
                canAnswer = false;

                questionFeedback.setText("Time's up! No Answer was submitted");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary, null));
                notAnswered++;
                showNextBtn();
            }
        };
        countDownTimer.start();
    }

    private void enableOptions() {
        optionOneBtn.setVisibility(View.VISIBLE);
        optionTwoBtn.setVisibility(View.VISIBLE);
        optionThreeBtn.setVisibility(View.VISIBLE);
        optionOneBtn.setEnabled(true);
        optionTwoBtn.setEnabled(true);
        optionThreeBtn.setEnabled(true);

        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void pickQuestions() {
        for (int i=0; i < totalQuestionsToAnswer; i++){
            int randomNumber = getRandomInteger(allQuestionsList.size(), 0);
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.remove(randomNumber);

            Log.d(TAG, "Questions " + i + " : " + questionsToAnswer.get(i).getQuestion());
        }
    }

    public static int getRandomInteger(int maximum, int minimum){
        return ((int) (Math.random()*(maximum - minimum))) + minimum;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.quiz_option_one:
                verifyAnswer(optionOneBtn);
                break;
            case R.id.quiz_option_two:
                verifyAnswer(optionTwoBtn);
                break;
            case R.id.quiz_option_three:
                verifyAnswer(optionThreeBtn);
                break;
            case R.id.quiz_next_btn:
                if(currentQuestion == totalQuestionsToAnswer) {
                    //Load results
                    submitResults();
                }else {
                    currentQuestion++;
                    loadQuestion(currentQuestion);
                    resetOptions();
                }
                break;
        }
    }

    private void submitResults() {
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("Correct", correctAnswers);
        resultMap.put("Wrong", wrongAnswers);
        resultMap.put("Unanswered", notAnswered);

        firebaseFirestore.collection("QuizList").document(quizId).collection("Results")
                .document(currentUserId).set(resultMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    //go to results page
                    QuizFragmentDirections.ActionQuizFragmentToResultFragment action = QuizFragmentDirections.actionQuizFragmentToResultFragment();
                    action.setQuizId(quizId);
                    navController.navigate(action);
                }else {
                    //show error
                    quizTitle.setText(task.getException().getMessage());
                }
            }
        });
    }

    private void resetOptions() {
        optionOneBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionTwoBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionThreeBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));

        optionOneBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));
        optionTwoBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));
        optionThreeBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));

        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void verifyAnswer(Button selectedAnswerBtn) {
        if(canAnswer) {
            selectedAnswerBtn.setTextColor(getResources().getColor(R.color.colorDark, null));
            if(questionsToAnswer.get(currentQuestion-1).getAnswer().equals(selectedAnswerBtn.getText())){
                //Correct
                correctAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.correct_answer_btn_bg, null));

                questionFeedback.setText("Correct Answer");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            }else {
                //Wrong
                wrongAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.wrong_answer_btn_bg, null));

                questionFeedback.setText("Wrong Answer /n /n Correct Answer : " + questionsToAnswer.get(currentQuestion-1).getAnswer());
                questionFeedback.setTextColor(getResources().getColor(R.color.colorAccent, null));
            }
            canAnswer = false;

            countDownTimer.cancel();

            showNextBtn();
        }
    }

    private void showNextBtn() {
        if(currentQuestion == totalQuestionsToAnswer) {
            nextBtn.setText("Submit results");
        }
        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);
        nextBtn.setEnabled(true);
    }
}