
package com.android.systemui.statusbar.preferences;

import android.content.*;
import android.hardware.usb.UsbManager;
import android.view.*;
import android.widget.*;
import android.net.ConnectivityManager;

import com.android.systemui.R;

public class UsbTetherController extends SettingsController {

    private boolean mUsbConnected;
    private boolean mMassStorageActive;
    private ConnectivityManager cm;
    private String[] mUsbRegexs;

    private static final int STATE_OFF = 0;
    private static final int STATE_ON = 1;

    public UsbTetherController(Context context, View button) {
        super(context, button);
        mContext = context;
        cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUsbRegexs = cm.getTetherableUsbRegexs();
        getIcons(R.drawable.toggle_tether_off, R.drawable.toggle_tether);
        updateController();
    }

    protected int getPreferenceStatus() {
        String[] tethered = cm.getTetheredIfaces();
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex))
                    usbTethered = true;
            }
        }
        return ((usbTethered) ? STATE_ON : STATE_OFF);
    }

    protected void setPreferenceStatus(int status) {
        if (!mUsbConnected || mMassStorageActive) {
            mPreferenceState = STATE_OFF;
            updateControllerDrawable(STATE_OFF);
            return;
        }
        cm.setUsbTethering((status == STATE_ON) ? true : false);
    }

    protected void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
            mMassStorageActive = true;
            updateController();
        } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
            mMassStorageActive = false;
            updateController();
        } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
            mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
            updateController();
        }
    }

    protected String getSettingsIntent() {
        return "android.settings.TETHER_SETTINGS";
    }

    protected IntentFilter getBroadcastIntents() {
        IntentFilter intents = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        intents.addAction(UsbManager.ACTION_USB_STATE);
        intents.addAction(Intent.ACTION_MEDIA_SHARED);
        intents.addAction(Intent.ACTION_MEDIA_UNSHARED);
        return intents;
    }
}
