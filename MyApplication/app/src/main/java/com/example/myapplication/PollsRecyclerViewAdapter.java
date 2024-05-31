package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollsRecyclerViewAdapter extends RecyclerView.Adapter<PollsRecyclerViewAdapter.ViewHolder>{
    private Context context;
    private ArrayList<Map<String,Object>> pollsList;
    OptionsRecyclerViewAdapter optionsRecyclerViewAdapter;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String accountType;
    String email;
    ItemClickListener itemClickListener;
    public PollsRecyclerViewAdapter(Context context, ArrayList<Map<String, Object>> pollsList) {
        this.pollsList = pollsList;
        this.context = context;

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = mAuth.getCurrentUser().getEmail();

        if(email.endsWith("@std.yildiz.edu.tr")){
            accountType = "students";
        }else{
            accountType = "instructors";
        }
    }
    @NonNull
    @Override
    public PollsRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.polls_recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PollsRecyclerViewAdapter.ViewHolder holder, int position) {
        Map<String,Object> poll = pollsList.get(position);
        holder.usernameText.setText(poll.get("username").toString());
        holder.dateText.setText(poll.get("date").toString());
        holder.questionText.setText(poll.get("question").toString());
        holder.showhideCheckbox.setChecked((boolean)poll.get("status"));

        holder.questionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, PollDetailActivity.class);
                intent.putExtra("pollId", poll.get("pollId").toString());
                context.startActivity(intent);
            }
        });
        ArrayList<Map<String,Object>> options = new ArrayList<>();
        db.collection("polls").document(poll.get("pollId").toString()).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for(int i = 0; i < Integer.valueOf(poll.get("optionCount").toString()); i++){
                Map<String,Object> option = new HashMap<>();
                option.put("option"+(i+1), queryDocumentSnapshots.get("option"+(i+1)));
                options.add(option);
            }
            itemClickListener = new ItemClickListener() {
                @Override
                public void onClick(String s) {
                    holder.optionsRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            optionsRecyclerViewAdapter.notifyDataSetChanged();
                        }
                    });

                }
            };

            holder.optionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            optionsRecyclerViewAdapter = new OptionsRecyclerViewAdapter(options, itemClickListener,poll.get("pollId").toString());
            holder.optionsRecyclerView.setAdapter(optionsRecyclerViewAdapter);
        });


        if(!email.equals(poll.get("email").toString())) {
            holder.deleteButton.setVisibility(View.INVISIBLE);
            holder.showhideLayout.setVisibility(View.INVISIBLE);
        }else{
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.showhideLayout.setVisibility(View.VISIBLE);
            holder.showhideCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    db.collection("polls").document(poll.get("pollId").toString()).update("status",isChecked);
                }
            });
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(view,poll, position);
                }
            });
        }
    }


    private void showPopupMenu(View view, Map<String, Object> polls, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.getMenuInflater().inflate(R.menu.post_and_comment_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.deleteAction:
                        db.collection("polls").document(polls.get("pollId").toString()).collection("votes").get().addOnSuccessListener(queryDocumentSnapshots -> {
                            List<Task<Void>> tasks = new ArrayList<>();
                            for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                tasks.add(doc.getReference().delete());
                            }
                            Tasks.whenAll(tasks).addOnSuccessListener(aVoid -> {
                                db.collection("polls").document(polls.get("pollId").toString()).delete().addOnSuccessListener(aVoid1 -> {
                                    pollsList.remove(position);
                                    notifyDataSetChanged();
                                });
                            });
                        });
                        return true;
                    default:
                        return false;
                }
            }
        });

        popupMenu.show();
    }
    @Override
    public int getItemCount() {
        return pollsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, dateText, questionText;
        RecyclerView optionsRecyclerView;
        ImageButton deleteButton;
        LinearLayout showhideLayout;
        CheckBox showhideCheckbox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameTextView);
            dateText = itemView.findViewById(R.id.dateTextView);
            questionText = itemView.findViewById(R.id.questionTextView);
            optionsRecyclerView = itemView.findViewById(R.id.optionsRecyclerView);
            deleteButton = itemView.findViewById(R.id.deletePollButton);
            showhideLayout = itemView.findViewById(R.id.pollShowLinearLayout);
            showhideCheckbox = itemView.findViewById(R.id.showHidePollCheckBox);
        }
    }
}
