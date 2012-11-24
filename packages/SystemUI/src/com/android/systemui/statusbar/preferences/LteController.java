
package com.android.systemui.statusbar.preferences;

import org.teameos.jellybean.settings.EOSConstants;

import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.View;

import com.android.internal.telephony.Phone;
import com.android.systemui.R;

public class LteController extends SettingsController {
    private static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    private static final int CDMA_ONLY = Phone.NT_MODE_CDMA;
    private static final int LTE_CDMA = Phone.NT_MODE_GLOBAL;
    private static final String EOS_TELEPHONY_INTENT = EOSConstants.INTENT_TELEPHONY_LTE_TOGGLE;
    private static final String EOS_TELEPHONY_MODE_KEY = EOSConstants.INTENT_TELEPHONY_LTE_TOGGLE_KEY;
    private Context mContext;
    private ContentResolver mResolver;

    public LteController(Context context, View button) {
        super(context, button);
        mContext = context;
        mResolver = mContext.getContentResolver();
        getIcons(R.drawable.toggle_lte_off, R.drawable.toggle_lte);
        updateController();
    }

    @Override
    protected int getPreferenceStatus() {
        // TODO Auto-generated method stub
        int settingsNetworkMode = Settings.Secure.getInt(mResolver,
                Settings.Global.PREFERRED_NETWORK_MODE,
                preferredNetworkMode);
        switch (settingsNetworkMode) {
            case CDMA_ONLY:
                return STATE_OFF;
            case LTE_CDMA:
                return STATE_ON;
            default:
                return STATE_ON;
        }
    }

    protected void setPreferenceStatus(int status) {
        int newMode = 0;
        switch (status) {
            case STATE_ON:
                newMode = LTE_CDMA;
                break;
            case STATE_OFF:
                newMode = CDMA_ONLY;
                break;
        }
        Intent intent = new Intent(EOS_TELEPHONY_INTENT);
        intent.putExtra(EOS_TELEPHONY_MODE_KEY, newMode);
        mContext.sendBroadcast(intent);
    }

    @Override
    public boolean onLongClick(View V) {
        Intent intent = new Intent("com.android.phone.MobileNetworkSettings");
        intent.addCategory(Intent.ACTION_MAIN);
        intent.setClassName("com.android.phone", "com.android.phone.MobileNetworkSettings");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        StatusBarManager statusbar = (StatusBarManager) mContext.getSystemService("statusbar");
        statusbar.collapsePanels();
        return true;
    }
}
