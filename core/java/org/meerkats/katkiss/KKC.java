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

    public static final String SYSTEMUI_BATTERY_PERCENT_TAG = "systemui_battery_percent_tag";
    public static final String SYSTEMUI_BATTERY_PERCENT_VISIBLE = "systemui_battery_percent_visible";

    public static final String SYSTEMUI_RECENTS_KILLALL_BUTTON = "systemui_recents_killall_button";
    public static final String SYSTEMUI_RECENTS_MEM_DISPLAY = "systemui_recents_mem_display";


  }

  // Intents
  public final class I {

    public static final String CMD = "cmd";

    public static final String UI_CHANGED = "intent_ui_changed";

    public static final String CMD_BARTYPE_CHANGED = "bar_type_changed";
    public static final String CMD_BARSIZE_CHANGED = "bar_size_changed";
    public static final String EXTRA_RESTART_SYSTEMUI = "restart_systemui";
  }
}
