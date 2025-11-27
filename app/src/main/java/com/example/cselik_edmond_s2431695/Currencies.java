package com.example.cselik_edmond_s2431695;

import android.util.Log;

public class Currencies {

    // Member Variables
    private float exchangeRate;
    private String currencyName;
    private String currencyCode;
    private String pubDate;
    private int flagId;


    // Returns the unique currency code (e.g., GBP, USD)
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrentCurrencyCode(String currentCurrencyCode) {
        this.currencyCode = currentCurrencyCode;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public float getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(String exchangeRate) {
        try {
            if (exchangeRate != null && !exchangeRate.isEmpty()) {
                // Remove any non-numeric characters except the decimal point
                String cleaned = exchangeRate.replaceAll("[^\\d.]", "");
                this.exchangeRate = Float.parseFloat(cleaned);
            }
        } catch (Exception e) {
            // Default to 0.0 if parsing fails and log the error
            this.exchangeRate = 0.0f;
            Log.e("Currency", "Error parsing rate: " + exchangeRate);
        }
    }

    // Returns the publication date of the exchange rate
    public String getPublishDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    // Returns the drawable resource ID for the associated flag
    public int getFlagId() {
        return flagId;
    }

    public void setFlagId(int flagId) {
        this.flagId = flagId;
    }
}