
package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.EosObserverHandler.OnFeatureStateChangedListener;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.statusbar.preferences.EosSettings;

import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;

public class EosUiController implements OnFeatureStateChangedListener {

    static final String TAG = "EosUiController";

    // we set a flag in settingsProvider indicating
    // that we intentionally killed systemui. so we
    // can restore states if need be
    public static final String EOS_KILLED_ME = "eos_killed_me";

    static final int STOCK_NAV_BAR = com.android.systemui.R.layout.navigation_bar;
    static final int EOS_NAV_BAR = com.android.systemui.R.layout.eos_navigation_bar;

    private ArrayList<View> mBatteryList = new ArrayList<View>();

    private static boolean DEBUG = false;

    private Context mContext;

    private PhoneStatusBarView mStatusBarView;
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
    private int MSG_HYBRID_BAR_SETTINGS;

    // Eos classes
    private EosSettings mEosLegacyToggles;
    private SystembarStateHandler mSystembarHandler;
    private EosObserverHandler mObserverHandler;

    private boolean mIsClockVisible = true;
    private int mCurrentNavLayout;

    // our one and only instance
    private static EosUiController eosUiController;

    public static void initEos(Context context, SystembarStateHandler handler) {
        eosUiController = new EosUiController(context, handler);
    }

    public static EosUiController getEosUiController() {
        return eosUiController;
    }

    public EosUiController(Context context, SystembarStateHandler handler) {
        mContext = context;
        mSystembarHandler = handler;
        EosObserverHandler.initHandler(context);
        mObserverHandler = EosObserverHandler.getEosObserverHandler();
        mResolver = mContext.getContentResolver();
        registerUriList();
    }

    private void registerUriList() {
        // battery
        MSG_BATTERY_ICON_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE);
        MSG_BATTERY_TEXT_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE);
        MSG_BATTERY_TEXT_COLOR_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR);

        // clock
        MSG_CLOCK_VISIBLE_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_CLOCK_VISIBLE);
        MSG_CLOCK_COLOR_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_CLOCK_COLOR);

        // legacy toggles
        MSG_LEGACY_TOGGLES_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_SETTINGS_ENABLED);
        MSG_HYBRID_BAR_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR);

        mObserverHandler.setOnFeatureStateChangedListener((OnFeatureStateChangedListener) this);
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
            handleLegacyTogglesChange();
            return;
        } else if (msg == MSG_HYBRID_BAR_SETTINGS) {
            restartSystemUIServce();
            return;
        }
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
                EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR,
                EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR_DEF)
                == EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR_DEF
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
            EosNxHandler.create(mContext, mNavigationBarView, mService);
        }

        // send view to glass
        EosGlassController.setNavigationBar(mNavigationBarView);

        // give it back to SystemUI
        return mNavigationBarView;
    }

    public void setBar(PhoneStatusBar service) {
        mService = service;
    }

    public void setBarWindow(StatusBarWindowView window) {
        mStatusBarWindow = window;
        handleLegacyTogglesChange();
        mStatusBarWindow.setEosSettings(mEosLegacyToggles);
    }

    // we need this to be set when the theme engine creates new view
    public void setStatusBarView(PhoneStatusBarView bar) {
        mStatusBarView = bar;

        // now we're sure we're getting the correct batteries
        View text = mStatusBarView
                .findViewById(R.id.signal_battery_cluster)
                .findViewById(R.id.battery_text);
        text.setTag(EOSConstants.SYSTEMUI_BATTERY_PERCENT_TAG);
        mBatteryList.add(text);

        mBatteryList.add(mStatusBarView
                .findViewById(R.id.signal_battery_cluster)
                .findViewById(R.id.battery));

        // start here to include cap key devices
        EosGlassController.startGlass(mContext, mStatusBarView,
                mStatusBarWindow);

        handleBatteryChange();
        handleClockChange();
    }

    static void log(String s) {
        if (DEBUG)
            Log.i(TAG, s);
    }

    public EosSettings getEosSettings() {
        return mEosLegacyToggles;
    }

    private void restartSystemUIServce() {
        // time to die, but i shall return again soon
        // before we go let's set our flag
        Settings.System.putInt(mResolver, EOS_KILLED_ME, 1);
        System.exit(0);
    }

    private void handleBatteryChange() {
        int icon_visible = (Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE_DEF) == 1) ? View.VISIBLE : View.GONE;

        int text_visible = (Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE_DEF) == 1) ? View.VISIBLE : View.GONE;

        int color = Settings.System.getInt(mContext.getContentResolver(),
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
        View clock = mStatusBarView.findViewById(R.id.clock);
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
        TextView clock = (TextView) mStatusBarView.findViewById(R.id.clock);

        mIsClockVisible = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_CLOCK_VISIBLE,
                EOSConstants.SYSTEMUI_CLOCK_VISIBLE_DEF) == 1 ? true : false;
        showClock(true);
        int color = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_CLOCK_COLOR,
                EOSConstants.SYSTEMUI_CLOCK_COLOR_DEF);
        if (color == -1) {
            color = mContext.getResources()
                    .getColor(android.R.color.holo_blue_light);
        }
        clock.setTextColor(color);
    }

    private void handleLegacyTogglesChange() {
        boolean isTogglesEnabled = Settings.System.getInt(
                mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_SETTINGS_ENABLED,
                EOSConstants.SYSTEMUI_SETTINGS_ENABLED_DEF) == 1;

        if (isTogglesEnabled) {
            mEosLegacyToggles = new EosSettings(
                    (ViewGroup) mStatusBarWindow.findViewById(R.id.eos_toggles),
                    mContext);
            mEosLegacyToggles.setEnabled(isTogglesEnabled);
        } else {
            if (mEosLegacyToggles != null) {
                mEosLegacyToggles.setEnabled(isTogglesEnabled);
                mEosLegacyToggles = null;
            }
        }
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
}
