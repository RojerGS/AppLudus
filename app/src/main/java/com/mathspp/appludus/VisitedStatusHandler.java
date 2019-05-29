package com.mathspp.appludus;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

public class VisitedStatusHandler {
    private final String LogTAG = VisitedStatusHandler.class.getSimpleName();
    private SharedPreferences mSharedPreferences;

    /*  Establish an interface that allows to check the "visited"
        status of each place and allows one to store the changes in these values
    */

    public VisitedStatusHandler(SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
    }

    public boolean getVisitedStatus(String locName) {
        return mSharedPreferences.getBoolean(locName, false);
    }

    public void setVisitedStatus(String locName, boolean status) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(locName, status);
        editor.apply();
    }
}
