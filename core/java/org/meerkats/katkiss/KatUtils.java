package org.meerkats.katkiss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map.Entry;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;
import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.os.SystemClock;
import android.os.UserHandle;
import android.widget.Toast;
import android.provider.Settings.SettingNotFoundException;
import android.bluetooth.BluetoothAdapter;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.media.AudioManager;
import android.util.MathUtils;


public class KatUtils {
    public static String[] HDMIModes = {"center", "crop", "scale"};
    public static String TAG = "KatUtils";
    public static final boolean DEBUG = true;
    public static float[] previousAnimationScales;

    private Context mContext = null;

    static class AppInfo
    {
    	public Drawable icon;
    	public String appName;
    }
    
    public KatUtils(Context c) {mContext = c;}

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


  public static void expandedDesktop(Context c, boolean on)
  {
        Settings.System.putInt( c.getContentResolver(), KKC.S.USER_IMMERSIVE_MODE, on ? 1 : 0);
//        new WMController(c).forceRefreshTop();
  }

  public static boolean isExpanded(Context c)
  { return Settings.System.getInt( c.getContentResolver(), KKC.S.USER_IMMERSIVE_MODE,  0) == 1;}
  
  public static void expandedDesktopSwitch(Context c)
  { expandedDesktop(c, !isExpanded(c)); }

  public static void sendKeyDOWN(final int keyCode) { sendKey(keyCode, KeyEvent.ACTION_DOWN); }
  public static void sendKeyUP(final int keyCode) { sendKey(keyCode, KeyEvent.ACTION_UP); }
  public static void sendKey(final int keyCode, final int action) 
  {
	InputManager inputMgr = InputManager.getInstance();
	if(inputMgr == null) return;
	inputMgr.injectInputEvent( newKeyEvent(keyCode, action), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
  }

  public static KeyEvent newKeyEvent(final int keyCode, final int action) 
  { 
       long currentTime = SystemClock.uptimeMillis();
       return new KeyEvent(currentTime, currentTime, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD);
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

  public static void splitView(Context c, String action, String cmd)
  {
	int numapps = 2;
	if(cmd.startsWith(KKC.A.SPLITVIEW_AUTO) && cmd.length() > KKC.A.SPLITVIEW_AUTO.length())
		numapps = Integer.parseInt(cmd.substring(KKC.A.SPLITVIEW_AUTO.length()));
	
	splitView(c, action, cmd, numapps);
  }
	
  public static void splitView(Context c, String action, String cmd, int numapps) 
  {
        Intent intent = new Intent()
                .setAction(action)
                .putExtra(KKC.I.CMD,  cmd)
                .putExtra("numapps",  numapps);
        c.sendBroadcastAsUser(intent, new UserHandle(UserHandle.USER_ALL));
    }
/*
  public static void ethernetToggle(Context c)
  {
        EthernetManager ethernetManager = (EthernetManager) c.getSystemService(Context.ETHERNET_SERVICE);
	if(ethernetManager == null) return;

	if(ethernetManager.isEnabled()) 	ethernetManager.teardown();
	else 					ethernetManager.reconnect();
  }
*/

    public void muteVolume(boolean keyguardActive) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) 
	{
            if (!keyguardActive)
                audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
            int vibrateMode = AudioManager.RINGER_MODE_VIBRATE;
            // Check if vibrate in silent mode (default) should be overridden.
            if (android.provider.Settings.System.getInt( mContext.getContentResolver(), Settings.System.VIBRATE_IN_SILENT, vibrateMode) == AudioManager.RINGER_MODE_VIBRATE) 
	    {
                vibrateMode = AudioManager.RINGER_MODE_VIBRATE;
                Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(300);
            } else
                vibrateMode = AudioManager.RINGER_MODE_SILENT;
            audioManager.setRingerMode(vibrateMode);
        } else {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            if (!keyguardActive)
                audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
        }
    }
    public void wifiToggle() {
        WifiManager wifiManager = (WifiManager) mContext
                .getSystemService("wifi");
        boolean wifiState = wifiManager.isWifiEnabled();
        if (wifiState) {
            wifiManager.setWifiEnabled(false);
            Toast.makeText(mContext, "Wifi Disabled", Toast.LENGTH_SHORT).show();
        } else {
            wifiManager.setWifiEnabled(true);
            Toast.makeText(mContext, "Wifi Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    public void bluetoothToggle() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean btState = btAdapter.isEnabled();
        if (btState) {
            btAdapter.disable();
            Toast.makeText(mContext, "Bluetooth Disabled", Toast.LENGTH_SHORT).show();
        } else {
            btAdapter.enable();
            Toast.makeText(mContext, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
        }

    }

    public void brightnessControl(int keyCode)
    {
	if(keyCode == KeyEvent.KEYCODE_BRIGHTNESS_UP) brightnessControl(KKC.A.BRIGHTNESS_UP);
	else if(keyCode == KeyEvent.KEYCODE_BRIGHTNESS_DOWN) brightnessControl(KKC.A.BRIGHTNESS_DOWN);
	else if(keyCode == KeyEvent.KEYCODE_BRIGHTNESS_AUTO) brightnessControl(KKC.A.BRIGHTNESS_AUTO);
    }


    public void brightnessControl(String action) 
    {
        int max = 255;
        int min = 4;
        int currentLevel = getBrightness();
        int newLevel = currentLevel;

        int incrementBacklight = (currentLevel > 20)? 10:2; // use shorter step for levels below 20

        if (KKC.A.BRIGHTNESS_UP.equals(action))
            newLevel += incrementBacklight; 
        else if (KKC.A.BRIGHTNESS_DOWN.equals(action))
            newLevel -= incrementBacklight; 
        else if(KKC.A.BRIGHTNESS_AUTO.equals(action))
	    setBrightnessMode( getBrightnessMode() == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC: Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);


        if(!KKC.A.BRIGHTNESS_AUTO.equals(action))  setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        newLevel = MathUtils.constrain(newLevel, min, max);
        if(newLevel != currentLevel) setBrightness(newLevel); 

        Intent intent = new Intent(Intent.ACTION_SHOW_BRIGHTNESS_DIALOG);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
    }

    public int getBrightness() {
        try {
            int level = android.provider.Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
            return level;
        } catch (SettingNotFoundException e) {
            Log.e(TAG, "Couldn't get brightness setting. ", e);
            return 255;
        }
    }


    public int getBrightnessMode() {
        try {
            int mode = android.provider.Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            return mode;
        } catch (SettingNotFoundException e) {
            Log.e(TAG, "Couldn't get brightness mode. ", e);
            return 0;
        }

    }

    public void setBrightness(int level)
    {
        android.provider.Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, level);
        PowerManager powerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);

        if(powerManager != null) powerManager.setBacklightBrightness(level);
        return;
    }

    public void setBrightnessMode(int mode)
    {
        if(getBrightnessMode() != mode)
            android.provider.Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    public void launchSettings() {
        Intent intent = new Intent("android.settings.SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public void touchpadToggle() {
        int touchpadEnabled = android.provider.Settings.System.getInt( mContext.getContentResolver(), KKC.S.DEVICE_SETTINGS_TOUCHPAD_ENABLED, 1);
        touchpadEnabled = (touchpadEnabled == 1 ? 0:1);
        android.provider.Settings.System.putInt(mContext.getContentResolver(), KKC.S.DEVICE_SETTINGS_TOUCHPAD_ENABLED, touchpadEnabled);

        Toast.makeText(mContext, "Touchpad " + (touchpadEnabled ==1 ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();

     }


}
