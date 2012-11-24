package com.android.systemui.statusbar.preferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.provider.*;
import android.net.Uri;
import java.util.List;
import java.util.ArrayList;
import android.os.AsyncTask;
import com.android.systemui.statusbar.preferences.*;
import java.lang.Runnable;

  public class AdbController extends SettingsController 
  {
    private WifiManager mWifiManager;
    private String adbPort;

    public AdbController (Context context, View button) 
    {
      super (context, button);
      mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    protected int getPreferenceStatus()
    {
      return (SystemProperties.getInt("service.adb.tcp.port", -1) > 0) ? 1 : 0;
    }

    protected void setPreferenceStatus(final int status) 
    {
      if (status == 1)
        adbStart();
      else 
        adbStop();
    }

    protected boolean adbStart()
    {
      if (mWifiManager.isWifiEnabled()) // to check here.....
      {
        setAdbPort("5150");
        SystemProperties.set("service.adb.tcp.port", getAdbPort());
        SystemProperties.set("persist.service.adb.enable", "0");
        SystemProperties.set("persist.service.adb.enable", "1");
        String outputText = "Connect to " + getWifiIp()  + ":" + getAdbPort();
        updateController();
        Toast toast = Toast.makeText(mContext, outputText , Toast.LENGTH_LONG);
        toast.show();
      }
      else
      {
        Toast toast = Toast.makeText(mContext, (CharSequence) "Turn Wifi on before enabling Wireless ADB", Toast.LENGTH_LONG);
        toast.show();
        mPreferenceState=0;
      }
      return true;
    }

    protected boolean adbStop()
    {
      SystemProperties.set("service.adb.tcp.port", "-1");
      SystemProperties.set("persist.service.adb.enable", "0");
      SystemProperties.set("persist.service.adb.enable", "1");
      Toast toast = Toast.makeText(mContext, (CharSequence) "Disabling Adb Wireless Service", Toast.LENGTH_LONG);
      toast.show();
      mPreferenceState = 0;
      return true;
    }

    protected String getWifiIp()
    {
      int i = this.mWifiManager.getConnectionInfo().getIpAddress();
      String str = String.valueOf(i & 0xFF);
      StringBuilder localStringBuilder1 = new StringBuilder(str).append(".");
      int j = i >> 8 & 0xFF;
      StringBuilder localStringBuilder2 = localStringBuilder1.append(j).append(".");
      int k = i >> 16 & 0xFF;
      StringBuilder localStringBuilder3 = localStringBuilder2.append(k).append(".");
      int m = i >> 24 & 0xFF;
      StringBuilder localStringBuilder4 = localStringBuilder3.append(m);
      return localStringBuilder4.toString();
    }

    protected int getDrawableIcon() 
    {
      int status = mPreferenceState;
      switch (status) 
      {
        case 0: return 0x7f020106;
        case 1: return 0x7f020105;
      }
      return 0x0;
    }

    protected String getSettingsIntent() {
        return "android.settings.WIFI_SETTINGS";
    }

    public void setAdbPort(String i)
    {
      adbPort = i;
      return;
    }

    private String getAdbPort()
    {
      return adbPort;
    }
  }




