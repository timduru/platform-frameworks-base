package com.android.systemui.statusbar.preferences;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import org.teameos.jellybean.settings.EOSConstants;

public class EosSettings {

    private static String            APP_TAG = "EOS Settings";
    private static final int         MSG_EOS_UPDATE_SETTINGS = 2012;
    private static final int         MSG_EOS_UPDATE_INDICATOR = 2013;

    private Context                  mContext;
    private ContentResolver          mContentResolver;
    private ViewGroup                mParent;
    private List<SettingsController> visibleControllers;
    private List<View>               mIndicatorViews;
    private SettingsObserver         mSettingsObserver;
    private SettingsObserver         mIndicatorObserver;
    private H                        mHandler;

    public EosSettings(ViewGroup parent, Context context) {
        if (parent == null) {
            Log.i(APP_TAG, "Parent is null, not continuing");
            return;
        }

        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mParent = parent;
        mHandler = new H();
        mSettingsObserver = new SettingsObserver(mHandler, MSG_EOS_UPDATE_SETTINGS);
        mIndicatorObserver = new SettingsObserver(mHandler, MSG_EOS_UPDATE_INDICATOR);

        mContentResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_SETTINGS_ENABLED_CONTROLS),
                false, mSettingsObserver);
        mContentResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_HIDDEN),
                false, mIndicatorObserver);
        mContentResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_COLOR),
                false, mIndicatorObserver);

        setupControllers();
        updateIndicatorAppearance();
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

    private class H extends Handler {
        public void handleMessage(Message m) {
            super.handleMessage(m);
            switch (m.what) {
                case MSG_EOS_UPDATE_SETTINGS:
                    detach();
                    setupControllers();
                    updateIndicatorAppearance();
                    break;
                case MSG_EOS_UPDATE_INDICATOR:
                    updateIndicatorAppearance();
                    break;
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        Handler handler;
        int message;

        public SettingsObserver(Handler handler, int message) {
            super(handler);
            this.handler = handler;
            this.message = message;
        }

        public void onChange(boolean selfChange) {
            handler.sendEmptyMessage(message);
        }
    }
}
