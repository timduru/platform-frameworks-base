package com.android.systemui.statusbar.preferences;

import android.content.*;

public class Prefs {

    public static SharedPreferences.Editor edit(Context context) {
        SharedPreferences p = context.getSharedPreferences("status_bar", 0);
        return p.edit();
    }

    public static SharedPreferences read(Context context) {
        return context.getSharedPreferences("status_bar", 0);
    }
}
