package radonsoft.net.rta;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;

import java.nio.ByteBuffer;

public class GetAudio implements Runnable {
    private static final int audioEncoding = 2;
    int IsOK = 0;
    private int channelConfiguration;
    private int frameSize = 1024;
    private int frequency;
    private volatile boolean isPaused;
    private volatile boolean isRecording;
    private final Object mutex = new Object();
    private RtaView myRta;
    ByteBuffer smpBuffer = null;
    static AudioRecord recordInstance;
    public static int amplitude;
    private int mBufSize;
    private int bufferSize;
    int recframeSize;

    public GetAudio() {
        setFrequency(8000);
        setChannelConfiguration(16);
        setPaused(false);
    }

    public void run() {
        synchronized (this.mutex) {
            while (!this.isRecording) {
                try {
                    this.mutex.wait();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Wait() interrupted!", e);
                }
            }
        }
        Process.setThreadPriority(-19);
        recframeSize = AudioRecord.getMinBufferSize(getFrequency(), getChannelConfiguration(), getAudioEncoding());
        int bufferSize = recframeSize * audioEncoding;
        while (bufferSize < 8192) {
            bufferSize += recframeSize;
        }
        while (bufferSize % (this.frameSize * audioEncoding) != 0) {
            bufferSize += recframeSize;
        }
        mBufSize = recframeSize;
        if(RTA.audioSource.equals("Camcorder")){
            recordInstance = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, getFrequency(), getChannelConfiguration(), getAudioEncoding(), bufferSize);
        }
        else{
            recordInstance = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, getFrequency(), getChannelConfiguration(), getAudioEncoding(), bufferSize);
        }
        if (recordInstance.getState() == 0) {
            this.IsOK = -1;
            return;
        }
        try {
            recordInstance.startRecording();
            this.IsOK = 1;
            while (this.isRecording) {
                synchronized (this.mutex) {
                    getAmplitude();
                    if (this.isPaused) {
                        try {
                            this.mutex.wait(250);
                        } catch (InterruptedException e2) {
                            throw new IllegalStateException("Wait() interrupted!", e2);
                        }
                    }
                    if (this.smpBuffer != null) {
                        int bufferRead = recordInstance.read(this.smpBuffer, this.frameSize * audioEncoding);
                        if (bufferRead == -3) {
                            throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
                        } else if (bufferRead == -2) {
                            throw new IllegalStateException("read() returned AudioRecord.ERROR_BAD_VALUE");
                        } else if (bufferRead == -3) {
                            throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
                        } else {
                            this.myRta.SetData(this.smpBuffer);
                        }
                    } else {
                        continue;
                    }
                }
            }
            recordInstance.stop();
            recordInstance.release();
        } catch (IllegalStateException e3) {
            this.IsOK = -1;
        }
    }

    public void setRta(RtaView setRta) {
        this.myRta = setRta;
    }

    public void setframeSize(int frsize) {
        this.frameSize = frsize;
        this.smpBuffer = ByteBuffer.allocateDirect(this.frameSize * audioEncoding);
    }

    public void setRecording(boolean isRecording) {
//        synchronized (this.mutex) {
            this.isRecording = isRecording;
            if (this.isRecording) {
//                this.mutex.notify();
            }
//        }
    }

    public boolean isRecording() {
        boolean z;
        synchronized (this.mutex) {
            z = this.isRecording;
        }
        return z;
    }
    public void getAmplitude(){
        int readSize;
        int sum = 0;
        final byte buf[] = new byte[mBufSize];
        try {
            readSize = recordInstance.read(buf, 0, mBufSize);
        } catch (Exception e) {
            return;
        }
        for (int i = 0; i < readSize; i++) {
            sum += buf[i] * buf[i];
        }
        if (readSize > 0) {
            amplitude = sum / readSize;
        }
    }
    public int micAmplitude() {
        return amplitude;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setChannelConfiguration(int channelConfiguration) {
        this.channelConfiguration = channelConfiguration;
    }

    public int getChannelConfiguration() {
        return this.channelConfiguration;
    }

    public int getAudioEncoding() {
        return audioEncoding;
    }

    public void setPaused(boolean isPaused) {
        synchronized (this.mutex) {
            this.isPaused = isPaused;
        }
    }

    public boolean isPaused() {
        boolean z;
        synchronized (this.mutex) {
            z = this.isPaused;
        }
        return z;
    }
}
