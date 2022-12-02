package radonsoft.net.rta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tyorikan.voicerecordingvisualizer.RecordingSampler;



import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import github.nisrulz.zentone.ToneStoppedListener;
import github.nisrulz.zentone.ZenTone;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;

public class RTA extends Activity {
    public static final int DIALOG_ANAINT = 7;
    public static final int DIALOG_BANDWIDTH = 3;
    public static final int DIALOG_CALIBRATE = 8;
    public static final int DIALOG_DEMO = 11;
    public static final int DIALOG_DISPOPTION = 6;
    public static final int DIALOG_DOCALIBRATE = 9;
    public static final int DIALOG_ERR = 10;
    public static final int DIALOG_HLDTIME = 4;
    public static final int DIALOG_OPTION = 1;
    public static final int DIALOG_PEAKHLD = 5;
    public static final String PREFS_NAME = "RTAPrefs";
    public static final int[] coltab = new int[]{65440, 41215, 16711840, 16752640, 10551040, 10486015, 16777215};
    public static final int[] hldt = new int[]{10, 20, 50};
    public static final int[] oct = new int[]{1, 2, 3, 4, 6, 12};
    public static final float[] stf = new float[]{31.25f, 22.0971f, 24.8031f, 22.0971f, 22.0971f, 22.0971f};
    boolean DoFinish = false;
    private GetAudio GetAudioInstance;
    boolean Pause = false;
    int app_bnd = 0;
    int app_hld = 1;
    int app_pkh = 0;
    int blksize = 0;
    int color = 0;
    int dlg_bnd = 2;
    int dlg_hld = 1;
    int dlg_pkh = 0;
    int iCalibMode;
    int iErrString = 0;
    int iTitle = R.string.err;
    int icalsize = 0;

    int PASS = 0;
    int FAILED = 1;
    int INIT = 2;
    public static int testStatus = 2;
    public static int testStatusVid = 2;

    private Button mButton_Start, button_playTone;
    OnClickListener mColorListener = new C00011();
    OnClickListener mOptionListener = new C00022();
    OnClickListener mStartListener = new C00033();
    boolean oldpause = false;
    byte[] pcalbuf = null;
    RtaView rta;
    int sf = 0;
    private Thread th;

    public static boolean threadRunning = false;



public static int REQ = 77;

    public static boolean testStatusRTA = false;
    public static boolean isTestPlayingRTA = false;

    public static int frequencyIndex;

//    private int freq[] = {4000, 8000, 12000, 16000};
    private int freq[] = {1000, 2000, 4000, 8000};

    private int duration = 1;
    public static boolean isPlaying = false;
    public static int counter = 0;

    public static String audioSource = null;

    private LinearLayout startButton;
    private ImageView testResult;
    private ListView testLists;
    private String[] FREQUENCIES;
    private boolean testRunning;
    private View microphoneView;
    private View videoMicrophoneView;
    private boolean vidPass;
    private boolean vidFail;
    private boolean micPass;
    private boolean micFail;
    private boolean micClicked;
    private boolean vidmicClicked;
    public static int micCode;
    public static int vidCode;
    private int audioCounter = 0;
    private boolean startClicked = false;

    private Button back_button, done_button;

    boolean autostart;
    public static boolean receiver;

    public static boolean micQuality, vidMicQuality = false;

    public TextView freq1k, freq2k, freq4k, freq8k, vmfrek1k, vmfrek2k, vmfrek4k, vmfrek8k;
    public RelativeLayout relativeLayout, relativeLayout2;

    public static String AUTO_MIC_QUALITY = "autoMicTesting";
    public static String AUTO_START_MIC_QUALITY = "autoMicQualityTest";
    public static String MIC_QUALITY_TEST_STATUS = "micqualityTestStatus";
    public static String VIDMIC_QUALITY_TEST_STATUS = "vidmicqualityTestStatus";

    public SharedPreferences sharedPreferences;
    Editor editor;
    public boolean alreadyStarted = false;

    public boolean TEST_LOCK = false;

    public toneThread thread;

    public RecordingSampler recordingSampler;

    public boolean isTestRunning = false;

    public TextView amplitudeTextMic, amplitudeTextVidMic;
    View var4;
    TextView var6;

    class C00011 implements OnClickListener {
        C00011() {
        }

        public void onClick(View v) {
            RTA rta = RTA.this;
            int i = rta.color + 1;
            rta.color = i;
            if (i >= RTA.coltab.length) {
                RTA.this.color = 0;
            }
            RTA.this.rta.SetColor(RTA.coltab[RTA.this.color]);
        }
    }

