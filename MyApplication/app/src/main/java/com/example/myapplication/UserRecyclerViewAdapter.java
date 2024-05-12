package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class UserRecyclerViewAdapter extends RecyclerView.Adapter<UserRecyclerViewAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<Map<String,String>> userList;
    private static ItemClickListener mClickListener;

    public UserRecyclerViewAdapter(Context context, ArrayList<Map<String,String>> userList) {
        this.userList = userList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(context).inflate(R.layout.user_recycler_view_item,parent,false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserRecyclerViewAdapter.MyViewHolder holder, int position) {

        Map<String,String> user = userList.get(position);

        if(!user.get("profileImageUrl").isEmpty()){
            Picasso.get().load(user.get("profileImageUrl")).into(holder.profileImage);
        }else{
            holder.profileImage.setImageResource(R.drawable.baseline_account_circle_24);
        }

        holder.nameText.setText(user.get("nameSurname"));
        if(user.get("isEmailPrivate").equals("false")){
            holder.emailText.setText(user.get("email"));
        }else{
            holder.emailText.setText("*****");
        }

        holder.accountTypeText.setText(user.get("accountType"));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }



    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView profileImage;
        TextView nameText, emailText,accountTypeText;

         public MyViewHolder(@NonNull View itemView) {
             super(itemView);

             profileImage = itemView.findViewById(R.id.profileImage);
             nameText = itemView.findViewById(R.id.nameText);
             emailText = itemView.findViewById(R.id.emailText);
             accountTypeText = itemView.findViewById(R.id.accountTypeText);

             itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    Map<String,String> getItem(int id) {
        return userList.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public void filter(String charText, ArrayList<Map<String,String>> users) {
        charText = charText.toLowerCase(Locale.getDefault());
        userList.clear();
        if (charText.isEmpty()) {
            userList.addAll(users);
        } else {
            for (Map<String, String> user : users) {
                if (user.get("nameSurname").toLowerCase(Locale.getDefault()).contains(charText)) {
                    userList.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }
}
