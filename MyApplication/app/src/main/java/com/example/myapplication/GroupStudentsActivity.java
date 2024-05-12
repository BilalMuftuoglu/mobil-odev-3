package com.example.myapplication;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupStudentsActivity extends AppCompatActivity {

    ListView listView;
    Button addStudentButton;
    Button addStudentFromFileButton;

    ArrayList<String> emails;
    ArrayAdapter<String> myAdapter;
    Boolean isAutherized;
    String courseId;
    String instructorEmail;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_students);

        db = FirebaseFirestore.getInstance();

        Bundle extras = getIntent().getExtras();
        isAutherized = extras.getBoolean("isAutherized");
        courseId = extras.getString("courseId");
        instructorEmail = extras.getString("instructorEmail");

        listView = findViewById(R.id.listView);
        addStudentButton = findViewById(R.id.addStudentButton);
        addStudentFromFileButton = findViewById(R.id.addStudentFromFileButton);

        emails = new ArrayList<>();

        myAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,emails);
        listView.setAdapter(myAdapter);

        if(!isAutherized){
            ((ViewGroup) addStudentButton.getParent()).removeView(addStudentButton);
            ((ViewGroup) addStudentFromFileButton.getParent()).removeView(addStudentFromFileButton);
        }

        addStudentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popUpEditText();
            }
        });

        addStudentFromFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkAndRequestPermission()){
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("text/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent,"Select a csv file"),100);
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(GroupStudentsActivity.this, ProfileActivity.class);
                intent.putExtra("email", emails.get(i));
                intent.putExtra("isSelf",false);
                startActivity(intent);
            }
        });
        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                deleteStudent(i);
                return true;
            }
        });

        fetchStudents();
    }

    public void deleteStudent(int i){
        AlertDialog.Builder alert = new AlertDialog.Builder(GroupStudentsActivity.this);
        alert.setMessage("Öğrenciyi silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        db.collection("course-student").whereEqualTo("studentEmail",emails.get(i)).whereEqualTo("courseId",courseId).whereEqualTo("instructorEmail",instructorEmail).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                    doc.getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            emails.remove(i);
                                            myAdapter.notifyDataSetChanged();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 101){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,"Select a csv file"),100);
            }else {
                Toast.makeText(this, "Storage Permission is Required to select a file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkAndRequestPermission() {
        int storagePermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GroupStudentsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},101);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==100 && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();

            List<String[]> students = readCSV(uri);
            if (students == null){
                Toast.makeText(getApplicationContext(), "Dosya okunamadı", Toast.LENGTH_LONG).show();
            }else{
                for (int i = 0; i < students.size(); i++) {
                    saveStudentCourse(students.get(i)[0]);
                }
            }
        }
    }

    public List<String[]> readCSV(Uri uri){
        List<String[]> rows = new ArrayList<>();
        CSVReader csvReader = new CSVReader(GroupStudentsActivity.this,uri);
        try {
            rows = csvReader.readCSV();
            return rows;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void fetchStudents(){
        db.collection("course-student").whereEqualTo("courseId",courseId).whereEqualTo("instructorEmail",instructorEmail).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                    emails.add(doc.getString("studentEmail"));
                    myAdapter.notifyDataSetChanged();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void popUpEditText() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Öğrenci Ekleme");
        alert.setMessage("Ders grubuna eklemek istediğiniz öğrencinin mail adresini giriniz");
        alert.setCancelable(false);

        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setBackground(getResources().getDrawable(R.drawable.my_edit_text));
        input.setHeight(100);
        input.setHint("Email: ");
        input.setPadding(30, 0, 30, 0);
        alert.setView(input, 50, 0, 50, 0);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String email = input.getText().toString().trim();
                saveStudentCourse(email);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void saveStudentCourse(String email){
        if (email.isEmpty() || !email.endsWith("@std.yildiz.edu.tr")) {
            Toast.makeText(getApplicationContext(), "Lütfen öğrenci mail adresi giriniz", Toast.LENGTH_LONG).show();
        }else if(emails.contains(email)) {
            Toast.makeText(getApplicationContext(), "Bu öğrenci zaten derse kayıtlıdır!", Toast.LENGTH_LONG).show();
        }else{
            Map<String, Object> student = new HashMap<>();
            student.put("studentEmail",email);
            student.put("courseId",courseId);
            student.put("instructorEmail",instructorEmail);
            db.collection("course-student").add(student).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    emails.add(email);
                    myAdapter.notifyDataSetChanged();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}