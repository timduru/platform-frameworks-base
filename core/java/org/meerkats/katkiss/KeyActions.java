package org.meerkats.katkiss;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import android.util.Log;

import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import android.view.KeyEvent;
import java.util.regex.Pattern;


public class KeyActions 
{
  private static final String TAG = "KeyActions";
  private static final boolean DEBUG = KeyOverrideManager.DEBUG;

  private HashMap<Integer,ActionHandler> _flagsActionHandlerMap = new HashMap<Integer,ActionHandler>();
  private Context _context;
  private Integer _keyID;

  public KeyActions(Context context, Integer keyID)
  {
     if(context == null) {Log.w(TAG, "context is null"); return;}
     _keyID = keyID;
     _context = context;
  }

  public KeyActions(Context context, Integer keyID, String config) 
  { 
     if(context == null) {Log.w(TAG, "context is null"); return;}
     _keyID = keyID;
     _context = context;
     refreshConfig(config);
  }
    
  public Integer getID() { return _keyID;}

  public void initFromSettings()
  {
	String keyConf = Settings.System.getString(_context.getContentResolver(), KKC.S.KEYS_OVERRIDE_PREFIX + _keyID);
	if(keyConf == null || keyConf.equals("")) return;
	Log.d(TAG, "key="+_keyID + " keyConf=" + keyConf);
	refreshConfig(keyConf);
  }

  public void writeToSettings()
  {
	// Write key conf
	String conf = "";
	for(Entry<Integer,ActionHandler> flagAction : _flagsActionHandlerMap.entrySet()) 
	{
	    	Integer flag = flagAction.getKey();
		ActionHandler actionHandler = flagAction.getValue();
		if(!conf.equals("")) conf += ";";
		conf += "" + flag + "=" + actionHandler.getActionsString();
	}

	if(conf.equals("")) {deleteFromSettings(); return;}
	Settings.System.putString(_context.getContentResolver(), KKC.S.KEYS_OVERRIDE_PREFIX + _keyID, conf);
	

	// Add to global list if needed

	HashMap<Integer, KeyActions> keyOverrideMap = KatUtils.getKeyOverrideMap(_context);
//	if(keyOverrideMap != null && keyOverrideMap.containsKey(getID()) ) return; //already in list 

	conf = "";
	if(keyOverrideMap == null) keyOverrideMap = new HashMap<Integer, KeyActions> ();
	keyOverrideMap.put(getID(), this);

	KatUtils.writeKeyOverrideListToSettings(_context, keyOverrideMap);
  }

  public void deleteFromSettings()
  {
        Settings.System.putString(_context.getContentResolver(), KKC.S.KEYS_OVERRIDE_PREFIX + _keyID, null);

        // Remove from global list
        HashMap<Integer, KeyActions> keyOverrideMap = KatUtils.getKeyOverrideMap(_context);
        if(keyOverrideMap == null || (keyOverrideMap != null && !keyOverrideMap.containsKey(getID())) ) return; // not in list

        keyOverrideMap.remove(getID());

	KatUtils.writeKeyOverrideListToSettings(_context, keyOverrideMap);
  }


  // flagInt1=action1|action2;flagInt2=action3|action4
  private void refreshConfig(String config)
  {
    _flagsActionHandlerMap.clear();
    List<String> flagsActionsList = Arrays.asList(config.split(Pattern.quote(";")));
    for(String sModifierActions : flagsActionsList)
    {
      String[] flagActions = sModifierActions.split(Pattern.quote("="));
      if(flagActions.length != 2) {Log.w(TAG, "No proper flagActions in " + sModifierActions); continue;}
      
      Integer flag = Integer.parseInt(flagActions[0]);
      String sActions = flagActions[1];

       if(DEBUG) Log.v(TAG, "flag=" + flag + " sActions=" + sActions);
      _flagsActionHandlerMap.put(flag, new ActionHandler(_context, sActions));
    }
  }


  public void delFlag(Integer flag)
  {
      if(_flagsActionHandlerMap != null)
        _flagsActionHandlerMap.remove(flag);
  }

  public void addFlagAction(Integer flag, String action)
  {
	ActionHandler actionHandler = _flagsActionHandlerMap.get(flag);
	if(actionHandler == null) actionHandler = new ActionHandler(_context, action);
	else actionHandler.addAction(action);

      _flagsActionHandlerMap.put(flag, actionHandler);
  }

  public boolean execute(KeyEvent keyEvent)
  {
    if(DEBUG) Log.d(TAG, "execute: " + keyEvent);
    if(keyEvent.getAction() != KeyEvent.ACTION_DOWN) return false;
    ActionHandler actionHandler = _flagsActionHandlerMap.get(keyEvent.getMetaState());
    if(actionHandler != null) { actionHandler.executeAllActions(); return true; }
    else return false;
  }

  public String toString()
  {
	String res = "";
        for(Entry<Integer,ActionHandler> flagAction : _flagsActionHandlerMap.entrySet())
        {
			if(!res.equals("")) res += "\n";
	
			String sFlag = "";
	                Integer flag = flagAction.getKey();
			if((flag & KeyEvent.META_SHIFT_LEFT_ON) !=0) sFlag += " +ShiftL";
			if((flag & KeyEvent.META_SHIFT_RIGHT_ON) !=0) sFlag += " +ShiftR";
			if((flag & KeyEvent.META_CTRL_LEFT_ON) !=0) sFlag += " +CtrlL";
			if((flag & KeyEvent.META_CTRL_RIGHT_ON) !=0) sFlag += " +CtrlR";
			if((flag & KeyEvent.META_ALT_LEFT_ON) !=0) sFlag += " +AltL";
			if((flag & KeyEvent.META_ALT_RIGHT_ON) !=0) sFlag += " +AltR";
	
			res += sFlag;
	
			// Actions format
	        ActionHandler actionHandler = flagAction.getValue();
	        List<String> actions =  actionHandler.getActions();
	        res += " => " ;
	        String sActions = "";
	        for(String action: actions)
	        {	        	
	        	if(!sActions.equals("")) sActions += " + ";
	        	
	        	KatUtils.AppInfo appInfo = KatUtils.getAppInfoFromUri(_context, action);
	        	if(appInfo != null && appInfo.appName != null)
	                sActions += appInfo.appName;
	        	else 
	        		sActions +=  action;
	        }
	        res += sActions;
        }
	return res;
  }
}
