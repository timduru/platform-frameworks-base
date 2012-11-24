package com.android.systemui.statusbar.preferences;

import android.content.*;
import android.view.*;
import android.bluetooth.BluetoothAdapter;
import android.os.AsyncTask;

import com.android.systemui.R;

public class BluetoothController extends MultipleStateController {

    private static final int STATE_OFF = 0;
    private static final int STATE_TURNING_ON = 1;
    private static final int STATE_ON = 2;
    private static final int STATE_TURNING_OFF = 3;

    public static int stateTransitions[] = { STATE_TURNING_ON, STATE_ON, STATE_TURNING_OFF,
            STATE_OFF };

    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothController(Context context, View button) {
        super(context, button);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        getIcons(R.drawable.toggle_bluetooth_off, R.drawable.toggle_bluetooth);
        updateController();
    }

    public int[] getStateTransitions() {
        return stateTransitions;
    }

    protected int getPreferenceStatus() {
        switch (mBluetoothAdapter.getState()) {
            case (BluetoothAdapter.STATE_ON):
                return STATE_ON;
            case (BluetoothAdapter.STATE_TURNING_OFF):
                return STATE_TURNING_OFF;
            case (BluetoothAdapter.STATE_OFF):
                return STATE_OFF;
            case (BluetoothAdapter.STATE_TURNING_ON):
                return STATE_TURNING_ON;
            default:
                return STATE_OFF;
        }
    }

    protected void setPreferenceStatus(final int status) {
        if (status == STATE_ON || status == STATE_OFF)
            return;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                if (status == STATE_TURNING_ON) {
                    mBluetoothAdapter.enable();
                } else if (status == STATE_TURNING_OFF) {
                    mBluetoothAdapter.disable();
                }
                return null;
            }
        }.execute();
    }

    protected void handleBroadcast(Intent intent) {
        mPreferenceState = getPreferenceStatus();
    }

    protected String getSettingsIntent() {
        return "android.settings.BLUETOOTH_SETTINGS";
    }

    protected IntentFilter getBroadcastIntents() {
        IntentFilter intents = new IntentFilter();
        intents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        return intents;
    }

    @Override
    protected int getStateType(int state) {
        switch (state){
        case STATE_ON:
            return STATE_TYPE_ENABLED;
        case STATE_OFF:
            return STATE_TYPE_DISABLED;
        default:
            return STATE_TYPE_TRANSITION;
        }
    }
}
