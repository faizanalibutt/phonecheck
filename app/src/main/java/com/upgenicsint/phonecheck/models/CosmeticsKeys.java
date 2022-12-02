package com.upgenicsint.phonecheck.models;

import android.content.Context;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CosmeticsKeys {
    private static CosmeticsKeys mInstance;
    private static Context mCtx;


    @SerializedName("title")
    @Expose
    public String title;
    @SerializedName("platform")
    @Expose
    public String platform;
    @SerializedName("shortKey")
    @Expose
    public String shortkey;
    @SerializedName("options")
    @Expose
    public List<CosmeticsOptions> cosmeticsoptions = null;

//    private CosmeticsKeys(Context context) {
//        mCtx = context;
//    }
//
//    public static synchronized CosmeticsKeys getInstance(Context context) {
//        if (mInstance == null) {
//            mInstance = new CosmeticsKeys(context);
//        }
//        return mInstance;
//    }

    public String getPlatform() {
        return platform;
    }

    public String getShortkey() {
        return shortkey;
    }

    public String getTitle() {
        return title;
    }

    public List<CosmeticsOptions> getCosmetics() {
        return cosmeticsoptions;
    }

    public void setCosmetics(List<CosmeticsOptions> cosmetics) {
        this.cosmeticsoptions = cosmetics;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setShortkey(String shortkey) {
        this.shortkey = shortkey;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
