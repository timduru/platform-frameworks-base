package org.meerkats.katkiss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map.Entry;


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
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.statusbar.IStatusBarService;

public class KatUtils {
    public static String[] HDMIModes = {"center", "crop", "scale"};
    public static final boolean DEBUG = true;

    static class AppInfo
    {
    	public Drawable icon;
    	public String appName;
    }
    
    public static void initializeHdmiMode(Context c) {
        int currentMode = Settings.System.getInt(c.getContentResolver(), KKC.S.HDMI_MODE, 2);
        SystemProperties.set("nvidia.hwc.rotation", "ICS");
        SystemProperties.set("nvidia.hwc.mirror_mode", HDMIModes[currentMode]);
    }

    public static void switchHDMIMode(int i)
    {
        //SystemProperties.set("nvidia.hwc.rotation", "HC");
        SystemProperties.set("nvidia.hwc.rotation", "ICS");
        SystemProperties.set("nvidia.hwc.mirror_mode", HDMIModes[i]);
    }
    
    public static HashMap<Integer, KeyActions> getKeyOverrideMap(Context c)
    {
    	 HashMap<Integer, KeyActions>  keyOverrideMap = null;

	     String keysOverride = Settings.System.getString(c.getContentResolver(), KKC.S.KEYS_OVERRIDE);
	     if(keysOverride == null || keysOverride.equals("")) { return null; }
	
	    keyOverrideMap = new HashMap<Integer,KeyActions>();
	    List<String> keys = Arrays.asList(keysOverride.split(Pattern.quote("|")));
	
	    for(String key : keys)
	    {
	     Integer keyID = Integer.parseInt(key);
             KeyActions action = new KeyActions(c, keyID);
             action.initFromSettings();
	     keyOverrideMap.put(keyID, action);
	    }
	    
	    return keyOverrideMap;
    }

    public static ArrayList<KeyActions> getKeyOverrideList(Context c)
    {
    	HashMap<Integer, KeyActions>  keyOverrideMap = getKeyOverrideMap(c);
    	if(keyOverrideMap ==null) return null;
    	
    	return new ArrayList<KeyActions> (keyOverrideMap.values());
    }

  public static void writeKeyOverrideListToSettings(Context c, HashMap<Integer, KeyActions> keyOverrideMap)
  {
	String conf = "";
        for(Entry<Integer,KeyActions> entry : keyOverrideMap.entrySet())
        {
                if(!conf.equals("")) conf += "|";
                conf += "" + entry.getKey();
        }
        Settings.System.putString(c.getContentResolver(), KKC.S.KEYS_OVERRIDE, null); 
        Settings.System.putString(c.getContentResolver(), KKC.S.KEYS_OVERRIDE, conf);
  }

  public static AppInfo getAppInfoFromUri(Context c, String uri) 
  {
	  final AppInfo info = new AppInfo();
	  
	  if (uri.startsWith("app:")) 
	  {
	    String activity = uri.substring(4);
	    PackageManager pm = c.getPackageManager();
	    ComponentName component = ComponentName.unflattenFromString(activity);
	    ActivityInfo activityInfo = null;
	    Boolean noError = false;
	    try { activityInfo = pm.getActivityInfo(component, PackageManager.GET_RECEIVERS); } 
	    catch (NameNotFoundException e) {  }
	    
	    if (activityInfo != null) 
	    {
	        info.icon = activityInfo.loadIcon(pm);
	        info.appName = activityInfo.loadLabel(pm).toString();
	    }
	  }
	  
	  return info;
  }
  
