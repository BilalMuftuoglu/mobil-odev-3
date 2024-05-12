package com.example.myapplication;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CourseDetailsActivity extends AppCompatActivity {

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

    Button addInstructorGroupButton;
    Button saveButton;
    Button updateButton;

    Button deleteButton;

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


    int hour,minute;

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
                        docId = doc.getId();

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

                        if(!accountType.equals("instructor")){
                            ((ViewGroup) saveButton.getParent()).removeView(saveButton);
                            ((ViewGroup) updateButton.getParent()).removeView(updateButton);
                            ((ViewGroup) deleteButton.getParent()).removeView(deleteButton);
                            ((ViewGroup) addInstructorGroupButton.getParent()).removeView(addInstructorGroupButton);
                            ((ViewGroup) startHourButton.getParent()).removeView(startHourButton);
                            ((ViewGroup) endHourButton.getParent()).removeView(endHourButton);
                            courseNameText.setEnabled(false);
                            courseIdText.setEnabled(false);
                            daySpinner.setEnabled(false);
                            termSpinner.setEnabled(false);
                            isCompletedBox.setEnabled(false);
                        }else{
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