    class C00022 implements OnClickListener {
        C00022() {
        }

        public void onClick(View v) {
            RTA.this.showDialog(1);
        }
    }

    class C00033 implements OnClickListener {
        C00033() {
        }

        public void onClick(View v) {
            if (RTA.this.Pause) {
                RTA.this.StopAudio();
                RTA.this.StartAudio();
                RTA.this.Pause = false;
                RTA.this.mButton_Start.setText(RTA.this.getString(R.string.stop));
            } else {
                RTA.this.Pause = true;
                RTA.this.mButton_Start.setText(RTA.this.getString(R.string.start));
            }
            RTA.this.GetAudioInstance.setPaused(RTA.this.Pause);
            RTA.this.rta.setPaused(RTA.this.Pause);
        }
    }

    class C00055 implements OnCancelListener {
        C00055() {
        }

        public void onCancel(DialogInterface dialog) {
            RTA.this.removeDialog(1);
            RTA.this.RestoreAudioState();
        }
    }

    class C00066 implements DialogInterface.OnClickListener {
        C00066() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            if (whichButton > 2) {
                RTA.this.removeDialog(3);
                RTA.this.showDialog(11);
                return;
            }
            RTA.this.dlg_bnd = whichButton;
        }
    }

    class C00077 implements DialogInterface.OnClickListener {
        C00077() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            RTA.this.StopAudio();
            RTA.this.app_bnd = RTA.this.dlg_bnd;
//            RTA.this.app_bnd = 40;
            RTA.this.StartAudio();
            RTA.this.GetAudioInstance.setPaused(RTA.this.oldpause);
            RTA.this.rta.setPaused(RTA.this.oldpause);
        }
    }

    class C00088 implements DialogInterface.OnClickListener {
        C00088() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            RTA.this.dlg_bnd = 40;
            RTA.this.removeDialog(3);
            RTA.this.RestoreAudioState();
        }
    }

    class C00099 implements DialogInterface.OnClickListener {
        C00099() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            RTA.this.dlg_pkh = whichButton;
        }
    }

    @SuppressLint("CutPasteId")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        setContentView(R.layout.rta_view);
        getWindow().addFlags(128);
        this.rta = (RtaView) findViewById(R.id.View01);
        ((Button) findViewById(R.id.Button01)).setOnClickListener(this.mColorListener);
        ((Button) findViewById(R.id.Button02)).setOnClickListener(this.mOptionListener);
        back_button = findViewById(R.id.nav_back);
        done_button = findViewById(R.id.nav_done);
        button_playTone = findViewById(R.id.Button04);
        startButton = findViewById(R.id.startTest);
        relativeLayout = findViewById(R.id.maxThreshold);
        relativeLayout2 = findViewById(R.id.maxThreshold2);

        freq1k = findViewById(R.id.max1k);
        freq2k = findViewById(R.id.max2k);
        freq4k = findViewById(R.id.max4k);
        freq8k = findViewById(R.id.max8k);
        vmfrek1k = findViewById(R.id.vmmax1k);
        vmfrek2k = findViewById(R.id.vmmax2k);
        vmfrek4k = findViewById(R.id.vmmax4k);
        vmfrek8k = findViewById(R.id.vmmax8k);

        Intent intent = getIntent();
        autostart = intent.getExtras().getBoolean("AutoStart");
        receiver = intent.getExtras().getBoolean("Receiver");

        this.mButton_Start = (Button) findViewById(R.id.Button03);
        this.mButton_Start.setOnClickListener(this.mStartListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
        }
        final ViewGroup micContainer = (ViewGroup) findViewById(R.id.micTestLayout);
        microphoneView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.audio_input_test_layout, null);
        micContainer.addView(microphoneView);

        videoMicrophoneView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.audio_input_test_layout, null);
        micContainer.addView(videoMicrophoneView);

        sharedPreferences = getApplicationContext().getSharedPreferences(AUTO_START_MIC_QUALITY, MODE_PRIVATE);
        alreadyStarted = sharedPreferences.getBoolean(AUTO_MIC_QUALITY, false);
        testStatus = sharedPreferences.getInt(MIC_QUALITY_TEST_STATUS, 2);
        testStatusVid = sharedPreferences.getInt(VIDMIC_QUALITY_TEST_STATUS, 2);


        recordingSampler = new RecordingSampler();

        View var6 = this.microphoneView;
        View var3;
        TextView var7;
        if(this.microphoneView != null) {
            var3 = var6;
            var7 = (TextView)var3.findViewById(R.id.nameTextView);
            amplitudeTextMic = findViewById(R.id.amplitudeTextView);
            Intrinsics.checkExpressionValueIsNotNull(var7, "microphoneView.nameTextView");
            var7.setText((CharSequence)getString(R.string.microphne));
            var7.setTextColor(getResources().getColor(R.color.mic_text));
            var7.setVisibility(View.VISIBLE);
            ((ImageView)var3.findViewById(R.id.imageView)).setImageResource(R.drawable.microphone);
            ((ImageView)var3.findViewById(R.id.statusImageView)).setImageResource(getImageForStatus(testStatus));
        }

        var6 = this.microphoneView;
        if(this.microphoneView != null) {
            var6.setBackgroundResource(R.drawable.selector_row);
        }

        var6 = this.videoMicrophoneView;
        if(this.videoMicrophoneView != null) {
            var3 = var6;
            var7 = (TextView)var3.findViewById(R.id.nameTextView);
            amplitudeTextVidMic = findViewById(R.id.amplitudeTextView);
            Intrinsics.checkExpressionValueIsNotNull(var7, "videoMicrophoneView.nameTextView");
            var7.setText((CharSequence)getString(R.string.vid_microphne));
            var7.setTextColor(getResources().getColor(R.color.mic_text));
            ((ImageView)var3.findViewById(R.id.imageView)).setImageResource(R.drawable.microphone);
            ((ImageView)var3.findViewById(R.id.statusImageView)).setImageResource(getImageForStatus(testStatusVid));
        }
