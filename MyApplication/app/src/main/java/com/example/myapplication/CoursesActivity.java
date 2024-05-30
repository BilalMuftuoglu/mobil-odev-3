package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoursesActivity extends AppCompatActivity implements CourseRecyclerViewAdapter.ItemClickListener, CourseRecyclerViewAdapter.ItemLongClickListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    RecyclerView recyclerView;
    Spinner termSpinner;
    Spinner statusSpinner;

    String accountType;
    String email;

    CourseRecyclerViewAdapter courseRecyclerViewAdapter;
    ArrayList<Map<String,String>> courses;
    ArrayList<Map<String,String>> filteredCourses;
    ArrayList<String> courseIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);

        setTitle("Dersler");
        courses = new ArrayList<>();
        filteredCourses = new ArrayList<>();

        termSpinner = findViewById(R.id.termSpinner);
        statusSpinner = findViewById(R.id.statusSpinner);

        String[] terms = new String[]{"Hepsi","2023-2024 Güz","2023-2024 Bahar","2023-2024 Yaz","2024-2025 Güz","2024-2025 Bahar","2024-2025 Yaz"};

        ArrayAdapter<String> termAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,terms);
        termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        termSpinner.setAdapter(termAdapter);

        String[] status = new String[]{"Hepsi","Tamamlandı","Devam Ediyor"};

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,status);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        courseRecyclerViewAdapter = new CourseRecyclerViewAdapter(getApplicationContext(),filteredCourses);
        courseRecyclerViewAdapter.setClickListener(this);
        courseRecyclerViewAdapter.setLongClickListener(this);
        recyclerView.setAdapter(courseRecyclerViewAdapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = mAuth.getCurrentUser().getEmail();
        if(email.endsWith("@yildiz.edu.tr")){
            accountType = "instructors";
        }else{
            accountType = "students";
        }

        termSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                filterCourses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                filterCourses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        courses.clear();
        filteredCourses.clear();
        fetchData();
    }

    private void filterCourses() {
        filteredCourses.clear();

        String selectedTerm = termSpinner.getSelectedItem().toString();
        String selectedStatus = statusSpinner.getSelectedItem().toString();

        System.out.println(selectedTerm);
        System.out.println(selectedStatus);
        if(selectedStatus.equals("Tamamlandı")){
            selectedStatus = "true";
        }else if(selectedStatus.equals("Devam Ediyor")){
            selectedStatus = "false";
        }

        for (Map<String,String> course : courses) {
            if ((selectedTerm.equals("Hepsi") || course.get("term").toString().equals(selectedTerm)) && (selectedStatus.equals("Hepsi") || course.get("isCompleted").toString().equals(selectedStatus))){
                filteredCourses.add(course);
            }
        }

        courseRecyclerViewAdapter.notifyDataSetChanged();
    }


    private int compareStringsInArray(String s1, String s2) {
        String[] terms = new String[]{"2023-2024 Güz","2023-2024 Bahar","2023-2024 Yaz","2024-2025 Güz","2024-2025 Bahar","2024-2025 Yaz"};

        int index1 = -1;
        int index2 = -1;

        for (int i = 0; i < terms.length; i++) {
            if (terms[i].equals(s1)) {
                index1 = i;
            }
            if (terms[i].equals(s2)) {
                index2 = i;
            }
            if (index1 != -1 && index2 != -1) {
                break;
            }
        }

        if (index1 == -1 || index2 == -1) {
            // Geçersiz terimler
            return 0;
        }

        return Integer.compare(index2, index1);
    }


    public void fetchData(){

        db.collection("courses").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                Collections.sort(documents, new Comparator<DocumentSnapshot>() {
                    @Override
                    public int compare(DocumentSnapshot o1, DocumentSnapshot o2) {
                        return compareStringsInArray(o1.getString("term"), o2.getString("term"));
                    }
                });

                //remove all subscriptions
                SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                int size = sharedPreferences.getInt("courseIdsSize",0);
                for(int i=0;i<size;i++){
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(sharedPreferences.getString("courseId"+i,""));
                    editor.remove("courseId"+i);
                }
                editor.apply();

                if(accountType.equals("students")){
                    courseIds = new ArrayList<>();

                    db.collection("course-student").whereEqualTo("studentEmail",email).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                            for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                courseIds.add(doc.getString("courseId"));
                            }

                            for(DocumentSnapshot doc: documents) {
                                Map<String, String> course = new HashMap<>();
                                course.put("courseName",doc.getString("courseName"));
                                course.put("courseId",doc.getString("courseId"));
                                course.put("isCompleted",doc.get("isCompleted").toString());
                                course.put("term",doc.getString("term"));

                                for(String id: courseIds){
                                    if(id.equals(doc.getString("courseId"))){
                                        FirebaseMessaging.getInstance().subscribeToTopic(id);
                                        courses.add(course);
                                        filteredCourses.add(course);
                                    }
                                }
                            }
                            courseRecyclerViewAdapter.notifyDataSetChanged();

                            //save all course ids to shared preferences
                            editor.putInt("courseIdsSize", courses.size());
                            for (int i = 0; i < courses.size(); i++) {
                                editor.putString("courseId" + i, courses.get(i).get("courseId"));
                            }
                            editor.apply();
                        }
                    });
                }else{
                    for(DocumentSnapshot doc: documents) {

                        int numberOfGroups = (int) (long) doc.get("numberOfGroups");

                        for(int i=1;i<=numberOfGroups;i++){
                            if(email.equals(doc.getString("Gr"+i))){
                                Map<String, String> course = new HashMap<>();
                                course.put("courseName",doc.getString("courseName"));
                                course.put("courseId",doc.getString("courseId"));
                                course.put("isCompleted",doc.get("isCompleted").toString());
                                course.put("term",doc.getString("term"));
                                FirebaseMessaging.getInstance().subscribeToTopic(doc.getString("courseId"));
                                courses.add(course);
                                filteredCourses.add(course);
                                break;
                            }
                        }
                    }

                    editor.putInt("courseIdsSize", courses.size());
                    for (int i = 0; i < courses.size(); i++) {
                        editor.putString("courseId" + i, courses.get(i).get("courseId"));
                    }
                    editor.apply();

                    courseRecyclerViewAdapter.notifyDataSetChanged();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.courses_page_menu, menu);

        if(!accountType.equals("instructors")){
            menu.removeItem(R.id.addCourseButton);
        }else if(!accountType.equals("students")){
            menu.removeItem(R.id.reportButton);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.addCourseButton){
            Intent intent = new Intent(CoursesActivity.this, CourseDetailsActivity.class);
            intent.putExtra("isFromRecycler",false);
            startActivity(intent);
        }else if(id == R.id.reportButton){
            Intent intent = new Intent(CoursesActivity.this, ReportActivity.class);
            intent.putExtra("isFromCourse",false);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View view, int position) {
        Intent intent = new Intent(CoursesActivity.this, CourseDetailsActivity.class);
        intent.putExtra("courseId", courseRecyclerViewAdapter.getItem(position).get("courseId"));
        intent.putExtra("accountType",accountType);
        intent.putExtra("isFromRecycler",true);
        startActivity(intent);
    }


    @Override
    public void onItemLongClick(View view, int position) {
        /*if(accountType.equals("students")){
            Intent intent = new Intent(CoursesActivity.this, ReportActivity.class);
            intent.putExtra("isFromCourse",true);
            intent.putExtra("courseId",courses.get(position).get("courseId"));
            startActivity(intent);
        }*/
        Intent intent = new Intent(CoursesActivity.this, ClassroomActivity.class);
        intent.putExtra("accountType",accountType);
        intent.putExtra("courseId",courses.get(position).get("courseId"));
        startActivity(intent);
    }
}