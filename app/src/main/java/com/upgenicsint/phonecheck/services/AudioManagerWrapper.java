package com.upgenicsint.phonecheck.services;

import android.media.AudioManager;

import com.upgenicsint.phonecheck.BuildConfig;
import com.upgenicsint.phonecheck.Loader;

public class AudioManagerWrapper {
    public static int getVolumeForMode(int mode, AudioManager audioManager)
    {
        return  (BuildConfig.DEBUG) ? 15 :  (!Loader.getInstance().isAutoAudioEnabled) ? (int)(audioManager.getStreamMaxVolume(mode) * 1.0) : audioManager.getStreamMaxVolume(mode);
    }
}
