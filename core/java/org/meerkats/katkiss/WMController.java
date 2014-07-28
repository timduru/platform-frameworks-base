package org.meerkats.katkiss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map.Entry;
import android.os.AsyncTask;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.statusbar.IStatusBarService;

public class WMController 
{
	public static final boolean DEBUG = true;
	private static final String TAG = "WMController";
	private static float[] _previousAnimationScales;
	private static Date _previousAnimationScalesTime = new Date();
	private IWindowManager _wm;
	Context _c;
	RunningTaskInfo _topTask;
	RunningTaskInfo _prevTask;
	boolean _isTopTaskSplitView = false;
	boolean _isPrevTaskSplitView = false;

	
	public WMController(Context c)
	{
		_c = c;
		_wm = (IWindowManager) WindowManagerGlobal.getWindowManagerService();
	}
	
	public synchronized float[] getAnimationScales()
	{
		final float[] anims = new float[3];
		for(int i=0; i<anims.length; i++)
			try {anims[i] = _wm.getAnimationScale(i);} catch(Exception e) {}
		return anims;
	}

	public synchronized void setAnimationScales(final float[] anims)
	{
		for(int i=0; i<anims.length; i++)
			try {_wm.setAnimationScale(i, anims[i]);} catch(Exception e) {Log.e(TAG, e.toString());}
	}

	public synchronized void saveAnimationScales()	
	{
		// MAJ only if not asked for saving shortly before 
		if(_previousAnimationScales == null || new Date().getTime() - _previousAnimationScalesTime.getTime() > 3000 )
		{
			_previousAnimationScales = getAnimationScales();
		}
		_previousAnimationScalesTime = new Date();
	}
	public synchronized void restoreAnimationScales()	{ setAnimationScales(_previousAnimationScales);	}
	public synchronized void disableAnimationScales()	
	{ 
		final float[] noAnims = {0,0,0};
		saveAnimationScales();
		setAnimationScales(noAnims);
	}
	
	private String getDefaultLauncherPackage()
	{
		String defaultHomePackage = "com.android.launcher";
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);

