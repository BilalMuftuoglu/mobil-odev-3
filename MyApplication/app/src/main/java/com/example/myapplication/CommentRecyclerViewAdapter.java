package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommentRecyclerViewAdapter extends RecyclerView.Adapter<CommentRecyclerViewAdapter.ViewHolder>{
    private Context context;
    private ArrayList<Map<String,Object>> commentList;

    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String email;

    public CommentRecyclerViewAdapter(Context context, ArrayList<Map<String,Object>> commentList) {
        this.commentList = commentList;
        this.context = context;

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = mAuth.getCurrentUser().getEmail();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String,Object> comment = commentList.get(position);

        holder.usernameText.setText(comment.get("username").toString());
        holder.dateText.setText(comment.get("date").toString());
        holder.commentText.setText(comment.get("comment").toString());
        if(!comment.get("profileImageUrl").toString().isEmpty()){
            Picasso.get().load(comment.get("profileImageUrl").toString()).into(holder.profileImage);
        }else{
            holder.profileImage.setImageResource(R.drawable.baseline_account_circle_24);
        }

        if(!email.equals(comment.get("email").toString())) {
            holder.deleteCommentButton.setVisibility(View.INVISIBLE);
        }else{
            holder.deleteCommentButton.setVisibility(View.VISIBLE);
            holder.deleteCommentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(view,comment, position);
                }
            });
        }
    }

    private void showPopupMenu(View view, Map<String, Object> comment, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.getMenuInflater().inflate(R.menu.post_and_comment_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.deleteAction:
                        db.collection("posts").document(comment.get("postId").toString()).collection("comments").document(comment.get("commentId").toString()).delete();
                        commentList.remove(position);
                        notifyDataSetChanged();
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
        return commentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, dateText, commentText;
        ImageView profileImage;
        ImageButton deleteCommentButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameTextView);
            dateText = itemView.findViewById(R.id.dateTextView);
            commentText = itemView.findViewById(R.id.commentTextView);
            profileImage = itemView.findViewById(R.id.profileImage);
            deleteCommentButton = itemView.findViewById(R.id.deleteCommentButton);
        }
    }
}