//         thread = new toneThread();

        microphoneView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TEST_LOCK)
                {
                    return;
                }
                TEST_LOCK = true;
                    audioSource = "MIC";
                    RtaView.testStatus = false;
                    micCode = 1;
                    isPlaying = false;
                    testRunning = true;
                    RtaView.test1000 = false;
                    RtaView.test2000 = false;
                    RtaView.test4000 = false;
                    RtaView.test8000 = false;
                    relativeLayout.setVisibility(View.INVISIBLE);
                    relativeLayout2.setVisibility(View.INVISIBLE);
                    threadRunning = true;
                    isTestRunning = true;
                    StartAudio();
                    thread = new toneThread();
                    thread.execute();
            }
        });

        videoMicrophoneView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TEST_LOCK)
                {
                    return;
                }
                TEST_LOCK = true;

                    audioSource = "Camcorder";
                    RtaView.testStatus = false;
                    vidCode = 1;
                    isPlaying = false;
                    testRunning = true;
                    RtaView.test8000 = false;
                    RtaView.test1000 = false;
                    RtaView.test2000 = false;
                    RtaView.test4000 = false;
                    relativeLayout.setVisibility(View.INVISIBLE);
                    relativeLayout2.setVisibility(View.INVISIBLE);
                    threadRunning = true;
                    isTestRunning = true;
                    StartAudio();
                    thread = new toneThread();
                    thread.execute();

            }
        });

        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TEST_LOCK)
                {
                    return;
                }
                TEST_LOCK = true;
                startClicked = true;
                testRunning = true;
                relativeLayout.setVisibility(View.INVISIBLE);
                relativeLayout2.setVisibility(View.INVISIBLE);
                threadRunning = true;
                isTestRunning = true;
                autoMicTest();
            }
        });

        back_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TEST_LOCK = false;
                threadRunning = false;
                if (thread != null){
                    thread.cancel(true);
                }
                ZenTone.getInstance().stop();
                isPlaying = false;
                if (testStatus == 2 && testStatusVid ==2){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
                    editor.commit();
                }
                else if (testStatus == 0 && testStatusVid == 2){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 0);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
                    editor.commit();
                }
                else if (testStatus == 1 && testStatusVid == 0){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 0);
                    editor.commit();
                }
                else if (testStatus == 1 && testStatusVid == 1) {
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
                    editor.commit();
                }
                else if (testStatus == 0 && testStatusVid == 0){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 0);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 0);
                    editor.commit();
                }
                finish();
            }
        });
        done_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if((micQuality && vidMicQuality) || (testStatus == 0 && testStatusVid == 0)){
                    finish();
                }
                else{
                    onNavDoneClick(v);
                }
            }
        });

        if (CheckSF(48000)) {
            this.sf = 48000;
            this.blksize = 2048;
        } else if (CheckSF(44100)) {
            this.sf = 44100;
            this.blksize = 2048;
        } else if (CheckSF(16000)) {
            this.sf = 16000;
            this.blksize = 1024;
        } else {
            this.sf = 8000;
            this.blksize = 1024;
        }
        showDialog(11);
    }

    private void onNavDoneClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(RTA.this);
        builder.setTitle(R.string.tst_incmplte);
        builder.setMessage(R.string.tst_desc);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.cntinue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                threadRunning = false;
                if (thread != null){
                    thread.cancel(true);
                }
                TEST_LOCK = false;
                ZenTone.getInstance().stop();
                isPlaying = false;
                if (testStatus == 2 && testStatusVid ==2){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
                    editor.commit();
                }
                else if (testStatus == 0 && testStatusVid == 2){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 0);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
                    editor.commit();
                }
                else if (testStatus == 1 && testStatusVid == 0){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 0);
                    editor.commit();
                }
                else if (testStatus == 1 && testStatusVid == 1) {
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
                    editor.commit();
                }
                else if (testStatus == 0 && testStatusVid == 0){
                    editor = sharedPreferences.edit();
                    editor.putInt(MIC_QUALITY_TEST_STATUS, 0);
                    editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 0);
                    editor.commit();
                }
                finish();

            }
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton(R.string.stay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    private void startTest() {
        startClicked = true;
        testRunning = true;
        isTestRunning = true;
        relativeLayout.setVisibility(View.INVISIBLE);
        relativeLayout2.setVisibility(View.INVISIBLE);
        autoMicTest();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "Audio Recording permission denied.", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void autoMicTest() {
        audioCounter++;
        if(audioCounter == 1){
            micCode = 1;
            audioSource = "MIC";
        }
        if(audioCounter == 2){
            vidCode = 1;
            audioSource = "Camcorder";
        }
        if(audioCounter == 2 || audioCounter > 2){
            audioCounter = 0;
            startClicked = false;
        }
        RtaView.testStatus = false;
        isPlaying = false;
        RtaView.test8000 = false;
        RtaView.test1000 = false;
        RtaView.test2000 = false;
        RtaView.test4000 = false;
        StartAudio();
        thread = new toneThread();
        thread.execute();
    }

    private class toneThread extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... param) {
            isPlaying = true;
            handleTonePlay();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            processResults();
//            if(thread!= null){
//                thread.cancel(true);
//            }
        }
        @Override
        protected void onPreExecute() {
        }
    }


    private void handleTonePlay() {
        if (!isPlaying) {
            ZenTone.getInstance().stop();
            isPlaying = false;
        }

        for (int aFreq : freq) {
            if (!isPlaying)
            {
                ZenTone.getInstance().stop();
                isPlaying = false;
                return;
            }
            counter++;
            duration = 2;
            frequencyIndex = aFreq;
            // Play Tone
            ZenTone.getInstance().generate(aFreq, duration, 0.8f, receiver, getApplicationContext(), new ToneStoppedListener() {
                @Override
                public void onToneStopped() {
//                    isPlaying = false;
//                        ZenTone.getInstance().stop();
                }
            });
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isPlaying = false;
    }

    private void processResults() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(counter == 4 || counter > 4){
                            testStatusRTA = false;
                            isTestPlayingRTA = true;
                            if (RtaView.test1000 && RtaView.test2000 && RtaView.test4000 && RtaView.test8000) {
                                testStatusRTA = true;
                                counter = 0;
//                                StopAudio();
                                updateTestStatus();
                            } else {
                                testStatusRTA = false;
                                counter = 0;
//                                StopAudio();
                                updateTestStatus();
                            }
                        }
                    }
                });
            }
        })).start();
    }

    private void updateTestStatus(){
        View var10000;
        if(isTestPlayingRTA && micCode == 1) {
            if(testStatusRTA) {
                var10000 = this.microphoneView;
                if(this.microphoneView == null) {
                    Intrinsics.throwNpe();
                }

                ((ImageView)var10000.findViewById(R.id.statusImageView)).setImageResource(getImageForStatus(0));
                this.micPass = true;
                testStatus = 0;
                relativeLayout.setVisibility(View.INVISIBLE);
                relativeLayout2.setVisibility(View.INVISIBLE);
                freq1k.setText(String.valueOf(RtaView.maxFreq1k));
                freq2k.setText(String.valueOf(RtaView.maxFreq2k));
                freq4k.setText(String.valueOf(RtaView.maxFreq4k));
                freq8k.setText(String.valueOf(RtaView.maxFreq8k));
                micQuality = true;
                TEST_LOCK = false;
                isTestRunning = false;
                setSharedPref();
                int amp = this.GetAudioInstance.micAmplitude();
                Log.d("amplitude", String.valueOf(amp));

                var4 = RTA.this.microphoneView;
                if (var4 != null) {
                    var6 = (TextView)var4.findViewById(R.id.amplitudeTextView);
                    var6.setTextColor(getResources().getColor(R.color.mic_text));
                    var6.setVisibility(View.INVISIBLE);
                    if (var6 != null) {
                        var6.setText((CharSequence)String.valueOf(amp));
                    }
                }
//                StopAudio();
            } else {
                var10000 = this.microphoneView;
                if(this.microphoneView == null) {
                    Intrinsics.throwNpe();
                }

                ((ImageView)var10000.findViewById(R.id.statusImageView)).setImageResource(getImageForStatus(1));
                this.micFail = true;
                testStatus = 1;
                relativeLayout.setVisibility(View.INVISIBLE);
                relativeLayout2.setVisibility(View.INVISIBLE);
                freq1k.setText(String.valueOf(RtaView.maxFreq1k));
                freq2k.setText(String.valueOf(RtaView.maxFreq2k));
                freq4k.setText(String.valueOf(RtaView.maxFreq4k));
                freq8k.setText(String.valueOf(RtaView.maxFreq8k));
                TEST_LOCK = false;
                micQuality = false;
                isTestRunning = false;
//                StopAudio();
                setSharedPref();
                int amp = this.GetAudioInstance.micAmplitude();
                Log.d("amplitude", String.valueOf(amp));

                var4 = RTA.this.microphoneView;
                if (var4 != null) {
                    var6 = (TextView)var4.findViewById(R.id.amplitudeTextView);
                    var6.setTextColor(getResources().getColor(R.color.mic_text));
                    var6.setVisibility(View.INVISIBLE);

                    if (var6 != null) {
                        var6.setText((CharSequence)String.valueOf(amp));
                    }
                }
            }
        }

        if(isTestPlayingRTA && vidCode == 1) {
            if(testStatusRTA) {
                var10000 = this.videoMicrophoneView;
                if(this.videoMicrophoneView == null) {
                    Intrinsics.throwNpe();
                }

                ((ImageView)var10000.findViewById(R.id.statusImageView)).setImageResource(getImageForStatus(0));
                this.vidPass = true;
                testStatusVid = 0;
                relativeLayout2.setVisibility(View.INVISIBLE);
                relativeLayout.setVisibility(View.INVISIBLE);

                vmfrek1k.setText(String.valueOf(RtaView.vmmaxFreq1k));
                vmfrek2k.setText(String.valueOf(RtaView.vmmaxFreq2k));
                vmfrek4k.setText(String.valueOf(RtaView.vmmaxFreq4k));
                vmfrek8k.setText(String.valueOf(RtaView.vmmaxFreq8k));
                TEST_LOCK = false;
                vidMicQuality = true;
                isTestRunning = false;
                setSharedPref();
                int amp = this.GetAudioInstance.micAmplitude();
                Log.d("amplitude", String.valueOf(amp));

                var4 = RTA.this.videoMicrophoneView;
                if (var4 != null) {
                    var6 = (TextView)var4.findViewById(R.id.amplitudeTextView);
                    var6.setTextColor(getResources().getColor(R.color.mic_text));
                    var6.setVisibility(View.INVISIBLE);
                    if (var6 != null) {
                        var6.setText((CharSequence)String.valueOf(amp));
                    }
                }

//                StopAudio();
            } else {
                var10000 = this.videoMicrophoneView;
                if(this.videoMicrophoneView == null) {
                    Intrinsics.throwNpe();
                }

                ((ImageView)var10000.findViewById(R.id.statusImageView)).setImageResource(getImageForStatus(1));
                this.vidFail = true;
                testStatusVid = 1;
                relativeLayout2.setVisibility(View.INVISIBLE);
                relativeLayout.setVisibility(View.INVISIBLE);
                vmfrek1k.setText(String.valueOf(RtaView.vmmaxFreq1k));
                vmfrek2k.setText(String.valueOf(RtaView.vmmaxFreq2k));
                vmfrek4k.setText(String.valueOf(RtaView.vmmaxFreq4k));
                vmfrek8k.setText(String.valueOf(RtaView.vmmaxFreq8k));
                vidMicQuality = false;
                TEST_LOCK = false;
                isTestRunning = false;
                setSharedPref();
                int amp = this.GetAudioInstance.micAmplitude();
                Log.d("amplitude", String.valueOf(amp));

                var4 = RTA.this.videoMicrophoneView;
                if (var4 != null) {
                    var6 = (TextView)var4.findViewById(R.id.amplitudeTextView);
                    var6.setTextColor(getResources().getColor(R.color.mic_text));
                    var6.setVisibility(View.INVISIBLE);
                    if (var6 != null) {
                        var6.setText((CharSequence)String.valueOf(amp));
                    }
                }

//                StopAudio();
            }
        }
        RTA.isTestPlayingRTA = false;
        RTA.testStatusRTA = false;
        vidCode = 0;
        micCode = 0;
//        this.rta.Exit();
        StopAudio();
        if(startClicked){
            relativeLayout.setVisibility(View.INVISIBLE);
            relativeLayout2.setVisibility(View.INVISIBLE);
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    autoMicTest();
                }
            })).start();
        } else{
            microphoneView.setEnabled(true);
            videoMicrophoneView.setEnabled(true);
            startButton.setEnabled(true);
            testRunning = false;
//            Toast.makeText(this, GetAudio.amplitude, Toast.LENGTH_SHORT).show();
            if (autostart){
                if (micQuality && vidMicQuality){
                    finish();
                }
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        ZenTone.getInstance().stop();

        if (isTestRunning){
            StopAudio();
        }
        isTestRunning = false;
        isPlaying = false;
        if (thread != null){
            thread.cancel(true);
        }

    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        TEST_LOCK = false;
        threadRunning = false;
        if (thread != null){
            thread.cancel(true);
        }
        ZenTone.getInstance().stop();
        isPlaying = false;
        if (testStatus == 2 && testStatusVid ==2){
            editor = sharedPreferences.edit();
            editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
            editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
            editor.commit();
        }
        else if (testStatus == 0 && testStatusVid == 2){
            editor = sharedPreferences.edit();
            editor.putInt(MIC_QUALITY_TEST_STATUS, 0);
            editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
            editor.commit();
        }
        else if (testStatus == 1 && testStatusVid == 0){
            editor = sharedPreferences.edit();
            editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
            editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 0);
            editor.commit();
        }
        else if (testStatus == 1 && testStatusVid == 1) {
            editor = sharedPreferences.edit();
            editor.putInt(MIC_QUALITY_TEST_STATUS, 1);
            editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 1);
            editor.commit();
        }
        else if (testStatus == 0 && testStatusVid == 0){
            editor = sharedPreferences.edit();
            editor.putInt(MIC_QUALITY_TEST_STATUS, 0);
            editor.putInt(VIDMIC_QUALITY_TEST_STATUS, 0);
            editor.commit();
        }
    }

    public void onStop() {
        super.onStop();
        Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putInt("band", this.app_bnd);
        editor.putInt("pkhold", this.app_hld);
        editor.putInt("peak", this.app_pkh);
        editor.putInt("color", this.color);
        editor.putInt("calib", this.iCalibMode);
        editor.commit();
        ZenTone.getInstance().stop();
        isPlaying = false;
        micQuality = false;
        vidMicQuality = false;
        if (thread != null){
            thread.cancel(true);
        }
    }

    boolean CheckSF(int sf) {
        int size = AudioRecord.getMinBufferSize(sf, 16, 2);
        if (size <= 0) {
            return false;
        }
        AudioRecord ar;
        try {
            ar = new AudioRecord(1, sf, 16, 2, size);
        } catch (IllegalArgumentException e) {
            ar = null;
        }
        if (ar == null) {
            return false;
        }
        if (ar.getState() != 1) {
            return false;
        }
        ar.release();
        return true;
    }

    void StartAudio() {
        if(testRunning){
            microphoneView.setEnabled(false);
            videoMicrophoneView.setEnabled(false);
            startButton.setEnabled(false);
        }
        this.rta.Init(1, oct[this.app_bnd] * 10, this.blksize, 1.0f / ((float) oct[this.app_bnd]), (2.0f * stf[this.app_bnd]) / ((float) this.sf));
        if (this.pcalbuf != null && this.iCalibMode == 3 && this.icalsize == (oct[this.app_bnd] * 10) * 4) {
            this.rta.setCalibBuf(ByteBuffer.wrap(this.pcalbuf).asIntBuffer());
        } else {
            this.iCalibMode = 1;
        }
        this.rta.setCalibMode(this.iCalibMode);
        this.GetAudioInstance = new GetAudio();
        this.GetAudioInstance.setRta(this.rta);
        this.GetAudioInstance.setframeSize(this.blksize);
        this.GetAudioInstance.setFrequency(this.sf);
        this.GetAudioInstance.setRecording(true);
        this.th = new Thread(this.GetAudioInstance);
        this.th.start();
    }
    public void setSharedPref(){
        editor = sharedPreferences.edit();
        editor.putBoolean(AUTO_MIC_QUALITY, true);
        editor.putInt(MIC_QUALITY_TEST_STATUS, testStatus);
        editor.putInt(VIDMIC_QUALITY_TEST_STATUS, testStatusVid);
        editor.commit();
    }

    void StopAudio() {
         try {
            this.GetAudioInstance.setRecording(false);
        }
        catch (RuntimeException e){
            e.printStackTrace();
        }
        try {
            this.th.join();
        } catch (InterruptedException | RuntimeException e) {
            e.printStackTrace();
        }
        this.rta.setPaused(true);
        this.iCalibMode = this.rta.getCalibMode();
        if (this.iCalibMode == 3) {
            this.icalsize = (oct[this.app_bnd] * 10) * 4;
            this.pcalbuf = new byte[this.icalsize];
            this.rta.getCalibBuf(ByteBuffer.wrap(this.pcalbuf).asIntBuffer());
        }
        this.rta.Exit();

    }

    void RestoreAudioState() {
        StopAudio();
        StartAudio();
        this.GetAudioInstance.setPaused(this.oldpause);
        this.rta.setPaused(this.oldpause);
    }

    protected void onPause() {
        super.onPause();
//        StopAudio();
    }

    protected void onResume() {
        super.onResume();
        if (!alreadyStarted){
            isPlaying = false;
            relativeLayout.setVisibility(View.INVISIBLE);
            relativeLayout2.setVisibility(View.INVISIBLE);

            if (this.sf < 44100) {
                this.iErrString = R.string.lowsf_string;
                this.iTitle = R.string.warning;
                showDialog(10);
            }

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startTest();
                        }
                    });
                }
            })).start();
        }
