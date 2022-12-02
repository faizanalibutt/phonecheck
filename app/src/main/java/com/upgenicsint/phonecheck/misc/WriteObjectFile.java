package com.upgenicsint.phonecheck.misc;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

public class WriteObjectFile {

    private File file;
    private FileInputStream fileIn;
    private FileOutputStream fileOut;
    private ObjectInputStream objectIn;
    private ObjectOutputStream objectOut;
    private Object outputObject;
    private String filePath;
    private OutputStreamWriter myOutWriter;


    private WriteObjectFile() { }

    public static WriteObjectFile getInstance() {
        return new WriteObjectFile();
    }

    public Object readObject(String fileName, Context context){
        try {
            filePath = context.getFilesDir().getAbsolutePath() + "/" + fileName;
            fileIn = new FileInputStream(filePath);
            objectIn = new ObjectInputStream(fileIn);
            outputObject = objectIn.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return outputObject;
    }



    public void writeObject(String inputObject, String fileName){
        try {
            File fileMainPath;
            File sdcard = new File("/sdcard");
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            file = new File(fileMainPath, fileName);
            fileOut = new FileOutputStream(file);
            myOutWriter = new OutputStreamWriter(fileOut);
            myOutWriter.append(inputObject);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (myOutWriter != null) {
                try {
                    myOutWriter.close();
                    fileOut.flush();
                    fileOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
