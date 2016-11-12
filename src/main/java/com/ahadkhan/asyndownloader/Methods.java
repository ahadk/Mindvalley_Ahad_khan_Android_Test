package com.ahadkhan.asyndownloader;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Methods {
    public static String mReadJsonData(File file) {
        try {
            FileInputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return  new String(buffer);
        } catch (IOException e) {
            return null;
        }
    }}
