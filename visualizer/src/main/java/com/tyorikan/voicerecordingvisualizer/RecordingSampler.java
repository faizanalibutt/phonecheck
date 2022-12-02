/*
 * Copyright (C) 2015 tyorikan
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tyorikan.voicerecordingvisualizer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sampling AudioRecord Input
 * This output send to {@link VisualizerView}
 * <p>
 * Created by tyorikan on 2015/06/09.
 * Modified by farhanahmed95
 */
public class RecordingSampler {

    private static int RECORDING_SAMPLE_RATE = 44100;
    public static final String TAG = RecordingSampler.class.getSimpleName();
    public static AudioRecord mAudioRecord;
    private boolean mIsRecording;
    private int mBufSize;
    private Thread recordingThread = null;

    private CalculateVolumeListener mVolumeListener;
    private int mSamplingInterval = 100;
    private Timer mTimer;
    private int amplitude;
    public static int amplitude2;
    private int currentAudioSource = MediaRecorder.AudioSource.MIC;

    private Listener listener;
    private List<VisualizerView> mVisualizerViews = new ArrayList<>();
    private boolean releaseVisualizer = false;
    private int bufferSize;
    protected boolean isInitialized;
    String filePathTemp = "";
    String filePathOut = "";
    FileOutputStream os = null;
    File recordFile = null;
    File recordFileOutput = null;
    public static boolean isRecording = false;
    private int decibel = 0;

    public RecordingSampler() {

    }

    public RecordingSampler(Listener listener) {
        setListener(listener);
        initAudioRecord();
    }

