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

import android.annotation.DrawableRes;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.ButtonDispatcher;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK;

import android.util.Log;
import org.meerkats.katkiss.ActionHandler;
import android.app.IActivityManager;
import android.app.ActivityManagerNative;



import com.android.systemui.R;

public class KeyButtonView extends ImageView implements ButtonDispatcher.ButtonInterface {
    private static final String TAG = "StatusBar.KeyButtonView";
    private static final boolean DEBUG = false;

    // TODO: Get rid of this
    public static final float DEFAULT_QUIESCENT_ALPHA = 1f;

    private int mContentDescriptionRes;
    private long mDownTime;
    private int mCode;
    private int mTouchSlop;
    private boolean mSupportsLongpress = true;
    boolean mCustomLongpressEnabled = false;
    boolean mIsLongPressing = false;
    String mDefaultLongClickAction;
    ActionHandler mClickActionHandler;
    CustomLongClick _customLongClick;
    private boolean mGestureAborted;
    private boolean mLongClicked;

    static AudioManager mAudioManager;
    static AudioManager getAudioManager(Context context) {
		if (mAudioManager == null)
		    mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return mAudioManager;
	}

    private final Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                 Log.d("KeyButtonView", "longpressed: mCustomLongpressEnabled=" +mCustomLongpressEnabled);
                if (isLongClickable() || mCustomLongpressEnabled) { mIsLongPressing = true; performLongClick();  mLongClicked = true;}
                else if (mCode != 0 && !mCustomLongpressEnabled) {
                     sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
                     sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                      mLongClicked = true;
                }
            }
        }
    };

    public KeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView,
                defStyle, 0);

        mCode = a.getInteger(R.styleable.KeyButtonView_keyCode, 0);

        mSupportsLongpress = a.getBoolean(R.styleable.KeyButtonView_keyRepeat, true);

        TypedValue value = new TypedValue();
        if (a.getValue(R.styleable.KeyButtonView_android_contentDescription, value)) {
            mContentDescriptionRes = value.resourceId;
        }

        setClickable(true);
        setLongClickable(true);


        setClickAction( a.getString(R.styleable.KeyButtonView_keyClickAction));
        setLongPressAction(a.getString(R.styleable.KeyButtonView_longPressAction));


        a.recycle();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mAudioManager = getAudioManager(context);
        setBackground(new KeyButtonRipple(context, this));
    }    

    private class CustomLongClick extends ActionHandler implements View.OnLongClickListener {
        String mAction;

        public CustomLongClick(Context context) { super(context); }

        public CustomLongClick(Context context, String action) {
            super(context);
            mAction = action;
        }

        public void setAction(String action) {mAction = action;}
        public String getAction() { return mAction;}


        @Override
        public boolean onLongClick(View v) {
            if(mAction == null || mAction.equals("none")) return false;

            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            playSoundEffect(SoundEffectConstants.CLICK);
            performTask(mAction);
            return true;
        }

        @Override
        public boolean handleAction(String action) {
            // TODO Auto-generated method stub
            return false;
        }
    }
    

    public void setCode(int code) {
        mCode = code;
    }

    public void loadAsync(String uri) {
        new AsyncTask<String, Void, Drawable>() {
            @Override
            protected Drawable doInBackground(String... params) {
                return Icon.createWithContentUri(params[0]).loadDrawable(mContext);
            }

            @Override
            protected void onPostExecute(Drawable drawable) {
                setImageDrawable(drawable);
            }
        }.execute(uri);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mContentDescriptionRes != 0) {
            setContentDescription(mContext.getString(mContentDescriptionRes));
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (mCode != 0 && !mCustomLongpressEnabled) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null));
            if (mSupportsLongpress || isLongClickable()) {
                info.addAction(
                        new AccessibilityNodeInfo.AccessibilityAction(ACTION_LONG_CLICK, null));
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != View.VISIBLE) {
            jumpDrawablesToCurrentState();
        }
    }

    @Override
    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (action == ACTION_CLICK && mCode != 0) {
            sendEvent(KeyEvent.ACTION_DOWN, 0, SystemClock.uptimeMillis());
            sendEvent(KeyEvent.ACTION_UP, 0);
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            playSoundEffect(SoundEffectConstants.CLICK);
            return true;
        } else if (action == ACTION_LONG_CLICK && mCode != 0) {
            sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
            sendEvent(KeyEvent.ACTION_UP, 0);
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
            return true;
        }
        return super.performAccessibilityActionInternal(action, arguments);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        int x, y;
        if (action == MotionEvent.ACTION_DOWN) {
            mGestureAborted = false;
        }
        if (mGestureAborted) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownTime = SystemClock.uptimeMillis();
                mLongClicked = false;
                setPressed(true);
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
                } else {
                    // Provide the same haptic feedback that the system offers for virtual keys.
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                removeCallbacks(mCheckLongPress);
                postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                x = (int)ev.getX();
                y = (int)ev.getY();
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
                final boolean doIt = isPressed() && !mLongClicked;
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

    public void playSoundEffect(int soundConstant) {
        mAudioManager.playSoundEffect(soundConstant, ActivityManager.getCurrentUser());
    };

    public void sendEvent(int action, int flags) {
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

  
  @Override
  public boolean performClick()
  {
    if(mClickActionHandler != null) mClickActionHandler.executeAllActions();
    return super.performClick();
  }

  @Override
  public boolean performLongClick()
  {
    try
    {
      IActivityManager activityManager = ActivityManagerNative.getDefault();
      Log.v(TAG, "activityManager.isInLockTaskMode():" + activityManager.isInLockTaskMode());
/*TOCHECK      if(activityManager.isInLockTaskMode()) activityManager.stopLockTaskModeOnCurrent();
      else */if( mCustomLongpressEnabled && _customLongClick != null) return _customLongClick.onLongClick(this);
    }
    catch (Exception e) {}

    return super.performLongClick();
  }


    @Override
    public void abortCurrentGesture() {
        setPressed(false);
        mGestureAborted = true;
    }

    public void setClickAction(String action)
    {
        if( action != null) mClickActionHandler = new ActionHandler(getContext(), action);
    }

    public void setLongPressAction(String action)
    {
        if(action == null) return;

        if(mSupportsLongpress)
        {
            _customLongClick = new CustomLongClick(getContext());
            setOnLongClickListener(_customLongClick);

            mDefaultLongClickAction = action;
            _customLongClick.setAction(action);
            mCustomLongpressEnabled = _customLongClick.getAction() != null;
        }
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    @Override
    public void setLandscape(boolean landscape) {
        //no op
    }

    @Override
    public void setCarMode(boolean carMode) {
        // no op
    }
}


