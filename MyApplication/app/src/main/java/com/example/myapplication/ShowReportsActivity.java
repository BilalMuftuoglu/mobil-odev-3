package com.example.myapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShowReportsActivity extends AppCompatActivity {

    ReportRecyclerViewAdapter reportsAdapter;
    RecyclerView reportsRecyclerView;
    ArrayList<Map<String, Object>> reportList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_reports);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = mAuth.getCurrentUser().getEmail();

        reportList = new ArrayList<>();

        fetchData();
        setTitle("Bildiriler");

        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        reportsRecyclerView.setHasFixedSize(true);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        reportsAdapter = new ReportRecyclerViewAdapter(getApplicationContext(),reportList);
        reportsRecyclerView.setAdapter(reportsAdapter);
    }

    public void fetchData(){

        String scope;
        if(email.endsWith("@yildiz.edu.tr")){
            scope = "Ders";
        }else{
            scope = "Uygulama";
        }

        db.collection("reports").whereEqualTo("scope",scope).orderBy("date", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                    Map<String, Object> report = new HashMap<>();
                    report.put("body",doc.getString("body"));
                    report.put("subject",doc.getString("subject"));

                    Date date = doc.getTimestamp("date").toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                    String formattedDate = sdf.format(date);
                    report.put("date",formattedDate);

                    report.put("sentBy",doc.getString("sentBy"));
                    report.put("scope",doc.getString("scope"));

                    if(scope.equals("Ders")){
                        db.collection("courses").whereEqualTo("courseId",doc.getString("courseId")).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                    int numberOfGroups = (int)(long) doc.get("numberOfGroups");
                                    for(int i = 1; i<= numberOfGroups; i++){
                                        if (email.equals(doc.getString("Gr"+i))) {
                                            report.put("courseId",doc.getString("courseId"));
                                            reportList.add(report);
                                            reportsAdapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                            }
                        });
                    }else{
                        reportList.add(report);
                        reportsAdapter.notifyDataSetChanged();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            }
        });

    }
}