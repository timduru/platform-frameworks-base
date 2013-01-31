
package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManager;

import com.android.systemui.statusbar.phone.NavigationBarView;

import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;

public class SystembarStateHandler {
    private static final String TAG = "EOS_SystembarStateHandler";

    // special key for PhoneStatusBar to initialize navbar for us
    public static final String EOS_ADD_NAVBAR_KEY = "eos_add_navbar_key";

    // we dont want to send responses and request to ourself
    private static final String CHECK_CALLER = "came_from_me";

    public interface OnBarStateChangedListener {
        public void onBarStateChanged(int state);
    }

    // holds registered callbacks
    private ArrayList<OnBarStateChangedListener> mListeners = new ArrayList<OnBarStateChangedListener>();

    // there are two hidden states:
    // STATE 1: the bars are hidden on boot. there may be
    // extra initialization required such as listeners, etc
    // so we set this flag true to know to handle adding the
    // the bars back correctly first time after boot
    // the flag then returns to false
    // STATE 2: bars were available on boot but the user wants
    // them gone now. we just remove from windowmanager and notify
    // interested parties

    private boolean mNavBarPreviouslyHiddenByConf = false;

    // if true handle systemui initialization
    private boolean mNavBarHidesOnBoot = false;

    // by default, hiding bars means only hide the
    // navigation bar. but if the user want's, the
    // statusbar will hide when navigation bar hides
    // there is no state in which only statusbar can
    // be hidden by itself
    private boolean mStatBarHidesToo = false;

    // current visibility state we get from SettingsProvider
    // this class is the only place that actually reads and
    // writes to this. interested parties can request current
    // state through callback or broadcast or request state
    // change through broadcast
    private int currentVisibilityState;

    // value from window manager
    // I should become apparent why we can't change the
    // boolean value in the WindowManager. We would have
    // no real basis in which to determine what the original
    // state of the device is. Also, there's nothing in
    // WindowManager that we cant do from here
    private boolean mHasNavigationBar = true;

    // check for capacitive button devices so we
    // don't have to look for a navigation bar
    private boolean mHasStatusBarOnly = false;

    // working in SystemUI context
    private Context mContext;

    // hold our view instances and layout params
    // need to update if services restarts
    private NavigationBarView mNavigationBarView;
    private WindowManager.LayoutParams mNavigationBarParams;
    private View mStatusBarView;
    private WindowManager.LayoutParams mStatusBarParams;
    private WindowManager mWindowManager;
    
    public SystembarStateHandler (Context context, OnBarStateChangedListener listener) {
        mContext = context;
        mListeners.add(listener);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        init();
    }

