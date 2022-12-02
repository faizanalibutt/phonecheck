package com.upgenicsint.phonecheck.misc;

import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.upgenicsint.phonecheck.models.Column;
import com.upgenicsint.phonecheck.models.Grade;
import com.upgenicsint.phonecheck.models.GradeChild;
import com.upgenicsint.phonecheck.models.TestModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class ReadTestJsonFile {

    private TextView error;
    private RecyclerView hideMe;
    private String jsonStr = null;
    private File jsonFile = null;
    private FileInputStream stream = null;

    public ReadTestJsonFile(TextView textView, RecyclerView recyclerView) {
        this.error = textView;
        this.hideMe = recyclerView;
    }

    public ReadTestJsonFile() { }

    public static ReadTestJsonFile getInstance() {
        return new ReadTestJsonFile();
    }

    private void setProperties(int visibility, String text, int visibilty) {
        error.setText(text);
        error.setVisibility(visibility);
        hideMe.setVisibility(visibilty);
    }

    public String returnNewObject(File file) {
        if (file != null) {
            try {
                stream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            return jsonStr;
        }
        return null;
    }



    public List<TestModel> getTestsList() {
        if (jsonFile == null) {
            jsonFile = new File(Environment.getExternalStorageDirectory(), "TestList.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
            setProperties(View.VISIBLE, "File not readable", View.INVISIBLE);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    setProperties(View.VISIBLE, "File not readable", View.INVISIBLE);
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                String jsonobj = jsonObj.getString("Test");
                List<TestModel> testsList = new ArrayList<>();
                StringTokenizer testObj = new StringTokenizer(jsonobj, ",");
                while (testObj.hasMoreTokens()) {
                    TestModel testModel = new TestModel(testObj.nextToken());
                    testsList.add(testModel);
                }
                return testsList;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

        } else {
            setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
            return null;
        }
    }

    public List<TestModel> getCustomizationList() {
        if (jsonFile == null) {
            jsonFile = new File(Environment.getExternalStorageDirectory(), "ClientCustomization.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
            setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);

                if (jsonObj.length() == 0) {
                    setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                }

                List<TestModel> testsList = new ArrayList<>();
                Iterator<String> iter = jsonObj.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        Object value = jsonObj.get(key);
                        TestModel testModel = new TestModel(key + " = " + value.toString());
                        testsList.add(testModel);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return testsList;
            } catch (JSONException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                return null;
            }
        } else {
            setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
            return null;
        }
    }

    public Column getColumnApi() {
        if (jsonFile == null) {
            File fileMainPath;
            File sdcard = new File("/sdcard");
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            jsonFile = new File(fileMainPath, "BatteryApi.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            if (stream != null) {
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                jsonStr = Charset.defaultCharset().decode(bb).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                try {
                    return new Column(jsonObj.getString("LicenseID"), jsonObj.getString("Serial"),
                            jsonObj.getInt("TransactionID"), "", "",
                            "", "", "", "");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public JSONObject getAndroidConfiguration() {
        if (jsonFile == null) {
            File fileMainPath;
            File sdcard = new File("/sdcard");
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            jsonFile = new File(fileMainPath, "AndroidDeviceConfiguration.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            if (stream != null) {
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                jsonStr = Charset.defaultCharset().decode(bb).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                return jsonObj;
            } catch (JSONException e) {
                e.printStackTrace();
                return new JSONObject();
            }
        } else {
            return new JSONObject();
        }
    }

    public Grade getGrades() {
        if (jsonFile == null) {
            File fileMainPath;
            File sdcard = new File("/sdcard");
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            jsonFile = new File(fileMainPath, "GradeConfig.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            if (stream != null) {
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                jsonStr = Charset.defaultCharset().decode(bb).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                JSONArray gradeObject = jsonObj.getJSONArray("Grades");
                JSONArray additionalObject = jsonObj.getJSONArray("Additional");
                List<GradeChild> gradeList = new ArrayList<>();
                List<GradeChild> additionalList = new ArrayList<>();
                for (int i = 0; i < gradeObject.length(); i++) {
                    gradeList.add(new GradeChild(gradeObject.getString(i), i, false));
                }
                for (int j = 0; j < additionalObject.length(); j++) {
                    additionalList.add(new GradeChild(additionalObject.getString(j), j, false));
                }
                return new Grade(gradeList, additionalList);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public List<TestModel> getAllFilesOfNoObjName(String fileName) {
        if (jsonFile == null) {
            File fileMainPath;
            File sdcard = new File(Constants.SD_CARD);
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            jsonFile = new File(fileMainPath, fileName);
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
            setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                List<TestModel> testsList = new ArrayList<>();
                Iterator<String> iter = jsonObj.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        Object value = jsonObj.get(key);
                        String svalue = value.toString();
                        TestModel testModel = new TestModel(key + " = " + svalue);
                        testsList.add(testModel);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return testsList;
            } catch (JSONException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                return null;
            }

        } else {
            setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
            return null;
        }
    }

    public List<TestModel> getTestResults() {
        if (jsonFile == null) {
            File fileMainPath;
            File sdcard = new File("/sdcard");
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            jsonFile = new File(fileMainPath, "TestResults.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
            setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                List<TestModel> testsList = new ArrayList<>();
                Iterator<String> iter = jsonObj.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        Object value = jsonObj.get(key);
                        String svalue = value.toString();
                        if (svalue.equals("1")) {
                            svalue = "Fail";
                        }
                        if (svalue.equals("0")) {
                            svalue = "Pass";
                        }
                        if (svalue.equals("2")) {
                            svalue = "Not Performed";
                        }
                        TestModel testModel = new TestModel(key + " = " + svalue);
                        testsList.add(testModel);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return testsList;
            } catch (JSONException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                return null;
            }

        } else {
            setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
            return null;
        }
    }

    public List<TestModel> getAndroidDeviceConfig() {
        if (jsonFile == null) {
            File fileMainPath;
            File sdcard = new File("/sdcard");
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            jsonFile = new File(fileMainPath, "AndroidDeviceConfiguration.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
            setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                List<TestModel> testsList = new ArrayList<>();
                Iterator<String> iter = jsonObj.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        Object value = jsonObj.get(key);
                        String svalue = value.toString();
                        /*if (svalue.equals("1")) {
                            svalue = "Fail";
                        }
                        if (svalue.equals("0")) {
                            svalue = "Pass";
                        }
                        if (svalue.equals("2")) {
                            svalue = "Not Performed";
                        }*/
                        TestModel testModel = new TestModel(key + " = " + svalue);
                        testsList.add(testModel);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return testsList;
            } catch (JSONException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                return null;
            }

        } else {
            setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
            return null;
        }
    }

    public List<TestModel> getTimeResults() {
        if (jsonFile == null) {
            File fileMainPath;
            File sdcard = new File("/sdcard");
            if (sdcard.exists()) {
                fileMainPath = sdcard;
            } else {
                fileMainPath = Environment.getExternalStorageDirectory();
            }
            jsonFile = new File(fileMainPath, "time.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
            setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                JSONObject tests = jsonObj.getJSONObject("Tests");
                List<TestModel> testsList = new ArrayList<>();
                Iterator<String> iter = tests.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        Object value = tests.get(key);
                        String svalue = value.toString();
                        TestModel testModel = new TestModel(key + " = " + svalue);
                        testsList.add(testModel);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                    }
                }
                return testsList;
            } catch (JSONException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                return null;
            }

        } else {
            setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
            return null;
        }
    }

    public List<TestModel> getCosmeticsResults() {
        if (jsonFile == null) {
            jsonFile = new File(Environment.getExternalStorageDirectory(), "CosmeticResults.json");
            try {
                stream = new FileInputStream(jsonFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
            }
        }
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonStr = Charset.defaultCharset().decode(bb).toString();
        } catch (Exception e) {
            e.printStackTrace();
            setProperties(View.VISIBLE, "File not exists", View.INVISIBLE);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            JSONObject jsonObj;
            try {
                jsonObj = new JSONObject(jsonStr);
                List<TestModel> testsList = new ArrayList<>();
                Iterator<String> iter = jsonObj.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    Object value = jsonObj.get(key);
                    String svalue = value.toString();
                    if (svalue.equals("1")) {
                        svalue = "Fail";
                    }
                    if (svalue.equals("0")) {
                        svalue = "Pass";
                    }
                    if (svalue.equals("2")) {
                        svalue = "Not Performed";
                    }
                    TestModel testModel = new TestModel(key + " = " + svalue);
                    testsList.add(testModel);
                }
                return testsList;
            } catch (JSONException e) {
                e.printStackTrace();
                setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
                return null;
            }

        } else {
            setProperties(View.VISIBLE, "Nothing to show", View.INVISIBLE);
            return null;
        }
    }
}