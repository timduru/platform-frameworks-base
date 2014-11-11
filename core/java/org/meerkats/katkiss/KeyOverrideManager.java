package org.meerkats.katkiss;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.util.Log;

import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.KeyEvent;
import java.util.regex.Pattern;



public class KeyOverrideManager implements CustomObserver.ChangeNotification
{
  private static final String TAG = "KeyOverrideManager";
  public static final boolean DEBUG = true;

  private CustomObserver _confObserver;
  private HashMap<Integer, KeyActions>  _keyOverrideMap;
  private Context _context;

  public KeyOverrideManager(Context context) { init(context); }

  private void init(Context context)
  {
     if(context == null) {Log.w(TAG, "context is null"); return;}

     _context = context;
     _confObserver = new CustomObserver(context, this);
     refreshKeysConf();
  }

 // CustomObserver ChangeNotifications
  @Override
  public ArrayList<Uri> getObservedUris()
  {
    ArrayList<Uri> uris = new  ArrayList<Uri>();
    uris.add(Settings.System.getUriFor(KKC.S.KEYS_OVERRIDE));
    return uris;
  }

  @Override
  public void onChangeNotification(Uri uri)
  {
    Log.d(TAG, "onChangeNotification:" + uri);
    refreshKeysConf();
  }

  private void refreshKeysConf()
  {
    _keyOverrideMap = KatUtils.getKeyOverrideMap(_context);
    Log.d(TAG, "_keyOverrideMap:" + _keyOverrideMap);
  }

  public boolean executeOverrideIfNeeded(KeyEvent keyEvent)
  {
//    if(DEBUG) Log.d(TAG, "_keyOverrideMap:" + _keyOverrideMap);
//   if(DEBUG) Log.d(TAG, "keyEvent=" + keyEvent.getKeyCode() +":" + keyEvent.getMetaState());

    if(_keyOverrideMap == null || keyEvent == null) return false;

    KeyActions action = _keyOverrideMap.get(keyEvent.getKeyCode());
    if(action != null) return action.execute(keyEvent);
    else return false;
  }

}
