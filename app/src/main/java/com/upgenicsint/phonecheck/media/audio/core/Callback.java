package com.upgenicsint.phonecheck.media.audio.core;

public interface Callback {
    void onBufferAvailable(byte[] buffer);
}