    // we'll call this from BaseStatusBar so we can get a clean
    // initial state before the PhoneStatusBar is even created
    private void init() {
        loadStatesFromProvider();

        try {
            mHasNavigationBar = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE)).hasNavigationBar();
        } catch (Exception e) {
            log("failed to access WindowManager, not a good sign!" + e.toString());
        }
        mHasStatusBarOnly = !mHasNavigationBar;

        // we must make sure we are properly initialized when
        // systemui service starts

        // what if the user reboot with bars hidden but HideOnBoot
        // is not selected? We must reset this value before everything
        // startes to inflate. This will keep us in the proper state on boot
        if (!mNavBarHidesOnBoot && currentVisibilityState == View.GONE) {
            Settings.System.putInt(mContext.getContentResolver(), EOSConstants.SYSTEMUI_HIDE_BARS,
                    EOSConstants.SYSTEMUI_HIDE_BARS_DEF);
        }

        // opposite case. hide on boot is checked, but the bars were not
        // hidden on reboot. so we properly initialize our state before
        // we even begin handling actual visibility. in this case, we
        // don't manually hide bar, we just force PhoneStatusBar to not
        // add them to WindowManager
        // and we adjust for a special case in which Eos manually kills
        // systemui to implement features
        // let's override the override if eos restarted systemui
        // check the flag set when we received the kill intent
        boolean eosKilledMe = Settings.System.getInt(mContext.getContentResolver(),
                EosUiController.EOS_KILLED_ME, 0) == 1;

        if (mNavBarHidesOnBoot && currentVisibilityState == View.VISIBLE && !eosKilledMe) {
            Settings.System.putInt(mContext.getContentResolver(), EOSConstants.SYSTEMUI_HIDE_BARS,
                    1);
        }

        if (!eosKilledMe) {
            // coming off a reboot or hopefully not a systemui crash
            // caused by something else
            mNavBarPreviouslyHiddenByConf = mNavBarHidesOnBoot;
        } else {
            // eos killed us, reset the flag
            mNavBarPreviouslyHiddenByConf = false;
            Settings.System.putInt(mContext.getContentResolver(), EosUiController.EOS_KILLED_ME, 0);
        }

        // refresh values
        loadStatesFromProvider();

        // now our states are clean we can notify listeners
        // of accurate visiblity state
        notifyVisibilityChanged();

        IntentFilter filter = new IntentFilter();
        filter.addAction(EOSConstants.INTENT_SYSTEMUI_BAR_STATE);
        filter.addAction(EOSConstants.INTENT_SYSTEMUI_BAR_STATE_REQUEST_TOGGLE);
        mContext.registerReceiver(mReceiver, filter);
    }

    public void setNavigationBar(NavigationBarView view, WindowManager.LayoutParams lp) {
        mNavigationBarView = view;
        mNavigationBarParams = lp;
    }

    public void setStatusBar(View view, WindowManager.LayoutParams lp) {
        mStatusBarView = view;
        mStatusBarParams = lp;
    }

    public boolean isBarPreviouslyHiddenByConf() {
        return mNavBarPreviouslyHiddenByConf;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (EOSConstants.INTENT_SYSTEMUI_BAR_STATE.equals(action)) {
                // break any feedback loop
                String cameFromMe = intent.getStringExtra(CHECK_CALLER);
                if (cameFromMe != null && cameFromMe.equals(TAG))
                    return;
                Intent response = new Intent()
                        .setAction(EOSConstants.INTENT_SYSTEMUI_BAR_STATE)
                        .putExtra(EOSConstants.INTENT_SYSTEMUI_BAR_STATE_KEY,
                                currentVisibilityState)
                        .putExtra(CHECK_CALLER, TAG);
                mContext.sendBroadcast(response);
            } else if (EOSConstants.INTENT_SYSTEMUI_BAR_STATE_REQUEST_TOGGLE.equals(action)) {
                toggleVisibility();
            } else {
                return;
            }
        }
    };

    private void sendExceptionBroadcast() {
        // send this so reqesting app can update ui accordingly
        Intent exception = new Intent()
                .setAction(EOSConstants.INTENT_SYSTEMUI_BAR_STATE_CHANGED_EXCEPTION)
                .putExtra(EOSConstants.INTENT_SYSTEMUI_BAR_STATE_KEY, currentVisibilityState);
        mContext.sendBroadcast(exception);
    }

    public void addStatusbarWindow() {
        boolean isNavbarHidden = currentVisibilityState == View.GONE;
        if (!isNavbarHidden
                || (isNavbarHidden && !mStatBarHidesToo)) {
            mWindowManager.addView(mStatusBarView, mStatusBarParams);
        }
    }

    private boolean toggleStatusBar(boolean shouldHide) {
        if (shouldHide) {
            try {
                mWindowManager.removeView(mStatusBarView);
            } catch (Exception e) {
                sendExceptionBroadcast();
                resetStatesToProvider();
                log("Could not remove statusbar from WindowManager " + e.toString());
                return false;
            }
        } else {
            // let's try to put the statusbar back
            try {
                mWindowManager.addView(mStatusBarView, mStatusBarParams);
            } catch (Exception e) {
                sendExceptionBroadcast();
                resetStatesToProvider();
                log("Could not add statusbar to WindowManager " + e.toString());
                return false;
            }
        }
        return true;
    }

    private boolean toggleNavigationBar(boolean shouldHide) {
        if (shouldHide) {
            try {
                mWindowManager.removeView(mNavigationBarView);
            } catch (Exception e) {
                sendExceptionBroadcast();
                resetStatesToProvider();
                log("Could not remove navigationbar from WindowManager " + e.toString());
                return false;
            }
        } else {
            // let's try to put the navigationbar back
            try {
                mWindowManager.addView(mNavigationBarView, mNavigationBarParams);
            } catch (Exception e) {
                sendExceptionBroadcast();
                resetStatesToProvider();
                log("Could not add statusbar to WindowManager " + e.toString());
                return false;
            }
        }
        return true;
    }

    private void toggleVisibility() {
        if ((mHasNavigationBar && mNavigationBarView == null)
                || mStatusBarView == null) {
            resetStatesToProvider();
            return;
        }

        loadStatesFromProvider();

        boolean shouldHide = currentVisibilityState == View.VISIBLE;
        boolean statusBarSuccess = false;
        boolean navBarSuccess = false;

        if (mStatBarHidesToo || mHasStatusBarOnly) {
            statusBarSuccess = toggleStatusBar(shouldHide);
        } else {
            statusBarSuccess = true;
        }

        // our first request since boot to show bars again
        if (mNavBarPreviouslyHiddenByConf) {
            // state flag disable until reboot or
            // systemui restart
            mNavBarPreviouslyHiddenByConf = false;
            Intent intent = new Intent()
                    .setAction(Intent.ACTION_CONFIGURATION_CHANGED)
                    .putExtra(EOS_ADD_NAVBAR_KEY, true);
            mContext.sendBroadcast(intent);
            navBarSuccess = true;
        } else {
            if (mHasNavigationBar) {
                navBarSuccess = toggleNavigationBar(shouldHide);
            } else {
                navBarSuccess = true;
            }
        }

        if (statusBarSuccess && navBarSuccess) {
            // finally, we can give a solid state update to settingsProvider
            Settings.System.putInt(mContext.getContentResolver(),
                    EOSConstants.SYSTEMUI_HIDE_BARS,
                    shouldHide ? 1 : EOSConstants.SYSTEMUI_HIDE_BARS_DEF);
            loadStatesFromProvider();
            notifyVisibilityChanged();
            Intent response = new Intent()
                    .setAction(EOSConstants.INTENT_SYSTEMUI_BAR_STATE)
                    .putExtra(EOSConstants.INTENT_SYSTEMUI_BAR_STATE_KEY,
                            currentVisibilityState)
                    .putExtra(CHECK_CALLER, TAG);
            mContext.sendBroadcast(response);
        } else {
            log("I think the universe failed if we get here");
        }
    }

    private void loadStatesFromProvider() {
        updateHideBarsOnBootEnabled();
        updateStatBarHidesTooEnabled();
        updateNavBarHidden();
    }

    // this is only called if we throw an exception
    // or something else is terribly wrong
    // we try to write default values to provider
    // so we don't get caught in some crazy fail
    // loop. Also, settings can correctly update
    private void resetStatesToProvider() {
        resetHideBarsOnBootEnabled();
        resetStatBarHidesTooEnabled();
        resetNavBarHidden();
    }

    public void setOnBarStateChangeListener(OnBarStateChangedListener l) {
        l.onBarStateChanged(currentVisibilityState);
        mListeners.add(l);
        log(l.toString() + " added to arraylist");
    }

    private void resetHideBarsOnBootEnabled() {
        Settings.System.putInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_HIDE_NAVBAR_ON_BOOT,
                EOSConstants.SYSTEMUI_HIDE_NAVBAR_ON_BOOT_DEF);
    }

    private void resetStatBarHidesTooEnabled() {
        Settings.System.putInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_HIDE_STATBAR_TOO,
                EOSConstants.SYSTEMUI_HIDE_STATBAR_TOO_DEF);
    }

    private void resetNavBarHidden() {
        Settings.System.putInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_HIDE_BARS,
                EOSConstants.SYSTEMUI_HIDE_BARS_DEF);
    }

    private void updateHideBarsOnBootEnabled() {
        mNavBarHidesOnBoot = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_HIDE_NAVBAR_ON_BOOT,
                EOSConstants.SYSTEMUI_HIDE_NAVBAR_ON_BOOT_DEF) == 1;
    }

    private void updateStatBarHidesTooEnabled() {
        mStatBarHidesToo = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_HIDE_STATBAR_TOO,
                EOSConstants.SYSTEMUI_HIDE_STATBAR_TOO_DEF) == 1;
    }

    private void updateNavBarHidden() {
        currentVisibilityState = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_HIDE_BARS,
                EOSConstants.SYSTEMUI_HIDE_BARS_DEF) == 1 ? View.GONE : View.VISIBLE;
    }

    private void notifyVisibilityChanged() {
        for (OnBarStateChangedListener l : mListeners) {
            l.onBarStateChanged(currentVisibilityState);
            log(l.toString() + " notified");
        }
    }

    private static void log(String s) {
        Log.i(TAG, s);
    }
}
