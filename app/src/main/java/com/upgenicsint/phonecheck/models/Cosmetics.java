package com.upgenicsint.phonecheck.models;

import android.arch.lifecycle.Lifecycle;
import android.util.EventLog;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class Cosmetics {
    @SerializedName("Cosmetics")
    @Expose
    public List<CosmeticsKeys> cosmetics = null;

    public List<CosmeticsKeys> getCosmetics() {
        return cosmetics;
    }
    public void setCosmetics(List<CosmeticsKeys> cosmetics) {
        this.cosmetics = cosmetics;
    }
}
