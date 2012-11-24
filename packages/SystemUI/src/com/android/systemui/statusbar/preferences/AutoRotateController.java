package com.android.systemui.statusbar.preferences;

import android.os.*;
import android.view.*;
import android.content.*;
import android.provider.*;
import java.util.List;
import java.util.ArrayList;
import android.net.Uri;

import com.android.systemui.R;

public class AutoRotateController extends SettingsController {

    public static String AUTOROTATE_CHANGED = "android.eos.quick_controls.auto_rotate";
    private ContentResolver mContentResolver;

    public AutoRotateController(Context context, View button) {
        super(context, button);
        mContentResolver = context.getContentResolver();
        getIcons(R.drawable.toggle_rotate_off, R.drawable.toggle_rotate);
        updateController();
    }

    protected int getPreferenceStatus() {
        return Settings.System.getInt(mContentResolver, "accelerometer_rotation", 1);
    }

    protected void setPreferenceStatus(final int status) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {

                IBinder windowBinder = ServiceManager.getService("window");
                IWindowManager windowManager = IWindowManager.Stub.asInterface(windowBinder);
                try {
                    if (status == 1)
                        windowManager.thawRotation();
                    else
                        windowManager.freezeRotation(-1);

                    Intent intent = new Intent(AUTOROTATE_CHANGED);
                    mContext.sendBroadcast(intent);
                } catch (RemoteException e) {
                }

                return null;
            }
        }.execute();
    }

    protected String getSettingsIntent() {
        return "android.settings.DISPLAY_SETTINGS";
    }

    protected IntentFilter getBroadcastIntents() {
        IntentFilter intents = new IntentFilter();
        intents.addAction(AUTOROTATE_CHANGED);

        return intents;
    }

    protected List<Uri> getObservedUris() {
        ArrayList<Uri> uris = new ArrayList<Uri>();
        uris.add(Settings.Secure.getUriFor(Settings.System.ACCELEROMETER_ROTATION));

        return uris;
    }
}
