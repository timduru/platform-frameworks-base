
package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerImpl;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.KeyButtonView;

import org.teameos.jellybean.settings.EOSConstants;
import org.teameos.jellybean.settings.ActionHandler;

import java.util.ArrayList;
import java.util.Arrays;

public class EosUiController extends ActionHandler {
    static final String TAG = "NavigationAreaController";

    static final int BACK_KEY = com.android.systemui.R.id.back;
    static final int HOME_KEY = com.android.systemui.R.id.home;
    static final int RECENT_KEY = com.android.systemui.R.id.recent_apps;
    static final int MENU_KEY = com.android.systemui.R.id.menu;

    // we'll cheat just a little to help with the two navigation bar views
    static final int NAVBAR_ROT_90 = com.android.systemui.R.id.rot90;
    static final int NAVBAR_ROT_0 = com.android.systemui.R.id.rot0;

    static final String BACK_KEY_URI_TAG = EOSConstants.SYSTEMUI_SOFTKEY_BACK;
    static final String HOME_KEY_URI_TAG = EOSConstants.SYSTEMUI_SOFTKEY_HOME;
    static final String RECENT_KEY_URI_TAG = EOSConstants.SYSTEMUI_SOFTKEY_RECENT;
    static final String MENU_KEY_URI_TAG = EOSConstants.SYSTEMUI_SOFTKEY_MENU;
    static final int BACK_KEY_LOCATION = 0;
    static final int HOME_KEY_LOCATION = 1;
    static final int RECENT_KEY_LOCATION = 2;
    static final int MENU_KEY_LOCATION = 3;

    private static boolean DEBUG = false;

    private ArrayList<SoftKeyObject> mSoftKeyObjects;

    private Context mContext;
    private View mStatusBarView;
    private View mStatusBarContainer;
    private View mRootView;
    private ContentResolver mResolver;
    private SettingsObserver mObserver;
    private ContentObserver mHideBarObserver;
    private ContentObserver mBatterySettingsObserver;
    private IWindowManager wm;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mSystemBarParams;
    private WindowManager.LayoutParams mStatusBarParams;
    private BatteryController mBatteryController;
    private boolean mHasNavBar = false;
    private boolean mHasStatusBar = false;
    private boolean mHasSystemBar = false;
    private boolean mIsHybridUiDevice = false;

