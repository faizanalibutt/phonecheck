package com.upgenicsint.phonecheck.models;

public class CosmeticsResults {
    private String shortKeyCosmetics;
    private boolean resultCosmetics;

    public CosmeticsResults() {

    }

    public CosmeticsResults(String shortkey, boolean result) {
        this.shortKeyCosmetics = shortkey;
        this.resultCosmetics = result;
    }

    public boolean getResultCosmetics() {
        return resultCosmetics;
    }

    public String getShortKeyCosmetics() {
        return shortKeyCosmetics;
    }

    public void setResultCosmetics(boolean resultCosmetics) {
        this.resultCosmetics = resultCosmetics;
    }

    public void setShortKeyCosmetics(String shortKeyCosmetics) {
        this.shortKeyCosmetics = shortKeyCosmetics;
    }
}
