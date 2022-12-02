/*
 * Copyright (C) 2010 ZXing authors
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
 */

package com.upgenicsint.phonecheck.barcode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.upgenicsint.phonecheck.BuildConfig;
import com.upgenicsint.phonecheck.Loader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import kotlin.jvm.internal.Intrinsics;

/**
 * Manages beeps and vibrations for {@link CaptureActivity}.
 */
final public class BeepManagerES implements MediaPlayer.OnErrorListener, Closeable {

    private static final String TAG = BeepManagerES.class.getSimpleName();

    private static final float BEEP_VOLUME = 1.0f;
    private static final long VIBRATE_DURATION = 200L;

    public final Activity activity;
    private boolean isWavFile = false;
    private MediaPlayer mediaPlayer;
    private boolean playBeep = true;
    private boolean vibrate = true;
    private boolean earpiecePlay = false;
    static AudioManager audioService;
    AudioManager audioManager;
    private boolean isClicked = false;
    private boolean isSpeakerClicked = false;
    private boolean isSpeakerFailed = false;
    private boolean isSamsung;
    private boolean isMicrophone;
    private boolean isMicC;
    private boolean isMicPlayBack;

    private final int getVolumeForMode(int mode) {
        if (BuildConfig.DEBUG) {
            return (int) ((double) audioManager.getStreamMaxVolume(mode) * 1F);
        } else {
            AudioManager audioManager = this.audioManager;
            if (this.audioManager == null) {
                Intrinsics.throwNpe();
            }
            return (int) ((double) audioManager.getStreamMaxVolume(mode) * 1F);
        }
    }

    public BeepManagerES(Activity activity, boolean earpiece, boolean isSamsung, boolean isMicrophone) {
        this.activity = activity;
        this.mediaPlayer = null;
        this.earpiecePlay = earpiece;
        this.isSamsung = isSamsung;
        this.isMicrophone = isMicrophone;
    }

    public BeepManagerES(Activity activity, boolean earpiece, boolean isMicPlayback) {
        this.activity = activity;
        this.mediaPlayer = null;
        this.earpiecePlay = earpiece;
        this.isMicPlayBack = isMicPlayback;
    }

    public BeepManagerES(boolean earpiece, boolean isWavFile, Activity activity) {
        this.activity = activity;
        this.mediaPlayer = null;
        this.earpiecePlay = earpiece;
        this.isWavFile = isWavFile;
    }

    public BeepManagerES(Activity activity, boolean earpiece, boolean isSamsung, boolean isMicrophone, boolean isMicC) {
        this.activity = activity;
        this.mediaPlayer = null;
        this.earpiecePlay = earpiece;
        this.isSamsung = isSamsung;
        this.isMicrophone = isMicrophone;
        this.isMicC = isMicC;
    }

    public synchronized void updatePrefs() {
        if (mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
            // so we now play on the music stream.
            if (earpiecePlay) {
                activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            } else {
                activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
            }
            mediaPlayer = buildMediaPlayer(activity);
        }
    }

    public synchronized void playBeepSoundAndVibrate() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
//    if (vibrate) {
//      Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
//      vibrator.vibrate(VIBRATE_DURATION);
//    }
    }

    private static boolean shouldBeep(SharedPreferences prefs, Context activity) {
        boolean shouldPlayBeep = prefs.getBoolean(PreferencesActivity.KEY_PLAY_BEEP, true);
        if (shouldPlayBeep) {
            // See if sound settings overrides this
            audioService = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                shouldPlayBeep = false;
            }
        }
        return shouldPlayBeep;
    }


    @SuppressLint("SdCardPath")
    private MediaPlayer buildMediaPlayer(Activity activity) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        try {
            if (Loader.getInstance().isAutoESEnabled && Loader.getInstance().isMicESEnabled && isSamsung) {
                if (isMicrophone) {
                    setSpeakerType(audioManager, mediaPlayer, "audio/beep_test.wav");
                } else if (isMicC) {
                    setSpeakerType(audioManager, mediaPlayer, "audio/mic_update.mp3");
                } else {
                    setSpeakerType(audioManager, mediaPlayer, "audio/mic_es_update1.mp3");
                }
            } else if (isMicPlayBack) {
                setSpeakerType(audioManager, mediaPlayer, "audio/hello_sound.mp3");
            } else if (isWavFile) {
                String fileMainPath;
                File sdcard = new File("/sdcard");
                if (sdcard.exists()) {
                    fileMainPath = "/sdcard/Temp.wav";
                } else {
                    fileMainPath = Environment.getExternalStorageDirectory() + "/Temp.wav";
                }
                setSpeakerType(activity, audioManager, fileMainPath, mediaPlayer);
            } else {
                if (isSamsung) {
                    if (isMicrophone) {
                        setSpeakerType(audioManager, mediaPlayer, "audio/beep_test.wav");
                    } else if (isMicC) {
                        setSpeakerType(audioManager, mediaPlayer, "audio/mic_update.mp3");
                    } else {
                        setSpeakerType(audioManager, mediaPlayer, "audio/mic_es_update1.mp3");
                    }
                } else {
                    setSpeakerType(audioManager, mediaPlayer, "audio/beep_test.wav");
                }
            }
            return mediaPlayer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setSpeakerType(AudioManager audioManager, final MediaPlayer mediaPlayer, String fileName) {
        try {
            AssetFileDescriptor file = activity.getAssets().openFd(fileName);
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            mediaPlayer.setOnErrorListener(this);
            if (earpiecePlay) {
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, getVolumeForMode(AudioManager.STREAM_VOICE_CALL), 0);
                audioManager.setSpeakerphoneOn(false);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, getVolumeForMode(AudioManager.STREAM_MUSIC), 0);
                audioManager.setSpeakerphoneOn(true);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer.release();
        }
    }

    private void closeImmediate(final MediaPlayer mediaPlayer, final Activity activity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (activity != null && !activity.isFinishing()) {
                   activity.runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           if (mediaPlayer != null) {
                               try {
                                   Log.d("beep", mediaPlayer.getCurrentPosition()+"");
                                   if (mediaPlayer.getCurrentPosition() > 2500) {
                                       close();
                                   }
                               } catch (Exception e) {
                                   Log.d("beep", "current position is null");
                               }
                           }
                       }
                   });
                }
            }
        }).start();
    }

    private void setSpeakerType(Activity activity, AudioManager audioManager, String fileName, MediaPlayer mediaPlayer) {
        try {
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.setOnErrorListener(this);
            if (earpiecePlay) {
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, getVolumeForMode(AudioManager.STREAM_VOICE_CALL), 0);
                audioManager.setSpeakerphoneOn(false);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, getVolumeForMode(AudioManager.STREAM_MUSIC), 0);
                audioManager.setSpeakerphoneOn(true);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.prepare();
            closeImmediate(mediaPlayer, activity);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer.release();
        }
    }

    @Override
    public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required and finish
            activity.finish();
        } else {
            // possibly media player error, so release and recreate
            close();
        }
        return true;
    }

    @Override
    public synchronized void close() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void setDevice(boolean isSamsung) {
        this.isSamsung = isSamsung;
    }

    public void setMicrophone(boolean isMicrophone) {
        this.isMicrophone = isMicrophone;
    }

    public void setMicC(boolean isMic) {
        this.isMicC = isMic;
    }

    public void setSpeakerTone(boolean speaker) {
        this.isSpeakerClicked = speaker;
    }

    public void checkSpeaker(boolean speakerTestFailed) {
        this.isSpeakerFailed = speakerTestFailed;
    }
}
