package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class PollMainActivity extends AppCompatActivity {
    private String courseId, accountType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_main);

        Bundle extras = getIntent().getExtras();
        courseId = extras.getString("courseId");
        accountType = extras.getString("accountType");
        setTitle(courseId + " Sınıfı Anketleri");

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.polls_menu, menu);
        if(!accountType.equals("instructors")){
            menu.removeItem(R.id.addPostButton);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.addPollButton){
            startActivity(new Intent(PollMainActivity.this, PollAddActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

}