package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class AllAttendances extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    String email;

    Button exportData, back;

    ArrayList<String> dateList;
    ArrayList<String> courseStudents;
    ArrayList<HashMap<String, String>> attendanceStudents;

    String courseId;
    TableLayout attendanceTable;
    String docId;

    public static final int PERM_CODE = 101;

    Boolean isAuthorized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_attendances);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        attendanceTable = findViewById(R.id.attendanceTable);
        dateList = new ArrayList<>();
        courseStudents = new ArrayList<>();
        attendanceStudents = new ArrayList<>();
        exportData = findViewById(R.id.exportData);
        back = findViewById(R.id.backButton);

        email = mAuth.getCurrentUser().getEmail();
        Bundle extras = getIntent().getExtras();
        courseId = extras.getString("courseId");
        docId = extras.getString("courseId");

        db.collection("attendance")
                .whereEqualTo("courseId", courseId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        int documentCount = queryDocumentSnapshots.size();
                        Toast.makeText(getApplicationContext(), "Uzunluk: " + courseId + " " + documentCount, Toast.LENGTH_SHORT).show();

                        // TableLayout'un referansını al
                        attendanceTable.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
                        attendanceTable.setDividerDrawable(getDrawable(R.drawable.table_divider));

                        // Katılımcılar için bir TableRow oluştur ve tabloya ekle
                        TableRow headerRow = new TableRow(getApplicationContext());
                        headerRow.setLayoutParams(new TableRow.LayoutParams(
                                TableLayout.LayoutParams.WRAP_CONTENT,
                                TableLayout.LayoutParams.WRAP_CONTENT
                        ));
                        TextView participantsHeader = new TextView(getApplicationContext());
                        participantsHeader.setText("Katılımcılar");
                        participantsHeader.setGravity(Gravity.CENTER);

                        participantsHeader.setPadding(10,10,10,10);
                        participantsHeader.setTypeface(null, Typeface.BOLD);

                        headerRow.addView(participantsHeader);

                        // Hafta sayıları için sütun başlıklarını ekle
                        for (int i = 1; i <= documentCount; i++) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(i - 1);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                            String dateFormatted = dateFormat.format(document.getDate("date"));
                            dateList.add(dateFormatted);

                            TextView weekHeader = new TextView(headerRow.getContext());
                            weekHeader.setText("Hafta " + i + " (" + dateFormatted + ")");
                            weekHeader.setGravity(Gravity.CENTER);

                            weekHeader.setPadding(10,10,10,10);

                            weekHeader.setTypeface(null, Typeface.BOLD);
                            weekHeader.setGravity(Gravity.CENTER);
                            weekHeader.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                            headerRow.addView(weekHeader);
                        }

                        attendanceTable.addView(headerRow);

                        db.collection("course-student").whereEqualTo("courseId", courseId).get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                            courseStudents.add(document.getString("studentEmail"));
                                        }

                                        db.collection("attendance-student").whereEqualTo("courseId", courseId)
                                                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                                            HashMap<String, String> obj = new HashMap<>();
                                                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                                                            String dateFormatted = dateFormat.format(document.getDate("date"));
                                                            obj.put("date", dateFormatted);
                                                            obj.put("student", document.getString("student"));
                                                            attendanceStudents.add(obj);
                                                        }
                                                        for (String student : courseStudents) {
                                                            TableRow newRow = new TableRow(getApplicationContext());
                                                            newRow.setLayoutParams(new TableRow.LayoutParams(
                                                                    TableLayout.LayoutParams.WRAP_CONTENT,
                                                                    TableLayout.LayoutParams.WRAP_CONTENT
                                                            ));
                                                            TextView participantMail = new TextView(getApplicationContext());
                                                            participantMail.setText(student);

                                                            newRow.addView(participantMail);

                                                            for (String date : dateList) {
                                                                TextView dateAttendance = new TextView(getApplicationContext());
                                                                dateAttendance.setTextSize(20);
                                                                boolean status = false;

                                                                for (HashMap<String, String> att : attendanceStudents) {
                                                                    if (date.equals(att.get("date")) && student.equals(att.get("student"))) {
                                                                        dateAttendance.setText(" + ");
                                                                        dateAttendance.setGravity(Gravity.CENTER);
                                                                        dateAttendance.setTextColor(Color.GREEN);
                                                                        status = true;
                                                                        break;
                                                                    }
                                                                }
                                                                if (!status) {
                                                                    dateAttendance.setText(" - ");
                                                                    dateAttendance.setTextColor(Color.RED);
                                                                    dateAttendance.setGravity(Gravity.CENTER);

                                                                }
                                                                newRow.addView(dateAttendance);
                                                            }
                                                            attendanceTable.addView(newRow);
                                                        }
                                                    }
                                                });
                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("hata", e.toString());
                    }
                });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AllAttendances.this, AttendanceActivity.class);
                intent.putExtra("isAuthorized", isAuthorized);
                intent.putExtra("courseId", courseId);
                intent.putExtra("attendanceId", docId);

                startActivity(intent);
            }
        });

        exportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CSV dosyasını oluştur
                StringBuilder csvData = new StringBuilder();
                TableRow firstRow = (TableRow) attendanceTable.getChildAt(0);

                // Get the number of columns in the first row
                int colCount = firstRow.getChildCount();
                for (int i = 0; i < attendanceTable.getChildCount(); i++) {
                    TableRow row = (TableRow) attendanceTable.getChildAt(i);
                    for (int j = 0; j < colCount; j++) {
                        TextView tXdata = (TextView) row.getChildAt(j);
                        String data = tXdata.getText().toString();
                        csvData.append(data).append(",");
                    }

                    csvData.append("\n");
                }

                // CSV dosyasını dışa aktar
                try {
                    File file = new File(getExternalFilesDir(null), "All_attendance.csv");
                    FileWriter writer = new FileWriter(file);
                    writer.append(csvData.toString());
                    writer.flush();
                    writer.close();

                    // Başarılı mesajı göster
                    AlertDialog.Builder builder = new AlertDialog.Builder(AllAttendances.this);
                    builder.setTitle("CSV Dosyası Oluşturuldu");
                    builder.setMessage("CSV dosyası başarıyla oluşturuldu:\n" + file.getAbsolutePath());
                    builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    // Hata mesajı göster
                    Toast.makeText(AllAttendances.this, "CSV dosyası oluşturulurken bir hata oluştu.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
}