package com.example.myapplication;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PollDetailActivity extends AppCompatActivity {
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String pollId;
    int optionCount;
    String email;
    String question;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_poll_detail);
        PieChart pieChart = findViewById(R.id.pieChart);
        BarChart barChart = findViewById(R.id.barChart);
        ImageButton saveCsv = findViewById(R.id.saveCsvButton);
        ImageButton saveBarChart = findViewById(R.id.saveBarChartButton);
        ImageButton savePieChart = findViewById(R.id.savePieChartButton);

        //get extra intent
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        pollId = extras.getString("pollId");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        email = mAuth.getCurrentUser().getEmail();
        if(email.endsWith("@std.yildiz.edu.tr")){
            //Student
            saveBarChart.setVisibility(View.INVISIBLE);
            saveBarChart.setEnabled(false);
            savePieChart.setVisibility(View.INVISIBLE);
            savePieChart.setEnabled(false);
        }else{
            //Instructor
            saveBarChart.setVisibility(View.VISIBLE);
            saveBarChart.setEnabled(true);
            savePieChart.setVisibility(View.VISIBLE);
            savePieChart.setEnabled(true);
        }
        //initalize a list that contains the options
        Map<String,Object> options = new HashMap<>();

        db.collection("polls").document(pollId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().get("email").toString().equals(email)){
                    saveCsv.setVisibility(View.INVISIBLE);
                    saveCsv.setEnabled(false);
                }
            }
        });
        //get the option from the database
        db.collection("polls").document(pollId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                optionCount = Integer.parseInt(task.getResult().get("optionCount").toString());
                question = task.getResult().get("question").toString();
                for(int i = 1; i < optionCount+1; i++){
                    options.put("option"+i,task.getResult().get("option"+i));
                    options.put("option"+i+"Vote",task.getResult().get("option"+i+"Vote"));
                }
                ArrayList<BarEntry> barEntries = new ArrayList<>();
                for (int i = 1; i < optionCount+1; i++){
                    barEntries.add(new BarEntry(i, Integer.parseInt(options.get("option"+i+"Vote").toString())));
                }
                saveCsv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //save the data to a csv file
                        try {
                            File file = new File(getExternalFilesDir(null), question + ".csv");
                            CSVWriter writer = new CSVWriter(new FileWriter(file));
                            for (int i = 1; i < optionCount+1; i++){
                                String[] data = {options.get("option"+i).toString(), options.get("option"+i+"Vote").toString()};
                                writer.writeNext(data);
                            }
                            writer.close();

                            // Show a toast message
                            Toast.makeText(PollDetailActivity.this, "CSV saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                BarDataSet barDataSet = new BarDataSet(barEntries, "Options");
                barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                barDataSet.setValueTextColor(android.R.color.black);
                barDataSet.setValueTextSize(16f);

                BarData barData = new BarData(barDataSet);

                barChart.setFitBars(true);
                barChart.setData(barData);
                barChart.getDescription().setText(question);
                barChart.animateY(2000);
                //save the chart as a png file
                saveBarChart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        barChart.invalidate();
                        Bitmap chartBitmap = barChart.getChartBitmap();
                        saveImageToGallery(chartBitmap, question + "_bar_chart.png");
                        Toast.makeText(PollDetailActivity.this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                    }
                });


                ArrayList<PieEntry> pieEntries = new ArrayList<>();
                for (int i = 1; i < optionCount+1; i++){
                    pieEntries.add(new PieEntry(Integer.parseInt(options.get("option"+i+"Vote").toString()), options.get("option"+i).toString()));
                }
                PieDataSet pieDataSet = new PieDataSet(pieEntries, "Options");
                pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                pieDataSet.setValueTextColor(android.R.color.black);
                pieDataSet.setValueTextSize(16f);

                PieData pieData = new PieData(pieDataSet);

                pieChart.setData(pieData);
                pieChart.getDescription().setText(question);
                pieChart.animateY(2000);
                //save the chart as a png file
                savePieChart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pieChart.invalidate();
                        Bitmap chartBitmap = pieChart.getChartBitmap();
                        saveImageToGallery(chartBitmap, question + "_pie_chart.png");
                        //toast massage saved location
                        Toast.makeText(PollDetailActivity.this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void saveImageToGallery(Bitmap bitmap, String filename) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver resolver = getContentResolver();
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri imageUri = resolver.insert(collection, values);

        try {
            OutputStream out = resolver.openOutputStream(imageUri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(imageUri, values, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}