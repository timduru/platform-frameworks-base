package org.meerkats.katkiss;

import java.util.ArrayList;
import android.util.Log;

import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

public class CustomObserver extends ContentObserver
{
  private static final String TAG = "CustomObserver";


  private Context _context;
  private ContentResolver _resolver;
  private ChangeNotification _changeCallback;


  public CustomObserver(Context context) { super(null); init(context); }
  public CustomObserver(Context context, ChangeNotification callback) 
  { 
    //TODO use custom threaded handler
    super(new Handler());
    init(context); 
    setCallback(callback); 
    startObserving();
  }

  public void setCallback(ChangeNotification callback) { _changeCallback = callback; } 

  private void init(Context context)
  {
     if(context == null) {Log.w(TAG, "context is null"); return;}

     _context = context;
     _resolver = context.getContentResolver();
  }

  public interface ChangeNotification
  {
    public ArrayList<Uri> getObservedUris();
    public void onChangeNotification(Uri uri);
  }

  public void startObserving()
  {
    ArrayList<Uri> uris = _changeCallback.getObservedUris();
    if (uris == null) return;
    for (Uri uri : uris)
    {
      Log.d(TAG, "observing "+uri.toString());
      _resolver.registerContentObserver( uri, true, this);
    }
  }
  
  public void stopObserving()
  {
    _resolver.unregisterContentObserver(this);
  }

  @Override
  public void onChange(boolean selfChange) { this.onChange(selfChange, null); }     

  @Override
  public void onChange(boolean selfChange, Uri uri) 
  {
     Log.d(TAG, "onChange "+uri.toString());
     if(_changeCallback == null) {Log.w(TAG, "_changeCallback is null"); return;}

    _changeCallback.onChangeNotification(uri);  
  }     

}
