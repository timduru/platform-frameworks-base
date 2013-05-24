
package com.android.systemui.statusbar;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.UserHandle;

import com.android.systemui.recent.RecentsActivity;

import java.util.ArrayList;
import java.util.List;

public class ActivityWatcher {
    public static final String WINDOW_TYPE_RECENT = "recent";
    public static final String WINDOW_TYPE_KEYGUARD = "keyguard";
    public static final String WINDOW_TYPE_HOME = "home";
    public static final String PACKAGE_NAME_ERROR = "Could not acquire front app";

    public interface ActivityListener {
        public void onActivityChanged(String componentName);
    }

    List<ActivityListener> mListeners = new ArrayList<ActivityListener>();
    List<ComponentName> mLaunchers = new ArrayList<ComponentName>();

    Context mContext;

    public ActivityWatcher(Context context) {
        mContext = context;
        new LauncherList().execute();
    }

    public void setActivityListener(ActivityListener listener) {
        mListeners.add(listener);
    }

    public void notifyTopAppChanged() {
        final String frontComponentName = getFrontAppComponent();
        String frontAppType;
        if (isKeyguardForeground()) {
            frontAppType = WINDOW_TYPE_KEYGUARD;
        } else if (isRecentsForeground()) {
            frontAppType = WINDOW_TYPE_RECENT;
        } else if (isLauncherForeground(frontComponentName)) {
            frontAppType = WINDOW_TYPE_HOME;
        } else {
            frontAppType = frontComponentName;
        }
        for (ActivityListener listener : mListeners) {
            listener.onActivityChanged(frontAppType);
        }
    }

    private boolean isLauncherForeground(String frontApp) {
        if (frontApp.equals(PACKAGE_NAME_ERROR))
            return false;
        final ComponentName frontComponent = ComponentName.unflattenFromString(frontApp);
        for (ComponentName comp : mLaunchers) {
            if (frontComponent.equals(comp))
                return true;
        }
        return false;
    }

    private String getFrontAppComponent() {
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
            return intent.getComponent().flattenToString();
        } else {
            return PACKAGE_NAME_ERROR;
        }
    }

    private boolean isKeyguardForeground() {
        KeyguardManager km = (KeyguardManager) mContext
                .getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null)
            return false;
        return km.isKeyguardLocked();
    }

    private boolean isRecentsForeground() {
        return RecentsActivity.isForeground();
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