  public static String getDefaultLauncherPackage(Context c)
  {
        String defaultHomePackage = "com.android.launcher";
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        final ResolveInfo res = c.getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
            defaultHomePackage = res.activityInfo.packageName;
        }
        return defaultHomePackage;
  }

  public static boolean isDefaultLauncherOrSystemUI(Context c, String packageName)
  {
    return (packageName.equals(getDefaultLauncherPackage(c)) || packageName.equals("com.android.systemui"));
  }

  public static RunningTaskInfo getTaskBeforeTop(Context c) { return getTask(c, 1, false); }
  public static RunningTaskInfo getTopTask(Context c) { return getTask(c, 0, false); }

  public static RunningTaskInfo getFirstSplitViewTaskBeforeTop(Context c) { return getTask(c, 1, true); }
  public static RunningTaskInfo getTopSplitViewTask(Context c) { return getTask(c, 0, true); }
  

  public static RunningTaskInfo getTask(Context c, int nTasksBeforeTop, boolean splitViewTaskOnly)
  {
    if(c == null) return null; 

    RunningTaskInfo taskFound = null;;
    int current = nTasksBeforeTop;
    final ActivityManager am = (ActivityManager) c.getSystemService(Activity.ACTIVITY_SERVICE);
    List <ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(10);

    try
    {
	    final IWindowManager wm = (IWindowManager) WindowManagerGlobal.getWindowManagerService();
	
	    while ((taskFound == null) && (current < tasks.size())) 
	    {
	    	boolean isSplitViewTask = false;
	    	RunningTaskInfo currentTask = tasks.get(current);
	    	if(wm != null) isSplitViewTask = wm.isTaskSplitView(currentTask.id);
 
	        String packageName = currentTask.topActivity.getPackageName();
	        if (!isDefaultLauncherOrSystemUI(c, packageName))
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
Log.v("KatUtils", "getTask:"+(taskFound!=null ? taskFound.baseActivity:null));
    
    return taskFound;
  }

  public static boolean isTaskSplitView(Context c, int taskID)
  {
	  try
	  {
 	      final IWindowManager wm = (IWindowManager) WindowManagerGlobal.getWindowManagerService();
          return wm.isTaskSplitView(taskID);
	  }
	  catch(Exception e) {}
	  return false;
  }

  public static void switchTaskToSplitView(Context c, int taskID, boolean split, int moveFlags)
  {
	  try
	  {
 	      final IWindowManager wm = (IWindowManager) WindowManagerGlobal.getWindowManagerService();
	      wm.setTaskSplitView(taskID, split);

	      final ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
	      //am.moveTaskToFront(taskID, ActivityManager.MOVE_TASK_WITH_HOME, null);
	      am.moveTaskToFront(taskID, moveFlags, null);
	  }
	  catch(Exception e) {}
  }
  
  public static void switchToPreviousTask(Context c)
  {
	  RunningTaskInfo task = getTaskBeforeTop(c);
	  if(task == null) return;
	  
      final ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
      am.moveTaskToFront(task.id, 0, null);
  }
  
  public static void switchTopTaskToSplitView(Context c)
  {
	  RunningTaskInfo topTask;
	  RunningTaskInfo prevTask;
/*      if(split)
      {
    	  // switch previous Split task if found too.
    	  task = getTopSplitViewTask(c); 
    	  if(task != null)
    		  switchTaskToSplitView(c, task.id, split);
      }
*/
      topTask = getTopTask(c);
      if(topTask == null) return;

      boolean isTopTaskSplitView = isTaskSplitView(c, topTask.id);
      prevTask = getTaskBeforeTop(c);

      Log.v("KatUtils", "switchTopTaskToSplitView:topTask="+(topTask!=null ? topTask.baseActivity:null) + " prevTask="+ (prevTask!=null ? prevTask.baseActivity:null)+ " isTopTaskSplitView="+isTopTaskSplitView );

      if(prevTask != null && !isTopTaskSplitView) switchTaskToSplitView(c, prevTask.id, !isTopTaskSplitView,  0);
      if(topTask != null) switchTaskToSplitView(c, topTask.id, !isTopTaskSplitView,  (prevTask == null || isTopTaskSplitView)? ActivityManager.MOVE_TASK_WITH_HOME :0);
  }
  
  public static void showRecentAppsSystemUI() 
  {
      try { IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar")).toggleRecentApps(); } 
      catch (Exception e) { }
  }


}
