
package com.android.systemui.statusbar.preferences;

import android.content.*;
import android.view.*;
import android.widget.*;
import android.os.AsyncTask;
import android.provider.*;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.android.systemui.R;

public class WifiTetherController extends MultipleStateController {

    private static final int STATE_OFF = 0;
    private static final int STATE_TURNING_ON = 1;
    private static final int STATE_ON = 2;
    private static final int STATE_TURNING_OFF = 3;

    public static int stateTransitions[] = {
            STATE_TURNING_ON, STATE_ON, STATE_TURNING_OFF,
            STATE_OFF
    };

    private WifiManager mWifiManager;
    ConnectivityManager mCm;

    public WifiTetherController(Context context, View button) {
        super(context, button);
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        getIcons(R.drawable.toggle_wifi_ap_off, R.drawable.toggle_wifi_ap);
        updateController();
    }

    public int[] getStateTransitions() {
        return stateTransitions;
    }

    protected int getPreferenceStatus() {
        switch (mWifiManager.getWifiApState()) {
            case (WifiManager.WIFI_AP_STATE_ENABLED):
                return STATE_ON;
            case (WifiManager.WIFI_AP_STATE_DISABLING):
                return STATE_TURNING_OFF;
            case (WifiManager.WIFI_AP_STATE_DISABLED):
                return STATE_OFF;
            case (WifiManager.WIFI_AP_STATE_ENABLING):
                return STATE_TURNING_ON;
            default:
                return STATE_OFF;
        }
    }

    protected void setPreferenceStatus(final int status) {
        if (status == STATE_ON || status == STATE_OFF)
            return;

        if (status == STATE_TURNING_ON
                && Settings.System.getInt(mContext.getContentResolver(),
                        "eos_quick_controls_airplane_wifi", 0) == 0
                && Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
            mPreferenceState = STATE_OFF;
            updateControllerDrawable(STATE_OFF);
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                if (status == STATE_TURNING_ON) {
                    setSoftapEnabled(true);
                    WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
                    mWifiManager.setWifiApEnabled(wifiConfig, true);
                } else if (status == STATE_TURNING_OFF) {
                    mWifiManager.setWifiApEnabled(null, false);
                    setSoftapEnabled(false);
                }
                return null;
            }
        }.execute();
    }

    protected void handleBroadcast(Intent intent) {
        mPreferenceState = getPreferenceStatus();
    }

    protected String getSettingsIntent() {
        return "android.settings.TETHER_SETTINGS";
    }

    protected IntentFilter getBroadcastIntents() {
        IntentFilter intents = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        intents.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        intents.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        return intents;
    }

    @Override
    protected int getStateType(int state) {
        switch (state) {
            case STATE_ON:
                return STATE_TYPE_ENABLED;
            case STATE_OFF:
                return STATE_TYPE_DISABLED;
            default:
                return STATE_TYPE_TRANSITION;
        }
    }

    public void setSoftapEnabled(boolean enable) {
        ContentResolver mResolver = mContext.getContentResolver();
        /**
         * Disable Wifi if enabling tethering
         */
        int wifiState = mWifiManager.getWifiState();

        if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
            mWifiManager.setWifiEnabled(false);
            Settings.Secure.putInt(mResolver, Settings.Global.WIFI_SAVED_STATE, 1);
        }
        /**
         * If needed, restore Wifi on tether disable
         */
        if (!enable) {
            int wifiSavedState = 0;
            try {
                wifiSavedState = Settings.Secure
                        .getInt(mResolver, Settings.Global.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException e) {
                ;
            }
            if (wifiSavedState == 1) {
                mWifiManager.setWifiEnabled(true);
                Settings.Secure.putInt(mResolver, Settings.Global.WIFI_SAVED_STATE, 0);
            }
        }
    }
}