//        StartAudio();
        long lstart = System.currentTimeMillis();
//        while (this.GetAudioInstance.IsOK == 0) {
//            if (System.currentTimeMillis() - lstart > 10000) {
//                this.iErrString = R.string.startaudio_string;
//                this.DoFinish = true;
//                showDialog(10);
//                break;
//            }
//        }
//        if (this.GetAudioInstance.IsOK == -1) {
//            this.iErrString = R.string.wrongsf_string;
//            this.DoFinish = true;
//            showDialog(10);
//        } else {
//            this.mButton_Start.setText(getString(R.string.stop));
//        }
//        (new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                thread = new toneThread();
//                thread.execute();
//            }
//        })).start();
    }

    protected void onStart() {
        boolean z = true;
        super.onStart();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int i = settings.getInt("band", 1);
        this.dlg_bnd = i;
        this.app_bnd = i;
        i = settings.getInt("pkhold", 1);
        this.dlg_hld = i;
        this.app_hld = i;
        i = settings.getInt("peak", 0);
        this.dlg_pkh = i;
        this.app_pkh = i;
        this.color = settings.getInt("color", 0);
        this.iCalibMode = settings.getInt("calib", 1);
        if (this.iCalibMode == 3) {
            this.iCalibMode = 1;
        }
        this.rta.SetPeakTime(hldt[this.app_hld]);
        RtaView rtaView = this.rta;
        if (this.app_pkh != 1) {
            z = false;
        }
        rtaView.SetPeakHold(z);
        this.rta.SetColor(coltab[this.color]);
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_OPTION /*1*/:
                final int[] optiondlg = new int[]{3, 4, 5, 6, 7, 8};
                this.oldpause = this.GetAudioInstance.isPaused();
                this.GetAudioInstance.setPaused(true);
                this.rta.setPaused(true);
                return new Builder(this).setTitle(R.string.option_title).setItems(R.array.option_dialog_items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int id = optiondlg[which];
                        if (id == 4 || id == 6 || id == 7) {
                            RTA.this.showDialog(11);
                        } else {
                            RTA.this.showDialog(optiondlg[which]);
                        }
                        RTA.this.removeDialog(1);
                    }
                }).setOnCancelListener(new C00055()).create();
            case DIALOG_BANDWIDTH /*3*/:
                return new Builder(this).setTitle(R.string.sel_bnd).setCancelable(false).setSingleChoiceItems(R.array.bnd_dialog_items, this.dlg_bnd, new C00066()).setPositiveButton(R.string.ok, new C00077()).setNegativeButton(R.string.cancel, new C00088()).create();
            case DIALOG_PEAKHLD /*5*/:
                return new Builder(this).setTitle(R.string.sel_pkmode).setCancelable(false).setSingleChoiceItems(R.array.pm_dialog_items, this.dlg_pkh, new C00099()).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        boolean z = true;
                        RTA.this.app_pkh = RTA.this.dlg_pkh;
                        RtaView rtaView = RTA.this.rta;
                        if (RTA.this.app_pkh != 1) {
                            z = false;
                        }
                        rtaView.SetPeakHold(z);
                        RTA.this.RestoreAudioState();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        RTA.this.dlg_pkh = RTA.this.app_pkh;
                        RTA.this.removeDialog(5);
                        RTA.this.RestoreAudioState();
                    }
                }).create();
            case DIALOG_CALIBRATE /*8*/:
                return new Builder(this).setTitle(R.string.sel_calib).setMessage(R.string.calib).setCancelable(false).setPositiveButton(R.string.cont, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        RTA.this.showDialog(9);
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        RTA.this.RestoreAudioState();
                    }
                }).setNeutralButton(R.string.noisegen, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            RTA.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://search?q=pname:radonsoft.net.noisegen")));
                            RTA.this.finish();
                        } catch (ActivityNotFoundException e) {
                            RTA.this.RestoreAudioState();
                        }
                    }
                }).create();
            case DIALOG_DOCALIBRATE /*9*/:
                return new Builder(this).setTitle(R.string.calibopt).setItems(R.array.calib_dialog_items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        RTA.this.StopAudio();
                        RTA.this.StartAudio();
                        switch (which) {
                            case 0:
                                if (RTA.this.Pause) {
                                    RTA.this.Pause = false;
                                    RTA.this.mButton_Start.setText(RTA.this.getString(R.string.stop));
                                }
                                RTA.this.GetAudioInstance.setPaused(false);
                                RTA.this.rta.setPaused(false);
                                RTA.this.rta.setCalibMode(0);
                                return;
                            case RTA.DIALOG_OPTION /*1*/:
                                RTA.this.showDialog(11);
                                return;
                            case 2:
                                RTA.this.rta.setCalibMode(2);
                                RTA.this.GetAudioInstance.setPaused(RTA.this.oldpause);
                                RTA.this.rta.setPaused(RTA.this.oldpause);
                                return;
                            case RTA.DIALOG_BANDWIDTH /*3*/:
                                RTA.this.rta.setCalibMode(1);
                                RTA.this.GetAudioInstance.setPaused(RTA.this.oldpause);
                                RTA.this.rta.setPaused(RTA.this.oldpause);
                                return;
                            default:
                                return;
                        }
                    }
                }).setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        RTA.this.RestoreAudioState();
                    }
                }).create();
            case DIALOG_ERR /*10*/:
                return new Builder(this).setTitle(this.iTitle).setMessage(this.iErrString).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (RTA.this.DoFinish) {
                            RTA.this.finish();
                        }
                        RTA.this.iTitle = R.string.err;
                        RTA.this.removeDialog(10);
                    }
                }).create();
//            case DIALOG_DEMO /*11*/:
//                return new Builder(this).setTitle(R.string.demotitle).setMessage(R.string.demo_string).setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        RTA.this.RestoreAudioState();
//                    }
//                }).setNeutralButton(R.string.buy, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        try {
//                            RTA.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://search?q=pname:radonsoft.net.rtapro")));
//                            RTA.this.finish();
//                        } catch (ActivityNotFoundException e) {
//                            RTA.this.RestoreAudioState();
//                        }
//                    }
//                }).setPositiveButton(R.string.apps, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        try {
//                            RTA.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://search?q=radonsoft.net")));
//                            RTA.this.finish();
//                        } catch (ActivityNotFoundException e) {
//                            RTA.this.RestoreAudioState();
//                        }
//                    }
//                }).create();
            default:
                return null;
        }
    }
    protected final int getImageForStatus(int value) {
        return value == PASS ? R.drawable.blue_check :
                (value == FAILED ? R.drawable.not_working :
                        R.drawable.warning);
    }
}
