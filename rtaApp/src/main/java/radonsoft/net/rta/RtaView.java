package radonsoft.net.rta;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static radonsoft.net.rta.RTA.frequencyIndex;


@SuppressWarnings("JniMissingFunction")
public class RtaView extends View implements Runnable {
    private static String[] xval = new String[]{"32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"};
    int DoCal = 0;
    boolean InitPrepared = false;
    boolean IsInit = false;
    boolean SizeOk = false;
    boolean bopHold = false;
    String[] calibstr;
    int[] caltab = null;
    boolean clrpkrms = true;
    int ctrlheight = 245;
    int ctrlwidth = 480;
    float highdb = 0.0f;
    int iCalibmode = 1;
    int iCnt = 0;
    int iColor = 65440;
    long lastnow = 0;
    float lowdb = -100.0f;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mPaint = new Paint();
    private Paint mPaint2 = new Paint();
    int nbands;
    int offx = 8;
    int offy = 5;
    float pk;
    int[] pkcnt = null;
    int pkhld = 20;
    int[] pkval = null;
    float rms;
    int[] rtaval = null;
    boolean running = false;
    String sHz;
    String sPeak;
    String sRMS;
    String sdB;
    String sdBFS;
    public static int[] smval = null;
    float space;
    private Thread tha;
    int updspeed = 21;
    int f0x = 360;
    int x2 = (this.f0x + 29);
    float xfak = 1.0f;
    int f1y = 220;
    int i;
    public static boolean test1000 = false;
    public static boolean test2000 = false;
    public static boolean test4000 = false;
    public static boolean test8000 = false;

    public static int maxFreq1k = 0;
    public static int maxFreq2k = 0;
    public static int maxFreq4k = 0;
    public static int maxFreq8k = 0;

    public static int vmmaxFreq1k = 0;
    public static int vmmaxFreq2k = 0;
    public static int vmmaxFreq4k = 0;
    public static int vmmaxFreq8k = 0;

//    TestStatus test;

   public static boolean testStatus = false;
   public static boolean isTestPlaying = false;

    public native void DoRta(int i, ByteBuffer byteBuffer, int[] iArr);

    public native void ExitRta();

    public native void GetCalib(int[] iArr);

    public native void InitRta(int i, int i2, float f, float f2, int i3);

    public native void SetCalib(int[] iArr);

    public native void SetWindow(int i);

    static {
//        try {
            System.loadLibrary("RTA-jni");
//        } catch (UnsatisfiedLinkError e){
//            Log.d("JNI", "WARNING: Could not load RTA-jni.so");
//        }
    }

    public RtaView(Context context) {
        super(context);
        setFocusable(true);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTypeface(Typeface.SERIF);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint2.setColor(getResources().getColor(R.color.white_color));
    }

