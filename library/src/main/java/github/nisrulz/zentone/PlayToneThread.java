package github.nisrulz.zentone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

/**
 * The type Play tone thread.
 */
class PlayToneThread extends Thread {

    private boolean isPlaying = false;
    private final int freqOfTone;
    private final int duration;
    private AudioTrack audioTrack = null;
    private ToneStoppedListener toneStoppedListener;
    private float volume = 0f;
    AudioManager audioManager;
    Context context;

    private boolean receiverMic;

    /**
     * Instantiates a new Play tone thread.
     *
     * @param freqOfTone          the freq of tone
     * @param duration            the duration
     * @param volume              the volume
     * @param toneStoppedListener the tone stopped listener
     */
    public PlayToneThread(int freqOfTone, int duration, float volume, boolean receiveMic, Context context,
                          ToneStoppedListener toneStoppedListener) {
        this.freqOfTone = freqOfTone;
        this.duration = duration;
        this.toneStoppedListener = toneStoppedListener;
        this.volume = volume;
        this.receiverMic = receiveMic;
        this.context = context;
    }

    public PlayToneThread(int freqOfTone, int duration, float volume) {
        this.freqOfTone = freqOfTone;
        this.duration = duration;
        this.volume = volume;
    }

    public PlayToneThread() {
        freqOfTone = 0;
        duration = 0;
    }

    @Override
    public void run() {
        super.run();

        //Play tone
        playTone();
    }

    private void playTone() {
        if (!isPlaying) {
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            isPlaying = true;
            int sampleRate = 44100;// 44.1 KHz

            double dnumSamples = (double) duration * sampleRate;
            dnumSamples = Math.ceil(dnumSamples);
            int numSamples = (int) dnumSamples;
            double[] sample = new double[numSamples];
            byte[] generatedSnd = new byte[2 * numSamples];

            for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
                sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
            }

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalized.
            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            int i;

            int ramp = numSamples / 20;  // Amplitude ramp as a percent of sample count

            for (i = 0; i < ramp; ++i) {  // Ramp amplitude up (to avoid clicks)
                // Ramp up to maximum
                final short val = (short) (sample[i] * 32767 * i / ramp);
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }

            for (i = ramp; i < numSamples - ramp;
                 ++i) {                        // Max amplitude for most of the samples
                // scale to maximum amplitude
                final short val = (short) (sample[i] * 32767);
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }

            for (i = numSamples - ramp; i < numSamples; ++i) { // Ramp amplitude down
                // Ramp down to zero
                final short val = (short) (sample[i] * 32767 * (numSamples - i) / ramp);
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }

            try {
                int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                if (receiverMic) {
                    audioManager.setSpeakerphoneOn(false);
                    audioTrack = new AudioTrack(AudioManager.MODE_NORMAL, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                } else {
                    audioManager.setSpeakerphoneOn(true);
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                }
                audioTrack.setNotificationMarkerPosition(numSamples);
                audioTrack.setPlaybackPositionUpdateListener(
                        new AudioTrack.OnPlaybackPositionUpdateListener() {
                            @Override
                            public void onPeriodicNotification(AudioTrack track) {
                                // nothing to do
                            }

                            @Override
                            public void onMarkerReached(AudioTrack track) {
                                toneStoppedListener.onToneStopped();
                            }
                        });

                // Sanity Check for max volume, set after write method to handle issue in android
                // v 4.0.3
                float maxVolume = AudioTrack.getMaxVolume();

                if (volume > maxVolume) {
                    volume = maxVolume;
                } else if (volume < 0) {
                    volume = 0;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioTrack.setVolume(volume);
                } else {
                    audioTrack.setStereoVolume(volume, volume);
                }

                audioTrack.play(); // Play the track
                audioTrack.write(generatedSnd, 0, generatedSnd.length);    // Load the track
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopTone();
        }
    }

    /**
     * Stop tone.
     */
    void stopTone() {
        if (audioTrack != null && audioTrack.getState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.stop();
            audioTrack.release();
            Log.d("Zentone", "Tone Thread Stopped 1");
            isPlaying = false;
        }
    }

    void stopTone2() {
        try {
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                Log.d("Zentone", "Tone Thread Stopped 2");
                isPlaying = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
