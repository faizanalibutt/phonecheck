package com.upgenicsint.phonecheck.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.CameraError;
import com.androidhiddencamera.CameraPreview;
import com.androidhiddencamera.HiddenCameraActivity;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;
import com.androidhiddencamera.config.CameraRotation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.misc.AlertButtonListener;
import com.upgenicsint.phonecheck.models.Cosmetics;
import com.upgenicsint.phonecheck.test.SubTest;
import com.upgenicsint.phonecheck.test.Test;
import com.upgenicsint.phonecheck.test.chip.AutofocusTest;
import com.upgenicsint.phonecheck.utils.DialogUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import radonsoft.net.rta.*;

public class FlashTestActivity extends HiddenCameraActivity {
    private CameraConfig mCameraConfig;

    Button flashTest, withFlash, withoutFlash;
    ImageView img1, img2, testStatusImg;
    TextView txt1, txt2, resultDifference;

    private CameraPreview mCameraPreview;

    Button navBack, navDone, navTitle;

    private static int RESULT_LOAD_IMAGE = 1;
    Bitmap bitmap1, bitmap2, bitmap;
    String rgbValue1, rgbValue2;
    long Red = 0, Green = 0, Blue = 0;
    double luminance1, luminance2;
    String luminosity1, luminosity2;
    ProgressDialog progressDialog;
    String path = Environment.getExternalStorageDirectory().toString();
    DeviceTestableActivity deviceTestableActivity;

    public static boolean flashTestStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_flash_test);
        flashTest = findViewById(R.id.flash);
        img1 = findViewById(R.id.imageView1);
        img2 = findViewById(R.id.imageView2);
        txt1 = findViewById(R.id.textView1);
        txt2 = findViewById(R.id.textView2);
        withFlash = findViewById(R.id.withfla);
        withoutFlash = findViewById(R.id.withoutfla);
        navBack = findViewById(R.id.nav_back);
        navDone = findViewById(R.id.nav_done);
        navTitle = findViewById(R.id.nav_title);
        testStatusImg = findViewById(R.id.testPassFail);
        resultDifference = findViewById(R.id.difference);

        navTitle.setText("Flash Test");

        File file = new File(path, "Picture.jpeg");

        img1.setVisibility(View.INVISIBLE);
        img2.setVisibility(View.INVISIBLE);
        txt1.setVisibility(View.INVISIBLE);
        txt2.setVisibility(View.INVISIBLE);
        testStatusImg.setVisibility(View.INVISIBLE);

        deviceTestableActivity = new DeviceTestableActivity() {
        };

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.flashTest_prgs));
        progressDialog.setMessage(getString(R.string.flashTst_msg));

        mCameraConfig = new CameraConfig()
                .getBuilder(this)
                .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                .setImageRotation(CameraRotation.ROTATION_90)
                .build();

        //Check for the camera permission for the runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            //Start camera preview
            startCamera(mCameraConfig);

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

        AutofocusTest test = (AutofocusTest)FlashTestActivity.this.deviceTestableActivity.getTest();
        if(test != null) {
            test.setStatus(Test.FAILED);
            test.sub(Test.rearCameraFlashTestKey).setValue(Test.FAILED);
        }

        flashTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
