package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    CardView educationCard;
    TextView accountTypeText,nameText,emailText,idText;
    ImageView profileImage,phoneIcon,emailIcon;
    CheckBox emailBox,phoneNumberBox,instagramBox,xBox,linkedinBox;
    EditText phoneNumberText,instagramText,xText,linkedinText,departmentText;
    Spinner degreeSpinner,yearSpinner;
    Button saveButton;

    String accountType;
    private FirebaseFirestore db;
    FirebaseStorage storage;
    public static final int PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int STORAGE_REQUEST_CODE = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        accountTypeText = findViewById(R.id.accountTypeText);
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        profileImage = findViewById(R.id.profileImage);
        emailBox = findViewById(R.id.emailBox);
        phoneIcon = findViewById(R.id.phoneIcon);
        emailIcon = findViewById(R.id.emailIcon);
        phoneNumberBox = findViewById(R.id.phoneNumberBox);
        instagramBox = findViewById(R.id.instagramBox);
        xBox = findViewById(R.id.xBox);
        linkedinBox = findViewById(R.id.linkedinBox);
        phoneNumberText = findViewById(R.id.phoneNumberText);
        instagramText = findViewById(R.id.instagramText);
        xText = findViewById(R.id.xText);
        linkedinText = findViewById(R.id.linkedinText);
        idText = findViewById(R.id.idText);
        departmentText = findViewById(R.id.departmentText);
        degreeSpinner = findViewById(R.id.degreeSpinner);
        yearSpinner = findViewById(R.id.yearSpinner);
        saveButton = findViewById(R.id.saveButton);
        educationCard = findViewById(R.id.educationCard);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Bundle extras = getIntent().getExtras();
        String email = extras.getString("email");

        fetchData(email);

        if(accountType.equals("instructors")){
            ((ViewGroup) educationCard.getParent()).removeView(educationCard);
        }

        if(!extras.getBoolean("isSelf")){
            profileImage.setEnabled(false);
            emailBox.setEnabled(false);
            phoneNumberText.setEnabled(false);
            phoneNumberBox.setEnabled(false);
            instagramText.setEnabled(false);
            instagramBox.setEnabled(false);
            xText.setEnabled(false);
            xBox.setEnabled(false);
            linkedinText.setEnabled(false);
            linkedinBox.setEnabled(false);
            departmentText.setEnabled(false);
            degreeSpinner.setEnabled(false);
            yearSpinner.setEnabled(false);
            ((ViewGroup) saveButton.getParent()).removeView(saveButton);
        }


        View rootLayout = findViewById(android.R.id.content);
        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard();
                return false;
            }
        });

        String[] degrees = new String[]{"Seçiniz","Ön Lisans","Lisans","Yüksek Lisans","Doktora"};
        String[] years = new String[]{"Seçiniz","Hazırlık","1","2","3","4","4+"};

        ArrayAdapter<String> degreeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,degrees);
        degreeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        degreeSpinner.setAdapter(degreeAdapter);

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);



        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkAndRequestPermissions(ProfileActivity.this)){
                    chooseImage(ProfileActivity.this);
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
            }
        });

        phoneIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhoneIntent();
            }
        });

        emailIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailText.getText()));
                startActivity(emailIntent);
            }
        });

    }

    private void choosePhoneIntent(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.alert_app_chooser, null);
        ImageView wp = dialogLayout.findViewById(R.id.whatsappButton);
        ImageView phone = dialogLayout.findViewById(R.id.phoneButton);

        wp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String url = "https://api.whatsapp.com/send?phone="+phoneNumberText.getText();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

                //If whatsapp is not downloaded show toast
                /*
                String url = "https://api.whatsapp.com/send?phone=" + phoneNumberText.getText();
                try {
                    PackageManager pm = getApplicationContext().getPackageManager();
                    pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (PackageManager.NameNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Whatsapp uygulaması yüklü değil!", Toast.LENGTH_SHORT).show();
                }*/
            }
        });

        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri number = Uri.parse("tel:" + phoneNumberText.getText());
                Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
                startActivity(callIntent);
            }
        });

        builder.setView(dialogLayout);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveData(){
        String email = emailText.getText().toString();

        db.collection(accountType).whereEqualTo("email",email).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                    Map<String,Object> newData = new HashMap<>();
                    newData.put("isEmailPrivate",emailBox.isChecked());
                    newData.put("phoneNumber",phoneNumberText.getText().toString());
                    newData.put("isPhoneNumberPrivate",phoneNumberBox.isChecked());
                    newData.put("instagram",instagramText.getText().toString());
                    newData.put("isInstagramPrivate",instagramBox.isChecked());
                    newData.put("x",xText.getText().toString());
                    newData.put("isXPrivate",xBox.isChecked());
                    newData.put("linkedin",linkedinText.getText().toString());
                    newData.put("isLinkedinPrivate",linkedinBox.isChecked());

                    if(accountType.equals("students")){
                        newData.put("department",departmentText.getText().toString());
                        newData.put("degree",degreeSpinner.getSelectedItem().toString());
                        newData.put("year",yearSpinner.getSelectedItem().toString());
                    }

                    db.collection(accountType).document(doc.getId()).update(newData).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(getApplicationContext(),"Başarıyla Kaydedildi",Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(),"Kaydederken bir hata oluştu: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                        }
                    });


                }
            }
        });

    }

    private void savePP(){
        String email = emailText.getText().toString();
        StorageReference ppRef = storage.getReference().child("profilePhotos").child(email+".png");

        Bitmap bitmap = ((BitmapDrawable) profileImage.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = ppRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                ppRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String imageUrl = uri.toString();

                        db.collection(accountType).whereEqualTo("email",email).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                                    db.collection(accountType).document(doc.getId()).update("profileImageUrl",imageUrl);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void fetchData(String email){

        if(email.endsWith("@std.yildiz.edu.tr")){
            accountType = "students";
            accountTypeText.setText("Öğrenci");
        }else{
            accountType = "instructors";
            accountTypeText.setText("Akademisyen");
        }

        db.collection(accountType).whereEqualTo("email",email).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){

                    if(!doc.getString("profileImageUrl").isEmpty()){
                        Picasso.get().load(doc.getString("profileImageUrl")).into(profileImage);
                    }

                    emailText.setText(doc.get("email").toString());
                    nameText.setText(doc.get("nameSurname").toString());
                    emailBox.setChecked(doc.getBoolean("isEmailPrivate"));
                    phoneNumberText.setText(doc.get("phoneNumber").toString());
                    phoneNumberBox.setChecked(doc.getBoolean("isPhoneNumberPrivate"));
                    instagramText.setText(doc.get("instagram").toString());
                    instagramBox.setChecked(doc.getBoolean("isInstagramPrivate"));
                    xText.setText(doc.get("x").toString());
                    xBox.setChecked(doc.getBoolean("isXPrivate"));
                    linkedinText.setText(doc.get("linkedin").toString());
                    linkedinBox.setChecked(doc.getBoolean("isLinkedinPrivate"));

                    Bundle extras = getIntent().getExtras();
                    if(!extras.getBoolean("isSelf")){
                        if(doc.getBoolean("isEmailPrivate")){
                            emailText.setText("*****");
                        }
                        if(doc.getBoolean("isPhoneNumberPrivate")){
                            phoneNumberText.setText("*****");
                        }
                        if(doc.getBoolean("isInstagramPrivate")){
                            instagramText.setText("*****");
                        }
                        if(doc.getBoolean("isXPrivate")){
                            xText.setText("*****");
                        }
                        if(doc.getBoolean("isLinkedinPrivate")){
                            linkedinText.setText("*****");
                        }
                    }

                    if(accountType.equals("students")){
                        idText.setText("Öğrenci No: " + doc.get("studentNo").toString());
                        departmentText.setText(doc.get("department").toString());

                        if(!doc.get("degree").toString().isEmpty()){
                            degreeSpinner.setSelection(getIndex(degreeSpinner, doc.get("degree").toString()));
                        }
                        if(!doc.get("year").toString().isEmpty()){
                            yearSpinner.setSelection(getIndex(yearSpinner, doc.get("year").toString()));
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
    }

    //private method of your class
    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }
        return 0;
    }

    public static boolean checkAndRequestPermissions(final Activity context) {
        int storagePermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded
                    .add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(context, listPermissionsNeeded
                            .toArray(new String[listPermissionsNeeded.size()]),
                    PERM_CODE);
            return false;
        }
        return true;
    }

    private void chooseImage(Context context){
        final CharSequence[] optionsMenu = {"Fotoğraf Çek", "Galeriden Seç"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setItems(optionsMenu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(optionsMenu[i].equals("Fotoğraf Çek")){
                    // Open the camera and get the photo
                    Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture,CAMERA_REQUEST_CODE );
                }
                else if(optionsMenu[i].equals("Galeriden Seç")){
                    // choose from  external storage
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto , STORAGE_REQUEST_CODE);
                }
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERM_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                chooseImage(ProfileActivity.this);
            }else {
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case CAMERA_REQUEST_CODE:
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        profileImage.setImageBitmap(selectedImage);
                        savePP();
                    }
                    break;
                case STORAGE_REQUEST_CODE:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                profileImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                savePP();
                                cursor.close();
                            }
                        }
                    }
                    break;
            }
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null){
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }
}