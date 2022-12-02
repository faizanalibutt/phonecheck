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

package com.upgenicsint.phonecheck.media;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.upgenicsint.phonecheck.BuildConfig;
import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.barcode.CaptureActivity;
import com.upgenicsint.phonecheck.barcode.PreferencesActivity;

import java.io.Closeable;
import java.io.IOException;

import kotlin.jvm.internal.Intrinsics;

/**
 * Manages beeps and vibrations for {@link CaptureActivity}.
 */
final public class BeepManagerCustom implements MediaPlayer.OnErrorListener, Closeable {

    private static final String TAG = BeepManagerCustom.class.getSimpleName();

    private static final float BEEP_VOLUME = 1.0f;
    private static final long VIBRATE_DURATION = 200L;

    public final Activity activity;
    private MediaPlayer mediaPlayer;
    private boolean playBeep = true;
    private boolean vibrate = true;
    private boolean earpiecePlay = false;
    static AudioManager audioService;
    AudioManager audioManager;
    private boolean isClicked = false;
    private boolean isSpeakerClicked = false;
    private boolean isSpeakerFailed = false;
    private boolean loop = false;

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

    public BeepManagerCustom(Activity activity, boolean earpiece) {
        this.activity = activity;
        this.mediaPlayer = null;
        this.earpiecePlay = earpiece;
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


    private MediaPlayer buildMediaPlayer(Context activity) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        try {
            AssetFileDescriptor file = activity.getAssets().openFd("audio/crop_audio.mp3");
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            mediaPlayer.setOnErrorListener(this);
            if (earpiecePlay) {
                audioManager.setStreamVolume(AudioManager.MODE_NORMAL, getVolumeForMode(AudioManager.MODE_NORMAL), 0);
                audioManager.setSpeakerphoneOn(false);
                mediaPlayer.setAudioStreamType(AudioManager.MODE_NORMAL);

            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, getVolumeForMode(AudioManager.STREAM_MUSIC), 0);
                audioManager.setSpeakerphoneOn(true);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer.release();
            return null;
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
            mediaPlayer.setLooping(false);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }
}
