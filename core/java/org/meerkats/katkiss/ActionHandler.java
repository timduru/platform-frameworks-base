package org.meerkats.katkiss;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActionHandler {
    protected ArrayList<String> mActions;
    protected Context mContext;
    static final String TAG = "ActionHandler";

    public ActionHandler(Context context, ArrayList<String> actions) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        mContext = context;
        mActions = actions;
    }

    public ActionHandler(Context context, String actions) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        mContext = context;
        mActions = new ArrayList<String>();
        mActions.addAll(Arrays.asList(actions.split("\\|")));
    }

    public ActionHandler(Context context) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        mContext = context;
    }


    public void addAction(String action) {
      if (mActions == null)              
          mActions = new ArrayList<String>();
      mActions.add(action);
    }
    
    /**
     * Set the actions to perform.
     * 
     * @param actions
     */
    public void setActions(List<String> actions) {
        if (actions == null) {
            mActions = null;
        } else {
            mActions = new ArrayList<String>();
            mActions.addAll(actions);
        }
    }

    public List<String> getActions() { return mActions; }

    /**
     * Event handler. This method must be called when the event should be triggered.
     * 
     * @param location
     * @return
     */
    public final boolean handleEvent(int location) {
        if (mActions == null) {
            Log.d("ActionHandler", "Discarding event due to null actions");
            return false;
        }

        String action = mActions.get(location);
        if (action == null || action.equals("")) {
            return false;
        } else {
            performTask(action);
            return true;
        }
    }

    public void executeAllActions() {
        for(String action : mActions)
            performTask(action);
    }

    public String getActionsString() {
        String res = "";
        for(String action : mActions) {
          if(!res.equals("")) res +="|";
          res+= action;
        }
        return res;
    }

    public void performTask(String action) {
        if (action.equals(KKC.A.SYSTEMUI_TASK_KILL_PROCESS))
        	killProcess();
        else if (action.equals(KKC.A.SYSTEMUI_TASK_SCREENSHOT))
        	takeScreenshot();        
        else if (action.equals(KKC.A.SYSTEMUI_TASK_SCREENOFF))
        	screenOff();
/*        else if (action.equals(KKC.A.SYSTEMUI_TASK_ASSIST))
        	startAssistActivity(); */
        else if(action.equals(KKC.A.SYSTEMUI_RECENT))
        	WMController.showRecentAppsSystemUI();
        else if(action.equals(KKC.A.SYSTEMUI_SWITCH_TOPREVIOUS_TASK))
        	new WMController(mContext).switchToPreviousTask();
        else if(action.equals(KKC.A.SPLITVIEW_AUTO))
        	new WMController(mContext).switchTopTaskToSplitView(-1);
        else if(action.equals(KKC.A.SPLITVIEW_1))
        	new WMController(mContext).switchTopTaskToSplitView(0);
        else if(action.equals(KKC.A.SPLITVIEW_2))
        	new WMController(mContext).switchTopTaskToSplitView(1);
        else if(action.equals(KKC.A.RELAUNCH_FLOATING))
        	new WMController(mContext).switchTopAsFloating();
        else if(action.equals(KKC.A.SHOW_HIDE_ALL_FLOATING))
        	new WMController(mContext).switchAllFloating();

        else if(action.equals(KKC.A.EXPANDED_DESKTOP))
        	KatUtils.expandedDesktopSwitch(mContext, 2);
        else if(action.equals(KKC.A.EXPANDED_DESKTOP_KEEPSTATUSBAR))
        	KatUtils.expandedDesktopSwitch(mContext, 1);
        else if(action.equals(KKC.A.AUTOROTATION_TOGGLE))
        	KatUtils.rotationToggle(mContext);
        else if(action.equals(KKC.A.ETHERNET_TOGGLE))
        	KatUtils.ethernetToggle(mContext);
        else if(action.equals(KKC.A.SHOW_POWERMENU) || action.equals(KKC.A.WIFI_TOGGLE) || action.equals(KKC.A.BLUETOOTH_TOGGLE) || action.equals(KKC.A.TOUCHPAD_TOGGLE) || action.equals(KKC.A.LAUNCH_SETTINGS)
                || action.equals(KKC.A.BRIGHTNESS_DOWN) || action.equals(KKC.A.BRIGHTNESS_UP) || action.equals(KKC.A.BRIGHTNESS_AUTO)
                || action.equals(KKC.A.MEDIA_PREVIOUS) || action.equals(KKC.A.MEDIA_NEXT) || action.equals(KKC.A.MEDIA_PLAYPAUSE)
                || action.equals(KKC.A.AUDIO_DOWN) || action.equals(KKC.A.AUDIO_UP) || action.equals(KKC.A.AUDIO_MUTE)
               )
        	KatUtils.sendIntentToWindowManager(mContext, KKC.I.GLOBAL_ACTIONS, action, false);
        else if(action.equals(KKC.A.SHOW_NOTIFICATIONS_PANEL))
        	new WMController(mContext).showNotificationsPanel();
        else if(action.equals(KKC.A.SHOW_SETTINGS_PANEL))
        	new WMController(mContext).showSettingsPanel();
//        else if (action.equals(KKC.A.SYSTEMUI_TASK_POWER_MENU))
//        	showPowerMenu();
        else if (action.startsWith(KKC.A.SENDKEY_BASE)) {
		int keyCode = Integer.parseInt(action.substring(KKC.A.SENDKEY_BASE.length()));
		KatUtils.sendKeyDOWN(keyCode);
		KatUtils.sendKeyUP(keyCode);
        }
        else if (action.startsWith("app:"))
        	launchActivity(action);
    }

    public Handler getHandler() {
        return H;
    }

    private Handler H = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

            }
        }
    };

    private void launchActivity(String action) {
        String activity = action.substring(4);
        ComponentName component = ComponentName.unflattenFromString(activity);

        /* Try to launch the activity from history, if available. */
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RecentTaskInfo task : activityManager.getRecentTasks(20,
                ActivityManager.RECENT_IGNORE_UNAVAILABLE)) {
            if (task != null && task.origActivity != null &&
                    task.origActivity.equals(component)) {
                activityManager.moveTaskToFront(task.id, ActivityManager.MOVE_TASK_WITH_HOME);
                postActionEventHandled(true);
                return;
            }
        }

        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(component);
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                | Intent.FLAG_ACTIVITY_TASK_ON_HOME
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);

        postActionEventHandled(true);
    }

    /**
     * functions needed for taking screenhots. This leverages the built in ICS
     * screenshot functionality
     */
    final Object mScreenshotLock = new Object();
    static ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(H.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        H.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;

                        /*
                         * remove for the time being if (mStatusBar != null &&
                         * mStatusBar.isVisibleLw()) msg.arg1 = 1; if
                         * (mNavigationBar != null &&
                         * mNavigationBar.isVisibleLw()) msg.arg2 = 1;
                         */

                        /* wait for the dialog box to close */
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }

                        /* take the screenshot */
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn;
                H.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    private void killProcess() {
        if (mContext
                .checkCallingOrSelfPermission(android.Manifest.permission.FORCE_STOP_PACKAGES) == PackageManager.PERMISSION_GRANTED) {
            ActivityManager am = (ActivityManager) mContext
                    .getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> task = am.getRunningTasks(1, 0, null);
            String packageName = task.get(0).baseActivity.getPackageName();

            /* Check that we're not killing the launcher */
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (packageName.equals(resolveInfo.activityInfo.packageName)) {
                postActionEventHandled(false);
                return;
            }

            am.forceStopPackage(task.get(0).baseActivity.getPackageName());
            postActionEventHandled(true);
        } else {
            Log.d("ActionHandler", "Caller cannot kill processes, aborting");
            postActionEventHandled(false);
        }
    }

    private void screenOff() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
    }
