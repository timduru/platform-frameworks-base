package org.meerkats.katkiss;

import java.util.ArrayList;
import java.util.Arrays;
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.statusbar.IStatusBarService;

import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.os.SystemClock;
import android.os.UserHandle;


public class KatUtils {
    public static String[] HDMIModes = {"center", "crop", "scale"};
    public static final boolean DEBUG = true;
    public static float[] previousAnimationScales;


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

  public static void expandedDesktop(Context c, boolean on, int style /* 2 both bars, 1: keep statusbar*/)
  {
        Settings.System.putInt( c.getContentResolver(), Settings.System.EXPANDED_DESKTOP_STATE, on ? 1 : 0);
        Settings.System.putInt( c.getContentResolver(), Settings.System.EXPANDED_DESKTOP_STYLE, style);
  }

  public static void expandedDesktopSwitch(Context c, int style /* 2 both bars, 1: keep statusbar*/)
  {
        boolean  currentlyExpanded = Settings.System.getInt( c.getContentResolver(), Settings.System.EXPANDED_DESKTOP_STATE,  0) == 1;
	expandedDesktop(c, !currentlyExpanded, style);
  }

  public static void sendKeyDOWN(final int keyCode) { sendKey(keyCode, KeyEvent.ACTION_DOWN); }
  public static void sendKeyUP(final int keyCode) { sendKey(keyCode, KeyEvent.ACTION_UP); }
  public static void sendKey(final int keyCode, final int action) 
  {
	InputManager inputMgr = InputManager.getInstance();
	if(inputMgr == null) return;
	long currentTime = SystemClock.uptimeMillis();
	inputMgr.injectInputEvent( 
		new KeyEvent(currentTime, currentTime, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD), 
		InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
  }

  public static Configuration invertOrientation(Configuration conf)
  {
	  conf.orientation = (conf.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ?  ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	  return conf;
  }

  public static void rotationToggle(Context c) 
  {
        boolean  on = Settings.System.getInt( c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION,  0) == 1;
        Settings.System.putInt( c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION,  !on? 1 : 0);
  }
  public static void rotationToggle(Context c, boolean on) 
  {
        Settings.System.putInt( c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, on ? 1 : 0);
  }

  public static void sendIntentToWindowManager(Context c, String action, String cmd, boolean shouldRestartUI) 
  {
        Intent intent = new Intent()
                .setAction(action)
                .putExtra(KKC.I.CMD,  cmd)
                .putExtra(KKC.I.EXTRA_RESTART_SYSTEMUI, shouldRestartUI);
        c.sendBroadcastAsUser(intent, new UserHandle(UserHandle.USER_ALL));
    }

}
