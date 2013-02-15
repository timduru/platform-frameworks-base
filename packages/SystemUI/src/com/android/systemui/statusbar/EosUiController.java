
package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
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
    private WindowManager mWindowManager;
    private ContentResolver mResolver;
    private Resources mRes;

    private int MSG_BATTERY_ICON_SETTINGS;
    private int MSG_BATTERY_TEXT_SETTINGS;
    private int MSG_BATTERY_TEXT_COLOR_SETTINGS;
    private int MSG_CLOCK_VISIBLE_SETTINGS;
    private int MSG_CLOCK_COLOR_SETTINGS;
    private int MSG_LEGACY_TOGGLES_SETTINGS;
    private int MSG_STATUSBAR_COLOR_SETTINGS;
    private int MSG_NAVBAR_COLOR_SETTINGS;
    private int MSG_HYBRID_BAR_SETTINGS;
    private int MSG_NX_BAR_SETTINGS;

    // Eos classes
    private EosSettings mEosLegacyToggles;
    private SystembarStateHandler mSystembarHandler;
    private EosObserverHandler mObserverHandler;

    private boolean mIsClockVisible = true;

    // NX
    private OnTouchListener mNxNavbarTouchListener;
    private boolean mSearchLightOn = false;
    private boolean mSearchLightLongPress = false;
    private boolean mNX = false;
    private int mCurrentNavLayout;
    private boolean mNxNavBarGlow = false;

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
        mRes = mContext.getResources();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
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

        // bar appearance
        MSG_STATUSBAR_COLOR_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_STATUSBAR_COLOR);
        MSG_NAVBAR_COLOR_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_NAVBAR_COLOR);
        MSG_HYBRID_BAR_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR);
        MSG_NX_BAR_SETTINGS = mObserverHandler
                .registerUri(EOSConstants.SYSTEMUI_USE_NX_NAVBAR);

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
        } else if (msg == MSG_STATUSBAR_COLOR_SETTINGS) {
            handleStatusbarColorChange();
            return;
        } else if (msg == MSG_NAVBAR_COLOR_SETTINGS) {
            handleNavigationBarColorChange();
            return;
        } else if (msg == MSG_HYBRID_BAR_SETTINGS) {
            restartSystemUIServce();
            return;
        } else if (msg == MSG_NX_BAR_SETTINGS) {
            handleNxChange();
            return;
        }
    }

    public NavigationBarView setNavigationBarView(WindowManager.LayoutParams lp) {
        mCurrentNavLayout = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR,
                EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR_DEF)
                == EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR_DEF
                        ? STOCK_NAV_BAR
                        : EOS_NAV_BAR;
        mNavigationBarView = (NavigationBarView) View.inflate(mContext, mCurrentNavLayout, null);
        mNavigationBarView.setEos(this);

        // see if need to init NX
        mNX = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_USE_NX_NAVBAR,
                EOSConstants.SYSTEMUI_USE_NX_NAVBAR_DEF) == 1;
        if (mCurrentNavLayout == STOCK_NAV_BAR && mNX) {
            startNX();
        }

        // softkey longpress action handler
        EosSoftkeyHandler.init(mContext, mNavigationBarView);

        mSystembarHandler.setNavigationBar(mNavigationBarView, lp);
        handleNavigationBarColorChange();
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

        handleStatusbarColorChange();
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

    private void handleNxChange() {
        mNX = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_USE_NX_NAVBAR,
                EOSConstants.SYSTEMUI_USE_NX_NAVBAR_DEF) == 1;
        if (mNX) {
            startNX();
        } else {
            stopNX();
        }
    }

    public boolean isNxEnabled() {
        return mNX;
    }

    public boolean isSearchLightOn() {
        return mSearchLightOn;
    }

    public boolean isSearchLightLongPress() {
        return mSearchLightLongPress;
    }

    public void setSearchLightOn(boolean state) {
        mSearchLightOn = state;
    }

    public void setSearchLightLongPress(boolean state) {
        mSearchLightLongPress = state;
    }

    private GestureDetector getNxDetector() {
        return new GestureDetector(mContext,
                new GestureDetector.OnGestureListener() {
                    public void onLongPress(MotionEvent e) {
                        if (!mSearchLightLongPress) {
                            injectKey(KeyEvent.KEYCODE_MENU);
                            mNavigationBarView
                                    .performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                            mNavigationBarView.playSoundEffect(SoundEffectConstants.CLICK);
                        }
                    }

                    public boolean onSingleTapUp(MotionEvent e) {
                        mService.getHandler().removeCallbacks(mService.mShowSearchPanel);
                        return false;
                    }

                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                            float velocityY) {
                        final float deltaParallel = isNavBarVertical() ? e2.getY() - e1.getY() : e2
                                .getX() - e1.getX();
                        final float deltaPerpendicular = isNavBarVertical() ? e2.getX() - e1.getX()
                                : e2
                                        .getY() - e1.getY();

                        if (Math.abs(deltaPerpendicular) > (isNavBarVertical() ? mNavigationBarView
                                .getWidth() : mNavigationBarView.getHeight())) {
                            injectKey(KeyEvent.KEYCODE_HOME);
                        } else if (deltaParallel > 0) {
                            if (isNavBarVertical()) {
                                injectKey(KeyEvent.KEYCODE_BACK);
                            } else {
                                mService.toggleRecentApps();
                            }
                        } else if (deltaParallel < 0) {
                            if (isNavBarVertical()) {
                                mService.toggleRecentApps();
                            } else {
                                injectKey(KeyEvent.KEYCODE_BACK);
                            }
                        }

                        mNxNavBarGlow = true;
                        handleNavigationBarColorChange();
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mNxNavBarGlow = false;
                                handleNavigationBarColorChange();
                            }
                        }, 100);

                        mNavigationBarView
                                .performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        mNavigationBarView.playSoundEffect(SoundEffectConstants.CLICK);

                        return true;
                    }

                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
                        mSearchLightLongPress = false;
                        mSearchLightOn = false;
                        return false;
                    }

                    public void onShowPress(MotionEvent e) {
                    }

                    public boolean onDown(MotionEvent e) {
                        return false;
                    }
                });
    }

    /*
     * this is a bit on the hacky side here but it seems there's no way to
     * actually remove a OnTouchListener. so if NX is disabled after being
     * enabled, simply pass the event on.
     */
    private OnTouchListener getNxNavbarTouchListener(GestureDetector detector) {
        final GestureDetector nxDetector = detector;

        return new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mNX) {
                    return nxDetector.onTouchEvent(event);
                } else {
                    return mNavigationBarView.onTouchEvent(event);
                }
            }
        };
    }

    private void startNX() {
        if (mNxNavbarTouchListener == null) {
            mNxNavbarTouchListener = getNxNavbarTouchListener(getNxDetector());
        }
        mNavigationBarView.setNxLayout();
        mNavigationBarView.setOnTouchListener(mNxNavbarTouchListener);
    }

    private void stopNX() {
        mNavigationBarView.reorient();
        mNxNavbarTouchListener = null;
    }

    public void injectKey(int keycode) {
        final long eventTime = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keycode, 0);

        InputManager.getInstance().injectInputEvent(keyEvent,
                InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);

        keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
        InputManager.getInstance().injectInputEvent(keyEvent,
                InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private boolean isNavBarVertical() {
        Display mDisplay = mWindowManager.getDefaultDisplay();
        int rotation = mDisplay.getRotation();
        int screenConfig = mRes.getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean landscape = (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);

        if (screenConfig == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            return landscape;
        } else {
            return false;
        }
    }

    // applies to Navigation Bar or Systembar
    private void handleNavigationBarColorChange() {
        int color = 0;
        if (mNxNavBarGlow) {
            color = mRes.getColor(com.android.internal.R.color.holo_blue_light);
        } else {
            color = Settings.System.getInt(mContext.getContentResolver(),
                    EOSConstants.SYSTEMUI_NAVBAR_COLOR,
                    EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF);
            if (color == -1)
                color = EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF;
        }
        // we don't want alpha here
        color = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
        mNavigationBarView.setBackgroundColor(color);
    }

    // applies to Statusbar only
    private void handleStatusbarColorChange() {
        int color = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_STATUSBAR_COLOR,
                EOSConstants.SYSTEMUI_STATUSBAR_COLOR_DEF);
        // For themes
        mStatusBarView.setBackground(mContext.getResources().getDrawable(
                R.drawable.status_bar_background));
        if (color != -1) {
            // we don't want alpha here
            mStatusBarWindow.setBackground(null);
            color = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
            mStatusBarView.setBackgroundColor(color);
        }
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
