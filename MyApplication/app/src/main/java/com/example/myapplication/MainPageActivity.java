package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainPageActivity extends AppCompatActivity implements UserRecyclerViewAdapter.ItemClickListener {

    SearchView searchView;
    RecyclerView myRecyclerView;
    UserRecyclerViewAdapter userRecyclerViewAdapter;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    ArrayList<Map<String,String>> users;
    ArrayList<Map<String,String>> filteredUsers;
    ProgressBar progressBar;
    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = mAuth.getCurrentUser().getEmail();

        users = new ArrayList<>();
        filteredUsers = new ArrayList<>();

        fetchData();

        setTitle("Tüm Kullanıcılar");
        myRecyclerView = findViewById(R.id.recyclerView);
        myRecyclerView.setHasFixedSize(true);
        myRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userRecyclerViewAdapter = new UserRecyclerViewAdapter(getApplicationContext(),filteredUsers);
        userRecyclerViewAdapter.setClickListener(this);
        myRecyclerView.setAdapter(userRecyclerViewAdapter);

        View rootLayout = findViewById(android.R.id.content);
        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard();
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                userRecyclerViewAdapter.filter(s,users);
                return false;
            }
        });

    }

    @Override
    public void onItemClick(View view, int position) {
        Intent intent = new Intent(MainPageActivity.this, ProfileActivity.class);
        intent.putExtra("email", userRecyclerViewAdapter.getItem(position).get("email"));
        intent.putExtra("isSelf",false);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_page_menu, menu);

        if(!email.endsWith("@yildiz.edu.tr") && !email.endsWith("@std.yildiz.edu.tr")){
            menu.removeItem(R.id.profileButton);
            menu.removeItem(R.id.coursesButton);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.logOutButton) {
            mAuth.signOut();
            Intent intent = new Intent(MainPageActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        }else if(id == R.id.profileButton){
            Intent intent = new Intent(MainPageActivity.this, ProfileActivity.class);
            intent.putExtra("email",mAuth.getCurrentUser().getEmail());
            intent.putExtra("isSelf",true);
            startActivity(intent);
        }else if (id == R.id.coursesButton){
            Intent intent = new Intent(MainPageActivity.this,CoursesActivity.class);
            startActivity(intent);
        }else if(id == R.id.reportButton){
            if(email.endsWith("@std.yildiz.edu.tr")){
                Intent intent = new Intent(MainPageActivity.this, ReportActivity.class);
                intent.putExtra("isFromCourse",false);
                startActivity(intent);
            }else{
                Intent intent = new Intent(MainPageActivity.this, ShowReportsActivity.class);
                startActivity(intent);
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null){
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    private void fetchData(){

        db.collection("instructors").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                    Map<String, String> user = new HashMap<>();
                    user.put("nameSurname",doc.getString("nameSurname"));
                    user.put("email",doc.getString("email"));
                    user.put("profileImageUrl",doc.getString("profileImageUrl"));
                    user.put("isEmailPrivate",doc.get("isEmailPrivate").toString());
                    user.put("accountType","Akademisyen");
                    users.add(user);
                    filteredUsers.add(user);
                }
                db.collection("students").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()) {
                            Map<String, String> user = new HashMap<>();
                            user.put("nameSurname",doc.getString("nameSurname"));
                            user.put("email",doc.getString("email"));
                            user.put("profileImageUrl",doc.getString("profileImageUrl"));
                            user.put("isEmailPrivate",doc.get("isEmailPrivate").toString());
                            user.put("accountType","Öğrenci");
                            users.add(user);
                            filteredUsers.add(user);
                        }
                        //Toast.makeText(getApplicationContext(),filteredUsers.toString(),Toast.LENGTH_LONG).show();
                        ((ViewGroup) progressBar.getParent()).removeView(progressBar);
                        userRecyclerViewAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

}