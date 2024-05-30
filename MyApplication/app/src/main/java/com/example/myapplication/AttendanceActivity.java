package com.example.myapplication;

import static android.content.ContentValues.TAG;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.collect.Table;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity {
    Button allAttendanceButton;

    Button exportAttendanceButton;
    Button refreshAttendanceButton;

    Button startAttendanceButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private int REQUEST_LOCATION = 99;
    private String provider;
    private LocationListener mylistener;
    private Criteria criteria;
    String email;

    private String lat;
    private String lon;
    String courseId;
    String accountType;
    public static final int PERM_CODE = 101;

    Boolean isAutherized = false;
    int numberOfGroups;
    String docId;
    private LocationManager locationManager;
    boolean start_stop;
    String belgeId;
    String attendanceId;

    TableLayout attendanceTable;
    FusedLocationProviderClient mFusedLocationClient;
    public static boolean checkAndRequestPermissions(final Activity context) {
        int coarselocation = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int finelocation = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (coarselocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendance);
        start_stop = false;


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        email = mAuth.getCurrentUser().getEmail().toString();
        Bundle extras = getIntent().getExtras();
        courseId = extras.getString("courseId");
        attendanceId = extras.getString("attendanceId");


        attendanceTable = (TableLayout) findViewById(R.id.attendanceTable);
        allAttendanceButton = findViewById(R.id.allAttendanceButton);
        exportAttendanceButton = findViewById(R.id.exportAttendanceButton);
        refreshAttendanceButton = findViewById(R.id.refreshAttendanceButton);
        startAttendanceButton = findViewById(R.id.startAttendanceButton);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkAndRequestPermissions(AttendanceActivity.this);

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
        db.collection("attendance")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("status", "active")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Eğer dönen sonucun boyutu 0'dan büyükse
                            startAttendanceButton.setText("Yoklamayı durdur");
                            startAttendanceButton.setBackgroundColor(Color.RED);

                            // Dönen değerin id değerini al
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            belgeId = document.getId();
                            start_stop=true;
                            // belgeId ile ilgili işlemleri burada yapabilirsiniz
                            Toast.makeText(getApplicationContext(), "Aktif yoklama bulundu", Toast.LENGTH_LONG).show();
                        } else {
                            // Eğer dönen sonucun boyutu 0 ise
                            Toast.makeText(getApplicationContext(), "Aktif yoklama bulunamadı", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                lat= String.valueOf(location.getLatitude());
                lon=String.valueOf(location.getLongitude());

                Log.d("KOnum ",lat+" "+lon);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("HATAAAAA", e.toString());

            }
        });

        allAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendanceActivity.this, AllAttendances.class);
                intent.putExtra("isAutherized",isAutherized);
                intent.putExtra("courseId",courseId);
                intent.putExtra("attendanceId",docId);

                startActivity(intent);
            }
        });

        exportAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CSV dosyasını oluştur
                StringBuilder csvData = new StringBuilder();
                csvData.append("No,Email\n");

                for (int i = 0; i < attendanceTable.getChildCount(); i++) {
                    TableRow row = (TableRow) attendanceTable.getChildAt(i);
                    TextView noTextView = (TextView) row.getChildAt(0);
                    TextView emailTextView = (TextView) row.getChildAt(1);

                    String no = noTextView.getText().toString();
                    String email = emailTextView.getText().toString();

                    csvData.append(no).append(",").append(email).append("\n");
                }

                // CSV dosyasını dışa aktar
                try {
                    File file = new File(getExternalFilesDir(null), "attendance.csv");
                    FileWriter writer = new FileWriter(file);
                    writer.append(csvData.toString());
                    writer.flush();
                    writer.close();

                    // Başarılı mesajı göster
                    AlertDialog.Builder builder = new AlertDialog.Builder(AttendanceActivity.this);
                    builder.setTitle("CSV Dosyası Oluşturuldu");
                    builder.setMessage("CSV dosyası başarıyla oluşturuldu:\n" + file.getAbsolutePath());
                    builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Kullanıcı Tamam'a tıkladığında, dialog kapanır
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();                } catch (IOException e) {
                    e.printStackTrace();
                    // Hata mesajı göster
                    Toast.makeText(AttendanceActivity.this, "CSV dosyası oluşturulurken bir hata oluştu.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        refreshAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("attendance-student").whereEqualTo("attendanceId",belgeId)
                        .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                // Clear the existing rows from the table
                                Toast.makeText(AttendanceActivity.this, "yoklama ıd"+belgeId , Toast.LENGTH_SHORT).show();

                                attendanceTable.removeAllViews();

                                List<DocumentSnapshot> doc = queryDocumentSnapshots.getDocuments();
                                for (int i = 0; i < doc.size(); i++) {
                                    // Yeni bir satır oluştur
                                    TableRow newRow = new TableRow(attendanceTable.getContext());
                                    newRow.setTag(doc.get(i).getId()); // Satırın etiketini belge ID'si olarak ayarla
                                    // Satır için düzen parametrelerini ayarla
                                    TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
                                    lp.setMargins(0, 10, 0, 10); // Aralara boşluk bırak
                                    newRow.setLayoutParams(lp);

                                    // 1. sütun: Artan değerler
                                    TextView textViewNo = new TextView(attendanceTable.getContext());
                                    textViewNo.setPadding(10, 0, 0, 0); // Sol kenara biraz boşluk bırak
                                    textViewNo.setText(String.valueOf(i + 1)); // Artan değerler 1'den başlayacak şekilde ayarla
                                    newRow.addView(textViewNo);

                                    // 2. sütun: E-posta adresi
                                    TextView textViewEmail = new TextView(attendanceTable.getContext());
                                    textViewEmail.setPadding(10, 0, 0, 0); // Sol kenara biraz boşluk bırak
                                    textViewEmail.setText(doc.get(i).getString("student")); // E-posta adresini buraya ekleyin
                                    newRow.addView(textViewEmail);

                                    // 3. sütun: Sil butonu
                                    Button deleteButton = new Button(attendanceTable.getContext());
                                    deleteButton.setText("Sil");
                                    deleteButton.setPadding(10, 0, 0, 0); // Sol kenara biraz boşluk bırak
                                    deleteButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // Silme butonuna tıklandığında satırı sil
                                            attendanceTable.removeView(newRow);
                                            // Firestore'dan belgeyi sil
                                            db.collection("attendance-student").document(newRow.getTag().toString()).delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Toast.makeText(AttendanceActivity.this, "Satır silindi", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(AttendanceActivity.this, "Silme işleminde hata oluştu", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    });
                                    // Buton için düzen parametrelerini ayarla
                                    TableRow.LayoutParams buttonParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                                    buttonParams.setMargins(10, 0, 0, 0); // Sol kenara biraz boşluk bırak
                                    deleteButton.setLayoutParams(buttonParams);
                                    newRow.addView(deleteButton);

                                    // Yeni satırı tabloya ekle
                                    attendanceTable.addView(newRow);
                                }
                            }
                        });
            }
        });


        startAttendanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (!start_stop) {
                    start_stop = !start_stop;

                    // Belge verilerini hazırla
                    Map<String, Object> yeniVeri = new HashMap<>();
                    yeniVeri.put("courseId", courseId); // Değerleri kendinize göre güncelleyin
                    yeniVeri.put("date", new Date()); // Değerleri kendinize göre güncelleyin
                    yeniVeri.put("instructorMail", email); // Değerleri kendinize göre güncelleyin
                    yeniVeri.put("status", "active");
                    yeniVeri.put("lat", lat);
                    yeniVeri.put("lon", lon);

                    // Belgeyi ekleyin
                    // Belgeyi ekleyin
                    db.collection("attendance").document().set(yeniVeri)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Belge ekleme başarılı olduğunda
                                    db.collection("attendance").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            // Eklenen son belgeyi al
                                            DocumentSnapshot lastDocument = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                                            belgeId = lastDocument.getId();
                                            Toast.makeText(AttendanceActivity.this, "Yoklama tanımlandı", Toast.LENGTH_SHORT).show();
                                            startAttendanceButton.setText("Yoklamayı durdur");
                                            startAttendanceButton.setBackgroundColor(Color.RED);
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Belge ekleme başarısız olduğunda
                                    Toast.makeText(AttendanceActivity.this, "Yoklama tanımlamada hata oluştu ", Toast.LENGTH_SHORT).show();
                                }
                            });


                } else {
                    // Belgeyi güncelle
                    Toast.makeText(AttendanceActivity.this,belgeId , Toast.LENGTH_SHORT).show();

                    db.collection("attendance").document(belgeId).update("status", "finished")
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Belge güncelleme başarılı olduğunda
                                    Toast.makeText(AttendanceActivity.this, "Yoklama tamamlandı", Toast.LENGTH_SHORT).show();
                                    startAttendanceButton.setText("Yoklama Bitmiştir");
                                    startAttendanceButton.setBackgroundColor(Color.YELLOW);
                                    startAttendanceButton.setFocusable(false);
                                    startAttendanceButton.setClickable(false);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Belge güncelleme başarısız olduğunda
                                    Toast.makeText(AttendanceActivity.this, "Yoklama güncellemede hata oluştu ", Toast.LENGTH_SHORT).show();
                                }
                            });

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