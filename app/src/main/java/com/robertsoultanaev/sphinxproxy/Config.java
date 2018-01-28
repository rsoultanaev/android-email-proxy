package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    public static void setKey(String key, String value, Context context) {
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getKey(String key, Context context) {
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        return sharedPreferences.getString(key, null);
    }
}