		final ResolveInfo res = _c.getPackageManager().resolveActivity(intent, 0);
		if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
			defaultHomePackage = res.activityInfo.packageName;
		}
		return defaultHomePackage;
	}

	private boolean isDefaultLauncherOrSystemUI(String packageName)
	{
		return (packageName.equals(getDefaultLauncherPackage()) || packageName.equals("com.android.systemui"));
	}

	private RunningTaskInfo getTaskBeforeTop() { return getTask(1, false); }
	private RunningTaskInfo getTopTask() { return getTask(0, false); }

	private RunningTaskInfo getFirstSplitViewTaskBeforeTop() { return getTask(1, true); }
	private RunningTaskInfo getTopSplitViewTask() { return getTask(0, true); }

	private RunningTaskInfo getTask(int nTasksBeforeTop, boolean splitViewTaskOnly)
	{
		if(_c == null) return null; 

		RunningTaskInfo taskFound = null;;
		int current = nTasksBeforeTop;
		final ActivityManager am = (ActivityManager) _c.getSystemService(Activity.ACTIVITY_SERVICE);
		List <ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(10);

		try
		{
			while ((taskFound == null) && (current < tasks.size())) 
			{
				RunningTaskInfo currentTask = tasks.get(current);
				boolean isSplitViewTask = isTaskSplitView(currentTask.id);

				String packageName = currentTask.topActivity.getPackageName();
				if (!isDefaultLauncherOrSystemUI(packageName))
				{
					if(splitViewTaskOnly)
					{
						if(isSplitViewTask) taskFound = currentTask;	        	
					}
					else
						taskFound = tasks.get(current);
				}
				current++;
			}
		}catch (Exception e) {}
		Log.v(TAG, "getTask:"+(taskFound!=null ? taskFound.baseActivity:null));

		return taskFound;
	}

	public boolean isFloating(ActivityManager.RecentTaskInfo taskInfo)
	{
		Intent intent = new Intent(taskInfo.baseIntent);
		if (taskInfo.origActivity != null) intent.setComponent(taskInfo.origActivity);
                return isFloating(intent);
	}

        public boolean isFloating(Intent intent)
        {
                return (intent.getFlags() & Intent.FLAG_FLOATING_WINDOW) == Intent.FLAG_FLOATING_WINDOW;
        }



		public boolean isTaskSplitView(int taskID)
		{
			try { return _wm.isTaskSplitView(taskID); }
			catch(Exception e) {}
			return false;
		}

		private void setWMTaskFlagSplitView(int taskID, boolean split)
		{ setWMTaskFlagSplitView(taskID, split, -1); }
		
		private void setWMTaskFlagSplitView(int taskID, boolean split, int slot)
		{
			try 
			{
				_wm.setTaskSplitView(taskID, split); 
				if(split) _wm.setTaskLocation(taskID, slot);
			}
			catch(Exception e) {}
			
		}

		public synchronized void switchTaskToSplitView(int taskID, boolean split, int slot, int moveFlags)
		{
				setWMTaskFlagSplitView(taskID, split, slot);

				final ActivityManager am = (ActivityManager) _c.getSystemService(Context.ACTIVITY_SERVICE);
				//am.moveTaskToFront(taskID, ActivityManager.MOVE_TASK_WITH_HOME, null);
				am.moveTaskToFront(taskID, moveFlags, null);
		}

		private synchronized  void switchToPreviousTask(final int delayMS)
		{ 
			AsyncTask.execute(new Runnable() {
				public void run() 
				{
					if(delayMS >0) try{Thread.sleep(delayMS);} catch(Exception e) {}
					switchToPreviousTask();
				} });
		}

		public synchronized void switchToPreviousTask()
		{
	//		disableAnimationScales();
			RunningTaskInfo task = getTaskBeforeTop();
			if(task == null) return;

			final ActivityManager am = (ActivityManager) _c.getSystemService(Context.ACTIVITY_SERVICE);
			am.moveTaskToFront(task.id, 0, null);
			
	//		restoreAnimationScales();		
		}

		public synchronized void refreshTopAndPrevTasks()
		{
			_topTask = getTopTask();
			_prevTask = getTaskBeforeTop();
			if(_topTask != null) _isTopTaskSplitView = isTaskSplitView(_topTask.id);
			if(_prevTask != null) _isPrevTaskSplitView = isTaskSplitView(_prevTask.id);
			Log.v(TAG, "refreshTopAndPrevTasks:topTask="
				+ (_topTask!=null ? _topTask.baseActivity:null) 
				+ " prevTask="+ (_prevTask!=null ? _prevTask.baseActivity:null)
				+ " isTopTaskSplitView="+_isTopTaskSplitView 
				+ " isPrevTaskSplitView="+_isPrevTaskSplitView );
		}
		// Auto mode
		public synchronized void switchTopTaskToSplitView()	
		{
			switchTopTaskToSplitView(-1); 
		}

		public synchronized void switchTopTaskToSplitView(int slot)
		{
			Log.d(TAG, "switchTopTaskToSplitView++");
			int prevSlot = -1;
			if(slot != -1)
				prevSlot = (slot == 0 ? 1:0);  

			disableAnimationScales();
			refreshTopAndPrevTasks();
			if(_topTask == null) return;

			if(_prevTask != null)
			{	
				if(_isTopTaskSplitView) setWMTaskFlagSplitView(_prevTask.id, false, prevSlot);
				else switchTaskToSplitView(_prevTask.id, !_isTopTaskSplitView, prevSlot,  0);
			}
			if(_topTask != null) switchTaskToSplitView(_topTask.id, !_isTopTaskSplitView, slot,   (_prevTask == null || _isTopTaskSplitView)? ActivityManager.MOVE_TASK_WITH_HOME :0);

			forceLayout2LastTasks();
			Log.d(TAG, "switchTopTaskToSplitView--");
		}

		private synchronized void forceLayout2LastTasks()
		{
			// Workaround to force relayout of apps that don't layout cleanly after switching    
			AsyncTask.execute(new Runnable() {
				public void run()
				{
					try{Thread.sleep(200);} catch(Exception e) {}
					switchToPreviousTask();
					try{Thread.sleep(100);} catch(Exception e) {}
					switchToPreviousTask();
					try{Thread.sleep(1000);} catch(Exception e) {}
					restoreAnimationScales();
				} });
		}
		
		public synchronized static void showRecentAppsSystemUI() 
		{
			try { IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar")).toggleRecentApps(); } 
			catch (Exception e) { }
		}

		public synchronized static void showNotificationsPanel() 
		{
			try { IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar")).expandNotificationsPanel(); } 
			catch (Exception e) { }
		}

		public synchronized static void showSettingsPanel() 
		{
			try { IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar")).expandSettingsPanel(); } 
			catch (Exception e) { }
		}

		public synchronized static void killApp(Context c, String packageName)
		{
			if(packageName == null) return;
			final ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
			am.forceStopPackage(packageName);
		}

        private ArrayList<ActivityManager.RecentTaskInfo> getRecentTaskList(int max, boolean getRegular, boolean getSplit, boolean getFloating)
        {
                if(_c == null) return null;

                ArrayList<ActivityManager.RecentTaskInfo> tasksFound = new ArrayList<ActivityManager.RecentTaskInfo>();
                int current = 0;

                final ActivityManager am = (ActivityManager) _c.getSystemService(Activity.ACTIVITY_SERVICE);
                List<ActivityManager.RecentTaskInfo> tasks = am.getRecentTasks(max, ActivityManager.RECENT_IGNORE_UNAVAILABLE| ActivityManager.RECENT_WITH_EXCLUDED | ActivityManager.RECENT_DO_NOT_COUNT_EXCLUDED);

                try
                {
                        while (current < tasks.size())
                        {
                                final ActivityManager.RecentTaskInfo currentTaskInfo = tasks.get(current);

                                boolean isSplitViewTask = isTaskSplitView(currentTaskInfo.id);
                                boolean isFloating = isFloating(currentTaskInfo);
                                Log.v(TAG, "task=" +currentTaskInfo.baseIntent + " isSplitViewTask="+isSplitViewTask + " isFloating="+isFloating);

//                                String packageName = currentTask.topActivity.getPackageName();
//                                if (!isDefaultLauncherOrSystemUI(packageName))
                                if( (getRegular && !isSplitViewTask && !isFloating) || (getSplit && isSplitViewTask) || (getFloating && isFloating))
                                        tasksFound.add(currentTaskInfo);

                                current++;
                        }
                }catch (Exception e) {}

                return tasksFound;
        }

	public synchronized void switchTopAsFloating()
	{
		RunningTaskInfo top =  getTopTask();
		if(top == null) { Log.w(TAG, "relaunchTopAsFloating: no TopTask"); return; }

		String packageName = top.baseActivity.getPackageName();
		if(packageName == null) { Log.w(TAG, "relaunchTopAsFloating: no packageName"); return; }
		Log.d(TAG, "relaunchTopAsFloating: packageName=" + packageName );

		//killApp(killApp);
		PackageManager pm = _c.getPackageManager();
		Intent intent = pm.getLaunchIntentForPackage(packageName);
		if(intent == null) { Log.w(TAG, "relaunchTopAsFloating: no intent for packageName:" + packageName); return; }

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if(isTopFloating()) killApp(_c, packageName);
		else intent.addFlags(Intent.FLAG_FLOATING_WINDOW);
		_c.startActivityAsUser(intent, UserHandle.CURRENT);
	}

	public synchronized boolean isTopFloating()
	{
		final ArrayList<ActivityManager.RecentTaskInfo> regularWithFloating = getRecentTaskList(20, true, true, true);
		if(regularWithFloating.size() == 0) return false;
		return isFloating(regularWithFloating.get(0));
	}

	public synchronized void switchAllFloating()
	{
		final ArrayList<ActivityManager.RecentTaskInfo> allFloating = getRecentTaskList(20, false, false, true);
		final ActivityManager am = (ActivityManager) _c.getSystemService(Context.ACTIVITY_SERVICE);
		boolean isTopFloating = isTopFloating();
		Log.v(TAG, "isTopFloating=" + isTopFloating);

		if(isTopFloating) // move first non floating to Top
		{
			final ArrayList<ActivityManager.RecentTaskInfo> regular = getRecentTaskList(20, true, true, false);
			if(regular.size() == 0) return;
			am.moveTaskToFront(regular.get(0).id,  ActivityManager.MOVE_TASK_WITH_HOME, null);
		}
		else // move all floating tasks to top
			AsyncTask.execute(new Runnable() {
                                public void run()
                                {
                                        Collections.reverse(allFloating);
                                        for(ActivityManager.RecentTaskInfo task: allFloating)
                                        {
                                                Log.v(TAG, "displayAllFloating: "+ task.baseIntent);
                                                am.moveTaskToFront(task.id,  0, null);
                                        }
                                      //  try{Thread.sleep(100);} catch(Exception e) {}
                                } });
	}
}
