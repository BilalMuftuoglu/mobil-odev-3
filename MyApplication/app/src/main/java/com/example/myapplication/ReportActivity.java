package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    Spinner scopeSpinner;
    EditText courseIdText, subjectText, bodyText;
    Button sendButton;
    LinearLayout courseIdLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    Boolean isFromCourse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        scopeSpinner = findViewById(R.id.reportScopeSpinner);
        courseIdText = findViewById(R.id.courseIdText);
        subjectText = findViewById(R.id.subjectText);
        bodyText = findViewById(R.id.bodyText);
        sendButton = findViewById(R.id.sendButton);
        courseIdLayout = findViewById(R.id.courseIdLayout);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Bundle extras = getIntent().getExtras();
        isFromCourse = extras.getBoolean("isFromCourse");

        String[] scopes = new String[]{"Uygulama","Ders"};
        ArrayAdapter<String> scopeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,scopes);
        scopeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scopeSpinner.setAdapter(scopeAdapter);

        if (isFromCourse){
            courseIdText.setText(extras.getString("courseId"));
            courseIdText.setEnabled(false);
            scopeSpinner.setSelection(1);
            scopeSpinner.setEnabled(false);
        }else{
            scopeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if(i == 0){
                        courseIdLayout.setVisibility(View.INVISIBLE);
                    }else{
                        courseIdLayout.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }



        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendReport();
            }
        });

    }

    public void sendReport(){
        String courseId = courseIdText.getText().toString().trim();
        String scope = scopeSpinner.getSelectedItem().toString();
        String subject = subjectText.getText().toString().trim();
        String body = bodyText.getText().toString().trim();
        if(courseId.isEmpty() && scope.equals("Ders")){
            Toast.makeText(getApplicationContext(),"Lütfen ders kodu giriniz!",Toast.LENGTH_LONG).show();
        }else if(subject.isEmpty()){
            Toast.makeText(getApplicationContext(),"Lütfen konu giriniz!",Toast.LENGTH_LONG).show();
        }else if(body.isEmpty()){
            Toast.makeText(getApplicationContext(),"Lütfen bildiri nedeni giriniz!",Toast.LENGTH_LONG).show();
        }else{
            if(scope.equals("Ders")){
                db.collection("courses").whereEqualTo("courseId",courseId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if(queryDocumentSnapshots.getDocumentChanges().isEmpty()){
                            Toast.makeText(getApplicationContext(),"Bu ders koduna ait bir ders bulunamadı!",Toast.LENGTH_LONG).show();
                        }else{
                            for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                int numberOfGroups = (int)(long) doc.get("numberOfGroups");

                                Map<String,Object> report = new HashMap<>();
                                report.put("courseId",courseId);
                                report.put("scope",scope);
                                report.put("subject",subject);
                                report.put("body",body);
                                report.put("sentBy",mAuth.getCurrentUser().getEmail());
                                report.put("date", FieldValue.serverTimestamp());
                                db.collection("reports").add(report).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        finish();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(),e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                                    }
                                });

                                //Send email with intent to all instructors in the course
                                ArrayList<String> emails = new ArrayList<>();

                                for(int i = 1; i<= numberOfGroups; i++){
                                    String email = doc.getString("Gr"+i);
                                    emails.add(email);
                                }

                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("message/rfc822");
                                intent.putExtra(Intent.EXTRA_EMAIL, emails.toArray(new String[0]));
                                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                                intent.putExtra(Intent.EXTRA_TEXT, courseId + " - " + body);
                                startActivity(Intent.createChooser(intent, "Send Email"));
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                    }
                });
            }else{
                Map<String,Object> report = new HashMap<>();
                report.put("scope",scope);
                report.put("subject",subject);
                report.put("body",body);
                report.put("sentBy",mAuth.getCurrentUser().getEmail());
                report.put("date", FieldValue.serverTimestamp());

                db.collection("reports").add(report).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                    }
                });

                //Send email to admin
                String adminEmail = "bilalmuftuoglu@hotmail.com";
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{adminEmail});
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(intent, "Send Email"));
            }

        }
    }
}