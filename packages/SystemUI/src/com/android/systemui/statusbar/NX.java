
package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Display;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

import com.android.systemui.statusbar.EosObserver.FeatureListener;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.PhoneUiController;

import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;

public class NX implements FeatureListener {

    private Context mContext;
    private NavigationBarView mNavigationBarView;
    private PhoneStatusBar mService;
    private WindowManager mWindowManager;
    private PhoneUiController mEos;

    private OnTouchListener mNxNavbarTouchListener;
    public boolean mSearchLightOn = false;
    public boolean mSearchLightLongPress = false;
    private boolean mNX = false;
    private boolean mNxNavBarGlow = false;

    private int MSG_NX_BAR_SETTINGS;

    public NX(Context context, NavigationBarView view, PhoneStatusBar bar, PhoneUiController eos) {
        mContext = context;
        mNavigationBarView = view;
        mService = bar;
        mEos = eos;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        handleNxChange();
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

    private void handleNxChange() {
        mNX = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_USE_NX_NAVBAR,
                EOSConstants.SYSTEMUI_USE_NX_NAVBAR_DEF) == 1;
        if (mNX) {
            startNX();
        } else {
            stopNX();
        }
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
                        final float deltaParallel = isNavBarVertical() ? e2.getY() - e1.getY()
                                : e2.getX() - e1.getX();
                        final float deltaPerpendicular = isNavBarVertical() ? e2.getX()
                                - e1.getX()
                                : e2.getY() - e1.getY();

                        if (Math.abs(deltaPerpendicular) > (isNavBarVertical() ? mNavigationBarView
                                .getWidth()
                                : mNavigationBarView.getHeight())) {
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
                        /* disable bar glow haptic feedback for now
                        final int holoColor = mContext.getResources()
                                .getColor(com.android.internal.R.color.holo_blue_light);
                        mNavigationBarView.setBackgroundColor(Color.argb(125, Color.red(holoColor),
                                Color.green(holoColor), Color.blue(holoColor)));

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mEos.updateGlass();
                            }
                        }, 100);
                        */

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

    private void injectKey(int keycode) {
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
        int screenConfig = mContext.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        boolean landscape = (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);

        if (screenConfig == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            return landscape;
        } else {
            return false;
        }
    }

    private void handleNavigationBarColorChange() {
        int color = 0;
        if (mNxNavBarGlow) {
            color = mContext.getResources().getColor(
                    com.android.internal.R.color.holo_blue_light);
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

    @Override
    public void onFeatureStateChanged(int msg) {
        if (msg == MSG_NX_BAR_SETTINGS) {
            handleNxChange();
            return;
        }
    }

    @Override
    public ArrayList<String> onRegisterClass() {
        ArrayList<String> uris = new ArrayList<String>();
        uris.add(EOSConstants.SYSTEMUI_USE_NX_NAVBAR);
        return uris;
    }

    @Override
    public void onSetMessage(String uri, int msg) {
        if (uri.equals(EOSConstants.SYSTEMUI_USE_NX_NAVBAR)) {
            MSG_NX_BAR_SETTINGS = msg;
        }
    }
}
