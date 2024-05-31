package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PollListActivity extends AppCompatActivity {
    String courseId, accountType, email,myUsername;
    RecyclerView pollRecyclerView;
    PollsRecyclerViewAdapter pollsRecyclerViewAdapter;
    ArrayList<Map<String,Object>> pollList;
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_list);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        courseId = extras.getString("courseId");
        accountType = extras.getString("accountType");
        setTitle(courseId + " Sınıfı Anketleri");

        pollRecyclerView = findViewById(R.id.pollRecyclerView);
        pollRecyclerView.setHasFixedSize(true);
        pollRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        email = mAuth.getCurrentUser().getEmail();
        pollList = new ArrayList<>();

        fetchPolls();

        pollsRecyclerViewAdapter = new PollsRecyclerViewAdapter(this, pollList);
        pollRecyclerView.setAdapter(pollsRecyclerViewAdapter);






    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.polls_menu, menu);
        if(!accountType.equals("instructors")){
            menu.removeItem(R.id.addPollButton);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.addPollButton){
            Intent intent = new Intent(this, PollAddActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("accountType", accountType);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void fetchPolls(){
        // Fetch polls from database
        pollList.clear();

        Query query = db.collection("polls").whereEqualTo("courseId", courseId);
        if (accountType.equals("students")) {
            query = query.whereEqualTo("status", true);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Map<String, Object>> tempPollList = new ArrayList<>();

            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                Map<String, Object> poll = doc.getData();
                Date date = doc.getTimestamp("date").toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy   HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(date);
                poll.put("date", formattedDate);
                poll.put("pollId", doc.getId());
                tempPollList.add(poll);
            }
            List<Task<QuerySnapshot>> tasks = new ArrayList<>();

            for (Map<String, Object> poll : tempPollList) {
                String email = poll.get("email").toString();
                Task<QuerySnapshot> task = db.collection("instructors").whereEqualTo("email", email).get();
                tasks.add(task);
            }
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(taskResults -> {
                for (int i = 0; i < taskResults.size(); i++) {
                    QuerySnapshot querySnapshot = (QuerySnapshot) taskResults.get(i);
                    Map<String, Object> poll = tempPollList.get(i);

                    for (DocumentSnapshot doc2 : querySnapshot.getDocuments()) {
                        String username = doc2.getString("nameSurname");
                        poll.put("username", username);
                    }
                    pollList.add(poll);
                    pollsRecyclerViewAdapter.notifyDataSetChanged();
                }
                System.out.println(pollList);
            });
        });
    }

}