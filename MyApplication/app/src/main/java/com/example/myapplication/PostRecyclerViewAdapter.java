package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostRecyclerViewAdapter extends RecyclerView.Adapter<PostRecyclerViewAdapter.ViewHolder>{
    private Context context;
    private ArrayList<Map<String,Object>> postList;
    private String commentProfileImageUrl;
    private String myUsername;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String accountType;
    String email;
    private RequestQueue requestQueue;

    public PostRecyclerViewAdapter(Context context, ArrayList<Map<String,Object>> postList, String commentProfileImageUrl, String myUsername) {
        this.postList = postList;
        this.context = context;
        this.commentProfileImageUrl = commentProfileImageUrl;
        this.myUsername = myUsername;

        requestQueue = Volley.newRequestQueue(context);

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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_recycler_view_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Map<String,Object> post = postList.get(position);

        holder.usernameText.setText(post.get("username").toString());
        holder.dateText.setText(post.get("date").toString());
        holder.postText.setText(post.get("post").toString());

        if(!post.get("profileImageUrl").toString().isEmpty()){
            Picasso.get().load(post.get("profileImageUrl").toString()).into(holder.profileImage);
        }else{
            holder.profileImage.setImageResource(R.drawable.baseline_account_circle_24);
        }

        if(!commentProfileImageUrl.isEmpty()){
            Picasso.get().load(commentProfileImageUrl).into(holder.commentProfileImage);
        }else{
            holder.commentProfileImage.setImageResource(R.drawable.baseline_account_circle_24);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(holder.commentRecyclerView.getContext(), LinearLayoutManager.VERTICAL, false);

        ArrayList<Map<String,Object>> comments = new ArrayList<>();

        CommentRecyclerViewAdapter commentRecyclerViewAdapter = new CommentRecyclerViewAdapter(context, comments);
        holder.commentRecyclerView.setLayoutManager(layoutManager);
        holder.commentRecyclerView.setAdapter(commentRecyclerViewAdapter);

        fetchComments(comments,post,commentRecyclerViewAdapter);

        holder.sendCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String comment = holder.commentEditText.getText().toString().trim();
                if(!comment.isEmpty()){
                    addComment(comments,comment,post,commentRecyclerViewAdapter);
                    holder.commentEditText.setText("");
                }else{
                    holder.commentEditText.setError("Yorum boş olamaz!");
                }
            }
        });

        if(!email.equals(post.get("email").toString())) {
            holder.deletePostButton.setVisibility(View.INVISIBLE);
        }else{
            holder.deletePostButton.setVisibility(View.VISIBLE);
            holder.deletePostButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(view,post, position);
                }
            });
        }

        holder.showHideCommentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(holder.commentRecyclerView.getVisibility() == View.VISIBLE){
                    holder.commentRecyclerView.setVisibility(View.GONE);
                    holder.showHideCommentsButton.setText("Yorumları Göster");
                }else{
                    holder.commentRecyclerView.setVisibility(View.VISIBLE);
                    holder.showHideCommentsButton.setText("Yorumları Gizle");
                }
            }
        });
    }

    private void showPopupMenu(View view, Map<String, Object> post, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.getMenuInflater().inflate(R.menu.post_and_comment_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.deleteAction:
                        db.collection("posts").document(post.get("postId").toString()).collection("comments").get().addOnSuccessListener(queryDocumentSnapshots -> {
                            List<Task<Void>> tasks = new ArrayList<>();
                            for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                                tasks.add(doc.getReference().delete());
                            }
                            Tasks.whenAll(tasks).addOnSuccessListener(aVoid -> {
                                db.collection("posts").document(post.get("postId").toString()).delete().addOnSuccessListener(aVoid1 -> {
                                    postList.remove(position);
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

    public void fetchComments(ArrayList<Map<String,Object>> comments, Map<String,Object> post,CommentRecyclerViewAdapter commentRecyclerViewAdapter){
        comments.clear();
        db.collection("posts").document(post.get("postId").toString()).collection("comments").orderBy("date", Query.Direction.ASCENDING).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Map<String, Object>> tempCommentList = new ArrayList<>();

            for (DocumentSnapshot doc: queryDocumentSnapshots.getDocuments()){
                Map<String, Object> comment = doc.getData();

                Date date = doc.getTimestamp("date").toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy   HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(date);
                comment.put("date", formattedDate);
                comment.put("commentId", doc.getId());
                comment.put("postId", post.get("postId"));

                tempCommentList.add(comment);
            }

            List<Task<QuerySnapshot>> tasks = new ArrayList<>();

            for (Map<String, Object> comment : tempCommentList) {
                String email = comment.get("email").toString();
                Task<QuerySnapshot> task;
                if (email.endsWith("@std.yildiz.edu.tr")){
                    task = db.collection("students").whereEqualTo("email", email).get();
                }else{
                    task = db.collection("instructors").whereEqualTo("email", email).get();
                }
                tasks.add(task);
            }

            Tasks.whenAllSuccess(tasks).addOnSuccessListener(taskResults -> {
                for (int i = 0; i < taskResults.size(); i++) {
                    QuerySnapshot querySnapshot = (QuerySnapshot) taskResults.get(i);
                    Map<String, Object> comment = tempCommentList.get(i);

                    for (DocumentSnapshot doc2 : querySnapshot.getDocuments()) {
                        String username = doc2.getString("nameSurname");
                        comment.put("username", username);
                        String profileImageUrl = doc2.getString("profileImageUrl");
                        comment.put("profileImageUrl", profileImageUrl);
                    }

                    comments.add(comment);
                    commentRecyclerViewAdapter.notifyDataSetChanged();
                }
            });
        });
    }


    public void addComment(ArrayList<Map<String,Object>> commentList, String commentText, Map<String,Object> post, CommentRecyclerViewAdapter commentRecyclerViewAdapter){
        Map<String,Object> comment = new HashMap<>();

        comment.put("email",email);
        comment.put("date", FieldValue.serverTimestamp());
        comment.put("comment", commentText);

        db.collection("posts").document(post.get("postId").toString()).collection("comments").add(comment).addOnSuccessListener(documentReference -> {
            documentReference.get().addOnSuccessListener(documentSnapshot -> {
                Date date = documentSnapshot.getTimestamp("date").toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy   HH:mm", Locale.getDefault());
                String formattedDate = sdf.format(date);
                comment.put("date", formattedDate);
                comment.put("username", myUsername);
                comment.put("profileImageUrl", commentProfileImageUrl);
                comment.put("commentId", documentReference.getId());
                comment.put("postId", post.get("postId"));
                commentList.add(comment);
                commentRecyclerViewAdapter.notifyDataSetChanged();

                if (post.get("alert").toString().equals("true")){
                    sendNotification(commentText,post.get("courseId").toString());
                }
            });
        });
    }

    private void sendNotification(String commentText, String courseId){
        JSONObject object = new JSONObject();
        try {
            object.put("to","/topics/"+courseId);
            JSONObject notification = new JSONObject();
            notification.put("title",courseId + " sınıfında yeni duyuru!");
            notification.put("body",commentText);

            JSONObject data = new JSONObject();
            data.put("courseId",courseId);

            object.put("data",data);
            object.put("notification",notification);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, "https://fcm.googleapis.com/fcm/send", object, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    Toast.makeText(context,"Bildirim başarıyla gönderildi!",Toast.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Toast.makeText(context,volleyError.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                }
            }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> headers = new HashMap<>();
                    headers.put("Content-Type","application/json");
                    headers.put("Authorization","key=AAAAIIZbZG4:APA91bFo6e_rJOaLmJad1BGcNU49V_8WqqVcPR9uf2G0YXcY4sFMjxFMKG-Q4Ijyx4nrfVDNscLiKBYB1vMUaqaCBsXUmONKrZvURgp8g49Bs0ZNSRYSBB5qkuiq87lPE4fVMtVAD73q");
                    return headers;
                }
            };

            requestQueue.add(request);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, dateText, postText;
        ImageView profileImage, commentProfileImage;
        RecyclerView commentRecyclerView;
        EditText commentEditText;
        ImageButton sendCommentButton, deletePostButton;
        TextView showHideCommentsButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameTextView);
            dateText = itemView.findViewById(R.id.dateTextView);
            postText = itemView.findViewById(R.id.postTextView);
            profileImage = itemView.findViewById(R.id.profileImage);
            commentProfileImage = itemView.findViewById(R.id.commentProfileImage);
            commentRecyclerView = itemView.findViewById(R.id.commentRecyclerView);
            commentEditText = itemView.findViewById(R.id.commentEditText);
            sendCommentButton = itemView.findViewById(R.id.sendCommentButton);
            deletePostButton = itemView.findViewById(R.id.deletePostButton);
            showHideCommentsButton = itemView.findViewById(R.id.showHideCommentsButton);
        }
    }
}
