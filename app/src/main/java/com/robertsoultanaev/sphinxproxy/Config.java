package com.robertsoultanaev.sphinxproxy;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    public static void setKey(int keyId, String value, Context context) {
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        String key = context.getString(keyId);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getKey(int keyId, Context context) {
        String sharedPreferencesFile = context.getString(R.string.key_preference_file);
        String key = context.getString(keyId);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesFile, Context.MODE_PRIVATE);

        return sharedPreferences.getString(key, null);
    }
}
