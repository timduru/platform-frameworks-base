
package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.ActivityWatcher.ActivityListener;
import com.android.systemui.statusbar.EosObserver.FeatureListener;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.statusbar.preferences.EosSettings;

import org.teameos.jellybean.settings.EOSConstants;
import org.teameos.jellybean.settings.EOSUtils;

import java.util.ArrayList;

public class EosUiController implements FeatureListener, ActivityListener {

    static final String TAG = "EosUiController";

    // we set a flag in settingsProvider indicating
    // that we intentionally killed systemui. so we
    // can restore states if need be
    public static final String EOS_KILLED_ME = "eos_killed_me";

    static final int STOCK_NAV_BAR = com.android.systemui.R.layout.navigation_bar;
    static final int EOS_NAV_BAR = com.android.systemui.R.layout.eos_navigation_bar;

    private static final int max_notification_normal_ui = 5;
    private static final int max_notification_tablets_on_tablet_ui_port = 4;
    private static final int max_notification_phones_on_tablet_ui_port = 2;
    private static final int max_notification_phones_on_tablet_ui_land = 3;

    private ArrayList<View> mBatteryList = new ArrayList<View>();

    private static boolean DEBUG = false;

    private Context mContext;

    /* View that contains the statusbar or systembar */
    private View mStatusBarView;
    private View mClockView;

    // holds legacy toggles on tablets (temporary)
    private View mTabletSettingsView;

    private PhoneStatusBar mService;
    private NavigationBarView mNavigationBarView;
    private StatusBarWindowView mStatusBarWindow;
    private ContentResolver mResolver;

    private int MSG_BATTERY_ICON_SETTINGS;
    private int MSG_BATTERY_TEXT_SETTINGS;
    private int MSG_BATTERY_TEXT_COLOR_SETTINGS;
    private int MSG_CLOCK_VISIBLE_SETTINGS;
    private int MSG_CLOCK_COLOR_SETTINGS;
    private int MSG_LEGACY_TOGGLES_SETTINGS;

    // Eos classes
    private SystembarStateHandler mSystembarHandler;
    private EosObserver mObserver;
    private EosGlassController mGlass;
    private NX mNx;
    private EosSettings mEosLegacyToggles;
    private ActivityWatcher mActivityWatcher;

    private View mClockCenter;
    private View mClockCluster;
    private View mCurrentClockView;

    private boolean mIsClockVisible = true;
    private int mCurrentNavLayout;

    private boolean mIsTabletUi;
    private boolean mIsNormalScreen;

    /*
     * special case for large screen max icons keep it clean
     */
    private boolean mIsLargeScreen;

    public EosUiController(Context context, SystembarStateHandler handler, EosObserver observer) {
        mContext = context;
        mSystembarHandler = handler;
        mObserver = observer;
        mActivityWatcher = new ActivityWatcher(mContext);
        mActivityWatcher.setActivityListener((ActivityListener) this);
        mResolver = mContext.getContentResolver();
        mIsTabletUi = EOSUtils.hasSystemBar(context);
        mIsNormalScreen = EOSUtils.isNormalScreen();
        mIsLargeScreen = EOSUtils.isLargeScreen();
    }

    @Override
    public ArrayList<String> onRegisterClass() {
        ArrayList<String> uris = new ArrayList<String>();
        uris.add(EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE);
        uris.add(EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE);
        uris.add(EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR);
        uris.add(EOSConstants.SYSTEMUI_CLOCK_VISIBLE);
        uris.add(EOSConstants.SYSTEMUI_CLOCK_COLOR);
        uris.add(EOSConstants.SYSTEMUI_SETTINGS_ENABLED);
        return uris;
    }

