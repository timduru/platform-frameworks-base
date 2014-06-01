package com.android.systemui.quicksettings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.net.NetworkInfo.DetailedState;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;

import org.meerkats.katkiss.KatUtils;
import com.android.internal.ethernet.EthernetInfo;
import com.android.internal.ethernet.EthernetManager;


public class EthernetTile extends QuickSettingsTile {

    private static final String TAG = "EthernetTile";
    Context mContext = null;
    boolean mConnected = false;

    public EthernetTile(Context context, final QuickSettingsController qsc) {
        super(context, qsc);

        mContext = context;
        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                qsc.mBar.collapseAllPanels(true);
                KatUtils.ethernetToggle(mContext);
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //startSettingsActivity(Settings.ACTION_DISPLAY_SETTINGS);
                return true;
            }

        };
        EthernetManager mEthernetManager = (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
        mConnected = mEthernetManager != null && mEthernetManager.isEnabled();
        updateTile();

        IntentFilter mIntentFilter = new IntentFilter(EthernetManager.INTERFACE_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, mIntentFilter);

        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), this);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(EthernetManager.INTERFACE_STATE_CHANGED_ACTION)) {
                EthernetInfo ei = (EthernetInfo) intent.getParcelableExtra(EthernetManager.EXTRA_ETHERNET_INFO);
                NetworkInfo ni = ei.getNetworkInfo();
                mConnected = ni.getState() == NetworkInfo.State.CONNECTED;
                updateResources();
            }
        }
    };

    @Override
    void onPostCreate() {
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        int mode;
        mDrawable = mConnected ? R.drawable.stat_sys_ethernet_connected : R.drawable.stat_sys_ethernet_disconnected;
        mLabel = mContext.getString(R.string.quick_settings_ethernet_label);
    }
}
