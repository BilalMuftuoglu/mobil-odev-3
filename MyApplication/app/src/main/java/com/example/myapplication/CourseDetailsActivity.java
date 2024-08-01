package com.example.myapplication;

import static android.app.ActionBar.DISPLAY_SHOW_CUSTOM;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseDetailsActivity extends AppCompatActivity {
    public static final int PERM_CODE = 101;
    private static final double EARTH_RADIUS = 6371000;
    FusedLocationProviderClient mFusedLocationClient;
    public static boolean checkAndRequestPermissions(final Activity context) {
        int coarselocation = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int finelocation = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (coarselocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (finelocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded
                    .add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(context, listPermissionsNeeded
                            .toArray(new String[listPermissionsNeeded.size()]),
                    PERM_CODE);
            return false;
        }
        return true;
    }
    EditText courseIdText;
    EditText courseNameText;
    Spinner daySpinner;
    Spinner termSpinner;
    Button startHourButton;
    Button endHourButton;
    TextView startHourText;
    TextView endHourText;
    ListView listView;
    CheckBox isCompletedBox;

    Button joinAttendanceButton;

    Button addInstructorGroupButton;
    Button saveButton;
    Button updateButton;

    Button deleteButton;

    Button attendanceButton;

    ArrayList<String> emails;
    ArrayList<String> listStrings;

    ArrayAdapter<String> emailAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    String email;
    String courseId;
    String accountType;
    Boolean isAutherized = false;
    int numberOfGroups;
    String docId;

    String instructor_lat;
    String instructor_lon;

    String  student_lat;
    String  student_lon;


    int hour,minute;

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        email = mAuth.getCurrentUser().getEmail().toString();

        Bundle extras = getIntent().getExtras();
        Boolean isFromRecycler = extras.getBoolean("isFromRecycler");

        courseIdText = findViewById(R.id.courseIdText);
        courseNameText = findViewById(R.id.courseNameText);
        daySpinner = findViewById(R.id.daySpinner);
        termSpinner = findViewById(R.id.termSpinner);
        startHourButton = findViewById(R.id.startHourButton);
        endHourButton = findViewById(R.id.endHourButton);
        startHourText = findViewById(R.id.startHourTextView);
        endHourText = findViewById(R.id.endHourTextView);
        addInstructorGroupButton = findViewById(R.id.addInstructorGroupButton);
        saveButton = findViewById(R.id.saveButton);
        updateButton = findViewById(R.id.updateButton);
        deleteButton = findViewById(R.id.deleteButton);
        isCompletedBox = findViewById(R.id.isCompletedBox);
        attendanceButton=findViewById(R.id.attendanceButton);
        joinAttendanceButton=findViewById(R.id.joinAttendanceButton);
        joinAttendanceButton.setVisibility(View.INVISIBLE);
        joinAttendanceButton.setFocusable(false);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkAndRequestPermissions(CourseDetailsActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.d("HATAAAAA", "izin yokmus ");
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                student_lat= String.valueOf(location.getLatitude());
                student_lon=String.valueOf(location.getLongitude());

                Log.d(" ogrenci KOnum ",student_lat+" "+student_lon);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("HATAAAAA", e.toString());

            }
        });

        //((ViewGroup) joinAttendanceButton.getParent()).removeView(joinAttendanceButton);

        String[] days = new String[]{"Pazartesi","Salı","Çarşamba","Perşembe","Cuma","Cumartesi","Pazar"};
        String[] terms = new String[]{"2023-2024 Güz","2023-2024 Bahar","2023-2024 Yaz","2024-2025 Güz","2024-2025 Bahar","2024-2025 Yaz"};

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(dayAdapter);

        ArrayAdapter<String> termAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,terms);
        termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        termSpinner.setAdapter(termAdapter);

        listView = findViewById(R.id.listView);
        listView.setItemsCanFocus(true);

        emails = new ArrayList<>();
        listStrings = new ArrayList<>();

        emailAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,listStrings);
        listView.setAdapter(emailAdapter);

        db.collection("attendance-student")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("student",email)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Toast.makeText(getApplicationContext(), email+courseId+"durummm"+queryDocumentSnapshots.getDocuments().size(), Toast.LENGTH_LONG).show();

                        if (!queryDocumentSnapshots.isEmpty()) {
                            // En yakın tarihli belgeyi al
                            Toast.makeText(getApplicationContext(), "durummm2", Toast.LENGTH_LONG).show();

                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            Date date = document.getDate("date");

                            // Şu anki tarih
                            Date currentDate = new Date();
                            // 3 gün öncesi
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(currentDate);
                            calendar.add(Calendar.DAY_OF_YEAR, -3);
                            Date threeDaysAgo = calendar.getTime();

                            // Son 3 gün içinde olup olmadığını kontrol et
                            boolean isOlderThanThreeDays = date.before(threeDaysAgo);

                            // Sonuçlara göre işlemler
                            if (!isOlderThanThreeDays) {
                                // 3 günden eski değil
                                joinAttendanceButton.setClickable(false);
                                joinAttendanceButton.setBackgroundColor(Color.YELLOW);
                                joinAttendanceButton.setText("Katılındı");

                            }

                    }}
                });



        joinAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("attendance").whereEqualTo("courseId",courseId)

                        .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    // En yakın tarihli kaydı al
                                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                                    docId=document.getId();
                                    double lat = Double.valueOf(document.get("lat").toString());
                                    double lon = Double.valueOf(document.get("lon").toString());

                                    // Öğrencinin konumu ile kayıttaki konumu karşılaştır
                                    float[] results = new float[1];
                                    Location.distanceBetween(Double.valueOf(student_lat), Double.valueOf(student_lon), lat, lon, results);
                                    float distanceInMeters = results[0];

                                    Toast.makeText(getApplicationContext(), "Konumunuz "+String.valueOf(distanceInMeters), Toast.LENGTH_LONG).show();


                                    if (distanceInMeters <= 12) {
                                        // İşlemleri gerçekleştirin
                                        Map<String, Object> yeniVeri = new HashMap<>();
                                        yeniVeri.put("courseId", courseId);
                                        yeniVeri.put("date", new Date());
                                        yeniVeri.put("student", email);
                                        yeniVeri.put("lat", student_lat);
                                        yeniVeri.put("lon", student_lon);
                                        yeniVeri.put("attendanceId", docId);

                                        db.collection("attendance-student").document().set(yeniVeri).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(getApplicationContext(), "Başarıyla derse katılındı", Toast.LENGTH_LONG).show();
                                                joinAttendanceButton.setClickable(false);
                                                joinAttendanceButton.setBackgroundColor(Color.YELLOW);
                                                joinAttendanceButton.setText("Katılındı");
                                            }
                                        });
                                    } else {
                                        // Mesafe 12 metreden büyükse
                                        Toast.makeText(getApplicationContext(), "Konumunuz 12 metreden uzakta", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    // Belirtilen courseId ile eşleşen döküman bulunamadı
                                    Toast.makeText(getApplicationContext(), "Ders kaydı bulunamadı", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });













        if(isFromRecycler){
            courseId = extras.getString("courseId");
            accountType = extras.getString("accountType");

            db.collection("courses").whereEqualTo("courseId",courseId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){

                        courseNameText.setText(doc.getString("courseName"));
                        courseIdText.setText(doc.getString("courseId"));
                        startHourText.setText(doc.getString("courseStartHour"));
                        endHourText.setText(doc.getString("courseEndHour"));
                        isCompletedBox.setChecked(doc.getBoolean("isCompleted"));
                        daySpinner.setSelection(getIndex(daySpinner, doc.get("courseDay").toString()));
                        termSpinner.setSelection(getIndex(termSpinner,doc.get("term").toString()));
                        numberOfGroups = (int) (long) doc.get("numberOfGroups");

                        int i;
                        for(i=1;i<=numberOfGroups;i++){
                            emails.add(doc.getString("Gr"+i));
                        }

                        i = 1;
                        for (String s: emails) {

                            int finalI = i;
                            db.collection("course-student").whereEqualTo("courseId",courseId).whereEqualTo("instructorEmail",s).count().get(AggregateSource.SERVER).addOnSuccessListener(new OnSuccessListener<AggregateQuerySnapshot>() {
                                @Override
                                public void onSuccess(AggregateQuerySnapshot aggregateQuerySnapshot) {
                                    long size = aggregateQuerySnapshot.getCount();
                                    listStrings.add("Gr: " + finalI + " " + s + " - " + size + " kişi");
                                    emailAdapter.notifyDataSetChanged();
                                }
                            });

                            i++;
                        }

                        if(!accountType.equals("instructors")){
                            ((ViewGroup) saveButton.getParent()).removeView(saveButton);
                            ((ViewGroup) updateButton.getParent()).removeView(updateButton);
                            ((ViewGroup) deleteButton.getParent()).removeView(deleteButton);
                            ((ViewGroup) addInstructorGroupButton.getParent()).removeView(addInstructorGroupButton);
                            ((ViewGroup) startHourButton.getParent()).removeView(startHourButton);
                            ((ViewGroup) endHourButton.getParent()).removeView(endHourButton);
                            ((ViewGroup) attendanceButton.getParent()).removeView(attendanceButton);
                            courseNameText.setEnabled(false);
                            courseIdText.setEnabled(false);
                            daySpinner.setEnabled(false);
                            termSpinner.setEnabled(false);
                            isCompletedBox.setEnabled(false);

                            db.collection("course-student").whereEqualTo("courseId",courseId).whereEqualTo("studentEmail",email).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                                    for (DocumentSnapshot document : documents){

                                        db.collection("attendance").whereEqualTo("status","active").whereEqualTo("instructorMail",document.getString("instructorEmail"))
                                                .whereEqualTo("courseId",courseId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                            @Override
                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots2) {
                                                List<DocumentSnapshot> documents2 = queryDocumentSnapshots2.getDocuments();

                                                for (DocumentSnapshot document2 : documents2){
                                                        //((ViewGroup) joinAttendanceButton.getParent()).addView(joinAttendanceButton);
                                                    joinAttendanceButton.setVisibility(View.VISIBLE);
                                                    joinAttendanceButton.setFocusable(true);

                                                }
                                            }
                                        });

                                    }
                                }
                            });







                        }else{
                            ((ViewGroup) joinAttendanceButton.getParent()).removeView(joinAttendanceButton);

                            //dersin yürütücüsü değilse viewları gizle
                            System.out.println(emails);
                            for (String e: emails) {
                                System.out.println("deneme");
                                if(email.equals(e)){
                                    isAutherized = true;
                                    System.out.println(e);
                                    System.out.println(email);
                                }
                            }

                            if(!isAutherized){
                                ((ViewGroup) saveButton.getParent()).removeView(saveButton);
                                ((ViewGroup) updateButton.getParent()).removeView(updateButton);
                                ((ViewGroup) deleteButton.getParent()).removeView(deleteButton);
                                ((ViewGroup) addInstructorGroupButton.getParent()).removeView(addInstructorGroupButton);
                                ((ViewGroup) startHourButton.getParent()).removeView(startHourButton);
                                ((ViewGroup) endHourButton.getParent()).removeView(endHourButton);
                                ((ViewGroup) attendanceButton.getParent()).removeView(attendanceButton);
                                courseNameText.setEnabled(false);
                                courseIdText.setEnabled(false);
                                daySpinner.setEnabled(false);
                                termSpinner.setEnabled(false);
                                isCompletedBox.setEnabled(false);
                            }else{
                                courseIdText.setEnabled(false);
                                ((ViewGroup) saveButton.getParent()).removeView(saveButton);

                                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        if(email.equals(emails.get(i))){
                                            Intent intent = new Intent(CourseDetailsActivity.this, GroupStudentsActivity.class);
                                            intent.putExtra("isAutherized",isAutherized);
                                            intent.putExtra("courseId",courseId);
                                            intent.putExtra("instructorEmail",emails.get(i));
                                            startActivity(intent);
                                        }else{
                                            Toast.makeText(getApplicationContext(),"Sadece kendi grubunuzu görebilirsiniz!",Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });

                                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                                    @Override
                                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                                        if(email.equals(emails.get(i))){
                                            if(numberOfGroups > 1){
                                                deleteCourseGroup(i);
                                            }else{
                                                Toast.makeText(getApplicationContext(),"Tek grup olduğu için lütfen dersi siliniz yada önce başka bir grup ekleyiniz!",Toast.LENGTH_LONG).show();
                                            }
                                        }

                                        return true;
                                    }
                                });
                            }
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
            ((ViewGroup) updateButton.getParent()).removeView(updateButton);
            ((ViewGroup) deleteButton.getParent()).removeView(deleteButton);
            emails.add(email);
            listStrings.add("Gr: 1" + " " + email);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Toast.makeText(getApplicationContext(),"Öğrenci eklemeden önce dersi kaydedin!",Toast.LENGTH_LONG).show();
                }
            });
        }


        attendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CourseDetailsActivity.this, AttendanceActivity.class);
                intent.putExtra("isAutherized",isAutherized);
                intent.putExtra("courseId",courseId);
                intent.putExtra("attendanceId",docId);

                startActivity(intent);
            }
        });







        startHourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popTimePicker(startHourText);
            }
        });

        endHourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popTimePicker(endHourText);
            }
        });

        addInstructorGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popUpEditText();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCourse();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateCourse();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteCourse();
            }
        });

        View rootLayout = findViewById(android.R.id.content);
        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard();
                return false;
            }
        });

    }

    public void deleteCourseGroup(int index){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Grubunuzu ve kayıtlı tüm öğrencileri silmek istediğinize emin misiniz?");
        alert.setPositiveButton("Evet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Map<String,Object> updates = new HashMap<>();
                updates.put("Gr"+(index+1), FieldValue.delete());
                for(int j = index; j<numberOfGroups-1; j++){
                    updates.put("Gr"+(j+1),emails.get(j+1));
                }
                updates.put("Gr"+String.valueOf(numberOfGroups),FieldValue.delete());
                updates.put("numberOfGroups",numberOfGroups-1);
                db.collection("courses").document(docId).update(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        db.collection("course-student").whereEqualTo("courseId",courseId).whereEqualTo("instructorEmail",emails.get(index)).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                    doc.getReference().delete();

                                }
                                emails.remove(index);
                                emailAdapter.notifyDataSetChanged();
                                finish();
                            }
                        });
                    }
                });

            }
        }).setNegativeButton("Hayır",null);
        alert.show();
    }

    public void deleteCourse(){
        db.collection("courses").whereEqualTo("courseId",courseIdText.getText().toString()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                    doc.getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            db.collection("course-student").whereEqualTo("courseId",courseId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                        doc.getReference().delete();
                                    }
                                }
                            });
                            db.collection("posts").whereEqualTo("courseId",courseId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                        db.collection("posts").document(doc.getId()).collection("comments").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                            @Override
                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                for(DocumentSnapshot doc2: queryDocumentSnapshots.getDocuments()){
                                                    doc2.getReference().delete();
                                                }
                                                doc.getReference().delete();
                                            }
                                        });
                                    }
                                }
                            });
                            db.collection("polls").whereEqualTo("courseId",courseId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                        db.collection("polls").document(doc.getId()).collection("votes").get().addOnSuccessListener(queryDocumentSnapshotsInner -> {
                                            List<Task<Void>> tasks = new ArrayList<>();
                                            for (DocumentSnapshot doc2: queryDocumentSnapshotsInner.getDocuments()){
                                                tasks.add(doc2.getReference().delete());
                                            }
                                            Tasks.whenAll(tasks).addOnSuccessListener(aVoid -> {
                                                db.collection("polls").document(doc.getId()).delete().addOnSuccessListener(aVoid1 -> {
                                                });
                                            });
                                        });
                                    }
                                }
                            });

                            finish();
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void updateCourse(){
        if(courseNameText.getText().toString().isEmpty()){
            Toast.makeText(getApplicationContext(), "Lütfen ders ismi giriniz!", Toast.LENGTH_LONG).show();
        }else{
            db.collection("courses").whereEqualTo("courseId",courseIdText.getText().toString()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    Map<String, Object> course = new HashMap<>();
                    course.put("courseName",courseNameText.getText().toString());
                    course.put("courseDay",daySpinner.getSelectedItem().toString());
                    course.put("term",termSpinner.getSelectedItem().toString());
                    course.put("courseStartHour",startHourText.getText().toString());
                    course.put("courseEndHour",endHourText.getText().toString());
                    course.put("numberOfGroups",emails.size());
                    course.put("isCompleted",isCompletedBox.isChecked());

                    int i = 1;
                    for (String email: emails) {
                        course.put("Gr"+i,email);
                        i += 1;
                    }

                    for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                        db.collection("courses").document(doc.getId()).update(course).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                finish();
                            }
                        });
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void saveCourse(){

        if(courseNameText.getText().toString().isEmpty()){
            Toast.makeText(getApplicationContext(), "Lütfen ders ismi giriniz!", Toast.LENGTH_LONG).show();
        }else if(courseIdText.getText().toString().isEmpty()){
            Toast.makeText(getApplicationContext(), "Lütfen ders kodu giriniz!", Toast.LENGTH_LONG).show();
        }else{
            db.collection("courses").whereEqualTo("courseId",courseIdText.getText().toString()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if(queryDocumentSnapshots.getDocumentChanges().isEmpty()){

                        Map<String, Object> course = new HashMap<>();
                        course.put("courseName",courseNameText.getText().toString());
                        course.put("courseId",courseIdText.getText().toString());
                        course.put("courseDay",daySpinner.getSelectedItem().toString());
                        course.put("term",termSpinner.getSelectedItem().toString());
                        course.put("courseStartHour",startHourText.getText().toString());
                        course.put("courseEndHour",endHourText.getText().toString());
                        course.put("createdBy",email);
                        course.put("numberOfGroups",emails.size());
                        course.put("isCompleted",isCompletedBox.isChecked());

                        int i = 1;
                        for (String email: emails) {
                            course.put("Gr"+i,email);
                            i += 1;
                        }

                        db.collection("courses").add(course).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }else{
                        Toast.makeText(getApplicationContext(), "Bu ders koduna sahip başka bir ders var!", Toast.LENGTH_LONG).show();
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    public void popTimePicker(TextView textView){
        TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                hour = selectedHour;
                minute = selectedMinute;
                textView.setText(String.format(Locale.getDefault(),"%02d:%02d",hour,minute));
            }
        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,onTimeSetListener,hour,minute,true);
        timePickerDialog.setTitle("Select Time");
        timePickerDialog.show();
    }

    private void popUpEditText() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Akademisyen Ekleme");
        alert.setMessage("Ders grubu açmak istediğiniz akademisyenin mail adresini giriniz");
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
                if (email.isEmpty() || !email.endsWith("@yildiz.edu.tr")) {
                    Toast.makeText(getApplicationContext(), "Lütfen akademisyen mail adresi giriniz", Toast.LENGTH_LONG).show();
                }else {
                    emails.add(email);
                    listStrings.add("Gr: " + (listStrings.size() + 1) + " " + email);
                    emailAdapter.notifyDataSetChanged();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null){
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }
        return 0;
    }
}