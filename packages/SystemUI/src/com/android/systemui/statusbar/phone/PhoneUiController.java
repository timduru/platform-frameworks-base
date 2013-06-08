
package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.app.ActivityManager;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.BarUiController;
import com.android.systemui.statusbar.BottomBarGlassController;
import com.android.systemui.statusbar.NX;
import com.android.systemui.statusbar.StatusBarGlassController;

import org.teameos.jellybean.settings.EOSConstants;

public class PhoneUiController extends BarUiController {

    static final String TAG = "EosUiController";

    static final int STOCK_NAV_BAR = com.android.systemui.R.layout.navigation_bar;
    static final int EOS_NAV_BAR = com.android.systemui.R.layout.eos_navigation_bar;

    private View mStatusBarView;
    private PhoneStatusBar mService;
    private NavigationBarView mNavigationBarView;
    private StatusBarWindowView mStatusBarWindow;
    private StatusBarGlassController mStatusBarColorController;
    private BottomBarGlassController mBottomBarColorController;

    private int mCurrentNavLayout;

    private NX mNx;

    // private EosSettings mEosLegacyToggles;

    public PhoneUiController(Context context) {
        super(context);
    }

    public WindowManager.LayoutParams getNavigationBarLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
                0
                        | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        // this will allow the navbar to run in an overlay on devices that
        // support this
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }

        lp.setTitle("NavigationBar");
        lp.windowAnimations = 0;
        return lp;
    }

    public NavigationBarView setNavigationBarView() {
        mCurrentNavLayout = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_UI_MODE,
                EOSConstants.SYSTEMUI_UI_MODE_NAVBAR)
                == EOSConstants.SYSTEMUI_UI_MODE_NAVBAR
                        ? STOCK_NAV_BAR
                        : EOS_NAV_BAR;
        mNavigationBarView = (NavigationBarView) View.inflate(mContext, mCurrentNavLayout, null);

        /*
         * at this point, all views are now set except for navbar, which we now
         * have
         */

        // startNX the instance handles all states
        if (mCurrentNavLayout == STOCK_NAV_BAR) {
            mNx = new NX(mContext, mNavigationBarView, mService, PhoneUiController.this);
            mObserver.registerClass(mNx);
            mNavigationBarView.setNx(mNx);
            mService.setNx(mNx);
        }

        // send view to glass
        mBottomBarColorController = new BottomBarGlassController(mContext, mObserver,
                mNavigationBarView);
        mActivityWatcher.setActivityListener(mBottomBarColorController);

        // give it back to SystemUI
        return mNavigationBarView;
    }

    public void setBar(PhoneStatusBar service) {
        mService = service;
    }

    public void setBarWindow(StatusBarWindowView window) {
        mStatusBarWindow = window;
    }

    /*
     * private void handleLegacyTogglesChange() { boolean isTogglesEnabled =
     * Settings.System.getInt( mContext.getContentResolver(),
     * EOSConstants.SYSTEMUI_SETTINGS_ENABLED,
     * EOSConstants.SYSTEMUI_SETTINGS_ENABLED_DEF) == 1; final View toggleView =
     * mStatusBarWindow == null ? mTabletSettingsView : mStatusBarWindow; if
     * (isTogglesEnabled) { mEosLegacyToggles = new EosSettings( (ViewGroup)
     * toggleView.findViewById(R.id.eos_toggles), mContext, mObserver);
     * mEosLegacyToggles.setEnabled(isTogglesEnabled); } else { if
     * (mEosLegacyToggles != null) {
     * mEosLegacyToggles.setEnabled(isTogglesEnabled); mEosLegacyToggles = null;
     * } } }
     */

    @Override
    protected ImageView getBatteryIconView() {
        return (ImageView) mStatusBarView
                .findViewById(R.id.signal_battery_cluster)
                .findViewById(R.id.battery);
    }

    @Override
    protected TextView getBatteryTextView() {
        return (TextView) mStatusBarView
                .findViewById(R.id.signal_battery_cluster)
                .findViewById(R.id.battery_text);
    }

    @Override
    protected TextView getClockCenterView() {
        return (TextView) mStatusBarView.findViewById(R.id.clock_center);
    }

    @Override
    protected TextView getClockClusterView() {
        return (TextView) mStatusBarView.findViewById(R.id.system_icon_area).findViewById(
                R.id.clock);
    }

    @Override
    protected void registerBarView(View v) {
        mStatusBarView = v;
        mStatusBarColorController = new StatusBarGlassController(mContext, mObserver,
                mStatusBarView, mStatusBarWindow);
        mActivityWatcher.setActivityListener(mStatusBarColorController);
        notifyBarViewRegistered();
    }
}
