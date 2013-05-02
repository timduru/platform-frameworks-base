
package com.android.systemui.statusbar;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.recent.RecentsActivity;
import com.android.systemui.statusbar.EosObserver.FeatureListener;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusBarWindowView;

import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;
import java.util.List;

public class EosGlassController implements FeatureListener {
    private boolean mGlassEnabled = false;
    private boolean mGlassNavbarPreset = false;
    private boolean mGlassStatusbarPreset = false;
    private int mNavbarGlassLevel = NAVBAR_DEF;
    private int mStatusbarGlassLevel = STATUSBAR_DEF;

    private ArrayList<ComponentName> mLaunchers = new ArrayList<ComponentName>();

    private int mNavbarColor;
    private int mStatusbarColor;

    private static final int NAVBAR_DEF = EOSConstants.SYSTEMUI_NAVBAR_GLASS_PRESET;
    private static final int STATUSBAR_DEF = EOSConstants.SYSTEMUI_STATUSBAR_GLASS_PRESET;;
    private static final int KEYGUARD_NAV = Color.parseColor("#73000000");
    private static final int KEYGUARD_STAT = Color.parseColor("#73000000");
    private static final int RECENT_NAV = Color.parseColor("#e0000000");
    private static final int RECENT_STAT = Color.parseColor("#99000000");

    private int MSG_STATUSBAR_COLOR_SETTINGS;
    private int MSG_NAVBAR_COLOR_SETTINGS;
    private int MSG_GLASS_SETTINGS;
    private int MSG_GLASS_NAVBAR_SETTINGS;
    private int MSG_GLASS_STATUSBAR_SETTINGS;
    private int MSG_GLASS_NAVBAR_PRESET;
    private int MSG_GLASS_STATUSBAR_PRESET;

    private Context mContext;
    private ContentResolver mResolver;
    private NavigationBarView mNavigationBarView;
    private PhoneStatusBarView mStatusBarView;
    private StatusBarWindowView mWindowView;

    public EosGlassController(Context context, PhoneStatusBarView statbar,
            StatusBarWindowView window) {
        mContext = context;
        mResolver = context.getContentResolver();
        mStatusBarView = statbar;
        mWindowView = window;
        new LauncherList().execute();
        handleGlassChange();
    }

    // we have to set this separately for cap key devices
    // that don't have navigationbar
    public void setNavigationBar(NavigationBarView navbar) {
        mNavigationBarView = navbar;
        handleGlassChange();
    }

    @Override
    public ArrayList<String> onRegisterClass() {
        ArrayList<String> uris = new ArrayList<String>();
        uris.add(EOSConstants.SYSTEMUI_STATUSBAR_COLOR);
        uris.add(EOSConstants.SYSTEMUI_NAVBAR_COLOR);
        uris.add(EOSConstants.SYSTEMUI_USE_GLASS);
        uris.add(EOSConstants.SYSTEMUI_NAVBAR_GLASS_LEVEL);
        uris.add(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_LEVEL);
        uris.add(EOSConstants.SYSTEMUI_NAVBAR_GLASS_DEFAULT_ENABLED);
        uris.add(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_DEFAULT_ENABLED);
        return uris;
    }

