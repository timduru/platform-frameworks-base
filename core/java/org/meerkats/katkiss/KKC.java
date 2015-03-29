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

    public static final String SYSTEMUI_WALLPAPER_MODE = "kk_ui_wallpaper_mode";
    public static final int WALLPAPER_MODE_NORMAL = 0;
    public static final int WALLPAPER_MODE_DISABLE_SYSTEM = 1;
    public static final int WALLPAPER_MODE_DISABLE_ALL = 2;


    public static final String DEVICE_SETTINGS_TOUCHPAD_MODE = "device_settings_touchpad_mode"; // The touchpad gesture mode. (0 = spots, 1 = pointer)
    public static final String DEVICE_SETTINGS_TOUCHPAD_ENABLED = "device_settings_touchpad_enabled";
    public static final String DEVICE_SETTINGS_RIGHTCLICK_MODE = "device_settings_rightclick_mode";

    public static final String SYSTEMUI_BATTERY_BASECONF = "kk_ui_battery_";
    public static final String SYSTEMUI_BATTERY_ICON = "kk_ui_battery_icon";
    public static final String SYSTEMUI_BATTERY_TEXT = "kk_ui_battery_text";
    public static final String NATIVE_BATTERY_TEXT_ON_ICON = "status_bar_show_battery_percent";
    public static final String SYSTEMUI_BATTERY_TEXT_PERCENT = "kk_ui_battery_text_percent";
    
    public static final String SYSTEMUI_CLOCK_TIME = "kk_ui_clock_time";
    public static final String SYSTEMUI_CLOCK_DATE = "kk_ui_clock_date";
    
    public static final String SYSTEMUI_BTN_SWITCH_TOPREVIOUS = "kk_ui_btn_switch_toprevious";
    public static final String SYSTEMUI_BTN_SPLITVIEW_AUTO = "kk_ui_btn_splitview_auto";
    public static final String SYSTEMUI_BTN_RELAUNCH_FLOATING = "kk_ui_btn_relaunch_floating";

    
    public static final String SYSTEMUI_RECENTS_KILLALL_BUTTON = "kk_ui_recents_killall_button";
    public static final String SYSTEMUI_RECENTS_MEM_DISPLAY = "kk_ui_recents_mem_display";
    public static final String SYSTEMUI_RECENTS_MULTIWINDOW_ICONS = "kk_ui_recents_multiwindow_icons";


    public static final String SYSTEMUI_SOFTKEY_BACK = "systemui_softkey_back";
    public static final String SYSTEMUI_SOFTKEY_HOME = "systemui_softkey_home";
    public static final String SYSTEMUI_SOFTKEY_RECENT = "systemui_softkey_recent";
    public static final String SYSTEMUI_SOFTKEY_MENU = "systemui_softkey_menu";    

    public static final String INPUTMETHOD_SHOWNOTIFICATION = "inputmethod_shownotification";

    public static final String QUICK_SETTINGS_TILES = "quick_settings_tiles";
    public static final String QUICK_SETTINGS_RIBBON_TILES = "quick_settings_ribbon_tiles";
    public static final String QS_QUICK_ACCESS = "qs_quick_access";
    public static final String QS_QUICK_ACCESS_LINKED = "qs_quick_access_linked";

    public static final String QS_DYNAMIC_WIFI = "qs_dynamic_wifi";
    public static final String QS_DYNAMIC_ALARM = "qs_dynamic_alarm";
    public static final String QS_QUICK_PULLDOWN = "qs_quick_pulldown";
    public static final String QS_COLLAPSE_PANEL = "qs_collapse_panel";

    public static final String EXPANDED_VIEW_WIDGET = "expanded_view_widget";

    public static final String ENABLE_PANELS_DROPSHADOW = "enable_panels_dropshadow";
    
    
    public static final String ADB_PORT = "adb_port";
    public static final String HDMI_MODE = "hdmi_mode";

    public static final String KEYS_OVERRIDE = "keys_override";
    public static final String KEYS_OVERRIDE_PREFIX = "keys_override_";

    public static final String USER_IMMERSIVE_MODE = "user_immersive_mode";
    public static final String USER_IMMERSIVE_MODE_TYPE = "user_immersive_mode_type"; // 0 = full, 1=status only, 2=navonly
    public static final String AUTO_EXPANDED_DESKTOP_ONDOCK = "auto_expanded_desktop_ondock";
	public static final String SYSTEMUI_AUDIOADJUST_SOUND = "systemui_audioadjust_sound";

  }


  // Intents
  public final class I {

    public static final String CMD = "cmd";

    public static final String UI_CHANGED = "intent_ui_changed";
    public static final String GLOBAL_ACTIONS = "intent_global_actions";

    public static final String CMD_BARTYPE_CHANGED = "bar_type_changed";
    public static final String CMD_BARSIZE_CHANGED = "bar_size_changed";
    public static final String CMD_REBOOT = "reboot";
    public static final String EXTRA_RESTART_SYSTEMUI = "restart_systemui";

  }
  
  //Actions
  public final class A {
	    public static final String SYSTEMUI_TASK_SCREENSHOT = "screenshot";
	    public static final String SYSTEMUI_TASK_SCREENOFF = "screenoff";
	    public static final String SYSTEMUI_TASK_KILL_PROCESS = "killcurrent";
	    public static final String SYSTEMUI_TASK_ASSIST = "assist";
	    public static final String SYSTEMUI_RECENT = "recent";
	    public static final String SYSTEMUI_SWITCH_TOPREVIOUS_TASK = "switch_toprevious_task";
	    public static final String SPLITVIEW_AUTO = "splitview_auto";
	    public static final String SPLITVIEW_1 = "splitview_1";
	    public static final String SPLITVIEW_2 = "splitview_2";
	    public static final String EXPANDED_DESKTOP = "expanded_desktop";
	    public static final String EXPANDED_DESKTOP_KEEPSTATUSBAR = "expanded_desktop_keepstatusbar";
	    public static final String SHOW_NOTIFICATIONS_PANEL = "show_notifications_panel";
	    public static final String SHOW_SETTINGS_PANEL = "show_settings_panel";
	    public static final String SENDKEY_BASE = "sendkey_";
	    public static final String AUTOROTATION_TOGGLE = "autorotation";
            public static final String SHOW_POWERMENU = "show_powermenu";

            public static final String ETHERNET_TOGGLE = "ethernet_toggle";
            public static final String WIFI_TOGGLE = "wifi_toggle";
            public static final String BLUETOOTH_TOGGLE = "bluetooth_toggle";
            public static final String TOUCHPAD_TOGGLE = "touchpad_toggle";
            public static final String LAUNCH_SETTINGS = "launch_settings";
            public static final String BRIGHTNESS_DOWN = "brightness_down";
            public static final String BRIGHTNESS_UP = "brightness_up";
            public static final String BRIGHTNESS_AUTO = "brightness_auto";
            public static final String MEDIA_PREVIOUS = "media_previous";
            public static final String MEDIA_NEXT = "media_next";
            public static final String MEDIA_PLAYPAUSE = "media_playpause";
            public static final String AUDIO_DOWN = "audio_down";
            public static final String AUDIO_UP = "audio_up";
            public static final String AUDIO_MUTE = "audio_mute";

            public static final String RELAUNCH_FLOATING = "relaunch_floating";
            public static final String SHOW_HIDE_ALL_FLOATING = "show_hide_all_floating";
	    	    
//	    public static final String SYSTEMUI_TASK_POWER_MENU = "powermenu";
  }
}