//                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                takePicturewithoutFlash();

            }
        });
        withFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                takePicture();
            }
        });
        withoutFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                takePicturewithoutFlash();
            }
        });
        navBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        navDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flashTestStatus){
                    finish();
                    BaseActivity.shouldMoveToNextTest = true;
                }
                else{
                   onNavDoneClick(v);
                }
            }
        });
        testStatusImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flashTestStatus){
                    finish();
                    BaseActivity.shouldMoveToNextTest = true;
                }
                else{
                    onNavDoneClick(v);
                }
            }
        });
    }

    public final void onNavDoneClick(@NotNull View v) {
        Intrinsics.checkParameterIsNotNull(v, "v");
            this.showDoneAlert((AlertButtonListener)(new AlertButtonListener() {
                public void onClick(@NotNull DialogInterface dialog, @NotNull ButtonType type) {
                    Intrinsics.checkParameterIsNotNull(dialog, "dialog");
                    Intrinsics.checkParameterIsNotNull(type, "type");
                    if(Intrinsics.areEqual(type, ButtonType.RIGHT)) {
                        final Test test = deviceTestableActivity.getTest();
                        if(test != null) {
                            test.setStatus(Test.FAILED);
                            if(test.getHasSubTest()) {
                                Function1 predicate = (Function1)(new Function1() {
                                    // $FF: synthetic method
                                    // $FF: bridge method
                                    public Object invoke(Object var1) {
                                        return Boolean.valueOf(this.invoke((Map.Entry)var1));
                                    }

                                    public final boolean invoke(@NotNull Map.Entry it) {
                                        Intrinsics.checkParameterIsNotNull(it, "it");
                                        return test.resultsFilterMap.containsKey(it.getKey())?Intrinsics.areEqual((Boolean)test.resultsFilterMap.get(it.getKey()), Boolean.valueOf(true)):true;
                                    }
                                });
                                Map $receiver$iv = (Map)test.subTests;
                                Map destination$iv$iv = (Map)(new LinkedHashMap());
                                Iterator var9 = $receiver$iv.entrySet().iterator();

                                while(var9.hasNext()) {
                                    Map.Entry element$iv$iv = (Map.Entry)var9.next();
                                    if(((Boolean)predicate.invoke(element$iv$iv)).booleanValue()) {
                                        destination$iv$iv.put(element$iv$iv.getKey(), element$iv$iv.getValue());
                                    }
                                }

                                Iterator var13 = destination$iv$iv.entrySet().iterator();

                                while(var13.hasNext()) {
                                    Map.Entry element$iv = (Map.Entry)var13.next();
                                    if(((SubTest)element$iv.getValue()).getValue() == Test.INIT) {
                                        ((SubTest)element$iv.getValue()).setValue(Test.FAILED);
                                    }
                                }
                            }
                        }

                        FlashTestActivity.this.setResult(-1);
                        FlashTestActivity.this.finish();
                    }

                    dialog.dismiss();
                }
            }));
        }
    public final void showDoneAlert(@NotNull AlertButtonListener listener) {
        Intrinsics.checkParameterIsNotNull(listener, "listener");
        AlertDialog alertDialog = DialogUtils.createConfirmationAlert(this, R.string.test_incomplete, R.string.continue_test, "Stay", "Continue", listener);
        if(!this.isFinishing()) {
            alertDialog.show();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        progressDialog.show();
//        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                takePicturewithoutFlash();
            }
        })).start();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //noinspection MissingPermission
                startCamera(mCameraConfig);
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        if(bitmap1 == null) {
            bitmap1 = bitmap;
        }
        else if(bitmap2 == null){
            bitmap2 = bitmap;
        }
        else {
            Log.d("Image", "Empty bitmap");
        }
        if(bitmap1 != null && luminosity1 == null){
            calculateRGB1 calculatergb1 = new calculateRGB1();
            calculatergb1.execute();
        }
        else if(bitmap2 != null && luminosity2 == null){
            calculateRGB2 calculatergb2 = new calculateRGB2();
            calculatergb2.execute();
        }
        else {
            Log.d("Image", "Condition False, Unable to find bitmap");
        }
    }

    private class calculateRGB1 extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            int width = bitmap1.getWidth();
            int height = bitmap1.getHeight();
            int pixelCount = 0;
            int[] pix = new int[width * height];
            bitmap1.getPixels(pix, 0, width, 0, 0, width, height);
            for (int y = 0; y < height; y++){
                for (int x = 0; x < width; x++) {

//                    int c = bitmap1.getPixel(x, y);
//                    pixelCount++;
//                    Red += Color.red(c);
//                    Green += Color.green(c);
//                    Blue += Color.blue(c);

                    int index = y * width + x;
                    Red = (pix[index] >> 16) & 0xff;     //bitwise shifting
                    Green = (pix[index] >> 8) & 0xff;
                    Blue = pix[index] & 0xff;
                }
            }
            rgbValue1 = Red + " " + Green + " "+ Blue;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            luminance1 = 0.299 * Red + 0.587 * Green + 0.114 * Blue;
            luminosity1 = String.valueOf(luminance1);
//            Toast.makeText(getApplicationContext(),"With Flash " + luminosity1, Toast.LENGTH_SHORT).show();

            HiddenCameraActivity hiddenCameraActivity = new HiddenCameraActivity() {
                @Override
                public void onImageCapture(@NonNull File imageFile) {

                }

                @Override
                public void onCameraError(int errorCode) {

                }
            };
            hiddenCameraActivity.stopCamera();
            Log.d("FlashTestActivity", "Camera has been stopped");
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startCamera(mCameraConfig);
                            Log.d("FlashTestActivity", "Camera has been started");
                        }
                    });
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    takePicture();
                    Log.d("FlashTestActivity", "Ready to take picture");
                }
            })).start();
