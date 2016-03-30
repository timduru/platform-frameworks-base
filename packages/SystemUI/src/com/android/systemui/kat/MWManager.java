package com.android.systemui.kat;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.StackInfo;
import android.view.WindowManager;

import android.graphics.Point;
import android.graphics.Rect;

import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import android.os.UserHandle;
import java.lang.Math;

import org.meerkats.katkiss.KatUtils;


class MWManager
{
	private static String TAG = "KatMWManager";

	private Context _c;
	private ActivityManager mAM;
	private IActivityManager mIam;
    WindowManager mWm;

	private ArrayList<MWTask> mTasks;
	private int mNumTasks;
	private boolean mIsLandscape = true;
	
	private String homePackage = "";
	private final MWPositions mPOS;

	public MWManager(Context c, int numTasks)
	{
		Log.d(TAG, "MWManager: construct");

		_c = c;
		mNumTasks = numTasks;
		mAM = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
		mIam = ActivityManagerNative.getDefault();			
		mWm = (WindowManager) _c.getSystemService(Context.WINDOW_SERVICE);
		mPOS = new MWPositions(getWindowRect(), numTasks);
		refreshOrientation();
	}
	
	class MWTask
	{
		private int taskId = -1; 
		private int stackId = -1;
		private Rect currentSize = new Rect();
		private RunningTaskInfo info;
		
		public MWTask(RunningTaskInfo runningInfo)
		{
			info = runningInfo;			
			taskId = runningInfo.id;
			StackInfo stackInfo = getMatchingStack(taskId);
			if(stackInfo == null) { Log.w(TAG, "MWTask: taskId=" +taskId  + ": no stack info"); return; }

			stackId = stackInfo.stackId;
			currentSize = stackInfo.bounds;
			
			Log.d(TAG, "MWTask: taskId=" +taskId + " stackId=" + stackId +  " currentSize=" + currentSize );
		}			
	}

	public void refresh(Context c, int numTaskMode)
	{
		_c = c;
		mNumTasks = numTaskMode;
		refreshOrientation();
		refreshTasks();
	}
	
	
	private void refreshTasks()
	{
		ArrayList<MWTask> tasks = new ArrayList<MWTask>();
		Log.d(TAG, "===== refreshTasks ===== ");
		
		List <RunningTaskInfo> runningTasks = mAM.getRunningTasks(6);
		for (RunningTaskInfo taskInfo : runningTasks) 
		{
			if(skipTask(taskInfo)) continue;
			
			Log.d(TAG, "RunningTaskInfo: " + taskInfo.baseActivity );
			tasks.add(new MWTask(taskInfo));
			
			if(tasks.size() > mNumTasks) break;
		}
					
		mTasks = tasks;
	}

	private void refreshOrientation()
	{
		mIsLandscape = isLandscape();
		mPOS.setOrientation(mIsLandscape);
	}
	
	
    private Rect getWindowRect() {
        Rect windowRect = new Rect();
        if (mWm == null) return windowRect;

        Point p = new Point();
        mWm.getDefaultDisplay().getRealSize(p);
        windowRect.set(0, 0, p.x, p.y);
        return windowRect;
    }

	private boolean skipTask(RunningTaskInfo taskInfo)
	{
		String packageName = "";
		try 
		{ packageName = taskInfo.topActivity.getPackageName(); }
		catch(Exception e) {}
		return isDefaultLauncherOrSystemUI(packageName);
	}
	
	private boolean isDefaultLauncherOrSystemUI(String packageName)
	{
		return (packageName.equals(getDefaultLauncherPackage()) || packageName.equals("com.android.systemui"));
	}

	private boolean isLandscape()
	{
		 int orientation=_c.getResources().getConfiguration().orientation;
		  return (orientation!=Configuration.ORIENTATION_PORTRAIT);
	}
	
	private String getDefaultLauncherPackage()
	{
		String defaultHomePackage = "com.android.launcher3";
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);

		final ResolveInfo res = _c.getPackageManager().resolveActivity(intent, 0);
		if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
			defaultHomePackage = res.activityInfo.packageName;
		}
		return defaultHomePackage;
	}

	private StackInfo getMatchingStack(int taskId)
	{
		try 
		{
			List<ActivityManager.StackInfo> infos = mIam.getAllStackInfos();

			//SparseArray<StackInfo> infos = mProxy.getAllStackInfos();
			for(StackInfo stackInfo : infos)
				for(int currentTaskId : stackInfo.taskIds)
					if(taskId == currentTaskId) return stackInfo;			
		}
		catch(Exception e) {}
		return null;
	}

	private boolean isTopFull()
	{
		if(mTasks.size() < 1) return false;
		Log.d(TAG, "mPOS.full=" + mPOS.getFull() + "mTasks.get(0).currentSize=" + mTasks.get(0).currentSize);

		return mPOS.isFullScreen(mTasks.get(0).currentSize);
	}		

	public void switchTopTask()
	{			
		if(mTasks.size() < 1) return;
		
		boolean isLandscape = isLandscape();
		boolean isTopFull  = isTopFull(); 
		Log.d(TAG, "isTopFull=" +isTopFull + " isLandscape=" + isLandscape);
		
		List<Rect> posRect = mPOS.getRects(); // TODO f(numTasks)
		

		for(int i=0; i<mNumTasks && i< mTasks.size(); i++)
		{
			MWTask task = mTasks.get(i);
			Rect newSize = isTopFull? posRect.get(i) : mPOS.getFull();
			Log.d(TAG, "taski=" +task.info.baseActivity + " newSize="+newSize);
			resizeTask(task.taskId, newSize);
		}

		for(int i=Math.min(mNumTasks, mTasks.size()) -1 ; i>=0; i--)
		{	
			MWTask task = mTasks.get(i);
			mAM.moveTaskToFront(task.taskId, 0, null);
		}
	}
	
    public void resizeTask(int taskId, Rect bounds) 
    {
        if (mIam == null) return;

        try { mIam.resizeTask(taskId, bounds); }
        catch (Exception e) {}
    }
}