    public RtaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTypeface(Typeface.SERIF);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint2.setColor(getResources().getColor(R.color.white_color));
    }

    public void Init(int mode, int nbands, int blksize, float space, float fstart) {
        this.nbands = nbands;
        this.space = space;
        this.rtaval = new int[(nbands + 2)];
        this.smval = new int[nbands];
        this.caltab = new int[nbands];
        this.pkval = new int[nbands];
        this.pkcnt = new int[nbands];
        Resources res = getResources();
        this.calibstr = res.getStringArray(R.array.calib_modes);
        this.sdB = res.getString(R.string.db);
        this.sHz = res.getString(R.string.hz);
        this.sdBFS = res.getString(R.string.dbfs);
        this.sPeak = res.getString(R.string.peak);
        this.sRMS = res.getString(R.string.rms);
        InitRta(mode, blksize, fstart, (float) Math.pow(2.0d, (double) space), nbands);
        this.rms = 0.0f;
        this.pk = 0.0f;
        this.InitPrepared = true;
        if (this.SizeOk) {
            this.IsInit = true;
        }
    }

    public void Exit() {
        setPaused(false);
        this.IsInit = false;
        this.InitPrepared = false;
        ExitRta();
    }

    public void SetData(ByteBuffer bbuf) {
        if (this.IsInit) {
            float pk1;
            if (bbuf != null) {
                DoRta(this.DoCal, bbuf, this.rtaval);
            }
            int ypos = (this.f1y + this.offy) << 8;
            int low = (int) (this.lowdb * 100.0f);
            float fak = ((float) this.f1y) / ((float) (((int) (this.highdb * 100.0f)) - low));
            for (i = 0; i < this.nbands; i++) {
                int[] iArr = this.smval;
                iArr[i] = iArr[i] * 15;
                iArr = this.smval;
                iArr[i] = iArr[i] >> 4;
                if (this.pkcnt[i] > 0) {
                    iArr = this.pkcnt;
                    iArr[i] = iArr[i] - 1;
                } else {
                    iArr = this.pkval;
                    iArr[i] = iArr[i] * 15;
                    iArr = this.pkval;
                    iArr[i] = iArr[i] >> 4;
                }
                int v = ((int) (((float) (this.rtaval[i] - low)) * fak)) << 8;
                if (v < 0) {
                    v = 0;
                }
                if (v > ypos) {
                    v = ypos;
                }
                if (v > this.smval[i]) {
                    this.smval[i] = v;
                }
                if (v > this.pkval[i]) {
                    this.pkval[i] = v;
                    this.pkcnt[i] = this.pkhld;
                }
            }
            float pkv = (float) this.rtaval[this.nbands];
            float rmv = (float) this.rtaval[this.nbands + 1];
            if (pkv == 0.0f) {
                pk1 = -99.9f;
            } else {
                pk1 = 10.0f * ((float) Math.log10((double) (pkv * 9.313226E-10f)));
            }
            if (rmv == 0.0f) {
                rmv = -99.9f;
            } else {
                rmv = 10.0f * ((float) Math.log10((double) (rmv * 9.313226E-10f)));
            }
            if (this.clrpkrms) {
                this.rms = rmv;
                this.pk = pk1;
                this.clrpkrms = false;
            }
            this.rms = (this.rms * 0.95f) + (0.05f * rmv);
            if (pk1 > this.pk) {
                this.pk = pk1;
            } else if (!this.bopHold) {
                this.pk = (this.pk * 0.97f) + (0.03f * pk1);
            }

            while (frequencyIndex == 1000){
                if(RtaView.smval[11] > RtaView.smval[10] && RtaView.smval[11] > RtaView.smval[9] && RtaView.smval[11] > RtaView.smval[8] && RtaView.smval[11] > RtaView.smval[7] && RtaView.smval[11] > RtaView.smval[12] && RtaView.smval[11] > RtaView.smval[13] && RtaView.smval[11] > RtaView.smval[14] && RtaView.smval[11] > RtaView.smval[15]){
                    test1000 = true;
//                    maxFreq1k = smval[11];
                    maxFreq1k = (int) this.rms;
                    vmmaxFreq1k = (int) this.rms;
                    Log.d("1000", "true");
                } else{
                    Log.d("1000", "false");
                    test1000 = false;
//                    maxFreq1k = smval[11];
                    vmmaxFreq1k = (int) this.rms;
                    maxFreq1k = (int) this.rms;
                }
                break;
            }
            while (frequencyIndex == 2000){
                if(RtaView.smval[13] > RtaView.smval[12] && RtaView.smval[13] > RtaView.smval[11] && RtaView.smval[13] > RtaView.smval[10] && RtaView.smval[13] > RtaView.smval[9] && RtaView.smval[13] > RtaView.smval[14] && RtaView.smval[13] > RtaView.smval[15] && RtaView.smval[13] > RtaView.smval[16] && RtaView.smval[13] > RtaView.smval[17]){
                    test2000 = true;
//                    maxFreq2k = smval[13];
                    maxFreq2k = (int) this.rms;
                    vmmaxFreq2k = (int) this.rms;
                    Log.d("2000", "true");
                } else{
                    Log.d("2000", "false");
                    test2000 = false;
//                    maxFreq2k = smval[13];
                    vmmaxFreq2k = (int) this.rms;
                    maxFreq2k = (int) this.rms;
                }
                break;
            }
            while (frequencyIndex == 4000){
                if(RtaView.smval[15] > RtaView.smval[14] && RtaView.smval[15] > RtaView.smval[13] && RtaView.smval[15] > RtaView.smval[12] && RtaView.smval[15] > RtaView.smval[11] && RtaView.smval[15] > RtaView.smval[16] && RtaView.smval[15] > RtaView.smval[17] && RtaView.smval[15] > RtaView.smval[18]){
                    test4000 = true;
                    Log.d("4000", "true");
//                    maxFreq4k = smval[15];
                    maxFreq4k = (int) this.rms;
                    vmmaxFreq4k = (int) this.rms;
                } else{
                    Log.d("4000", "false");
                    test4000 = false;
//                    maxFreq4k = smval[15];
                    vmmaxFreq4k = (int) this.rms;
                    maxFreq4k = (int) this.rms;
                }
                break;
            }
            while (frequencyIndex == 8000){
                if(RtaView.smval[17] > RtaView.smval[16] && RtaView.smval[17] > RtaView.smval[15] && RtaView.smval[17] > RtaView.smval[14] && RtaView.smval[17] > RtaView.smval[13] && RtaView.smval[17] > RtaView.smval[18] && RtaView.smval[17] > RtaView.smval[19]){
                    test8000 = true;
                    Log.d("8000", "true");
//                    maxFreq8k = smval[17];
                    vmmaxFreq8k = (int) this.rms;
                    maxFreq8k = (int) this.rms;
                } else{
                    Log.d("8000", "false");
//                    test8000 = false;
//                    maxFreq8k = smval[17];
                    vmmaxFreq8k = (int) this.rms;
                    maxFreq8k = (int) this.rms;
                }
                break;
            }

//            while (frequencyIndex == 1000){
//                if(RtaView.smval[11] > RtaView.smval[10] && RtaView.smval[11] > RtaView.smval[9] && RtaView.smval[11] > RtaView.smval[8] && RtaView.smval[11] > RtaView.smval[7] && RtaView.smval[11] > RtaView.smval[6] && RtaView.smval[11] > RtaView.smval[12] && RtaView.smval[11] > RtaView.smval[13] && RtaView.smval[11] > RtaView.smval[14] && RtaView.smval[11] > RtaView.smval[15] && RtaView.smval[11] > RtaView.smval[16]){
//                    test1000 = true;
////                    maxFreq1k = smval[11];
//                    maxFreq1k = (int) this.rms;
//                    vmmaxFreq1k = (int) this.rms;
//                    Log.d("1000", "true");
//                } else{
//                    Log.d("1000", "false");
//                    test1000 = false;
////                    maxFreq1k = smval[11];
//                    vmmaxFreq1k = (int) this.rms;
//                    maxFreq1k = (int) this.rms;
//                }
//                break;
//            }
//            while (frequencyIndex == 2000){
//                if(RtaView.smval[13] > RtaView.smval[12] && RtaView.smval[13] > RtaView.smval[11] && RtaView.smval[13] > RtaView.smval[10] && RtaView.smval[13] > RtaView.smval[9] && RtaView.smval[13] > RtaView.smval[8] && RtaView.smval[13] > RtaView.smval[14] && RtaView.smval[13] > RtaView.smval[15] && RtaView.smval[13] > RtaView.smval[16] && RtaView.smval[13] > RtaView.smval[17] && RtaView.smval[13] > RtaView.smval[18]){
//                    test2000 = true;
////                    maxFreq2k = smval[13];
//                    maxFreq2k = (int) this.rms;
//                    vmmaxFreq2k = (int) this.rms;
//                    Log.d("2000", "true");
//                } else{
//                    Log.d("2000", "false");
//                    test2000 = false;
////                    maxFreq2k = smval[13];
//                    vmmaxFreq2k = (int) this.rms;
//                    maxFreq2k = (int) this.rms;
//                }
//                break;
//            }
//            while (frequencyIndex == 4000){
//                if(RtaView.smval[15] > RtaView.smval[14] && RtaView.smval[15] > RtaView.smval[13] && RtaView.smval[15] > RtaView.smval[12] && RtaView.smval[15] > RtaView.smval[11] && RtaView.smval[15] > RtaView.smval[10] && RtaView.smval[15] > RtaView.smval[16] && RtaView.smval[15] > RtaView.smval[17] && RtaView.smval[15] > RtaView.smval[18] && RtaView.smval[15] > RtaView.smval[19]){
//                    test4000 = true;
//                    Log.d("4000", "true");
////                    maxFreq4k = smval[15];
//                    maxFreq4k = (int) this.rms;
//                    vmmaxFreq4k = (int) this.rms;
//                } else{
//                    Log.d("4000", "false");
//                    test4000 = false;
////                    maxFreq4k = smval[15];
//                    vmmaxFreq4k = (int) this.rms;
//                    maxFreq4k = (int) this.rms;
//                }
//                break;
//            }
//            while (frequencyIndex == 8000){
//                if(RtaView.smval[17] > RtaView.smval[16] && RtaView.smval[17] > RtaView.smval[15] && RtaView.smval[17] > RtaView.smval[14] && RtaView.smval[17] > RtaView.smval[13] && RtaView.smval[17] > RtaView.smval[18] && RtaView.smval[17] > RtaView.smval[19]){
//                    test8000 = true;
//                    Log.d("8000", "true");
////                    maxFreq8k = smval[17];
//                    vmmaxFreq8k = (int) this.rms;
//                    maxFreq8k = (int) this.rms;
//                } else{
//                    Log.d("8000", "false");
////                    test8000 = false;
////                    maxFreq8k = smval[17];
//                    vmmaxFreq8k = (int) this.rms;
//                    maxFreq8k = (int) this.rms;
//                }
//                break;
//            }

//            while (frequencyIndex == 8000){
//                if(RtaView.smval[17] > RtaView.smval[8] && RtaView.smval[17] > RtaView.smval[7] && RtaView.smval[17] > RtaView.smval[6] && RtaView.smval[17] > RtaView.smval[5] && RtaView.smval[17] > RtaView.smval[4] && RtaView.smval[17] > RtaView.smval[10] && RtaView.smval[17] > RtaView.smval[11] && RtaView.smval[17] > RtaView.smval[12] && RtaView.smval[17] > RtaView.smval[13] && RtaView.smval[17] > RtaView.smval[14]){
//                    test500 = true;
//                    Log.d("500", "true");
//                } else{
//                    Log.d("500", "false");
//                    test500 = false;
//                }
//                break;
//            }

            postInvalidate();
        }
    }

    protected void onDraw(Canvas dcanvas) {
        if (this.IsInit) {
            int i;
            Paint p = this.mPaint;
            Paint p2 = this.mPaint2;
            Canvas canvas = this.mCanvas;
            canvas.drawRect(0.0f, 0.0f, (float) this.ctrlwidth, (float) this.ctrlheight, p2);
            int i2 = this.iCnt;
            this.iCnt = i2 + 1;
            if (i2 > 20) {
                this.iCnt = 0;
            }
            p.setColor(getResources().getColor(R.color.white_color));
//            int offx2 = (this.offx + this.x2) + ((int) (78.0f * this.xfak));
//            canvas.drawRect(((float) this.offx) - (3.0f * this.xfak), (float) (this.offy - 1), ((float) (this.offx + this.f0x)) + (2.0f * this.xfak), (float) ((this.offy + this.f1y) + 1), p);
//            canvas.drawRect((float) (this.offx + this.x2), (float) (this.offy - 1), (float) offx2, (float) ((this.offy + this.f1y) + 1), p);
//            canvas.drawRect((float) (this.offx + this.x2), (float) ((this.offy + (this.f1y / 2)) - 2), (float) offx2, (float) ((this.offy + (this.f1y / 2)) - 1), p);
            int ypos = this.f1y + this.offy;
            int sp = this.f0x / this.nbands;
            int offx3 = (this.offx - 1) + ((this.f0x % this.nbands) / 2);
            int[] coltab = new int[sp];
            int ad = 255 / sp;
            int st = ad;
            for (i = 0; i < sp; i++) {
                coltab[i] = (st << 24) + this.iColor;
                st += ad;
            }
            for (i = 0; i < this.nbands; i++) {
                int v = this.smval[i] >> 8;
                int u = this.pkval[i] >> 8;
                for (int j = 0; j < sp - 1; j++) {
                    p.setColor(coltab[j + 1]);
                    canvas.drawLine((float) (((i * sp) + offx3) + j), (float) ypos, (float) (((i * sp) + offx3) + j), (float) (ypos - v), p);
                    canvas.drawLine((float) (((i * sp) + offx3) + j), (float) (ypos - u), (float) (((i * sp) + offx3) + j), (float) ((ypos - u) - 1), p);
                }
            }
            p.setTextSize(10.0f * this.xfak);
            p.setStyle(Style.FILL);
            p.setColor(getResources().getColor(R.color.solid_black));
            float a = this.highdb;
            float da = (this.lowdb - this.highdb) * 0.1f;
            int step = this.f1y / 10;
            for (i = 1; i <= 10; i++) {
                a += da;
                String s = String.valueOf((int) a);
                canvas.drawLine((float) ((this.offx + this.f0x) + 1), (float) ((this.offy + (i * step)) - 2), ((float) (this.offx + this.f0x)) + (3.0f * this.xfak), (float) ((this.offy + (i * step)) - 2), p);
                canvas.drawText(s, ((float) (this.offx + this.f0x)) + (4.0f * this.xfak), (float) (this.offy + (i * step)), p);
            }
            step = this.f0x / 10;
            int off = (this.offx - 1) + (step / 2);
            i = 0;
            while (i < 10) {
                if (i <= 4 || i >= 9) {
                    canvas.drawText(xval[i], (float) ((i * step) + off), ((float) (this.offy + this.f1y)) + (12.0f * this.xfak), p);
                } else {
                    canvas.drawText(xval[i], ((float) ((i * step) + off)) + (3.0f * this.xfak), ((float) (this.offy + this.f1y)) + (12.0f * this.xfak), p);
                }
                i++;
            }
//            canvas.drawText(this.sHz, ((float) this.offx) - (5.0f * this.xfak), ((float) (this.offy + this.f1y)) + (12.0f * this.xfak), p);
//            canvas.drawText(this.sdB, ((float) (this.offx + this.f0x)) + (4.0f * this.xfak), ((float) (this.offy + this.f1y)) + (12.0f * this.xfak), p);
//            if (this.iCalibmode != 0) {
//                canvas.drawText(this.calibstr[this.iCalibmode], ((float) (this.offx + this.x2)) + (3.0f * this.xfak), ((float) (this.offy + this.f1y)) + (12.0f * this.xfak), p);
//            } else if (this.iCnt < 10) {
//                canvas.drawText("", ((float) (this.offx + this.x2)) + (3.0f * this.xfak), ((float) (this.offy + this.f1y)) + (12.0f * this.xfak), p);
//            } else {
//                canvas.drawText(this.calibstr[this.iCalibmode], ((float) (this.offx + this.x2)) + (3.0f * this.xfak), ((float) (this.offy + this.f1y)) + (12.0f * this.xfak), p);
//            }
//            p.setTextSize(20.0f * this.xfak);
//            canvas.drawText(this.sPeak, ((float) (this.offx + this.x2)) + (15.0f * this.xfak), ((float) this.offy) + (25.0f * this.xfak), p);
//            canvas.drawText(this.sRMS, ((float) (this.offx + this.x2)) + (16.0f * this.xfak), ((float) this.offy) + (140.0f * this.xfak), p);
//            p.setTextSize(18.0f * this.xfak);
//            canvas.drawText(this.sdBFS, ((float) (this.offx + this.x2)) + (9.0f * this.xfak), ((float) this.offy) + (50.0f * this.xfak), p);
//            canvas.drawText(this.sdBFS, ((float) (this.offx + this.x2)) + (9.0f * this.xfak), ((float) this.offy) + (165.0f * this.xfak), p);
//            p.setTextSize(26.0f * this.xfak);
//            int ia = Math.abs((int) this.pk);
//            canvas.drawText("-" + String.valueOf(ia) + "." + String.valueOf(((int) (Math.abs(this.pk) * 10.0f)) - (ia * 10)), ((float) (this.offx + this.x2)) + (7.0f * this.xfak), ((float) this.offy) + (85.0f * this.xfak), p);
//            ia = Math.abs((int) this.rms);
//            canvas.drawText("-" + String.valueOf(ia) + "." + String.valueOf(((int) (Math.abs(this.rms) * 10.0f)) - (ia * 10)), ((float) (this.offx + this.x2)) + (7.0f * this.xfak), ((float) this.offy) + (200.0f * this.xfak), p);
            p.setStyle(Style.STROKE);

//            if(RTA.counter == 4) {
//                testStatus = false;
//                test = new TestStatus();
//                test.execute();
//                isTestPlaying = true;
//            }
            dcanvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, null);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            this.ctrlwidth = right - left;
            this.ctrlheight = bottom - top;
            this.xfak = ((float) this.ctrlheight) / 256.0f;
            this.offx = (int) (8.0f * this.xfak);
            this.offy = (int) (5.0f * this.xfak);
            this.f1y = this.ctrlheight - ((int) (25.0f * this.xfak));
            this.f0x = this.ctrlwidth - ((int) (40.0f * this.xfak));
            this.x2 = this.f0x + ((int) (29.0f * this.xfak));
            this.mBitmap = Bitmap.createBitmap(this.ctrlwidth, this.ctrlheight, Config.ARGB_8888);
            this.mCanvas = new Canvas(this.mBitmap);
            this.SizeOk = true;
            if (this.InitPrepared) {
                this.IsInit = true;
            }
        }
    }

    public void SetPeakHold(boolean setHold) {
        this.bopHold = setHold;
    }

    public void SetPeakTime(int setpkhld) {
        this.pkhld = setpkhld;
    }

    public void SetColor(int setColor) {
        this.iColor = getResources().getColor(R.color.bars);
        invalidate();
    }

    public void setPaused(boolean isPaused) {
        if (!this.IsInit) {
            return;
        }
        if (isPaused) {
            if (!this.running) {
                this.running = true;
                this.tha = new Thread(this);
                this.tha.start();
            }
            if (this.DoCal == 1) {
                setCalibMode(3);
            }
            this.DoCal = 0;
        } else if (this.running) {
            this.running = false;
            try {
                this.tha.join();
            } catch (InterruptedException e) {
            }
            this.clrpkrms = true;
        }
    }

    public void setCalibMode(int mode) {
        if (this.IsInit) {
            if (mode < 0) {
                mode = 0;
            }
            if (mode > 3) {
                mode = 3;
            }
            this.iCalibmode = mode;
            int i;
            switch (mode) {
                case 0:
                    this.DoCal = 1;
                    GetCalib(this.caltab);
                    return;
                case RTA.DIALOG_OPTION /*1*/:
                    this.DoCal = 0;
                    i = 0;
                    while (i < this.nbands) {
                        int i2 = i + 1;
                        this.caltab[i] = 1024;
                        i = i2;
                    }
                    SetCalib(this.caltab);
                    return;
                case 2:
                    this.DoCal = 0;
                    float fcal = 32.0f;
                    float dfcal = (float) Math.pow(2.0d, (double) (-this.space));
                    for (i = 0; i < this.nbands; i++) {
                        this.caltab[i] = (int) (((double) fcal) * 1024.0d);
                        fcal *= dfcal;
                        if (((double) fcal) < 1.0d) {
                            fcal = 1.0f;
                        }
                    }
                    SetCalib(this.caltab);
                    return;
                default:
                    return;
            }
        }
    }

    public int getCalibMode() {
        return this.iCalibmode;
    }

    public void getCalibBuf(IntBuffer ib) {
        GetCalib(this.caltab);
        ib.put(this.caltab);
    }

    public void setCalibBuf(IntBuffer ib) {
        ib.get(this.caltab);
        SetCalib(this.caltab);
    }

    public void run() {
        while (this.running) {
            long now = System.currentTimeMillis();
            if (now - this.lastnow > ((long) this.updspeed)) {
                int len = this.rtaval.length;
                for (int i = 0; i < len - 2; i++) {
                    this.rtaval[i] = -10000;
                }
                int[] iArr = this.rtaval;
                int i2 = len - 2;
                this.rtaval[len - 1] = 0;
                iArr[i2] = 0;
                SetData(null);
                this.lastnow = now;
            }
        }
    }
}