//            results1();
//            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
        }
    }


    private class calculateRGB2 extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... arg0) {
            int width = bitmap2.getWidth();
            int height = bitmap2.getHeight();
            int[] pix = new int[width * height];
            int pixelCount = 0;

            bitmap2.getPixels(pix, 0, width, 0, 0, width, height);
            for (int y = 0; y < height; y++){
                for (int x = 0; x < width; x++) {

//                    int c = bitmap1.getPixel(x, y);
//                    pixelCount++;
//                    Red += Color.red(c);
//                    Green += Color.green(c);
//                    Blue += Color.blue(c);

                    int index = y * width + x;
                    Red = (pix[index] >> 16) & 0xff;     //bitwise shifting
                    Green = (pix[index] >> 8) & 0xff;
                    Blue = pix[index] & 0xff;
                }
            }
            rgbValue2 = Red + " " + Green + " "+ Blue;
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            luminance2 = 0.299 * Red + 0.587 * Green + 0.114 * Blue;
            luminosity2 = String.valueOf(luminance2);
//            Toast.makeText(getApplicationContext(),"Without Flash " + luminosity2, Toast.LENGTH_SHORT).show();
            testResults();
//            results2();
//            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
        }
    }

    private void results1() {
        img1.setVisibility(View.VISIBLE);
        txt1.setVisibility(View.VISIBLE);
        img1.setImageBitmap(bitmap1);
        txt1.setText(luminosity1);
    }

    private void results2() {
        img2.setVisibility(View.VISIBLE);
        txt2.setVisibility(View.VISIBLE);
        img2.setImageBitmap(bitmap2);
        txt2.setText(luminosity2);
    }

    private void testResults() {
        if (luminosity1 != null && luminosity2 != null) {
            img1.setVisibility(View.VISIBLE);
            img2.setVisibility(View.VISIBLE);
            txt1.setVisibility(View.VISIBLE);
            txt2.setVisibility(View.VISIBLE);
            testStatusImg.setVisibility(View.VISIBLE);

            double result = Math.abs(luminance2 - luminance1);
            String resultDiff = String.valueOf(result);
            double percent1 = (luminance1 / 255.0) * 100;
            double percent2 = (luminance2 / 255.0) * 100;

            double resultPercent = Math.abs(percent1 - percent2);
            String resultantPercent = String.valueOf(resultPercent);
            resultDifference.setText(resultantPercent);

            Log.d("Luminosity", luminosity1 + " " + luminosity2);
            if (resultPercent > 0.92) {

                img1.setImageBitmap(bitmap1);
                img2.setImageBitmap(bitmap2);
                txt1.setText(luminosity1);
                txt2.setText(luminosity2);
                testStatusImg.setImageResource(R.drawable.blue_check);

//                Toast.makeText(FlashTestActivity.this, "Percentage: " +resultantPercent, Toast.LENGTH_SHORT).show();

                luminosity1 = null;
                luminosity2 = null;
//                bitmap.recycle();
//                bitmap1.recycle();
//                bitmap2.recycle();
                bitmap = null;
                bitmap1 = null;
                bitmap2 = null;
                progressDialog.dismiss();
//                Toast.makeText(this, "Flash Test Passed", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(MainActivity.this, TestPanel.class);
//                startActivity(intent);
                flashTestStatus = true;
                Loader.Companion.getInstance().getByClassType(AutofocusTest.class).sub(Test.rearCameraFlashTestKey).setValue(Test.PASS);
                Loader.Companion.getInstance().getByClassType(AutofocusTest.class).setStatus(Test.PASS);
                if (MainActivity.Companion.getAuto_start_mode()){
                    finish();
                }
            }
            else {
                img1.setImageBitmap(bitmap1);
                img2.setImageBitmap(bitmap2);
                txt1.setText(luminosity1);
                txt2.setText(luminosity2);
                testStatusImg.setImageResource(R.drawable.not_working);

//                Toast.makeText(FlashTestActivity.this, "Percentage: " + resultantPercent, Toast.LENGTH_SHORT).show();

                luminosity1 = null;
                luminosity2 = null;
//                bitmap.recycle();
//                bitmap1.recycle();
//                bitmap2.recycle();
                bitmap = null;
                bitmap1 = null;
                bitmap2 = null;
                progressDialog.dismiss();

//                Toast.makeText(this, "Flash Test Failed", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(MainActivity.this, TestPanel.class);
//                startActivity(intent);
                flashTestStatus = false;
                Loader.Companion.getInstance().getByClassType(AutofocusTest.class).sub(Test.rearCameraFlashTestKey).setValue(Test.FAILED);
                Loader.Companion.getInstance().getByClassType(AutofocusTest.class).setStatus(Test.FAILED);

            }
        }
        else{
                Toast.makeText(this, "Null values", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
    }
    @Override
    public void onCameraError(int errorCode) {
        switch (errorCode) {
            case CameraError.ERROR_CAMERA_OPEN_FAILED:
                //Camera open failed. Probably because another application
                //is using the camera
                Toast.makeText(this, "Cannot open camera.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_IMAGE_WRITE_FAILED:
                //Image write failed. Please check if you have provided WRITE_EXTERNAL_STORAGE permission
                Toast.makeText(this, "Cannot write image captured by camera.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                //camera permission is not available
                //Ask for the camra permission before initializing it.
                Toast.makeText(this, "Camera permission not available.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                //This error will never happen while hidden camera is used from activity or fragment
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                Toast.makeText(this, "Your device does not have front camera.", Toast.LENGTH_LONG).show();
                break;
        }
    }
}
