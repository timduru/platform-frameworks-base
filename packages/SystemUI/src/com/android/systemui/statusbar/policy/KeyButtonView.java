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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
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

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK;

import android.util.Log;
import org.meerkats.katkiss.KKC;
import org.meerkats.katkiss.ActionHandler;
import org.meerkats.katkiss.CustomObserver;
import android.net.Uri;
import java.util.ArrayList;
import android.app.IActivityManager;
import android.app.ActivityManagerNative;



import com.android.systemui.R;

public class KeyButtonView extends ImageView implements CustomObserver.ChangeNotification{
    private static final String TAG = "StatusBar.KeyButtonView";
    private static final boolean DEBUG = false;

    // TODO: Get rid of this
    public static final float DEFAULT_QUIESCENT_ALPHA = 1f;

    private long mDownTime;
    private int mCode;
    private int mTouchSlop;
    private float mDrawingAlpha = 1f;
    private float mQuiescentAlpha = DEFAULT_QUIESCENT_ALPHA;
    private boolean mSupportsLongpress = true;
    private AudioManager mAudioManager;
    private Animator mAnimateToQuiescent = new ObjectAnimator();
    boolean mCustomLongpressEnabled = false;
    boolean mIsLongPressing = false;
    String mConfigUri;
    String mDefaultLongClickAction;
    ActionHandler mClickActionHandler;
    CustomObserver _observer;
    CustomLongClick _customLongClick;
    
    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                 //Log.d("KeyButtonView", "longpressed: mCustomLongpressEnabled=" +mCustomLongpressEnabled);
                if (isLongClickable() || mCustomLongpressEnabled) { mIsLongPressing = true; performLongClick(); }
                else if (mCode != 0 && !mCustomLongpressEnabled) {
                     sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
                     sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
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


        setDrawingAlpha(mQuiescentAlpha);
        setClickable(true);
        setLongClickable(true);


        mConfigUri = a.getString(R.styleable.KeyButtonView_keyConfigUri);
        String clickAction = a.getString(R.styleable.KeyButtonView_keyClickAction);
        if( clickAction != null) mClickActionHandler = new ActionHandler(context, clickAction);

        if(mSupportsLongpress) 
        {
          _customLongClick = new CustomLongClick(mContext);
          setOnLongClickListener(_customLongClick);

         mDefaultLongClickAction = a.getString(R.styleable.KeyButtonView_longPressAction);

         _observer = new CustomObserver(context, this);
         updateCustomConfig();

         mCustomLongpressEnabled = _customLongClick.getAction() != null;
       }

        
        a.recycle();

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
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
            return false;
        }

        @Override
        public boolean handleAction(String action) {
            // TODO Auto-generated method stub
            return false;
        }
    }
    

    private void updateCustomConfig() {
        if(mConfigUri == null || _customLongClick == null) return;

        String action = Settings.System.getString(mContext.getContentResolver(), mConfigUri);

        if(action == null || action.equals("none"))
                action = mDefaultLongClickAction;

        _customLongClick.setAction(action);
        mCustomLongpressEnabled = _customLongClick.getAction() != null;
    }
    
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (mCode != 0 && !mCustomLongpressEnabled) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(ACTION_CLICK, null));
            if (mSupportsLongpress) {
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
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == ACTION_CLICK && mCode != 0) {
            sendEvent(KeyEvent.ACTION_DOWN, 0, SystemClock.uptimeMillis());
            sendEvent(KeyEvent.ACTION_UP, 0);
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            playSoundEffect(SoundEffectConstants.CLICK);
            return true;
        } else if (action == ACTION_LONG_CLICK && mCode != 0 && mSupportsLongpress) {
            sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
            sendEvent(KeyEvent.ACTION_UP, 0);
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    public void setQuiescentAlpha(float alpha, boolean animate) {
        mAnimateToQuiescent.cancel();
        alpha = Math.min(Math.max(alpha, 0), 1);
        if (alpha == mQuiescentAlpha && alpha == mDrawingAlpha) return;
        mQuiescentAlpha = alpha;
        if (DEBUG) Log.d(TAG, "New quiescent alpha = " + mQuiescentAlpha);
        if (animate) {
            mAnimateToQuiescent = animateToQuiescent();
            mAnimateToQuiescent.start();
        } else {
            setDrawingAlpha(mQuiescentAlpha);
        }
    }

    private ObjectAnimator animateToQuiescent() {
        return ObjectAnimator.ofFloat(this, "drawingAlpha", mQuiescentAlpha);
    }

    public float getQuiescentAlpha() {
        return mQuiescentAlpha;
    }

    public float getDrawingAlpha() {
        return mDrawingAlpha;
    }

    public void setDrawingAlpha(float x) {
        setImageAlpha((int) (x * 255));
        mDrawingAlpha = x;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        int x, y;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //Log.d("KeyButtonView", "press");
                mDownTime = SystemClock.uptimeMillis();
                setPressed(true);
                if (mCode != 0) {
                    sendEvent(KeyEvent.ACTION_DOWN, 0, mDownTime);
                } else {
                    // Provide the same haptic feedback that the system offers for virtual keys.
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                if (mSupportsLongpress) {
                    removeCallbacks(mCheckLongPress);
                    postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
                }
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

  
// ChangeNotifications
  @Override
  public ArrayList<Uri> getObservedUris()
  {
    ArrayList<Uri> uris = new  ArrayList<Uri>();
    uris.add(Settings.System.getUriFor(mConfigUri));
    return uris;
  }

  @Override
  public void onChangeNotification(Uri uri)
  {
    Log.d(TAG, "onChangeNotification:" + uri);
    if(uri.equals(Settings.System.getUriFor(mConfigUri))) updateCustomConfig();
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
      if(activityManager.isInLockTaskMode()) activityManager.stopLockTaskModeOnCurrent();
      else if( mCustomLongpressEnabled && _customLongClick != null) _customLongClick.onLongClick(this);
    }
    catch (Exception e) {}

    return super.performLongClick();
  }


}


