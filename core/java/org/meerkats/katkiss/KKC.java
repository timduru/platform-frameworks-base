package org.meerkats.katkiss;

public final class KKC {

    // Settings
  public final class S {
    public static final String SYSTEMUI_UI_MODE = "systemui_ui_mode";
    public static final int SYSTEMUI_UI_MODE_PHABLETUI = 4;
    public static final int SYSTEMUI_UI_MODE_NAVBAR_LEFT = 0;
    public static final int SYSTEMUI_UI_MODE_PHABLETUI_NO_NAVBAR = 1;
    public static final int SYSTEMUI_UI_MODE_SYSTEMBAR = 3; // TabletUI
    public static final int SYSTEMUI_UI_MODE_NOBAR = 2;


    public static final String SYSTEMUI_UI_BARSIZE = "systemui_ui_barsize";
    public static final int SYSTEMUI_BARSIZE_MODE_NORMAL = 0;
    public static final int SYSTEMUI_BARSIZE_MODE_SLIM = 1;
    public static final int SYSTEMUI_BARSIZE_MODE_TINY = 2;


    public static final String DEVICE_SETTINGS_TOUCHPAD_MODE = "device_settings_touchpad_mode"; // The touchpad gesture mode. (0 = spots, 1 = pointer)
    public static final String DEVICE_SETTINGS_TOUCHPAD_ENABLED = "device_settings_touchpad_enabled";

    public static final String SYSTEMUI_BATTERY_BASECONF = "kk_ui_battery_";
    public static final String SYSTEMUI_BATTERY_ICON = "kk_ui_battery_icon";
    public static final String SYSTEMUI_BATTERY_TEXT = "kk_ui_battery_text";
    public static final String SYSTEMUI_BATTERY_TEXT_PERCENT = "kk_ui_battery_text_percent";

    public static final String SYSTEMUI_CLOCK_TIME = "kk_ui_clock_time";
    public static final String SYSTEMUI_CLOCK_DATE = "kk_ui_clock_date";
    
    
    public static final String SYSTEMUI_RECENTS_KILLALL_BUTTON = "kk_ui_recents_killall_button";
    public static final String SYSTEMUI_RECENTS_MEM_DISPLAY = "kk_ui_recents_mem_display";


    public static final String SYSTEMUI_SOFTKEY_BACK = "systemui_softkey_back";
    public static final String SYSTEMUI_SOFTKEY_HOME = "systemui_softkey_home";
    public static final String SYSTEMUI_SOFTKEY_RECENT = "systemui_softkey_recent";
    public static final String SYSTEMUI_SOFTKEY_MENU = "systemui_softkey_menu";    

    public static final String INPUTMETHOD_SHOWNOTIFICATION = "inputmethod_shownotification";
  }

  // Intents
  public final class I {

    public static final String CMD = "cmd";

    public static final String UI_CHANGED = "intent_ui_changed";

    public static final String CMD_BARTYPE_CHANGED = "bar_type_changed";
    public static final String CMD_BARSIZE_CHANGED = "bar_size_changed";
    public static final String EXTRA_RESTART_SYSTEMUI = "restart_systemui";
  }
  
  //Actions
  public final class A {
	    public static final String SYSTEMUI_TASK_SCREENSHOT = "screenshot";
	    public static final String SYSTEMUI_TASK_SCREENOFF = "screenoff";
	    public static final String SYSTEMUI_TASK_KILL_PROCESS = "killcurrent";
	    public static final String SYSTEMUI_TASK_ASSIST = "assist";
//	    public static final String SYSTEMUI_TASK_POWER_MENU = "powermenu";
  }
}
