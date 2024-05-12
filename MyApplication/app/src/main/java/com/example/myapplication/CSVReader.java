package com.example.myapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
    Context context;
    Uri uri;
    List<String[]> rows = new ArrayList<>();

    public CSVReader(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
    }

    public List<String[]> readCSV() throws IOException {
        String filePath = getFilePathFromUri(uri);
        System.out.println(filePath);
        System.out.println(uri);
        InputStream is = context.getContentResolver().openInputStream(uri);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        String csvSplitBy = ";";

        br.readLine();

        while ((line = br.readLine()) != null) {
            String[] row = line.split(csvSplitBy);
            rows.add(row);
        }
        br.close();
        return rows;
    }

    public String getFilePathFromUri(Uri uri){
        String[] filename1;
        String fn;
        String filepath=uri.getPath();
        String filePath1[]=filepath.split(":");
        filename1 =filepath.split("/");
        fn=filename1[filename1.length-1];
        return Environment.getExternalStorageDirectory().getPath()+"/"+filePath1[1];
    }
}
