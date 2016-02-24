/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.os.SystemProperties;


public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "BatteryController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final ArrayList<BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private final PowerManager mPowerManager;

    private int mLevel;
    private boolean mPluggedIn;
    private boolean mCharging;
    private boolean mCharged;
    private boolean mPowerSave;
    private Context mContext;
    private RefreshDockStatus mRefreshDockThread; 
    
    private boolean hasDockBattery = SystemProperties.getInt("ro.nodockbattery", 0) != 1;

    private class RefreshDockStatus extends Thread
    {
    	private final static String DOCK_STATUS_PATH = "/sys/class/power_supply/dock_battery/status";
    	private final static String DOCK_CAPACITY_PATH = "/sys/class/power_supply/dock_battery/capacity";
    	private final static int REFRESH = 15*1000;

    	private String _status = "";
    	private int _capacity = 0 ;
    	private String _prevStatus = "";
    	private int _prevCapacity = 0 ;
    	private boolean _present = false ;
    	
    	private int readSysIntVal(String sysPath) throws IOException
    	{
			String val = readSysStringVal(sysPath);
			if(val == null) return -1;
			else return Integer.parseInt(val);
    	}

    	private String readSysStringVal(String sysPath) throws IOException
    	{
			BufferedReader reader = new BufferedReader(new FileReader(sysPath));
			String val = reader.readLine();
			reader.close();
			return val;
    	}
    	
    	private void refresh()
    	{
    		try 
    		{
    			_capacity = readSysIntVal(DOCK_CAPACITY_PATH);
    			_status = readSysStringVal(DOCK_STATUS_PATH);
    			_present = true;
    		}
    		catch(Exception e )
    		{_present = false;}
    		if(!_present) _capacity = -1;
    	}

    	private void postUpdateIfNeeded() { postUpdate(false); }

    	public void postUpdate(boolean force)
    	{
    		
    		if(!force && _prevStatus == _status && _prevCapacity == _capacity  || mContext == null) return;
    		
    		Intent batteryChangedIntent = new Intent(Intent.ACTION_DOCK_BATTERY_CHANGED);
    		batteryChangedIntent.putExtra(BatteryManager.EXTRA_LEVEL, _capacity);
    		batteryChangedIntent.putExtra(BatteryManager.EXTRA_PLUGGED, _status.equals("Charging")?1:0);
    		batteryChangedIntent.putExtra(BatteryManager.EXTRA_PRESENT, _present);
    		mContext.sendBroadcast(batteryChangedIntent);
    		
    		_prevStatus = _status; 
    		_prevCapacity = _capacity;
    	}

    	
    	@Override
    	public void run() {
    		while(true)
    		{
    			refresh();
    			postUpdateIfNeeded();

	    		try { Thread.sleep(REFRESH);} 
	    		catch (InterruptedException e) {}
    		}
    	}
    }
    

    public BatteryController(Context context) {
    	mContext = context;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING);
        context.registerReceiver(this, filter);
        updatePowerSave();
    	mRefreshDockThread = new RefreshDockStatus();
	if(hasDockBattery) mRefreshDockThread.start();
	else mRefreshDockThread.postUpdate(true); // Force update once if no dock Battery to hide it (SL101..)
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BatteryController state:");
        pw.print("  mLevel="); pw.println(mLevel);
        pw.print("  mPluggedIn="); pw.println(mPluggedIn);
        pw.print("  mCharging="); pw.println(mCharging);
        pw.print("  mCharged="); pw.println(mCharged);
        pw.print("  mPowerSave="); pw.println(mPowerSave);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
        cb.onBatteryLevelChanged(mLevel, mPluggedIn, mCharging);
    }

    public void removeStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            mLevel = (int)(100f
                    * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            mPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

            final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            mCharged = status == BatteryManager.BATTERY_STATUS_FULL;
            mCharging = mCharged || status == BatteryManager.BATTERY_STATUS_CHARGING;

            fireBatteryLevelChanged();
        } else if (action.equals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)) {
            updatePowerSave();
        } else if (action.equals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING)) {
            setPowerSave(intent.getBooleanExtra(PowerManager.EXTRA_POWER_SAVE_MODE, false));
        } else if (action.equals(Intent.ACTION_DOCK_EVENT)) {
           mRefreshDockThread.interrupt(); 
        }
    }

    public boolean isPowerSave() {
        return mPowerSave;
    }

    private void updatePowerSave() {
        setPowerSave(mPowerManager.isPowerSaveMode());
    }

    private void setPowerSave(boolean powerSave) {
        if (powerSave == mPowerSave) return;
        mPowerSave = powerSave;
        if (DEBUG) Log.d(TAG, "Power save is " + (mPowerSave ? "on" : "off"));
        firePowerSaveChanged();
    }

    private void fireBatteryLevelChanged() {    	    	
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onBatteryLevelChanged(mLevel, mPluggedIn, mCharging);
        }
    }

    private void firePowerSaveChanged() {
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onPowerSaveChanged();
        }
    }

    public interface BatteryStateChangeCallback {
        void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging);
        void onPowerSaveChanged();
    }
}
