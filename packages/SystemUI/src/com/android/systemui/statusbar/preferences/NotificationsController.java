package com.android.systemui.statusbar.preferences;

import android.content.*;
import android.view.*;
import android.widget.*;
import android.provider.*;
import android.net.Uri;
import java.util.List;
import java.util.ArrayList;

import com.android.systemui.R;
import com.android.systemui.statusbar.preferences.*;

public class NotificationsController extends SettingsController implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public NotificationsController(Context context, View button) {
        super(context, button);
        Prefs.read(mContext).registerOnSharedPreferenceChangeListener(this);
        getIcons(R.drawable.toggle_notifications_off, R.drawable.toggle_notifications);
        updateController();
    }

    protected int getPreferenceStatus() {
        SharedPreferences p = Prefs.read(mContext);
        return (p.getBoolean("do_not_disturb", false) ? 0 : 1);
    }

    protected void setPreferenceStatus(int status) {
        SharedPreferences.Editor p = Prefs.edit(mContext);
        p.putBoolean("do_not_disturb", (status == 0));
        p.apply();
    }

    protected String getSettingsIntent() {
        return null;
    }

    public void onSharedPreferenceChanged(SharedPreferences shared, String preference) {
        updateController();
    }
}
