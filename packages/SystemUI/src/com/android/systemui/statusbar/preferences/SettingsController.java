package com.android.systemui.statusbar.preferences;

import android.view.*;
import android.content.*;
import android.app.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.content.BroadcastReceiver;
import android.util.Log;

import com.android.systemui.R;
import java.util.*;

public abstract class SettingsController implements View.OnClickListener, View.OnLongClickListener {

    protected View controlWidget;;
    protected Context mContext;
    protected int mPreferenceState;
    protected SettingsBroadcastReciever mBroadcastReceiver;
    protected SettingsObserver mObserver;
    protected boolean mPerformedLongClick = false;
    protected Drawable[] mIcons = new Drawable[2];
    protected Drawable[] mIndicators = new Drawable[2];
    public static final int STATE_OFF = 0;
    public static final int STATE_ON = 1;
    protected EosIndicatorController mIndicatorController;

    protected abstract int getPreferenceStatus();
    
    protected void getIcons(int icon_off, int icon_on) {
        mIcons[STATE_OFF] = mContext.getResources().getDrawable(icon_off);
        mIcons[STATE_ON] = mContext.getResources().getDrawable(icon_on);
    }

    protected void setPreferenceStatus(int status) {
        return;
    }

    protected String getSettingsIntent() {
        return null;
    }

    protected void handleBroadcast(Intent intent) {
    };

    protected void handleUriChange(Uri uri) {
    };

    protected IntentFilter getBroadcastIntents() {
        return null;
    }

    protected List<Uri> getObservedUris() {
        return null;
    }

    protected void handleSettingsMessage(Message message) {
    }
    
    /**
     * Creates a Settings Controller object.
     * 
     * @param context
     *            Context of the settings controller.
     * @param controlWidget
     *            View of the settings controller.
     */
    public SettingsController(Context context, View controlWidget) {
        mContext = context;
        this.controlWidget = controlWidget;

        this.controlWidget.setOnClickListener(this);
        this.controlWidget.setOnLongClickListener(this);

        mIndicatorController = new EosIndicatorController(mContext);
        mIndicators[STATE_OFF] = mIndicatorController.getIndicator(EosIndicatorController.STATE_UNPRESSED);
        mIndicators[STATE_ON] = mIndicatorController.getIndicator(EosIndicatorController.STATE_PRESSED);

        attach();
    }

    protected void updateControllerDrawable(int state) {
        if (state == 1) {
            controlWidget.findViewById(R.id.eos_settings_status).setBackgroundDrawable(mIndicators[STATE_ON]);
            controlWidget.findViewById(R.id.eos_settings_main).setBackgroundResource(
                    R.drawable.eos_settings_widget_main_pressed);
            controlWidget.findViewById(R.id.eos_settings_icon).setBackgroundDrawable(
                    mIcons[STATE_ON]);            
        } else {
            controlWidget.findViewById(R.id.eos_settings_status).setBackgroundDrawable(mIndicators[STATE_OFF]);
            controlWidget.findViewById(R.id.eos_settings_main).setBackgroundResource(
                    R.drawable.eos_settings_widget_main_unpressed);
            controlWidget.findViewById(R.id.eos_settings_icon).setBackgroundDrawable(
                    mIcons[STATE_OFF]);
        }
    }

    public void updateIndicator(int color) {
        mIndicatorController.setColor(color);
        mIndicators[STATE_OFF] = mIndicatorController.getIndicator(EosIndicatorController.STATE_UNPRESSED);
        mIndicators[STATE_ON] = mIndicatorController.getIndicator(EosIndicatorController.STATE_PRESSED);
        updateController();
    }

    public void updateController() {
        mPreferenceState = getPreferenceStatus();
        updateControllerDrawable(mPreferenceState);
        controlWidget.invalidate();
    }

    public void onClick(View view) {
        mPreferenceState = (mPreferenceState == 1) ? 0 : 1; // Invert the
                                                            // preference state.
        setPreferenceStatus(mPreferenceState);
        updateControllerDrawable(mPreferenceState);
        controlWidget.invalidate();

        try {
            if (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED) == 1) {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator.hasVibrator()) vibrator.vibrate(10);
            }
        } catch (android.provider.Settings.SettingNotFoundException e) {
        }
    }

    public boolean onLongClick(View v) {
        String intentURI = getSettingsIntent();
        if (intentURI == null)
            return false;
        Intent intent = new Intent(intentURI);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        StatusBarManager statusbar = (StatusBarManager) mContext.getSystemService("statusbar");
        statusbar.collapsePanels();

        return true;
    }

    private class SettingsBroadcastReciever extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            handleBroadcast(intent);
            updateController();
        }
    }

    private class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler h) {
            super(h);
        }

        public void onChangeUri(Uri uri, boolean selfChange) {
            handleUriChange(uri);
            updateController();
        }
    }

    private class SettingsHandler extends Handler {
        public void handleMessage(Message message) {
            handleSettingsMessage(message);
        }
    }

    public void attach() {
        IntentFilter intentFilter = getBroadcastIntents();
        if (intentFilter != null) {
            mBroadcastReceiver = new SettingsBroadcastReciever();
            mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        }

        List<Uri> observedUris = getObservedUris();
        if (observedUris != null && observedUris.size() > 0) {
            mObserver = new SettingsObserver(new SettingsHandler());

            for (Uri uri : getObservedUris()) {
                mContext.getContentResolver().registerContentObserver(uri, true, mObserver);
            }
        }
    }

    public void detach() {
        try {
            if (mBroadcastReceiver != null)
                mContext.unregisterReceiver(mBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.w("Eos Settings", "Trying to unregister invalid reciever");
        }

        try {
            if (mObserver != null)
                mContext.getContentResolver().unregisterContentObserver(mObserver);
        } catch (IllegalArgumentException e) {
            Log.w("Eos Settings", "Trying to unregister invalid observer");
        }
    }
}
