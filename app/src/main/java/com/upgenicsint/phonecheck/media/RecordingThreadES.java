/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.upgenicsint.phonecheck.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RecordingThreadES implements Runnable{
    public interface CallBack {
        void onRecording(short[] samples);
        //void onStop();
    }
    private static final String LOG_TAG = RecordingThreadES.class.getSimpleName();
    private static final int SAMPLE_RATE = 44100;
    public static boolean isMic;
    public int currentAmplitude = 0;
    private FileOutputStream os = null;
    private int bufferSize = 0;
    private AudioRecord record = null;
    private Thread mThread;
    public static boolean mShouldContinue;
    private CallBack mListener;
    public RecordingThreadES(CallBack listener) {
        mListener = listener;
        mShouldContinue = true;

    }


    public boolean recording() {
        return  RecordingThreadES.mShouldContinue;
    }

    public void startRecording() {
        if (record == null)
        {
            initAudioRecorder();
        }

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        mShouldContinue = true;
        record.startRecording();
        mThread.start();

    }

    public void stopRecording() {
        mShouldContinue = false;
        if (record != null)
        {
            try{
                record.stop();
                record.release();
            }catch (Exception e){
                e.printStackTrace();
            }
            record = null;
        }

        mListener = null;

    }

    private void initAudioRecorder() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        record = new AudioRecord(isMic ? MediaRecorder.AudioSource.MIC : MediaRecorder.AudioSource.CAMCORDER,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        mThread = new Thread(this);
    }

    @Override
    public void run() {
        record();
    }

    private void record() {
        Log.v(LOG_TAG, "Start");

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        File fileMainPath;
        File sdcard = new File("/sdcard");
        if (sdcard.exists()) {
            fileMainPath = sdcard;
        } else {
            fileMainPath = Environment.getExternalStorageDirectory();
        }

        String filePathTemp = RecordingThreadES.isMic ? "microphoneTempES.pcm" : "videoMicrophoneTempES.pcm";

        File recordFile = new File(fileMainPath, filePathTemp);
        try {
            recordFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.v(LOG_TAG, "Start recording");

        long shortsRead = 0;
        byte bufRecord[] = new byte[bufferSize];

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(recordFile));
            while (mShouldContinue && record != null) {
                double sum = 0;
                int readSize = record.read(bufRecord, 0, bufRecord.length);
                for (int i = 0; i < readSize; i++) {
                    try {
                        output.writeShort(bufRecord [i]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sum += bufRecord [i] * bufRecord [i];
                }
                if (readSize > 0) {
                    currentAmplitude = (int) sum / readSize;

                    if (mListener!=null)
                    {
                        short[] shorts = new short[bufRecord.length/2];
                        ByteBuffer.wrap(bufRecord).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                        mListener.onRecording(shorts);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (output!=null)
        {
            try {
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        currentAmplitude = -1;

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }

    public static byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[(shortArrsize * 2)];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 255);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = (short) 0;
        }
        return bytes;
    }
}
