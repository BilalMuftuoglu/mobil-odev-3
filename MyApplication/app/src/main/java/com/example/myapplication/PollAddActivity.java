package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PollAddActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String courseID, UserID, accountType;
    private EditText editTextQuestion, editTextOption1, editTextOption2;
    private LinearLayout layoutOptions;
    private Button buttonAddOption, buttonAddPoll, deleteButton;

    private int optionCount = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_add);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        UserID = auth.getCurrentUser().getUid();

        Bundle extras = getIntent().getExtras();
        courseID = extras.getString("courseId");
        accountType = extras.getString("accountType");
        // Initialize views
        editTextQuestion = findViewById(R.id.editTextQuestion);
        editTextOption1 = findViewById(R.id.editTextOption1);
        editTextOption2 = findViewById(R.id.editTextOption2);
        layoutOptions = findViewById(R.id.layoutOptions);
        buttonAddOption = findViewById(R.id.buttonAddOption);
        buttonAddPoll = findViewById(R.id.buttonAddPoll);
        deleteButton = findViewById(R.id.buttonDeleteOption);

        deleteButton.setVisibility(View.GONE);

        buttonAddOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOptionEditText();
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOptionEditText();
            }
        });
        buttonAddPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPoll(optionCount);
            }
        });

    }
    private void addOptionEditText() {
        EditText editText = new EditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setHint("Option " + (optionCount + 1));

        editText.setBackground(getDrawable(R.drawable.my_edit_text));
        editText.setPadding(20,0,0,0);
        editText.setHeight(120);
        editText.setTag("editTextOption" + (optionCount + 1));
        //add margin to the editText
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) editText.getLayoutParams();
        params.setMargins(0, 20, 0, 0); //substitute parameters for left, top, right, bottom

        layoutOptions.addView(editText);
        optionCount++;
        deleteButtonVisibilty();
    }
    //deleteOptionEditText() method
    private void deleteOptionEditText() {
        if (optionCount > 2) {
            //delete the editText with the "editTextOption" + optionCount tag
            EditText editText = layoutOptions.findViewWithTag("editTextOption" + optionCount);
            layoutOptions.removeView(editText);
            optionCount--;
            deleteButtonVisibilty();
        }
    }

    private void deleteButtonVisibilty() {
        if (optionCount == 2) {
            deleteButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    private void addPoll(int optionCount){

        String question = editTextQuestion.getText().toString();
        String[] options = new String[optionCount];
        options[0] = editTextOption1.getText().toString();
        options[1] = editTextOption2.getText().toString();
        if(optionCount > 2){
            for(int i = 2; i < optionCount; i++){
                EditText editText = layoutOptions.findViewWithTag("editTextOption" + (i + 1));
                options[i] = editText.getText().toString();
            }
        }


        Map<String, Object> poll = new HashMap<>();
        poll.put("question", question);
        for(int i = 0; i < optionCount; i++){
            poll.put("option" + (i + 1), options[i]);
            poll.put("option" + (i + 1) + "Vote", 0);
        }
        poll.put("optionCount", optionCount);
        poll.put("totalVotes", 0);
        poll.put("email", user.getEmail());
        poll.put("status", true);
        poll.put("courseId", courseID);
        poll.put("date", FieldValue.serverTimestamp());

        db.collection("polls").add(poll).addOnSuccessListener(aVoid -> {
            Intent intent = new Intent(PollAddActivity.this, PollListActivity.class);
            intent.putExtra("courseId", courseID);
            intent.putExtra("accountType",accountType);
            startActivity(intent);
            finish();
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PollAddActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


}