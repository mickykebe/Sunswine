package com.example.micky.sunswine;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Micky on 11/10/2015.
 */
public class Utility {
    public static String getPreferredLocation(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(
                context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }
}
