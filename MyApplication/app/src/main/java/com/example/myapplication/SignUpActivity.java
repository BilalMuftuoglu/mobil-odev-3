package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;


public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    TextView loginText;
    EditText nameText,idText, emailText, passwordText;
    Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        loginText = findViewById(R.id.loginText);
        nameText = findViewById(R.id.nameText);
        idText = findViewById(R.id.idText);
        emailText = findViewById(R.id.emailText);
        passwordText = findViewById(R.id.passwordText);
        registerButton = findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }
        });

        emailText.addTextChangedListener(textWatcher);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nameText.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Lütfen isim alanını doldurunuz!", Toast.LENGTH_LONG).show();
                } else if (emailText.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Lütfen email alanını doldurunuz!", Toast.LENGTH_LONG).show();
                } else if (passwordText.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Lütfen şifre alanını doldurunuz!", Toast.LENGTH_LONG).show();
                } else if (!emailText.getText().toString().trim().endsWith("@std.yildiz.edu.tr") && !emailText.getText().toString().trim().endsWith("@yildiz.edu.tr")) {
                    Toast.makeText(getApplicationContext(), "Lütfen akademisyen veya öğrenci mail adresi ile üye olunuz!", Toast.LENGTH_LONG).show();
                } else {
                    String email = emailText.getText().toString().trim();
                    String password = passwordText.getText().toString().trim();
                    String name = nameText.getText().toString().trim();
                    String studentNo = idText.getText().toString().trim();
                    signup(name,studentNo,email, password);
                }
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


    public void signup(String name, String studentNo,String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {

                Map<String, Object> user = new HashMap<>();
                user.put("profileImageUrl","");
                user.put("nameSurname", name);
                user.put("email", email);
                user.put("isEmailPrivate",false);
                user.put("phoneNumber","");
                user.put("isPhoneNumberPrivate",false);
                user.put("instagram","");
                user.put("isInstagramPrivate",false);
                user.put("x","");
                user.put("isXPrivate",false);
                user.put("linkedin","");
                user.put("isLinkedinPrivate",false);

                String collectionName;
                if(email.contains("@std.yildiz.edu.tr")){
                    collectionName = "students";
                    user.put("studentNo", studentNo);
                    user.put("department","");
                    user.put("degree","");
                    user.put("year","");
                }else{
                    collectionName = "instructors";
                }

                db.collection(collectionName)
                        .add(user)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null){
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // this function is called before text is edited
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // this function is called when text is edited
            String email = emailText.getText().toString().trim();
            if(!email.isEmpty()){
                if(email.endsWith("@yildiz.edu.tr")){
                    idText.setEnabled(false);
                    idText.setText("");
                    idText.setHint("Sadece öğrenciler numara girebilir!");
                }else if(email.endsWith("@std.yildiz.edu.tr")){
                    idText.setEnabled(true);
                    idText.setHint("Öğrenci No:");
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // this function is called after text is edited
        }
    };
}