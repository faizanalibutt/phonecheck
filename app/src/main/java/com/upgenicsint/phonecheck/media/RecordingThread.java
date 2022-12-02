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
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RecordingThread {

    private static final String LOG_TAG = RecordingThread.class.getSimpleName();
    private static final int SAMPLE_RATE = 44100;
    public static boolean isMic;
    private FileOutputStream os = null;
    int bufferSize = 0;

    public RecordingThread(AudioDataReceivedListener listener) {
        mListener = listener;
    }

    public RecordingThread() { }

    public boolean getMicStatus() {
        return isMic;
    }

    public static boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;

    public boolean recording() {
        return mThread != null;
    }

    public void startRecording() {
        if (mThread != null)
            return;

        mShouldContinue = true;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        mThread.start();
    }

    public void stopRecording() {
        if (mThread == null)
            return;
        mShouldContinue = false;
        mThread = null;
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void record() {
        Log.v(LOG_TAG, "Start");
//        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];

        String filePathTemp;

        File fileMainPath;
        File sdcard = new File("/sdcard");
        if (sdcard.exists()) {
            fileMainPath = sdcard;
        } else {
            fileMainPath = Environment.getExternalStorageDirectory();
        }

        if (RecordingThread.isMic) {
            filePathTemp = "microphoneTemp.pcm";

            File recordFile = new File(fileMainPath, filePathTemp);
            try {
                recordFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os = new FileOutputStream(recordFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (!RecordingThread.isMic) {
            filePathTemp = "videoMicrophoneTemp.pcm";

            File recordFile2 = new File(fileMainPath, filePathTemp);
            try {
                recordFile2.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os = new FileOutputStream(recordFile2);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        AudioRecord record = new AudioRecord(isMic ? MediaRecorder.AudioSource.MIC : MediaRecorder.AudioSource.CAMCORDER,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        long shortsRead = 0;
        final byte bufRecord[] = new byte[bufferSize];
        final byte buf[] = short2byte(audioBuffer);


        while (mShouldContinue) {

            int numberOfShort = record.read(bufRecord, 0, bufferSize);
            shortsRead += numberOfShort;

            try {
                os.write(bufRecord, 0, bufferSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Notify waveform
            short[] shorts = new short[bufRecord.length/2];
            ByteBuffer.wrap(bufRecord).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            mListener.onAudioDataReceived(shorts);
        }

        record.stop();
        record.release();

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
