
package org.teameos.jellybean.settings;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EOSConstants {
    /* EOS SETTINGS STRINGS */
    /**
     * @hide
     */
    public static final String SYSTEMUI_HIDE_BARS = "eos_systemui_hide_bars";

    /**
     * @hide
     */
    public static final int SYSTEMUI_HIDE_BARS_DEF = 0;

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_ICON_VISIBLE = "eos_systemui_battery_icon_visible";

    /**
     * @hide
     */
    public static final int SYSTEMUI_BATTERY_ICON_VISIBLE_DEF = 1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_TEXT_VISIBLE = "eos_systemui_battery_text_visible";

    /**
     * @hide
     */
    public static final int SYSTEMUI_BATTERY_TEXT_VISIBLE_DEF = 1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_PERCENT_VISIBLE = "eos_systemui_battery_percent_visible";

    /**
     * @hide
     */
    public static final int SYSTEMUI_BATTERY_PERCENT_VISIBLE_DEF = 1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_PERCENT_TAG = "eos_systemui_battery_percent_tag";

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_TEXT_COLOR = "eos_systemui_battery_text_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_BATTERY_TEXT_COLOR_DEF = -1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_VISIBLE = "eos_systemui_clock_visible";

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_VISIBLE_DEF = 1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_COLOR = "eos_systemui_clock_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_COLOR_DEF = -1;

    /**
     * @hide
     */    
    public static final String SYSTEMUI_CLOCK_AMPM = "eos_systemui_clock_ampm";

    /**
     * @hide
     */    
    public static final String SYSTEMUI_NAVBAR_DISABLE_GESTURE = "eos_systemui_navbar_disable_gesture";

    /**
     * @hide
     */    
    public static final int SYSTEMUI_NAVBAR_DISABLE_GESTURE_DEF = 0;

    /**
     * @hide
     */
    public static final int SYSTEMUI_NAVBAR_SOFTKEY_DEFAULT = -1;

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_AMPM_DEF = 2;

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_ENABLED = "eos_systemui_settings_enabled";

    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_ENABLED_DEF = 0;

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVBAR_COLOR = "eos_systemui_navbar_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_NAVBAR_COLOR_DEF = 0xFF000000;

    /**
     * @hide
     */
    public static final String SYSTEMUI_STATUSBAR_COLOR = "eos_systemui_statusbar_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_STATUSBAR_COLOR_DEF = 0xFF000000;

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVKEY_COLOR = "eos_systemui_navkey_color";

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVGLOW_COLOR = "eos_systemui_navglow_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_SOFTKEY_BACK_ID = 0;

    /**
     * @hide
     */
    public static final int SYSTEMUI_SOFTKEY_HOME_ID = 1;

    /**
     * @hide
     */
    public static final int SYSTEMUI_SOFTKEY_RECENT_ID = 2;

    /**
     * @hide
     */
    public static final int SYSTEMUI_SOFTKEY_MENU_ID = 3;

    /**
     * @hide
     */
    public static final int SYSTEMUI_NAVKEY_COLOR_DEF = -1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_BACK = "eos_systemui_softkey_back";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_HOME = "eos_systemui_softkey_home";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_RECENT = "eos_systemui_softkey_recent";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_MENU = "eos_systemui_softkey_menu";
    
    /**
     * hide
     */
    public static final String SYSTEMUI_TASK_SCREENSHOT = "screenshot";

    /**
     * hide
     */
    public static final String SYSTEMUI_TASK_SCREENOFF = "screenoff";

    /**
     * hide
     */
    public static final String SYSTEMUI_TASK_KILL_PROCESS = "killcurrent";

    /**
     * hide
     */
    public static final String SYSTEMUI_SOFTKEY_MENU_PERSIST = "eos_systemui_softkey_persist";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_ENABLED_CONTROLS = "eos_systemui_settings_enabled_controls";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_PHONE_TOP = "eos_systemui_settings_phone_top";

    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_PHONE_TOP_DEF = 0;

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_PHONE_BOTTOM = "eos_systemui_settings_phone_bottom";

    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_PHONE_BOTTOM_DEF = 0;

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_AIRPLANE = "eos_systemui_settings_airplane";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_AUTO_ROTATE = "eos_systemui_settings_autorotate";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_BLUETOOTH = "eos_systemui_settings_bluetooth";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_GPS = "eos_systemui_settings_gps";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_NOTIFICATIONS = "eos_systemui_settings_notifications";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_SILENT = "eos_systemui_settings_silent";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_TORCH = "eos_systemui_settings_torch";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_WIFI = "eos_systemui_settings_wifi";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_MOBILEDATA = "eos_systemui_settings_mobiledata";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_LTE = "eos_systemui_settings_lte";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_WIFITETHER = "eos_systemui_settings_wifitether";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_USBTETHER = "eos_systemui_settings_usbtether";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_INDICATOR_HIDDEN = "eos_systemui_settings_indicator_hidden";

    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_INDICATOR_HIDDEN_DEF= 0;

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_INDICATOR_COLOR = "eos_systemui_settings_indicator_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_INDICATOR_COLOR_DEF = -1;

    /**
     * @hide
     */
    public static final String[] SYSTEMUI_SETTINGS_DEFAULTS = {
            SYSTEMUI_SETTINGS_WIFI,
            SYSTEMUI_SETTINGS_BLUETOOTH,
            SYSTEMUI_SETTINGS_GPS,
            SYSTEMUI_SETTINGS_AUTO_ROTATE,
            SYSTEMUI_SETTINGS_SILENT
    };

    /**
     * @hide
     */
    public static final Map<String, Boolean> getEosSystemUISettingsMap() {
        LinkedHashMap<String, Boolean> map = new LinkedHashMap<String, Boolean>();
        map.put(SYSTEMUI_SETTINGS_AIRPLANE, false);
        map.put(SYSTEMUI_SETTINGS_AUTO_ROTATE, true);
        map.put(SYSTEMUI_SETTINGS_BLUETOOTH, true);
        map.put(SYSTEMUI_SETTINGS_GPS, true);
        map.put(SYSTEMUI_SETTINGS_NOTIFICATIONS, false);
        map.put(SYSTEMUI_SETTINGS_SILENT, true);
        map.put(SYSTEMUI_SETTINGS_TORCH, false);
        map.put(SYSTEMUI_SETTINGS_WIFI, true);
        map.put(SYSTEMUI_SETTINGS_MOBILEDATA, false);
        map.put(SYSTEMUI_SETTINGS_WIFITETHER, false);
        map.put(SYSTEMUI_SETTINGS_USBTETHER, false);
        map.put(SYSTEMUI_SETTINGS_LTE, false);

        return map;
    }

    /***
     * EOS Intent constants
     */

    /**
     * Broadcast Action: Request to turn Eos Torch application off
     * 
     * @hide
     */
    public static final String ACTION_TORCH_OFF = "android.intent.action.TORCH_OFF";

    /**
     * @hide
     */
    public static final String INTENT_TELEPHONY_LTE_TOGGLE = "org.teameos.intent.action.TOGGLE_LTE";

    /**
     * @hide
     */
    public static final String INTENT_TELEPHONY_LTE_TOGGLE_KEY = "eos_intent_telephony_lte_mode_key";

    /**
     * @hide
     */
    public static final String INTENT_SYSTEMUI_KILL_SERVICE = "org.teameos.intent.action.KILL_SYSTEMUI";

    /**
     * @hide
     */
    public static final String INTENT_SYSTEMUI_REMOVE_BAR = "org.teameos.intent.action.REMOVE_BAR";

    /**
     * @hide
     */
    public static final String INTENT_SYSTEMUI_BAR_RESTORED = "org.teameos.intent.action.BAR_RESTORED";

    /**
     * @hide
     */
    public static final String INTENT_SETTINGS_RESTART_INTERFACE_SETTINGS = "org.teameos.intent.action.RESTART_EOS_INTERFACE";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_STANDARD_WIFI = "eos_systemui_settings_standard_wifi";
    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_STANDARD_WIFI_DEF = 1;
    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_STANDARD_AIRPLANE = "eos_systemui_settings_standard_airplane";
    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_STANDARD_AIRPLANE_DEF = 1;
    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_STANDARD_ROTATION = "eos_systemui_settings_standard_rotation";
    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_STANDARD_ROTATION_DEF = 1;
    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_STANDARD_VOLUME = "eos_systemui_settings_standard_volume";
    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_STANDARD_VOLUME_DEF = 0;
    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_STANDARD_BRIGHTNESS = "eos_systemui_settings_standard_brightness";
    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_STANDARD_BRIGHTNESS_DEF = 1;
    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_STANDARD_NOTIFICATIONS = "eos_systemui_settings_standard_notifications";
    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_STANDARD_NOTIFICATIONS_DEF = 1;
    /**
     * @hide
     */
    public static final String SYSTEMUI_SETTINGS_STANDARD_SETTINGS = "eos_systemui_settings_standard_settings";
    /**
     * @hide
     */
    public static final int SYSTEMUI_SETTINGS_STANDARD_SETTINGS_DEF = 1;
    /**
     * The touchpad gesture mode. (0 = spots, 1 = pointer)
     * @hide
     */
    public static final String DEVICE_SETTINGS_TOUCHPAD_MODE = "eos_device_settings_touchpad_mode";
    /**
     * Whether or not the touchpad is enabled. (0 = false, 1 = true)
     * @hide
     */
    public static final String DEVICE_SETTINGS_TOUCHPAD_STATUS = "eos_device_settings_touchpad_status";
    /**
     * Value for {@link #EOS_TOUCHPAD_STATUS} to use
     * the touchpad located on the hardware keyboard dock.
     * @hide
     */
    public static final int DEVICE_SETTINGS_TOUCHPAD_DISABLED = 0;
    /**
     * Value for {@link #EOS_TOUCHPAD_STATUS} to use
     * the touchpad located on the hardware keyboard dock.
     * @hide
     */
    public static final int DEVICE_SETTINGS_TOUCHPAD_ENABLED = 1;
    /**
     * On the grouper, we use this to switch between the hybrid UI and the tablet UI
     * @hide
     */
    public static final String SYSTEMUI_USE_TABLET_UI = "eos_systemui_tablet_ui";
    /**
     * @hide
     */
    public static final int SYSTEMUI_USE_TABLET_UI_DEF = 0;

    /**
     * @hide
     */
    public static final String SYSTEM_VOLUME_KEYS_SWITCH_ON_ROTATION = "eos_system_volume_keys_switch_on_rotation";

    /**
     * @hide
     */
    public static final int SYSTEM_VOLUME_KEYS_SWITCH_ON_ROTATION_DEF = 1;

    /**
     * @hide
     */
    public static final String SYSTEM_DEFAULT_VOLUME_STREAM = "eos_system_default_volume_stream";

    /**
     * @hide
     */
    public static final String SYSTEMUI_INTERFACE_ROTATIONLOCK_TOGGLE = "eos_interface_rotationlock_toggle";

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVRING_1 = "eos_systemui_navring_1";

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVRING_2 = "eos_systemui_navring_2";

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVRING_3 = "eos_systemui_navring_3";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_ASSIST = "assist";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_HIDE_BARS = "hidebars";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_POWER_MENU = "powermenu";

    /**
     * @hide
     */
    public static final String SYSTEMUI_RECENTS_KILLALL_BUTTON = "eos_interface_recents_killall_button";

    /**
     * @hide
     */
    public static final String SYSTEMUI_RECENTS_MEM_DISPLAY = "eos_interface_recents_mem_display";

    /**
     * @hide
     */
    public static final String NET_HOSTNAME = "eos_net_hostname";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SCREENSHOT_SCALE_INDEX = "eos_screenshot_index";

    /**
     * @hide
     */
    public static final String SYSTEMUI_BAR_SIZE_MODE = "eos_interface_bar_size_mode";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TABLET_BIG_CLEAR_BUTTON = "eos_interface_big_clear_button";
}