    public EosUiController(Context context) {
        super(context);
        mContext = context;
        mResolver = mContext.getContentResolver();
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        wm = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));

        try {
            mHasNavBar = wm.hasNavigationBar();
            mHasSystemBar = wm.hasSystemNavBar();
            mHasStatusBar = !mHasSystemBar;
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // no matter what, we set make bar visibility true on boot
        Settings.System.putInt(mResolver, EOSConstants.SYSTEMUI_HIDE_BARS,
                EOSConstants.SYSTEMUI_HIDE_BARS_DEF);

        mHideBarObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateBarVisibility();
            }
        };     

        mBatterySettingsObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                processBatterySettingsChange();
            }
        };
        
        mResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE), false,
                mBatterySettingsObserver);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE), false,
                mBatterySettingsObserver);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR), false,
                mBatterySettingsObserver);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_BATTERY_PERCENT_VISIBLE), false,
                mBatterySettingsObserver);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_HIDE_BARS), false,
                mHideBarObserver);

        mIsHybridUiDevice = mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_isHybridUiDevice);
        if (mIsHybridUiDevice) {
            // only initialize if hybrid ui
            IntentFilter filter = new IntentFilter();
            filter.addAction(EOSConstants.INTENT_SETTINGS_RESTART_INTERFACE_SETTINGS);
            EosInterfaceReceiver mEosInterfaceReceiver = new EosInterfaceReceiver();
            mContext.registerReceiver(mEosInterfaceReceiver, filter);
        }
    }
    
    public void setBatteryController(BatteryController bc) {
    	mBatteryController = bc;
    }

    class EosInterfaceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            // give a slight delay to ensure eos interface fragment is
            // dead
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mContext.unregisterReceiver(EosInterfaceReceiver.this);
                    Intent i = new Intent("android.settings.EOS_INTERFACE");
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(i);
                }
            }, 250);
        }
    }

    // we need this to be set when the theme engine creates new view
    public void setStatusBar(View container) {
        mStatusBarView = container;

        // only register if device has statusbar
        mResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_STATUSBAR_COLOR), false,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        updateStatusBarColor();
                    }
                });
        updateStatusBarColor();
        processBatterySettingsChange();
    }

    // we need this to add and remove the view from the windowmanager
    public void setStatusBarContainer(View container, WindowManager.LayoutParams lp) {
        mStatusBarContainer = container;
        mStatusBarParams = lp;
    }

    public void setRootContainer(View container, WindowManager.LayoutParams lp) {
        mRootView = container;
        mSystemBarParams = lp;

        // register here instead. Why register in constructor if device
        // does not have systembar/navbar ie crespo
        mResolver.registerContentObserver(
                Settings.System.getUriFor(EOSConstants.SYSTEMUI_NAVBAR_COLOR), false,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        updateSystemBarColor();
                    }
                });

        mSoftKeyObjects = new ArrayList<SoftKeyObject>();
        mObserver = new SettingsObserver(H);

        initSoftKeys();
        updateSystemBarColor();
        if (mHasSystemBar) processBatterySettingsChange();
    }

    static void log(String s) {
        if (DEBUG)
            Log.i(TAG, s);
    }

    void loadBackKey(ArrayList<View> parent) {
        SoftKeyObject back = new SoftKeyObject();
        back.setSoftKey(BACK_KEY,
                BACK_KEY_LOCATION,
                BACK_KEY_URI_TAG,
                new SoftkeyLongClickListener(BACK_KEY_LOCATION),
                parent);
        mSoftKeyObjects.add(back);
    }

    void loadHomeKey(ArrayList<View> parent) {
        SoftKeyObject home = new SoftKeyObject();
        home.setSoftKey(HOME_KEY,
                HOME_KEY_LOCATION,
                HOME_KEY_URI_TAG,
                new SoftkeyLongClickListener(HOME_KEY_LOCATION),
                parent);
        mSoftKeyObjects.add(home);
    }

    void loadRecentKey(ArrayList<View> parent) {
        SoftKeyObject recent = new SoftKeyObject();
        recent.setSoftKey(RECENT_KEY,
                RECENT_KEY_LOCATION,
                RECENT_KEY_URI_TAG,
                new SoftkeyLongClickListener(RECENT_KEY_LOCATION),
                parent);
        mSoftKeyObjects.add(recent);
    }

    void loadMenuKey(ArrayList<View> parent) {
        SoftKeyObject menu = new SoftKeyObject();
        menu.setSoftKey(MENU_KEY,
                MENU_KEY_LOCATION,
                MENU_KEY_URI_TAG,
                new SoftkeyLongClickListener(MENU_KEY_LOCATION),
                parent);
        mSoftKeyObjects.add(menu);
    }

    public void loadActions() {
        if (mActions == null)
            mActions = new ArrayList<String>();
        else
            mActions.clear();
        String[] actions = new String[4];
        for (SoftKeyObject s : mSoftKeyObjects) {
            actions[s.mPosition] = Settings.System.getString(mResolver, s.mUri);
            mResolver.registerContentObserver(Settings.System.getUriFor(s.mUri), false,
                    mObserver);
            s.loadListener();
        }
        mActions.addAll(Arrays.asList(actions));
    }

    public void unloadActions() {
        mResolver.unregisterContentObserver(mObserver);
        for (SoftKeyObject s : mSoftKeyObjects) {
            s.unloadListener();
        }
    }

    void initSoftKeys() {
        // softkey objects only need to the the parent view
        ArrayList<View> parent = new ArrayList<View>();
        if (mHasNavBar) {
            parent.add(mRootView.findViewById(NAVBAR_ROT_90));
            parent.add(mRootView.findViewById(NAVBAR_ROT_0));
        } else {
            parent.add(mRootView);
        }
        loadBackKey(parent);
//        loadHomeKey(parent);
        loadRecentKey(parent);
        loadMenuKey(parent);
    }

    public Handler getHandler() {
        return H;
    }

    private Handler H = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

            }
        }
    };

    private class SettingsObserver extends ContentObserver {
        Handler mHandler;

        public SettingsObserver(Handler handler) {
            super(handler);
            mHandler = handler;
        }

        public void onChange(boolean selfChange) {
            loadActions();
        }
    }

    @Override
    public boolean handleAction(String action) {
        return true;
    }

    class SoftkeyLongClickListener implements View.OnLongClickListener {
        int position;

        public SoftkeyLongClickListener(int i) {
            position = i;
        }

        @Override
        public boolean onLongClick(View v) {
            // TODO Auto-generated method stub
            handleEvent(position);
            return false;
        }

    }

    // Dummy cheaters class to help keep organized
    protected static class SoftKeyObject {
        private int mId;
        private ArrayList<View> mParent = new ArrayList<View>();
        private int mPosition;
        private String mUri;
        private SoftkeyLongClickListener mListener;
        private int mKeyCode;
        private boolean mSupportsLongPress = true;

        public void setSoftKey(int id, int position, String uri,
                SoftkeyLongClickListener l, ArrayList<View> parent) {
            mId = id;
            mPosition = position;
            mUri = uri;
            mListener = l;
            mParent = parent;
        }

        public void loadListener() {
            for (View v : mParent) {
                KeyButtonView key = (KeyButtonView) v.findViewById(mId);
                key.setOnLongClickListener(null);
                key.setOnLongClickListener(mListener);
                key.disableLongPressIntercept(true);
                mSupportsLongPress = key.getSupportsLongPress();
                if (!mSupportsLongPress) key.setSupportsLongPress(true);                
            }
        }

        public void unloadListener() {
            for (View v : mParent) {
                KeyButtonView key = (KeyButtonView) v.findViewById(mId);
                key.setOnLongClickListener(null);
                key.disableLongPressIntercept(false);
                if (key.getSupportsLongPress()) key.setSupportsLongPress(false);
            }
        }

        public void dump() {
            StringBuilder b = new StringBuilder();
            b.append("Id = " + String.valueOf(mId))
                    .append(" Postition = " + String.valueOf(mPosition))
                    .append(" Uri = " + mUri);
            log(b.toString());
        }
    }

    // applies to Navigation Bar or Systembar
    private void updateSystemBarColor() {
        int color = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_NAVBAR_COLOR,
                EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF);
        if (color == -1)
            color = EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF;
        // we don't want alpha here
        color = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
        mRootView.setBackgroundColor(color);
    }    

    // applies to Statusbar only 
    private void updateStatusBarColor() {
        int color = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_STATUSBAR_COLOR,
                EOSConstants.SYSTEMUI_STATUSBAR_COLOR_DEF);
        if (color == -1)
            color = EOSConstants.SYSTEMUI_NAVBAR_COLOR_DEF;
        // we don't want alpha here
        color = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
        mStatusBarView.setBackgroundColor(color);
    }

    private void updateBarVisibility() {
        boolean hideBar = (Settings.System.getInt(mResolver, EOSConstants.SYSTEMUI_HIDE_BARS,
                EOSConstants.SYSTEMUI_HIDE_BARS_DEF) == 1) ? true : false;
        // device must have either a systembar or statusbar but only maybe have
        // a navbar eg. crespo
        if (mHasStatusBar) {
            if(hideBar) {
                mWindowManager.removeView(mStatusBarContainer);
            } else {
                mWindowManager.addView(mStatusBarContainer, mStatusBarParams);
            }
        }
        if (mHasNavBar) {
            if (hideBar) {
            	mWindowManager.removeView(mRootView);
            } else {
            	mWindowManager.addView(mRootView, mSystemBarParams);
            }
        }
        if (mHasSystemBar) {
            View root = (FrameLayout)mRootView.getParent();
            if (hideBar) {
            	mWindowManager.removeView(root);
            } else {
            	mWindowManager.addView(root, mSystemBarParams);
            }
        }  
    }

    private void processBatterySettingsChange() {
        View bar = mHasStatusBar ? mStatusBarView : mRootView;

        ImageView batteryIcon = (ImageView) bar.findViewById(R.id.battery);
        TextView batteryText = (TextView) bar.findViewById(R.id.battery_text);
        batteryText.setTag(EOSConstants.SYSTEMUI_BATTERY_PERCENT_TAG);

        if (Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE_DEF) == 1) {
            if (batteryIcon != null) {
//                mBatteryController.addIconView(batteryIcon);
                batteryIcon.setVisibility(View.VISIBLE);
            }
        } else {
//            mBatteryController.removeIconView(batteryIcon);
            if (batteryIcon != null) batteryIcon.setVisibility(View.GONE);
        }

        if (Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE_DEF) == 1) {
            if (batteryText != null) {
                mBatteryController.addLabelView(batteryText);
                batteryText.setVisibility(View.VISIBLE);
            }
        } else {
//            mBatteryController.removeLabelView(batteryText);
            if (batteryIcon != null) batteryText.setVisibility(View.GONE);
        }
        int color = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR_DEF);
        if (color == -1) {
            color = mContext.getResources()
                    .getColor(android.R.color.holo_blue_light);
        }
        batteryText.setTextColor(color);
    }

    // utility to help bigclearbutton feature
    // seems ok here for now as it could be useful later
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