    @Override
    public void onSetMessage(String uri, int msg) {
        if (uri.equals(EOSConstants.SYSTEMUI_STATUSBAR_COLOR)) {
            MSG_STATUSBAR_COLOR_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_NAVBAR_COLOR)) {
            MSG_NAVBAR_COLOR_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_USE_GLASS)) {
            MSG_GLASS_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_NAVBAR_GLASS_LEVEL)) {
            MSG_GLASS_NAVBAR_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_LEVEL)) {
            MSG_GLASS_STATUSBAR_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_NAVBAR_GLASS_DEFAULT_ENABLED)) {
            MSG_GLASS_NAVBAR_PRESET = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_DEFAULT_ENABLED)) {
            MSG_GLASS_STATUSBAR_PRESET = msg;
        }
    }

    @Override
    public void onFeatureStateChanged(int msg) {
        if (msg == MSG_STATUSBAR_COLOR_SETTINGS) {
            handleStatusbarColorChange();
            return;
        } else if (msg == MSG_NAVBAR_COLOR_SETTINGS) {
            handleNavigationBarColorChange();
            return;
        } else if (msg == MSG_GLASS_SETTINGS
                || msg == MSG_GLASS_NAVBAR_SETTINGS
                || msg == MSG_GLASS_STATUSBAR_SETTINGS
                || msg == MSG_GLASS_NAVBAR_PRESET
                || msg == MSG_GLASS_STATUSBAR_PRESET) {
            handleGlassChange();
            return;
        }
    }

    private void handleGlassChange() {
        mGlassEnabled = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_USE_GLASS,
                EOSConstants.SYSTEMUI_USE_GLASS_DEF) == 1;
        mNavbarGlassLevel = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_NAVBAR_GLASS_LEVEL, NAVBAR_DEF);
        mStatusbarGlassLevel = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_STATUSBAR_GLASS_LEVEL, STATUSBAR_DEF);
        mGlassNavbarPreset = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_NAVBAR_GLASS_DEFAULT_ENABLED, 0) == 1;
        mGlassStatusbarPreset = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_STATUSBAR_GLASS_DEFAULT_ENABLED, 0) == 1;

        if (mGlassNavbarPreset)
            mNavbarGlassLevel = NAVBAR_DEF;
        if (mGlassStatusbarPreset)
            mStatusbarGlassLevel = STATUSBAR_DEF;
        updateBarColors();
    }

    public boolean isGlassEnabled() {
        return mGlassEnabled;
    }

    private boolean isLauncherForeground() {
        ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RecentTaskInfo> recentTasks = am
                .getRecentTasksForUser(
                        1, ActivityManager.RECENT_WITH_EXCLUDED,
                        UserHandle.CURRENT.getIdentifier());
        if (recentTasks.size() > 0) {
            ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(0);
            Intent intent = new Intent(recentInfo.baseIntent);
            if (recentInfo.origActivity != null) {
                intent.setComponent(recentInfo.origActivity);
            }
            for (ComponentName comp : mLaunchers) {
                if (intent.getComponent().equals(comp))
                    return true;
            }
            return false;
        }
        return false;
    }

    private boolean isKeyguardForeground() {
        KeyguardManager km = (KeyguardManager) mContext
                .getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null)
            return false;
        return km.isKeyguardLocked();
    }

    private void updateBarColors() {
        if (mGlassEnabled) {
            updateNavbarColorValue();
            updateStatusbarColorValue();
            applyGlassEffect();
        } else {
            handleNavigationBarColorChange();
            handleStatusbarColorChange();
        }
    }

    public void applyGlassEffect() {
        if (isKeyguardForeground()) {
            if (mNavigationBarView != null)
                mNavigationBarView
                        .setBackgroundColor(applyAlphaToColor(KEYGUARD_NAV, mNavbarColor));
            mStatusBarView
                    .setBackgroundColor(applyAlphaToColor(KEYGUARD_STAT, mStatusbarColor));
            return;
        } else if (isLauncherForeground()) {
            if (mNavigationBarView != null)
                mNavigationBarView.setBackgroundColor(applyAlphaToColor(
                        Color.argb(mNavbarGlassLevel, 0, 0, 0),
                        mNavbarColor));

            mStatusBarView.setBackgroundColor(applyAlphaToColor(
                    Color.argb(mStatusbarGlassLevel, 0, 0, 0),
                    mStatusbarColor));
            return;
        } else if (RecentsActivity.isForeground()) {
            if (mNavigationBarView != null)
                mNavigationBarView.setBackgroundColor(applyAlphaToColor(RECENT_NAV,
                        mNavbarColor));
            mStatusBarView.setBackgroundColor(applyAlphaToColor(RECENT_STAT, mStatusbarColor));
            return;
        } else {
            if (mNavigationBarView != null)
                mNavigationBarView.setBackgroundColor(mNavbarColor);
            mStatusBarView.setBackgroundColor(mStatusbarColor);
        }
    }

    private void updateNavbarColorValue() {
        mNavbarColor = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_NAVBAR_COLOR,
                EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF);
        if (mNavbarColor == -1)
            mNavbarColor = EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF;

        mNavbarColor = removeAlphaFromColor(mNavbarColor);
    }

    private void updateStatusbarColorValue() {
        mStatusbarColor = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_STATUSBAR_COLOR,
                EOSConstants.SYSTEMUI_STATUSBAR_COLOR_DEF);
        if (mStatusbarColor == -1 && mGlassEnabled) {
            mStatusbarColor = EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF;
            mStatusbarColor = removeAlphaFromColor(mStatusbarColor);
        }
    }

    private int removeAlphaFromColor(int color) {
        return Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
    }

    private int applyAlphaToColor(int alpha, int color) {
        return Color.argb(Color.alpha(alpha), Color.red(color), Color.green(color),
                Color.blue(color));
    }

    private void handleNavigationBarColorChange() {
        if (mNavigationBarView == null)
            return;
        updateNavbarColorValue();
        mNavigationBarView.setBackgroundColor(mNavbarColor);
    }

    private void handleStatusbarColorChange() {
        updateStatusbarColorValue();
        // For themes
        mStatusBarView.setBackground(mContext.getResources().getDrawable(
                R.drawable.status_bar_background));
        if (mStatusbarColor != -1) {
            // we don't want alpha here
            mWindowView.setBackground(null);
            mStatusbarColor = removeAlphaFromColor(mStatusbarColor);
            mStatusBarView.setBackgroundColor(mStatusbarColor);
        }
    }

    private class LauncherList extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> activities = mContext.getPackageManager().queryIntentActivities(
                    intent, 0);
            for (ResolveInfo ri : activities) {
                mLaunchers.add(new ComponentName(ri.activityInfo.packageName,
                        ri.activityInfo.name));
            }
            return null;
        }
    }
}
