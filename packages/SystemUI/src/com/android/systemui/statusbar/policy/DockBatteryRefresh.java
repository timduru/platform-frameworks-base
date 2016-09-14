package com.android.systemui.statusbar.policy;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.os.SystemProperties;


    public class DockBatteryRefresh extends Thread
    {
    	private final static String DOCK_STATUS_PATH = "/sys/class/power_supply/dock_battery/status";
    	private final static String DOCK_CAPACITY_PATH = "/sys/class/power_supply/dock_battery/capacity";
    	private final static int REFRESH = 15*1000;

    	private String _status = "";
    	private int _capacity = 0 ;
    	private String _prevStatus = "";
    	private int _prevCapacity = 0 ;
    	private boolean _present = false ;
    	private BatteryControllerImpl _cb;
    	
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
    		
    		if(!force && _prevStatus == _status && _prevCapacity == _capacity  /*|| mContext == null*/) return;
    		
    		/*Intent batteryChangedIntent = new Intent(Intent.ACTION_DOCK_BATTERY_CHANGED);
    		batteryChangedIntent.putExtra(BatteryManager.EXTRA_LEVEL, _capacity);
    		batteryChangedIntent.putExtra(BatteryManager.EXTRA_PLUGGED, _status.equals("Charging")?1:0);
    		batteryChangedIntent.putExtra(BatteryManager.EXTRA_PRESENT, _present);
    		mContext.sendBroadcast(batteryChangedIntent);
*/
		if(_cb != null) _cb.onDockBatteryLevelChanged(_capacity, _present, _status.equals("Charging"));
    		
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
    
    public void setCB (BatteryControllerImpl cb) {_cb = cb;}

    public interface DockStatusChangeCB {
        void onDockBatteryLevelChanged(int level, boolean present, boolean charging);
    }
}
