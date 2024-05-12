package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

public class ReportRecyclerViewAdapter extends RecyclerView.Adapter<ReportRecyclerViewAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<Map<String, Object>> reportList;

    public ReportRecyclerViewAdapter(Context context, ArrayList<Map<String, Object>> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.report_recycler_view_item,parent,false);
        return new ReportRecyclerViewAdapter.MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Map<String,Object> report = reportList.get(position);

        if(report.get("scope").toString().equals("Uygulama")){
            ((ViewGroup) holder.courseIdLayout.getParent()).removeView(holder.courseIdLayout);
        }else{
            holder.courseIdText.setText(report.get("courseId").toString());
        }

        holder.senderText.setText(report.get("sentBy").toString());
        holder.dateText.setText(report.get("date").toString());
        holder.subjectText.setText(report.get("subject").toString());
        holder.bodyText.setText(report.get("body").toString());

    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView senderText, dateText, courseIdText, subjectText, bodyText;
        LinearLayout courseIdLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            senderText = itemView.findViewById(R.id.senderText);
            dateText = itemView.findViewById(R.id.dateText);
            courseIdText = itemView.findViewById(R.id.courseIdText);
            subjectText = itemView.findViewById(R.id.subjectText);
            bodyText = itemView.findViewById(R.id.bodyText);
            courseIdLayout = itemView.findViewById(R.id.courseIdLayout);
        }
    }
}
