package org.meerkats.katkiss;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

  public KeyActions(Context context, String config) 
  { 
     if(context == null) {Log.w(TAG, "context is null"); return;}
     _context = context;
     refreshConfig(config);
  }
    

  // flagInt1:action1|action2;flagInt2:action3|action4
  private void refreshConfig(String config)
  {
    _flagsActionHandlerMap.clear();
    List<String> flagsActionsList = Arrays.asList(config.split(Pattern.quote(";")));
    for(String sModifierActions : flagsActionsList)
    {
      String[] flagActions = sModifierActions.split(Pattern.quote(":"));
      if(flagActions.length != 2) {Log.w(TAG, "No proper flagActions in " + sModifierActions); return;}
      
      Integer flag = Integer.parseInt(flagActions[0]);
      String sActions = flagActions[1];

       if(DEBUG) Log.v(TAG, "flag=" + flag + " sActions=" + sActions);
      _flagsActionHandlerMap.put(flag, new ActionHandler(_context, sActions));
    }
  }


  public boolean execute(KeyEvent keyEvent)
  {
    if(DEBUG) Log.d(TAG, "execute: " + keyEvent);
    if(keyEvent.getAction() != KeyEvent.ACTION_DOWN) return false;
    ActionHandler actionHandler = _flagsActionHandlerMap.get(keyEvent.getMetaState());
    if(actionHandler != null) { actionHandler.executeAllActions(); return true; }
    else return false;
  }
}
