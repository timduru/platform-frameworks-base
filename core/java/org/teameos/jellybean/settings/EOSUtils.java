
package org.teameos.jellybean.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.IWindowManager;

import com.android.internal.telephony.RILConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public final class EOSUtils {
    public static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    public static final String CONTROLCENTERNS = "http://schemas.android.com/apk/res/org.eos.controlcenter";
    public static final String URI_GRAVEYARD = "eos_graveyard_uri";

    private static final String S2W_PATH = "/sys/android_touch/sweep2wake";
    private static final String FFC_PATH = "/sys/kernel/fast_charge/force_fast_charge";

    // 10 inch tablets
    public static boolean isXLargeScreen() {
        int screenLayout = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    // 7 inch "phablets" i.e. grouper
    public static boolean isLargeScreen() {
        int screenLayout = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    // normal phones
    public static boolean isNormalScreen() {
        int screenLayout = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_NORMAL;
    }

    public static boolean isLandscape(Context context) {
        return Configuration.ORIENTATION_LANDSCAPE
                == context.getResources().getConfiguration().orientation;
    }

    public static boolean hasTorch() {
        Camera mCamera = null;
        Parameters parameters;
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            parameters = mCamera.getParameters();
            List<String> flashModes = parameters.getSupportedFlashModes();
            if (flashModes.contains(Parameters.FLASH_MODE_TORCH))
                return true;
        } catch (RuntimeException e) {
            Log.i("EosInterfaceSettings",
                    "Unable to acquire camera or failed to check if device is Torch capable");
        } finally {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }
        return false;
    }

    public static boolean hasData(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean isCdma(Context context) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return (tm.getCurrentPhoneType() == TelephonyManager.PHONE_TYPE_CDMA);
    }

    public static boolean isCdmaLTE(Context context) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLteOnCdmaMode() == RILConstants.LTE_ON_CDMA_TRUE;
    }

    public static boolean hasNavBar(Context context) {
        boolean mHasNavBar = false;
        IWindowManager mWindowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            mHasNavBar = mWindowManager.hasNavigationBar();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return mHasNavBar;
    }

    public static boolean hasSweep2Wake() {
        return new File(S2W_PATH).exists();
    }

    public static boolean isSweep2WakeEnabled() {
        String currentVal;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(S2W_PATH)));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            i = reader.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = reader.read();
            }
            reader.close();
            currentVal = byteArrayOutputStream.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            currentVal = "0";
            e.printStackTrace();
        }
        if (currentVal.equals("0")) {
            return false;
        } else if (currentVal.equals("1")) {
            return true;
        } else {
            return false;
        }
    }

    public static void setSweep2WakeEnabled(boolean enabled) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(S2W_PATH)));
            String output = "" + (enabled ? "1" : "0");
            writer.write(output.toCharArray(), 0, output.toCharArray().length);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void toggleSweep2Wake() {
        setSweep2WakeEnabled(!isSweep2WakeEnabled());
    }

    public static boolean hasFastCharge() {
        return new File(FFC_PATH).exists();
    }

    public static boolean isFastChargeEnabled() {
        String currentVal;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(FFC_PATH)));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            i = reader.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = reader.read();
            }
            reader.close();
            currentVal = byteArrayOutputStream.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            currentVal = "0";
            e.printStackTrace();
        }
        if (currentVal.equals("0")) {
            return false;
        } else if (currentVal.equals("1")) {
            return true;
        } else {
            return false;
        }
    }

    public static void setFastChargeEnabled(boolean enabled) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(FFC_PATH)));
            String output = "" + (enabled ? "1" : "0");
            writer.write(output.toCharArray(), 0, output.toCharArray().length);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void toggleFastCharge() {
        setFastChargeEnabled(!isFastChargeEnabled());
    }

    public static String getDevice() {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/getprop ro.goo.board");

            BufferedReader mBufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = mBufferedReader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            mBufferedReader.close();
            process.waitFor();

            return output.toString().trim();
        } catch (Exception e) {
            return "error getting device name";
        }
    }
}