/*
    private void startAssistActivity() {
        IWindowManager mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        boolean isKeyguardShowing = false;
        try {
            isKeyguardShowing = mWm.isKeyguardLocked();
        } catch (RemoteException e) {

        }

        if (isKeyguardShowing) {
            try {
                mWm.showAssistant();
            } catch (RemoteException e) {
            }
        } else {
            Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                    .getAssistIntent(mContext, true, UserHandle.USER_CURRENT);
            if (intent == null)
                return;

            try {
                ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
            } catch (RemoteException e) {
                // too bad, so sad...
            }

            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
            } catch (ActivityNotFoundException e) {
                Slog.w(TAG, "Activity not found for " + intent.getAction());
            }
        }
    }
*/
/*    private void showPowerMenu() {
        new PowerMenu(mContext);
        postActionEventHandled(true);
    }
*/

    /**
     * This method is called after an action is performed. This is useful for subclasses to
     * override, such as the one in the lock screen. As you need to unlock the device after
     * performing an action.
     * 
     * @param actionWasPerformed
     */
    protected boolean postActionEventHandled(boolean actionWasPerformed) {
        return actionWasPerformed;
    }

    /**
     * This the the fall over method that is called if this base class cannot process an action. You
     * do not need to manually call {@link postActionEventHandled}
     * 
     * @param action
     * @return
     */
    public boolean handleAction(String action){ return false;}
}
