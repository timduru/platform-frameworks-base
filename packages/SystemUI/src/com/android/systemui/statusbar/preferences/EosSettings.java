
package com.android.systemui.statusbar.preferences;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.statusbar.EosObserverHandler;
import com.android.systemui.statusbar.EosObserverHandler.OnFeatureStateChangedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import org.teameos.jellybean.settings.EOSConstants;

public class EosSettings {

    private static String APP_TAG = "EOS Settings";
    private int MSG_EOS_UPDATE_SETTINGS;
    private int MSG_EOS_INDICATOR_COLOR;
    private int MSG_EOS_INDICATOR_HIDDEN;

    private Context mContext;
    private ContentResolver mContentResolver;
    private ViewGroup mParent;
    private List<SettingsController> visibleControllers;
    private List<View> mIndicatorViews;

    public EosSettings(ViewGroup parent, Context context) {
        if (parent == null) {
            Log.i(APP_TAG, "Parent is null, not continuing");
            return;
        }

        mContext = context;
        mParent = parent;

        MSG_EOS_UPDATE_SETTINGS = EosObserverHandler.getEosObserverHandler().registerUri(
                EOSConstants.SYSTEMUI_SETTINGS_ENABLED_CONTROLS);
        MSG_EOS_INDICATOR_COLOR = EosObserverHandler.getEosObserverHandler().registerUri(
                EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_COLOR);
        MSG_EOS_INDICATOR_HIDDEN = EosObserverHandler.getEosObserverHandler().registerUri(
                EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_HIDDEN);

        EosObserverHandler.getEosObserverHandler().setOnFeatureStateChangedListener(
                new OnFeatureStateChangedListener() {
                    @Override
                    public void onFeatureStateChanged(int msg) {
                        if (msg == MSG_EOS_UPDATE_SETTINGS) {
                            detach();
                            setupControllers();
                            updateIndicatorAppearance();
                            return;
                        } else if (msg == MSG_EOS_INDICATOR_COLOR
                                || msg == MSG_EOS_INDICATOR_HIDDEN) {
                            updateIndicatorAppearance();
                            return;
                        }
                    }
                });
        setupControllers();
        updateIndicatorAppearance();
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            mParent.setVisibility(View.VISIBLE);
            attach();
        } else {
            mParent.setVisibility(View.GONE);
            EosObserverHandler.getEosObserverHandler().unregisterUri(MSG_EOS_UPDATE_SETTINGS);
            EosObserverHandler.getEosObserverHandler().unregisterUri(MSG_EOS_INDICATOR_COLOR);
            EosObserverHandler.getEosObserverHandler().unregisterUri(MSG_EOS_INDICATOR_HIDDEN);
            detach();
        }
    }

    private void setupControllers() {
        mParent.removeAllViews();
        visibleControllers = new LinkedList<SettingsController>();
        mIndicatorViews = new ArrayList<View>();
        String[] controls = null;

        String quickControls = Settings.System.getString(mContentResolver,
                EOSConstants.SYSTEMUI_SETTINGS_ENABLED_CONTROLS);
        if (quickControls != null) {
            controls = quickControls.split("\\|");
        } else {
            controls = EOSConstants.SYSTEMUI_SETTINGS_DEFAULTS;
        }

        if (controls.length > 1) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            for (String i : controls) {
                View child = inflater.inflate(R.layout.eos_quicksetting, mParent, false);
                mParent.addView(child);
                mIndicatorViews.add(child.findViewById(R.id.eos_settings_status));
                Log.w(APP_TAG, i);

                if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_AIRPLANE)) {
                    visibleControllers.add(new AirplaneController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_AUTO_ROTATE)) {
                    visibleControllers.add(new AutoRotateController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_BLUETOOTH)) {
                    visibleControllers.add(new BluetoothController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_GPS)) {
                    visibleControllers.add(new GPSController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_NOTIFICATIONS)) {
                    visibleControllers.add(new NotificationsController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_SILENT)) {
                    visibleControllers.add(new AudioController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_TORCH)) {
                    visibleControllers.add(new TorchController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_WIFI)) {
                    visibleControllers.add(new WifiController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_MOBILEDATA)) {
                    visibleControllers.add(new MobileDataController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_WIFITETHER)) {
                    visibleControllers.add(new WifiTetherController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_USBTETHER)) {
                    visibleControllers.add(new UsbTetherController(mContext, child));
                } else if (i.equals(EOSConstants.SYSTEMUI_SETTINGS_LTE)) {
                    visibleControllers.add(new LteController(mContext, child));
                }
            }
        }
    }

    private void updateIndicatorAppearance() {
        boolean mIndicatorHide = Settings.System.getInt(mContentResolver,
                EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_HIDDEN,
                EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_HIDDEN_DEF) == 1 ? true : false;
        int visibility = mIndicatorHide ? View.GONE : View.VISIBLE;
        int mIndicatorColor = Settings.System.getInt(mContentResolver,
                EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_COLOR, -1);
        for (View v : mIndicatorViews) {
            v.setVisibility(visibility);
        }
        for (SettingsController i : visibleControllers) {
            i.updateIndicator(mIndicatorColor);
        }
    }

    public void attach() {
        for (SettingsController i : visibleControllers)
            i.attach();
    }

    public void detach() {
        for (SettingsController i : visibleControllers)
            i.detach();
    }
}
