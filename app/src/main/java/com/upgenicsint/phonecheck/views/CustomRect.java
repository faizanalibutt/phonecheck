package com.upgenicsint.phonecheck.views;

import android.graphics.RectF;

public class CustomRect extends RectF {
    private boolean isGreen;

    public CustomRect(float left, float top, float right, float bottom, boolean isGreen) {
        super(left, top, right, bottom);
        this.isGreen = isGreen;
    }

    public boolean isGreen() {
        return isGreen;
    }

    public void setGreen(boolean green) {
        isGreen = green;
    }
}
