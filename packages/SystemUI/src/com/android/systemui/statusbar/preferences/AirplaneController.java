package com.android.systemui.statusbar.preferences;

import android.content.*;
import android.view.*;
import android.provider.*;
import android.net.Uri;
import java.util.List;
import java.util.ArrayList;

import com.android.systemui.R;

public class AirplaneController extends SettingsController {

    private ContentResolver mContentResolver;

    public AirplaneController(Context context, View button) {
        super(context, button);
        mContentResolver = context.getContentResolver();
        getIcons(R.drawable.toggle_airplane_off, R.drawable.toggle_airplane);
        updateController();
    }

    protected int getPreferenceStatus() {
        return Settings.System.getInt(mContentResolver, Settings.System.AIRPLANE_MODE_ON, 0);
    }

    protected void setPreferenceStatus(int status) {
        Settings.System.putInt(mContentResolver, Settings.System.AIRPLANE_MODE_ON, status);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", status == 1);
        mContext.sendBroadcast(intent);
    }

    protected void handleUriChange(Uri uri) {
        mPreferenceState = getPreferenceStatus();
        updateController();
    }

    protected String getSettingsIntent() {
        return "android.settings.AIRPLANE_MODE_SETTINGS";
    }

    protected List<Uri> getObservedUris() {
        ArrayList<Uri> uris = new ArrayList<Uri>();
        uris.add(Settings.Secure.getUriFor(Settings.System.AIRPLANE_MODE_ON));

        return uris;
    }

    protected void handleBroadcast(Intent intent) {
        mPreferenceState = getPreferenceStatus();
        updateController();
    }

    protected IntentFilter getBroadcastIntents() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        return filter;
    }
}
