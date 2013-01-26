
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
import java.io.InputStreamReader;
import java.util.List;

public final class EOSUtils {
    public static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    public static final String CONTROLCENTERNS = "http://schemas.android.com/apk/res/org.eos.controlcenter";
    public static final String URI_GRAVEYARD = "eos_graveyard_uri";

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
