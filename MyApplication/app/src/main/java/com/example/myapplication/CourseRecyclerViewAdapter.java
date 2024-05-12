package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

public class CourseRecyclerViewAdapter extends RecyclerView.Adapter<CourseRecyclerViewAdapter.MyViewHolder>{

    public Context context;
    private ArrayList<Map<String,String>> courseList;
    private static ItemClickListener mClickListener;
    private static ItemLongClickListener mLongClickListener;

    public CourseRecyclerViewAdapter(Context context,ArrayList<Map<String,String>> courseList){
        this.courseList = courseList;
        this.context = context;
    }

    @NonNull
    @Override
    public CourseRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.course_recycler_view_item,parent,false);

        return new CourseRecyclerViewAdapter.MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseRecyclerViewAdapter.MyViewHolder holder, int position) {
        Map<String,String> course = courseList.get(position);

        holder.courseNameText.setText(course.get("courseName"));
        holder.courseIdText.setText(course.get("courseId"));
        if(course.get("isCompleted").equals("true")){
            holder.courseStatusText.setText("Tamamlandı");
            holder.statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.green));
        }else{
            holder.courseStatusText.setText("Devam Ediyor");
            holder.statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.orange));
        }

    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    Map<String,String> getItem(int id) {
        return courseList.get(id);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{

        TextView courseNameText, courseIdText,courseStatusText;
        CardView statusCard;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);


            courseNameText = itemView.findViewById(R.id.courseNameText);
            courseIdText = itemView.findViewById(R.id.courseIdText);
            courseStatusText = itemView.findViewById(R.id.courseStatusText);
            statusCard = itemView.findViewById(R.id.statusCard);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            if (mLongClickListener != null) mLongClickListener.onItemLongClick(view, getAdapterPosition());
            return true;
        }
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    void setLongClickListener(ItemLongClickListener itemLongClickListener) {
        this.mLongClickListener = itemLongClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface ItemLongClickListener {
        void onItemLongClick(View view, int position);
    }

}