    @Override
    public void onSetMessage(String uri, int msg) {
        if (uri.equals(EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE)) {
            MSG_BATTERY_ICON_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE)) {
            MSG_BATTERY_TEXT_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR)) {
            MSG_BATTERY_TEXT_COLOR_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_CLOCK_VISIBLE)) {
            MSG_CLOCK_VISIBLE_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_CLOCK_COLOR)) {
            MSG_CLOCK_COLOR_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_SETTINGS_ENABLED)) {
            MSG_LEGACY_TOGGLES_SETTINGS = msg;
        }
    }

    @Override
    public void onFeatureStateChanged(int msg) {
        if (msg == MSG_BATTERY_ICON_SETTINGS
                || msg == MSG_BATTERY_TEXT_SETTINGS
                || msg == MSG_BATTERY_TEXT_COLOR_SETTINGS) {
            handleBatteryChange();
            return;
        } else if (msg == MSG_CLOCK_VISIBLE_SETTINGS
                || msg == MSG_CLOCK_COLOR_SETTINGS) {
            handleClockChange();
            return;
        } else if (msg == MSG_LEGACY_TOGGLES_SETTINGS) {
            // handleLegacyTogglesChange();
            return;
        }
    }

    public void notifyEosUiOnline() {
        mContext.sendBroadcastAsUser(new Intent()
                .setAction(EOSConstants.INTENT_EOS_UI_CHANGED)
                .putExtra(EOSConstants.INTENT_EOS_UI_CHANGED_REASON, "eos_ui_online"),
                new UserHandle(UserHandle.USER_ALL));
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

    public boolean isNormalScreen() {
        return mIsNormalScreen;
    }

    public boolean isTabletUi() {
        return mIsTabletUi;
    }

    public void notifyTopAppChanged() {
        mActivityWatcher.notifyTopAppChanged();
    }

    public int getNavbarHeightResource() {
        int barMode = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BAR_SIZE_MODE, 0);
        switch (barMode) {
            case 0:
                return com.android.internal.R.dimen.navigation_bar_height;
            case 1:
                return com.android.internal.R.dimen.navigation_bar_height_low_profile;
            case 2:
                return com.android.internal.R.dimen.navigation_bar_height_tiny_profile;
            default:
                return com.android.internal.R.dimen.navigation_bar_height;
        }
    }

    public int getNotificationPanelMinHeight() {
        return R.dimen.notification_panel_min_height;
    }

    public int getNotificationPanelWidth() {
        return mIsNormalScreen ? R.dimen.notification_panel_width_tablet_mode
                : R.dimen.notification_panel_width;
    }

    public int getNotificationIconSize() {
        int barMode = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BAR_SIZE_MODE, 0);
        switch (barMode) {
            case 0:
                return R.dimen.system_bar_icon_size_normal;
            case 1:
                return R.dimen.system_bar_icon_size_slim;
            case 2:
                return R.dimen.system_bar_icon_size_tiny;
            default:
                return R.dimen.system_bar_icon_size_normal;
        }
    }

    public int getTickerIconSize() {
        int barMode = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BAR_SIZE_MODE, 0);
        switch (barMode) {
            case 0:
                return R.dimen.notification_large_icon_height;
            case 1:
                return R.dimen.notification_large_icon_height_slim;
            case 2:
                return R.dimen.notification_large_icon_height_tiny;
            default:
                return R.dimen.notification_large_icon_height;
        }
    }

    public int getNotificationHeightMax() {
        return mIsTabletUi ? getNavbarHeightResource()
                : R.dimen.notification_max_height;
    }

    public int getNotificationHeightMin() {
        return mIsTabletUi ? getNavbarHeightResource()
                : R.dimen.notification_min_height;
    }

    public int getNavigationKeyWidth() {
        return mIsNormalScreen ? R.dimen.navigation_key_width_tablet_mode_on_phones
                : R.dimen.navigation_key_width_tablet_mode_on_tablets;
    }

    public int getMenuKeyWidth() {
        return mIsNormalScreen ? R.dimen.navigation_menu_key_width_tablet_mode_on_phones
                : R.dimen.navigation_menu_key_width_tablet_mode_on_tablets;
    }

    public int getMaxNotificationIcons() {
        final boolean isPortrait = !EOSUtils.isLandscape(mContext);
        if (mIsTabletUi) {
            if (mIsNormalScreen) {
                if (isPortrait) {
                    return max_notification_phones_on_tablet_ui_port;
                } else {
                    return max_notification_phones_on_tablet_ui_land;
                }
            } else if (mIsLargeScreen) {
                if (isPortrait) {
                    return max_notification_tablets_on_tablet_ui_port;
                } else {
                    return max_notification_normal_ui;
                }
            } else {
                return max_notification_normal_ui;
            }
        } else {
            return max_notification_normal_ui;
        }
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

        // send bar to visibility handler first
        mSystembarHandler.setNavigationBar(mNavigationBarView, getNavigationBarLayoutParams());

        // startNX the instance handles all states
        if (mCurrentNavLayout == STOCK_NAV_BAR) {
            mNx = new NX(mContext, mNavigationBarView, mService, EosUiController.this);
            mObserver.registerClass(mNx);
            mNavigationBarView.setNx(mNx);
            mService.setNx(mNx);
        }

        // send view to glass
        mGlass.setNavigationBar(mNavigationBarView);

        // give it back to SystemUI
        return mNavigationBarView;
    }

    public void setBar(PhoneStatusBar service) {
        mService = service;
    }

    public void setBarWindow(StatusBarWindowView window) {
        if (mIsTabletUi)
            return;
        mStatusBarWindow = window;
    }

    // we need this to be set when the theme engine creates new view
    public void setStatusBarView(View bar) {
        mStatusBarView = bar;

        // now we're sure we're getting the correct batteries
        View text = mStatusBarView
                .findViewById(mIsTabletUi ? R.id.notificationArea : R.id.signal_battery_cluster)
                .findViewById(R.id.battery_text);
        text.setTag(EOSConstants.SYSTEMUI_BATTERY_PERCENT_TAG);
        mBatteryList.add(text);

        mBatteryList.add(mStatusBarView
                .findViewById(mIsTabletUi ? R.id.notificationArea : R.id.signal_battery_cluster)
                .findViewById(R.id.battery));

        // start here to include cap key devices
        mGlass = new EosGlassController(mContext, mStatusBarView,
                mStatusBarWindow);
        mObserver.registerClass(mGlass);

        handleBatteryChange();

        handleBatteryChange();

        mClockCluster = mStatusBarView.findViewById(
                mIsTabletUi ? R.id.notificationArea : R.id.system_icon_area).findViewById(
                R.id.clock);

        mClockCenter = mStatusBarView.findViewById(R.id.clock_center);

        mObserver.registerClass((FeatureListener) mClockCluster);
        mObserver.registerClass((FeatureListener) mClockCenter);
        handleClockChange();

        if (mIsTabletUi) {
            mGlass = new EosGlassController(mContext);
            mGlass.setNavigationBar(mStatusBarView);
        } else {
            mGlass = new EosGlassController(mContext, mStatusBarView,
                    mStatusBarWindow);
        }
        mObserver.registerClass((FeatureListener) mGlass);
    }

    public void updateGlass() {
        if (mGlass != null && mGlass.isGlassEnabled()) {
            mGlass.applyGlassEffect();
        }
    }

    static void log(String s) {
        if (DEBUG)
            Log.i(TAG, s);
    }

    private void handleBatteryChange() {
        int icon_visible = (Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE_DEF) == 1) ? View.VISIBLE : View.GONE;

        int text_visible = (Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE_DEF) == 1) ? View.VISIBLE : View.GONE;

        int color = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR_DEF);
        if (color == -1) {
            color = mContext.getResources()
                    .getColor(android.R.color.holo_blue_light);
        }
        for (View v : mBatteryList) {
            if (v.getTag() != null
                    && v.getTag().equals(EOSConstants.SYSTEMUI_BATTERY_PERCENT_TAG)) {
                // this is our text view
                ((TextView) v).setTextColor(color);
                v.setVisibility(text_visible);
            } else {
                // this works for now as we are only controlling
                // two views at any time
                v.setVisibility(icon_visible);
            }
        }
    }

    public void showClock(boolean show) {
        final View clock = mClockView;
        if (clock != null) {
            if (mIsClockVisible) {
                clock.setVisibility(show ? View.VISIBLE : View.GONE);
            } else {
                clock.setVisibility(View.GONE);
            }
        }
    }

    private void handleClockChange() {
        if (mStatusBarView == null)
            return;

        if (mCurrentClockView == null)
            mCurrentClockView = mClockCluster;

        int clock_state = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_CLOCK_VISIBLE,
                EOSConstants.SYSTEMUI_CLOCK_CLUSTER);

        switch (clock_state) {
            case EOSConstants.SYSTEMUI_CLOCK_GONE:
                mIsClockVisible = false;
                mClockCenter.setVisibility(View.GONE);
                mClockCluster.setVisibility(View.GONE);
                break;
            case EOSConstants.SYSTEMUI_CLOCK_CLUSTER:
                mIsClockVisible = true;
                mClockCenter.setVisibility(View.GONE);
                mClockCluster.setVisibility(View.VISIBLE);
                mCurrentClockView = mClockCluster;
                break;
            case EOSConstants.SYSTEMUI_CLOCK_CENTER:
                mIsClockVisible = true;
                mClockCluster.setVisibility(View.GONE);
                mClockCenter.setVisibility(View.VISIBLE);
                mCurrentClockView = mClockCenter;
                break;
        }

        int color = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_CLOCK_COLOR,
                EOSConstants.SYSTEMUI_CLOCK_COLOR_DEF);
        if (color == -1) {
            color = mContext.getResources()
                    .getColor(android.R.color.holo_blue_light);
        }
        ((TextView) mClockCluster).setTextColor(color);
        ((TextView) mClockCenter).setTextColor(color);
    }

    private void handleLegacyTogglesChange() {
        boolean isTogglesEnabled = Settings.System.getInt(
                mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_SETTINGS_ENABLED,
                EOSConstants.SYSTEMUI_SETTINGS_ENABLED_DEF) == 1;

        final View toggleView = mStatusBarWindow == null ? mTabletSettingsView : mStatusBarWindow;

        if (isTogglesEnabled) {
            mEosLegacyToggles = new EosSettings(
                    (ViewGroup) toggleView.findViewById(R.id.eos_toggles),
                    mContext, mObserver);
            mEosLegacyToggles.setEnabled(isTogglesEnabled);
        } else {
            if (mEosLegacyToggles != null) {
                mEosLegacyToggles.setEnabled(isTogglesEnabled);
                mEosLegacyToggles = null;
            }
        }
    }

    /* utility to iterate a viewgroup and return a list of child views */
    public ArrayList<View> getAllChildren(View v) {

        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<View>();

        ViewGroup vg = (ViewGroup) v;
        for (int i = 0; i < vg.getChildCount(); i++) {

            View child = vg.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            result.addAll(viewArrayList);
        }
        return result;
    }

    // utility to help bigclearbutton feature
    // seems ok here for now as it could be useful later
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private Intent getEccIntent() {
        return new Intent()
                .setClassName("org.eos.controlcenter",
                        "org.eos.controlcenter.Main")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public void onActivityChanged(String componentName) {
        mObserver.setEnabled(componentName.equals(getEccIntent().getComponent().flattenToString()));
    }
}
