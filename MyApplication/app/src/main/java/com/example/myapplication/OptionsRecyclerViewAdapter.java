package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OptionsRecyclerViewAdapter extends RecyclerView.Adapter<OptionsRecyclerViewAdapter.ViewHolder> {
    private ArrayList<Map<String,Object>> optionList;
    private String pollId;
    ItemClickListener itemClickListener;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String userEmail, accountType, email;
    int optionSelected = -1;
    public OptionsRecyclerViewAdapter(ArrayList<Map<String,Object>> optionList, ItemClickListener itemClickListener,String pollId) {
        this.optionList = optionList;
        this.itemClickListener = itemClickListener;
        this.pollId = pollId;

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userEmail = mAuth.getCurrentUser().getEmail();
        if(userEmail.endsWith("@std.yildiz.edu.tr")){
            accountType = "students";
        }else{
            accountType = "instructors";
        }
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.options_recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionsRecyclerViewAdapter.ViewHolder holder, int position) {
        Map<String,Object> option = optionList.get(position);
        Object optionObject = option.get("option"+(position+1));
        if (optionObject != null) {
            holder.optionText.setText(optionObject.toString());
        }

        db.collection("polls").document(pollId).collection("votes").whereEqualTo("email", userEmail).get().addOnSuccessListener(queryDocumentSnapshots -> {
            queryDocumentSnapshots.getDocuments().forEach(documentSnapshot -> {
                optionSelected = Integer.parseInt(documentSnapshot.get("vote").toString());
                optionSelected--;
                // Check if the current option is the selected one
                if (position == optionSelected) {
                    holder.radioButton.setChecked(true);
                } else {
                    holder.radioButton.setChecked(false);
                }
            });
        });

        for(int i = 0; i < optionList.size(); i++) {
            int optionNo = i+1;
            db.collection("polls").document(pollId).collection("votes").whereEqualTo("vote", i + 1).get().addOnSuccessListener(queryDocumentSnapshots -> {
                int totalVotes = 0;
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    totalVotes++;
                }
                String fieldName = "option" + optionNo + "Vote";
                db.collection("polls").document(pollId).update(fieldName, totalVotes);
            });
        }
        db.collection("polls").document(pollId).collection("votes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            int totalVotes = 0;
            for(DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                totalVotes++;
            }
            db.collection("polls").document(pollId).update("totalVotes",totalVotes);
        });
        holder.radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    optionSelected = holder.getAdapterPosition();
                    Map<String, Object> voters = new HashMap<>();
                    voters.put("email", userEmail);
                    voters.put("vote", optionSelected+1);
                    db.collection("polls").document(pollId).collection("votes").document(userEmail).set(voters, SetOptions.merge());

                    if (itemClickListener != null && holder.radioButton.getText() != null) {
                        itemClickListener.onClick(holder.radioButton.getText().toString());
                    }
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return  position;
    }

    @Override
    public int getItemCount() {

        return optionList.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView optionText;
        RadioButton radioButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.optionRadioButton);
            optionText = itemView.findViewById(R.id.optionTextView);
        }
    }
}
