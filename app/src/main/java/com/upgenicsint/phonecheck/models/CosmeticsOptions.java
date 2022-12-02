package com.upgenicsint.phonecheck.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CosmeticsOptions {

    @SerializedName("response")
    @Expose
    public String response;
    @SerializedName("result")
    @Expose
    public Boolean result;



    public void setRespomse(String response) {
        this.response = response;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public Boolean getResult() {
        return result;
    }

    public String getResponse() {
        return response;
    }

    boolean isSelected = false;

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
    public boolean getSelected() {
        return isSelected;
    }
}
