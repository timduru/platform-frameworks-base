/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.teameos.jellybean.settings.ActionHandler;
import org.teameos.jellybean.settings.EOSConstants;

import android.animation.LayoutTransition;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;

import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.GlowPadView.OnTriggerListener;
import com.android.internal.widget.multiwaveview.TargetDrawable;
import com.android.systemui.R;
import com.android.systemui.recent.StatusBarTouchProxy;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.tablet.StatusBarPanel;
import com.android.systemui.statusbar.tablet.TabletStatusBar;

public class SearchPanelView extends FrameLayout implements
        StatusBarPanel, ActivityOptions.OnAnimationStartedListener {
    private static final int SEARCH_PANEL_HOLD_DURATION = 0;
    static final String TAG = "SearchPanelView";
    static final boolean DEBUG = TabletStatusBar.DEBUG || PhoneStatusBar.DEBUG || false;
    private static final String ASSIST_ICON_METADATA_NAME =
            "com.android.systemui.action_assist_icon";
    private final Context mContext;
    private BaseStatusBar mBar;
    private StatusBarTouchProxy mStatusBarTouchProxy;

    private boolean mShowing;
    private View mSearchTargetsContainer;
    private GlowPadView mGlowPadView;
    private PackageManager mPackageManager;
    private IWindowManager mWm;
    private Resources mResources;
    private TargetObserver mTargetObserver;
    private ContentResolver mContentResolver;
    private List<String> targetList;
    private MyActionHandler mActionHandler;

    public SearchPanelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        mResources = mContext.getResources();

        mContentResolver = mContext.getContentResolver();
        mTargetObserver = new TargetObserver(new Handler());
        mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        targetList = Arrays.asList(EOSConstants.SYSTEMUI_NAVRING_1,
                EOSConstants.SYSTEMUI_NAVRING_2, EOSConstants.SYSTEMUI_NAVRING_3);
        for (int i = 0; i < targetList.size(); i++) {
            mContentResolver.registerContentObserver(Settings.System.getUriFor(targetList.get(i)),
                    false, mTargetObserver);
        }
        mActionHandler = new MyActionHandler(mContext);
    }

    private boolean launchTarget(int target) {
        String targetKey;

        int targetListOffset;
        if (screenLayout() == Configuration.SCREENLAYOUT_SIZE_LARGE
                || screenLayout() == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            targetListOffset = -1;
        } else {
            if (isScreenPortrait() == true) {
                targetListOffset = -1;
            } else {
                targetListOffset = -3;
            }
        }

        if (target <= targetList.size()) {
            targetKey = Settings.System.getString(mContext.getContentResolver(),
                    targetList.get(target + targetListOffset));
        } else {
            return false;
        }

        if (targetKey == null || targetKey.equals("")) {
            return false;
        }

        mActionHandler.performTask(targetKey);
        return true;
    }

    class GlowPadTriggerListener implements GlowPadView.OnTriggerListener {
        boolean mWaitingForLaunch;

        public void onGrabbed(View v, int handle) {
        }

        public void onReleased(View v, int handle) {
        }

        public void onGrabbedStateChange(View v, int handle) {
            if (!mWaitingForLaunch && OnTriggerListener.NO_HANDLE == handle) {
                mBar.hideSearchPanel();
            }
        }

        public void onTrigger(View v, final int target) {
            boolean launch = launchTarget(target);
        }

        public void onFinishFinalAnimation() {
        }
    }

    final GlowPadTriggerListener mGlowPadViewListener = new GlowPadTriggerListener();

    @Override
    public void onAnimationStarted() {
        postDelayed(new Runnable() {
            public void run() {
                mGlowPadViewListener.mWaitingForLaunch = false;
                mBar.hideSearchPanel();
            }
        }, SEARCH_PANEL_HOLD_DURATION);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSearchTargetsContainer = findViewById(R.id.search_panel_container);
        mStatusBarTouchProxy = (StatusBarTouchProxy) findViewById(R.id.status_bar_touch_proxy);
        // TODO: fetch views
        mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(mGlowPadViewListener);

        setDrawables();
    }

    private void setDrawables() {
        String target2 = Settings.System.getString(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_NAVRING_2);
        if (target2 == null || target2.equals("")) {
            Settings.System.putString(mContext.getContentResolver(),
                    EOSConstants.SYSTEMUI_NAVRING_2, "assist");
        }

        // Custom Targets
        ArrayList<TargetDrawable> storedDraw = new ArrayList<TargetDrawable>();

        int startPosOffset;
        int endPosOffset;

        boolean isHybridUi = mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_isHybridUiDevice);
        boolean forcedTablet = isHybridUi && Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR,
                EOSConstants.SYSTEMUI_USE_HYBRID_STATBAR_DEF) == 1;
        if (screenLayout() == Configuration.SCREENLAYOUT_SIZE_XLARGE || forcedTablet) {
            startPosOffset = 1;
            endPosOffset = 8;
        } else if (screenLayout() == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            startPosOffset = 1;
            endPosOffset = 4;
        } else {
            if (isScreenPortrait() == true) {
                startPosOffset = 1;
                endPosOffset = 4;
            } else {
                startPosOffset = 3;
                endPosOffset = 2;
            }
        }

        List<String> targetActivities = Arrays.asList(Settings.System.getString(
                mContext.getContentResolver(), targetList.get(0)),
                Settings.System.getString(
                        mContext.getContentResolver(), targetList.get(1)),
                Settings.System.getString(
                        mContext.getContentResolver(), targetList.get(2)));

        // Place Holder Targets
        TargetDrawable cDrawable = new TargetDrawable(mResources,
                mResources.getDrawable(com.android.internal.R.drawable.ic_lockscreen_camera));
        cDrawable.setEnabled(false);

        // Add Initial Place Holder Targets
        for (int i = 0; i < startPosOffset; i++) {
            storedDraw.add(cDrawable);
        }

        // Add User Targets
        for (int i = 0; i < targetActivities.size(); i++)
            if (targetActivities.get(i) == null || targetActivities.get(i).equals("")
                    || targetActivities.get(i).equals("none")) {
                storedDraw.add(cDrawable);
            } else if (targetActivities.get(i).equals(EOSConstants.SYSTEMUI_TASK_SCREENSHOT)) {
                storedDraw.add(new TargetDrawable(mResources, mResources
                        .getDrawable(com.android.internal.R.drawable.ic_menu_gallery)));
            } else if (targetActivities.get(i).equals(EOSConstants.SYSTEMUI_TASK_KILL_PROCESS)) {
                storedDraw.add(new TargetDrawable(mResources, mResources
                        .getDrawable(com.android.internal.R.drawable.ic_dialog_alert)));
            } else if (targetActivities.get(i).equals(EOSConstants.SYSTEMUI_TASK_POWER_MENU)) {
                storedDraw.add(new TargetDrawable(mResources, mResources
                        .getDrawable(com.android.internal.R.drawable.ic_lock_reboot)));
            } else if (targetActivities.get(i).equals(EOSConstants.SYSTEMUI_TASK_SCREENOFF)) {
                storedDraw.add(new TargetDrawable(mResources, mResources
                        .getDrawable(com.android.internal.R.drawable.ic_lock_power_off)));
            } else if (targetActivities.get(i).equals(EOSConstants.SYSTEMUI_TASK_ASSIST)) {
                storedDraw.add(new TargetDrawable(mResources,
                        com.android.internal.R.drawable.ic_action_assist_generic));
            } else if (targetActivities.get(i).equals(EOSConstants.SYSTEMUI_TASK_HIDE_BARS)) {
                storedDraw.add(new TargetDrawable(mResources,
                        com.android.internal.R.drawable.ic_dialog_info));
            } else if (targetActivities.get(i).startsWith("app:")) {
                try {
                    ActivityInfo activityInfo = mPackageManager
                            .getActivityInfo(
                                    ComponentName.unflattenFromString(targetActivities.get(i)
                                            .substring(4)),
                                    PackageManager.GET_RECEIVERS);
                    Drawable activityIcon = activityInfo.loadIcon(mPackageManager);

                    storedDraw.add(new TargetDrawable(mResources, activityIcon));
                } catch (NameNotFoundException e) {
                    storedDraw.add(cDrawable);
                }
            }

        // Add End Place Holder Targets
        for (int i = 0; i < endPosOffset; i++) {
            storedDraw.add(cDrawable);
        }

        mGlowPadView.setTargetResources(storedDraw);
    }

    private void maybeSwapSearchIcon() {
        Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(mContext, UserHandle.USER_CURRENT);
        if (intent != null) {
            ComponentName component = intent.getComponent();
            if (component == null || !mGlowPadView.replaceTargetDrawablesIfPresent(component,
                    ASSIST_ICON_METADATA_NAME,
                    com.android.internal.R.drawable.ic_action_assist_generic)) {
                if (DEBUG)
                    Slog.v(TAG, "Couldn't grab icon for component " + component);
            }
        }
    }

    private boolean pointInside(int x, int y, View v) {
        final int l = v.getLeft();
        final int r = v.getRight();
        final int t = v.getTop();
        final int b = v.getBottom();
        return x >= l && x < r && y >= t && y < b;
    }

    public boolean isInContentArea(int x, int y) {
        if (pointInside(x, y, mSearchTargetsContainer)) {
            return true;
        } else if (mStatusBarTouchProxy != null &&
                pointInside(x, y, mStatusBarTouchProxy)) {
            return true;
        } else {
            return false;
        }
    }

    private final OnPreDrawListener mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            getViewTreeObserver().removeOnPreDrawListener(this);
            mGlowPadView.resumeAnimations();
            return false;
        }
    };

    private void vibrate() {
        Context context = getContext();
        if (Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, UserHandle.USER_CURRENT) != 0) {
            Resources res = context.getResources();
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(res.getInteger(R.integer.config_search_panel_view_vibration_duration));
        }
    }

    public void show(final boolean show, boolean animate) {
        if (!show) {
            final LayoutTransition transitioner = animate ? createLayoutTransitioner() : null;
            ((ViewGroup) mSearchTargetsContainer).setLayoutTransition(transitioner);
        }
        mShowing = show;
        if (show) {
            maybeSwapSearchIcon();
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
                // Don't start the animation until we've created the layer,
                // which is done
                // right before we are drawn
                mGlowPadView.suspendAnimations();
                mGlowPadView.ping();
                getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
                vibrate();
            }
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    public void hide(boolean animate) {
        if (mBar != null) {
            // This will indirectly cause show(false, ...) to get called
            mBar.animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    /**
     * We need to be aligned at the bottom. LinearLayout can't do this, so
     * instead, let LinearLayout do all the hard work, and then shift everything
     * down to the bottom.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // setPanelHeight(mSearchTargetsContainer.getHeight());
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Ignore hover events outside of this panel bounds since such events
        // generate spurious accessibility events with the panel content when
        // tapping outside of it, thus confusing the user.
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            return super.dispatchHoverEvent(event);
        }
        return true;
    }

    /**
     * Whether the panel is showing, or, if it's animating, whether it will be
     * when the animation is done.
     */
    public boolean isShowing() {
        return mShowing;
    }

    public void setBar(BaseStatusBar bar) {
        mBar = bar;
    }

    public void setStatusBarView(final View statusBarView) {
        if (mStatusBarTouchProxy != null) {
            mStatusBarTouchProxy.setStatusBar(statusBarView);
            // mGlowPadView.setOnTouchListener(new OnTouchListener() {
            // public boolean onTouch(View v, MotionEvent event) {
            // return statusBarView.onTouchEvent(event);
            // }
            // });
        }
    }

    private LayoutTransition createLayoutTransitioner() {
        LayoutTransition transitioner = new LayoutTransition();
        transitioner.setDuration(200);
        transitioner.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, null);
        return transitioner;
    }

    public boolean isAssistantAvailable() {
        return ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(mContext, UserHandle.USER_CURRENT) != null;
    }

    public int screenLayout() {
        final int screenSize = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenSize;
    }

    public boolean isScreenPortrait() {
        return mResources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public class TargetObserver extends ContentObserver {
        public TargetObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            setDrawables();
        }
    }

    class MyActionHandler extends ActionHandler {

        public MyActionHandler(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public boolean handleAction(String action) {
            // TODO Auto-generated method stub
            return false;
        }

    }
}
