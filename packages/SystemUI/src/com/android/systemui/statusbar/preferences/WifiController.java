package com.android.systemui.statusbar.preferences;

import android.content.*;
import android.view.*;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import com.android.systemui.R;

public class WifiController extends MultipleStateController {

    private static final int STATE_OFF = 0;
    private static final int STATE_TURNING_ON = 1;
    private static final int STATE_ON = 2;
    private static final int STATE_TURNING_OFF = 3;

    private static final int[] stateTransitions = {
            STATE_TURNING_ON, // Move from disconnected to connecting
            STATE_ON, // Move from turning on to on
            STATE_TURNING_OFF, // Move from on to turning off
            STATE_OFF // Move from turning off to off
    };

    private WifiManager mWifiManager;

    public WifiController(Context context, View button) {
        super(context, button);
        getIcons(R.drawable.toggle_wifi_off, R.drawable.toggle_wifi);
        mWifiManager = (WifiManager) context.getSystemService("wifi");
        updateController();
    }

    public int[] getStateTransitions() {
        return stateTransitions;
    }

    protected int getPreferenceStatus() {
        switch (mWifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_DISABLED:
                return STATE_OFF;
            case WifiManager.WIFI_STATE_ENABLING:
                return STATE_TURNING_ON;
            case WifiManager.WIFI_STATE_ENABLED:
                return STATE_ON;
            case WifiManager.WIFI_STATE_DISABLING:
                return STATE_TURNING_OFF;
            default:
                return STATE_OFF;
        }
    }

    protected void setPreferenceStatus(final int status) {
        if (mWifiManager == null) {
            mPreferenceState = STATE_OFF;
            updateControllerDrawable(STATE_OFF);
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                mWifiManager.setWifiEnabled(status == STATE_TURNING_ON);
                return null;
            }
        }.execute();
    }

    protected void handleBroadcast(Intent intent) {
        mPreferenceState = getPreferenceStatus();
    }

    protected String getSettingsIntent() {
        return "android.settings.WIFI_SETTINGS";
    }

    protected IntentFilter getBroadcastIntents() {
        IntentFilter intents = new IntentFilter();
        intents.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        return intents;
    }

    @Override
    protected int getStateType(int state) {
        switch (state) {
            case STATE_ON:
                return STATE_TYPE_ENABLED;
            case STATE_OFF:
                return STATE_TYPE_DISABLED;
            case STATE_TURNING_ON:
            case STATE_TURNING_OFF:
            default:
                return STATE_TYPE_TRANSITION;
        }
    }
}
