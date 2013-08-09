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
  }

  // Intents
  public final class I {

    public static final String CMD = "cmd";

    public static final String UI_CHANGED = "intent_ui_changed";
    public static final String CMD_BARTYPE_CHANGED = "bar_type_changed";
    public static final String EXTRA_RESTART_SYSTEMUI = "restart_systemui";
  }
}
