package org.meerkats.katkiss;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.os.SystemProperties;

public class KatUtils {
    public static String[] HDMIModes = {"center", "crop", "scale"};

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
}