    public RecordingSampler(int currentAudioSource, Listener listener) {
        this(listener);
        setAudioSource(currentAudioSource);
        initAudioRecord();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setReleaseVisualizer(boolean releaseVisualizer) {
        this.releaseVisualizer = releaseVisualizer;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * link to VisualizerView
     *
     * @param visualizerView {@link VisualizerView}
     */
    public void link(VisualizerView visualizerView) {
        mVisualizerViews.add(visualizerView);
    }

    /**
     * setter of CalculateVolumeListener
     *
     * @param volumeListener CalculateVolumeListener
     */
    public void setVolumeListener(CalculateVolumeListener volumeListener) {
        mVolumeListener = volumeListener;
    }

    /**
     * setter of samplingInterval
     *
     * @param samplingInterval interval volume sampling
     */
    public void setSamplingInterval(int samplingInterval) {
        mSamplingInterval = samplingInterval;
    }

    /**
     * getter isRecording
     *
     * @return true:recording, false:not recording
     */
    public boolean isRecording() {
        return mIsRecording;
    }

    public void setAudioSource(int source) {
        this.currentAudioSource = source;
    }

    public int getAudioSource() {
        return currentAudioSource;
    }

    public int getRecordingSampleRate() {
        return RECORDING_SAMPLE_RATE;
    }

    public void initAudioRecord() {
        if (mAudioRecord != null) {
            release();
        }
//        if (mAudioRecord != null && mRecorder != null) {
//            release();
//        }

        try {
//            int bufferSize = AudioRecord.getMinBufferSize(
//                    RECORDING_SAMPLE_RATE,
//                    AudioFormat.CHANNEL_IN_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT
//            );
//
            // Possible list of rates
            for (int rate : new int[]{8000, 11025, 16000, 22050, 44100}) {
                bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (bufferSize > 0) {
                    RECORDING_SAMPLE_RATE = 44100;
                    try {
                        mAudioRecord = new AudioRecord(
                                currentAudioSource,
                                RECORDING_SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                bufferSize
                        );

                        break;
                    } catch (Exception ignored) {

                    }
                }
            }

            if (mAudioRecord != null) {
                if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    Log.d(TAG, "mAudioRecord STATE_INITIALIZED");
                    mBufSize = bufferSize;
                    isInitialized = true;
                } else {
                    isInitialized = false;
                    if (listener != null) {
                        listener.onError(this, "mAudioRecord STATE_INITIALIZED");
                    }
                }
            } else {
                isInitialized = false;
                if (listener != null) {
                    listener.onError(this, "mAudioRecord STATE_INITIALIZED");
                }
            }

        } catch (Exception e) {
            if (listener != null) {
                listener.onError(this, e.getMessage());
            }
            e.printStackTrace();
        }


    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * start AudioRecord.read
     */

    public void startRecording() {

        if (mAudioRecord == null || !isInitialized())
            return;

        mTimer = new Timer();
        try {
            mAudioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mIsRecording = true;
        isRecording = true;

        File fileMainPath;
        File sdcard = new File("/sdcard");
        if (sdcard.exists()) {
            fileMainPath = sdcard;
        } else {
            fileMainPath = Environment.getExternalStorageDirectory();
        }

        if (currentAudioSource == 1) {

            filePathTemp = "microphoneTemp.pcm";
            //filePathOut = "microphone.wav";
            recordFile = new File(fileMainPath, filePathTemp);
            try {
                recordFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            recordFileOutput = new File(Environment.getExternalStorageDirectory(), filePathOut);
//            try {
//                recordFileOutput.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            try {
                os = new FileOutputStream(recordFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        } else if (currentAudioSource == 5) {
            filePathTemp = "videoMicrophoneTemp.pcm";
//            filePathOut = "videoMicrophone.wav";
            File recordFile2 = new File(fileMainPath, filePathTemp);
            try {
                recordFile2.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            recordFileOutput = new File(Environment.getExternalStorageDirectory(), filePathOut);
//            try {
//                recordFileOutput.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            try {
                os = new FileOutputStream(recordFile2);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        runRecording();
    }

    /**
     * stop AudioRecord.read
     */
    public void stopRecording() {

        mIsRecording = false;
        isRecording = false;
        Log.d(TAG, "releaseVisualizer " + releaseVisualizer + " Audio Source " + getAudioSource());
        if (mTimer != null)
            mTimer.cancel();
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            generateFinalAudio();
        }
        if (mVisualizerViews != null && !mVisualizerViews.isEmpty()) {
            for (int i = 0; i < mVisualizerViews.size(); i++) {
                mVisualizerViews.get(i).receive(0);
            }
        }
    }

    public int getAmplitude() {
        return amplitude;
    }

    private void runRecording() {
        if (mAudioRecord == null || !isInitialized()) {
            Log.e(TAG, "mAudioRecord null @runRecording");
            return;
        }

        final byte buf[] = new byte[mBufSize];

        recordingThread = new Thread(new Runnable() {

            public void run() {

//                if (!isRecording) {
//
//                }

                while (isRecording) {
                    try {
                        // stop recording
                        if (!mIsRecording && mAudioRecord != null) {
                            //Some Device (HTC EVO models) will throw Exceptions when stop is called
                            mAudioRecord.stop();
                            mTimer.cancel();
                            isRecording = false;
                            return;
                        }
                        int sum = 0;
                        //Sometimes null exception happen at mAudioRecord.read()
                        if (mAudioRecord == null) {
                            mTimer.cancel();
                            isRecording = false;
                            return;
                        }
                        int readSize;
                        try {
                            readSize = mAudioRecord.read(buf, 0, mBufSize);
                            Log.d("readSize",readSize+"");
                        } catch (Exception e) {
                            mTimer.cancel();
                            isRecording = false;
                            return;
                        }
                        for (int i = 0; i < readSize; i++) {
                            //output.writeShort(buf [i]);
                            sum += buf[i] * buf[i];
                        }
                        if (readSize > 0) {
                            amplitude = sum / readSize;
                        }

//                        double average = 0.0;
//                        //recording data;
//                        mAudioRecord.read(data, 0, bufferSize);
//
//                        mAudioRecord.stop();
//                        Log.e(TAG, "stop");
//                        for (short s : data)
//                        {
//                            if(s>0)
//                            {
//                                average += Math.abs(s);
//                            }
//                            else
//                            {
//                                bufferSize--;
//                            }
//                        }
//                        //x=max;
//                        double x = average/bufferSize;
//
//                        amplitude = (int) x;


//                        double sDataMax = 0;
//                        for (byte aBuf : buf) {
//                            if (Math.abs(aBuf) >= sDataMax) {
//                                sDataMax = Math.abs(aBuf);
//                            }
//                            amplitude2 = (int) sDataMax;
//
//                        }

//                        for (int i=0; i<readSize/2; i++) {
//                            short curSample = getShort(buf[i*2], buf[i*2+1]);
//                            if (curSample > amplitude2) {
//                                amplitude2 = curSample;
//                            }
//                        }

                        Log.d("amplitude",amplitude+"");

                        decibel = calculateDecibel(buf);

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (listener != null) {
                            listener.onError(RecordingSampler.this, e.getMessage());
                        }
                    }


                    // callback for return input value
                    if (mVolumeListener != null) {
                        mVolumeListener.onCalculateVolume(decibel);
                    }
                    try {
                        os.write(buf, 0, bufferSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }, "AudioRecorder Thread");
        recordingThread.start();


        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mVisualizerViews != null && !mVisualizerViews.isEmpty()) {
                    for (int i = 0; i < mVisualizerViews.size(); i++) {
                        mVisualizerViews.get(i).receive(decibel);
                    }
                }
            }
        }, 0, mSamplingInterval);



    }

    private short getShort(byte argB1, byte argB2) {
        return (short)(argB1 | (argB2 << 8));
    }



    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private int calculateDecibel(byte[] buf) {
        int sum = 0;
        for (int i = 0; i < mBufSize; i++) {
            sum += Math.abs(buf[i]);
        }
        // avg 10-50
        if (mBufSize != 0) {
            return sum / mBufSize;
        } else {
            return 0;
        }
    }

    public void generateFinalAudio() {
        CopyAudioTask task = new CopyAudioTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recordFile, recordFileOutput);
    }

    private class CopyAudioTask extends AsyncTask<File, File, Void> {
        @Override
        protected Void doInBackground(File... params) {
            String tmpPath = String.valueOf(params[0]);
            String outPath = String.valueOf(params[1]);

            try {
                FileInputStream in = new FileInputStream(tmpPath);
                FileOutputStream out = new FileOutputStream(outPath);
                byte[] data = new byte[mBufSize];

                writeWaveFileHeader(in, out);
                while (in.read(data) != -1) {
                    out.write(data);
                }
                in.close();
                out.close();

            } catch (Exception e) {
                Log.e("Recorder", e.getMessage(), e);
            }
            return null;
        }
    }

    private void writeWaveFileHeader(FileInputStream in, FileOutputStream out) throws IOException {
        int bitWidth = 32;
        long longSampleRate = 8000;
        int channels = 1;
        long totalAudioLen = in.getChannel().size();
        long totalDataLen = totalAudioLen + 36;
        long byteRate = bitWidth * longSampleRate * channels / 8;

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * bitWidth / 8); // block align
        header[33] = 0;
        header[34] = (byte) bitWidth; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    /**
     * release member object
     */
    public void release() {
        if (mAudioRecord == null)
            return;
//        if (mAudioRecord == null || mRecorder != null)
//            return;
        stopRecording();
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        isRecording = false;
        mIsRecording = false;
        recordingThread = null;
        mTimer = null;
//        mRecorder.stop();
//        mRecorder.release();
//        mRecorder = null;

    }

    public int getDecible() {
        return decibel;
    }

    public interface CalculateVolumeListener {

        /**
         * calculate input volume
         *
         * @param volume mic-input volume
         */
        void onCalculateVolume(int volume);
    }

    public interface Listener {
        void onError(RecordingSampler sampler, String e);
    }
}
