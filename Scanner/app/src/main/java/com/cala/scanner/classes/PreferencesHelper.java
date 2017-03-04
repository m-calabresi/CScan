package com.cala.scanner.classes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferencesHelper(Context context) {
        this.sharedPreferences = ((Activity) context).getPreferences(Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();
    }

    public boolean getPreferences(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public void savePreferences(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }
}
