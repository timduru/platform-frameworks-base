/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import java.util.Arrays;

import android.animation.Animator.AnimatorListener;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.HapticFeedbackConstants;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.android.systemui.R;

import org.teameos.jellybean.settings.EOSConstants;

public class KeyButtonView extends ImageView {
    private static final String TAG = "StatusBar.KeyButtonView";
    private static Mode mMode = Mode.SRC_ATOP;

    final float GLOW_MAX_SCALE_FACTOR = 1.8f;
    final float BUTTON_QUIESCENT_ALPHA = 0.70f;

    long mDownTime;
    int mCode;
    int mTouchSlop;
    Drawable mGlowBG;
    int mGlowWidth, mGlowHeight;
    float mGlowAlpha = 0f, mGlowScale = 1f, mDrawingAlpha = 1f;
    boolean mSupportsLongpress = true;
    RectF mRect = new RectF(0f, 0f, 0f, 0f);
    AnimatorSet mPressedAnim;
    boolean mDisabled = false;
    boolean mIsLongPressing = false;
    boolean mIsSw600Device = false;
    boolean mIsTabletMode = false;
    boolean mAnimRunning = false;
    float glowScaleWidth;
    float glowScaleHeight;
    IWindowManager mWindowManager;
    int mKeyFilterColor;
    int mGlowFilterColor;
    String mKeyColorUri;
    String mGlowColorUri;
    int mKeyIndex;

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                // Slog.d("KeyButtonView", "longpressed: " + this);
                if (mCode != 0 && !mDisabled) {
                    sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                } else {
                    // Just an old-fashioned ImageView
                    mIsLongPressing = true;
                    if (mDisabled) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        playSoundEffect(SoundEffectConstants.CLICK);
                    }
                    performLongClick();
                }
            }
        }
    };

    public void disableLongPressIntercept(Boolean disable) {
        mDisabled = disable;
    }

    public KeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView,
                defStyle, 0);

        // only apply special case to tablet mode
        mWindowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            mIsTabletMode = mWindowManager.hasSystemNavBar();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // apply special case to sw600 device in table tablet mode
        // use this call from tabletstatusbar to try and trim the
        // GlowBG width some. We need to do this because the glow
        // bleeds into the notfication area. Also, we don't want to
        // change the background resource name because it will interfere
        // with themes
        if (Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_USE_TABLET_UI, EOSConstants.SYSTEMUI_USE_TABLET_UI_DEF) == 1) {
            mIsSw600Device = true;
        }

        mCode = a.getInteger(R.styleable.KeyButtonView_keyCode, 0);
        mKeyIndex = a.getInteger(R.styleable.KeyButtonView_keyIdentifier, 0);
        glowScaleWidth = a.getFloat(R.styleable.KeyButtonView_glowScaleFactorWidth, 1.0f);
        glowScaleHeight = a.getFloat(R.styleable.KeyButtonView_glowScaleFactorHeight, 1.0f);
        mKeyColorUri = a.getString(R.styleable.KeyButtonView_keyColorUri);
        mGlowColorUri = a.getString(R.styleable.KeyButtonView_keyGlowColorUri);

        mSupportsLongpress = a.getBoolean(R.styleable.KeyButtonView_keyRepeat, true);

        mGlowBG = a.getDrawable(R.styleable.KeyButtonView_glowBackground);
        if (mGlowBG != null) {
            setDrawingAlpha(BUTTON_QUIESCENT_ALPHA);
            mGlowWidth = mGlowBG.getIntrinsicWidth();
            mGlowHeight = mGlowBG.getIntrinsicHeight();
            if (mIsTabletMode && mIsSw600Device) {
                mGlowWidth = Math.round(mGlowWidth * glowScaleWidth);
                mGlowHeight = Math.round(mGlowHeight * glowScaleHeight);
            }
        }

        a.recycle();

        setClickable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(mKeyColorUri), false,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        updateKeyFilter();
                    }
                });
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(mGlowColorUri), false,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        updateGlowFilter();
                    }
                });
        updateKeyFilter();
        updateGlowFilter();
    }

    private void updateKeyFilter() {
        int color = Settings.System.getInt(mContext.getContentResolver(),
                mKeyColorUri, EOSConstants.SYSTEMUI_NAVBAR_SOFTKEY_DEFAULT);
        if (color == EOSConstants.SYSTEMUI_NAVKEY_COLOR_DEF) {
            mKeyFilterColor = EOSConstants.SYSTEMUI_NAVKEY_COLOR_DEF;
            getDrawable().clearColorFilter();
        } else {
            mKeyFilterColor = Color.argb(0xFF, Color.red(color),
                    Color.green(color), Color.blue(color));
        }
        applyKeyFilter();
    }

    public void applyKeyFilter() {
        if (mKeyFilterColor != EOSConstants.SYSTEMUI_NAVKEY_COLOR_DEF)
            getDrawable().setColorFilter(mKeyFilterColor, mMode);
    }

    private void updateGlowFilter() {
        int color = mGlowFilterColor = Settings.System.getInt(mContext.getContentResolver(),
                mGlowColorUri, EOSConstants.SYSTEMUI_NAVBAR_SOFTKEY_DEFAULT);
        if (color == EOSConstants.SYSTEMUI_NAVKEY_COLOR_DEF) {
            mGlowFilterColor = EOSConstants.SYSTEMUI_NAVKEY_COLOR_DEF;
            mGlowBG.clearColorFilter();
        } else {
            mGlowFilterColor = Color.argb(0xFF, Color.red(color),
                    Color.green(color), Color.blue(color));
        }
        applyGlowFilter();
    }

    private void applyGlowFilter() {
        if (mGlowFilterColor != EOSConstants.SYSTEMUI_NAVKEY_COLOR_DEF)
            mGlowBG.setColorFilter(mGlowFilterColor, mMode);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mGlowBG != null) {
            canvas.save();
            if (mAnimRunning) applyGlowFilter();
            final int w = getWidth();
            final int h = getHeight();
            final float aspect = (float) mGlowWidth / mGlowHeight;
            final int drawW = (int) (h * aspect);
            final int drawH = h;
            final int margin = (drawW - w) / 2;
            canvas.scale(mGlowScale, mGlowScale, w * 0.5f, h * 0.5f);
            mGlowBG.setBounds(-margin, 0, drawW - margin, drawH);
            mGlowBG.setAlpha((int) (mDrawingAlpha * mGlowAlpha * 255));
            mGlowBG.draw(canvas);
            canvas.restore();
            mRect.right = w;
            mRect.bottom = h;
        }
        if (mAnimRunning) applyKeyFilter();
        super.onDraw(canvas);
    }

    public int getKeyCode() {
        return mCode;
    }

    public void setKeyCode(int keyCode) {
        mCode = keyCode;
    }

    public boolean getSupportsLongPress() {
        return mSupportsLongpress;
    }

    public void setSupportsLongPress(boolean longPress) {
        mSupportsLongpress = longPress;
    }

    public float getDrawingAlpha() {
        if (mGlowBG == null)
            return 0;
        return mDrawingAlpha;
    }

    public void setDrawingAlpha(float x) {
        if (mGlowBG == null)
            return;
        // Calling setAlpha(int), which is an ImageView-specific
        // method that's different from setAlpha(float). This sets
        // the alpha on this ImageView's drawable directly
        setAlpha((int) (x * 255));
        mDrawingAlpha = x;
    }

    public float getGlowAlpha() {
        if (mGlowBG == null)
            return 0;
        return mGlowAlpha;
    }

    public void setGlowAlpha(float x) {
        if (mGlowBG == null)
            return;
        mGlowAlpha = x;
        invalidate();
    }

    public float getGlowScale() {
        if (mGlowBG == null)
            return 0;
        return mGlowScale;
    }

    public void setGlowScale(float x) {
        if (mGlowBG == null)
            return;
        mGlowScale = x;
        final float w = getWidth();
        final float h = getHeight();
        if (GLOW_MAX_SCALE_FACTOR <= 1.0f) {
            // this only works if we know the glow will never leave our bounds
            invalidate();
        } else {
            final float rx = (w * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
            final float ry = (h * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
            com.android.systemui.SwipeHelper.invalidateGlobalRegion(
                    this,
                    new RectF(getLeft() - rx,
                            getTop() - ry,
                            getRight() + rx,
                            getBottom() + ry));

            // also invalidate our immediate parent to help avoid situations
            // where nearby glows
            // interfere
            ((View) getParent()).invalidate();
        }
    }

    public void setPressed(boolean pressed) {
        if (mGlowBG != null) {
            if (pressed != isPressed()) {
                if (mPressedAnim != null && mPressedAnim.isRunning()) {
                    mPressedAnim.cancel();
                }
                final AnimatorSet as = mPressedAnim = new AnimatorSet();
                mAnimRunning = true;
                as.addListener(new AnimatorListener() {
                    public void onAnimationEnd(Animator animation) {
                        applyKeyFilter();
                        applyGlowFilter();
                        mAnimRunning = false;
                    }

                    public void onAnimationCancel(Animator animation) {
                        mAnimRunning = false;
                    }

                    public void onAnimationRepeat(Animator animation) {
                        ;
                    }

                    public void onAnimationStart(Animator animation) {
                        ;
                    }
                });
                if (pressed) {
                    if (mGlowScale < GLOW_MAX_SCALE_FACTOR)
                        mGlowScale = GLOW_MAX_SCALE_FACTOR;
                    if (mGlowAlpha < BUTTON_QUIESCENT_ALPHA)
                        mGlowAlpha = BUTTON_QUIESCENT_ALPHA;
                    setDrawingAlpha(1f);
                    as.playTogether(
                            ObjectAnimator.ofFloat(this, "glowAlpha", 1f),
                            ObjectAnimator.ofFloat(this, "glowScale", GLOW_MAX_SCALE_FACTOR)
                            );
                    as.setDuration(50);
                } else {
                    as.playTogether(
                            ObjectAnimator.ofFloat(this, "glowAlpha", 0f),
                            ObjectAnimator.ofFloat(this, "glowScale", 1f),
                            ObjectAnimator.ofFloat(this, "drawingAlpha", BUTTON_QUIESCENT_ALPHA)
                            );
                    as.setDuration(500);
                }
                as.start();
            }
        }
        super.setPressed(pressed);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        int x, y;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Slog.d("KeyButtonView", "press");
                mDownTime = SystemClock.uptimeMillis();
                setPressed(true);
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
                } else {
                    // Provide the same haptic feedback that the system offers
                    // for virtual keys.
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                if (mSupportsLongpress) {
                    removeCallbacks(mCheckLongPress);
                    postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                x = (int) ev.getX();
                y = (int) ev.getY();
                setPressed(x >= -mTouchSlop
                        && x < getWidth() + mTouchSlop
                        && y >= -mTouchSlop
                        && y < getHeight() + mTouchSlop);
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                if (mCode != 0 && !mIsLongPressing) {
                    sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                }
                if (mSupportsLongpress) {
                    removeCallbacks(mCheckLongPress);
                    mIsLongPressing = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                final boolean doIt = isPressed();
                setPressed(false);
                if (mCode != 0) {
                    if (doIt & !mIsLongPressing) {
                        sendEvent(KeyEvent.ACTION_UP, 0);
                        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                        playSoundEffect(SoundEffectConstants.CLICK);
                    } else {
                        sendEvent(KeyEvent.ACTION_UP, KeyEvent.FLAG_CANCELED);
                    }
                } else {
                    // no key code, just a regular ImageView
                    if (doIt & !mIsLongPressing) {
                        performClick();
                    }
                }
                if (mSupportsLongpress) {
                    removeCallbacks(mCheckLongPress);
                    mIsLongPressing = false;
                }
                break;
        }

        return true;
    }

    void sendEvent(int action, int flags) {
        sendEvent(action, flags, SystemClock.uptimeMillis());
    }

    void sendEvent(int action, int flags, long when) {
        final int repeatCount = (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0;
        final KeyEvent ev = new KeyEvent(mDownTime, when, action, mCode, repeatCount,
                0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                flags | KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        InputManager.getInstance().injectInputEvent(ev,
                InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }
}
