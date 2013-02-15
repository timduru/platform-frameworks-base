
package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.android.systemui.statusbar.EosObserverHandler.OnFeatureStateChangedListener;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.policy.KeyButtonView;

import org.teameos.jellybean.settings.ActionHandler;
import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;
import java.util.Arrays;

public class EosSoftkeyHandler {
    static final String TAG = "EosSoftkeyHandler";
    private static final boolean DEBUG = true;

    private static EosSoftkeys eosSoftkeyHandler = null;

    public static void init(Context context, NavigationBarView view) {
        if (eosSoftkeyHandler == null) {
            eosSoftkeyHandler = new EosSoftkeys(context, view);
        }
    }

    private static class EosSoftkeys extends ActionHandler implements OnFeatureStateChangedListener {
        static final int BACK_KEY = com.android.systemui.R.id.back;
        static final int HOME_KEY = com.android.systemui.R.id.home;
        static final int RECENT_KEY = com.android.systemui.R.id.recent_apps;
        static final int MENU_KEY = com.android.systemui.R.id.menu;

        // we'll cheat just a little to help with the two navigation bar views
        static final int NAVBAR_ROT_90 = com.android.systemui.R.id.rot90;
        static final int NAVBAR_ROT_0 = com.android.systemui.R.id.rot0;

        // location for actionhandler
        static final int BACK_KEY_LOCATION = 0;
        static final int HOME_KEY_LOCATION = 1;
        static final int RECENT_KEY_LOCATION = 2;
        static final int MENU_KEY_LOCATION = 3;

        private ArrayList<SoftKeyObject> mSoftKeyObjects = new ArrayList<SoftKeyObject>();

        // handler messages
        private int MSG_SOFTKEY_LONGPRESS_ENABLED_SETTINGS;
        private int MSG_SOFTKEY_LONGPRESS_BACK_SETTINGS;
        private int MSG_SOFTKEY_LONGPRESS_HOME_SETTINGS;
        private int MSG_SOFTKEY_LONGPRESS_RECENT_SETTINGS;
        private int MSG_SOFTKEY_LONGPRESS_MENU_SETTINGS;

        private ContentResolver mResolver;
        private NavigationBarView mNavigationBarView;

        public EosSoftkeys(Context context, NavigationBarView parent) {
            super(context);
            mResolver = context.getContentResolver();
            mNavigationBarView = parent;
            registerUriList();
            initSoftkeys();
        }

        private void registerUriList() {
            // i really need to rename the string uri constant for this
            MSG_SOFTKEY_LONGPRESS_ENABLED_SETTINGS = EosObserverHandler.getEosObserverHandler()
                    .registerUri(EOSConstants.SYSTEMUI_NAVBAR_DISABLE_GESTURE);

            // softkey longpress actions
            MSG_SOFTKEY_LONGPRESS_BACK_SETTINGS = EosObserverHandler.getEosObserverHandler()
                    .registerUri(EOSConstants.SYSTEMUI_SOFTKEY_BACK);
            MSG_SOFTKEY_LONGPRESS_HOME_SETTINGS = EosObserverHandler.getEosObserverHandler()
                    .registerUri(EOSConstants.SYSTEMUI_SOFTKEY_HOME);
            MSG_SOFTKEY_LONGPRESS_RECENT_SETTINGS = EosObserverHandler.getEosObserverHandler()
                    .registerUri(EOSConstants.SYSTEMUI_SOFTKEY_RECENT);
            MSG_SOFTKEY_LONGPRESS_MENU_SETTINGS = EosObserverHandler.getEosObserverHandler()
                    .registerUri(EOSConstants.SYSTEMUI_SOFTKEY_MENU);
            EosObserverHandler.getEosObserverHandler()
                    .setOnFeatureStateChangedListener((OnFeatureStateChangedListener) EosSoftkeys.this);
        }

        private void loadBackKey(ArrayList<View> parent) {
            SoftKeyObject back = new SoftKeyObject();
            back.setSoftKey(BACK_KEY,
                    BACK_KEY_LOCATION,
                    EOSConstants.SYSTEMUI_SOFTKEY_BACK,
                    new SoftkeyLongClickListener(BACK_KEY_LOCATION),
                    parent);
            mSoftKeyObjects.add(back);
        }

        private void loadHomeKey(ArrayList<View> parent) {
            SoftKeyObject home = new SoftKeyObject();
            home.setSoftKey(HOME_KEY,
                    HOME_KEY_LOCATION,
                    EOSConstants.SYSTEMUI_SOFTKEY_HOME,
                    new SoftkeyLongClickListener(HOME_KEY_LOCATION),
                    parent);
            mSoftKeyObjects.add(home);
        }

        private void loadRecentKey(ArrayList<View> parent) {
            SoftKeyObject recent = new SoftKeyObject();
            recent.setSoftKey(RECENT_KEY,
                    RECENT_KEY_LOCATION,
                    EOSConstants.SYSTEMUI_SOFTKEY_RECENT,
                    new SoftkeyLongClickListener(RECENT_KEY_LOCATION),
                    parent);
            mSoftKeyObjects.add(recent);
        }

        private void loadMenuKey(ArrayList<View> parent) {
            SoftKeyObject menu = new SoftKeyObject();
            menu.setSoftKey(MENU_KEY,
                    MENU_KEY_LOCATION,
                    EOSConstants.SYSTEMUI_SOFTKEY_MENU,
                    new SoftkeyLongClickListener(MENU_KEY_LOCATION),
                    parent);
            mSoftKeyObjects.add(menu);
        }

        private void loadSoftkeyActions() {
            if (mActions == null)
                mActions = new ArrayList<String>();
            else
                mActions.clear();
            String[] actions = new String[4];
            for (SoftKeyObject s : mSoftKeyObjects) {
                actions[s.mPosition] = Settings.System.getString(mResolver, s.mUri);
                s.loadListener();
            }
            mActions.addAll(Arrays.asList(actions));
        }

        private void unloadSoftkeyActions() {
            for (SoftKeyObject s : mSoftKeyObjects) {
                s.unloadListener();
            }
        }

        private void initSoftkeys() {
            // softkey objects only need to the the parent view
            ArrayList<View> parent = new ArrayList<View>();
            parent.add(mNavigationBarView.findViewById(NAVBAR_ROT_90));
            parent.add(mNavigationBarView.findViewById(NAVBAR_ROT_0));
            loadBackKey(parent);
            // loadHomeKey(parent);
            loadRecentKey(parent);
            loadMenuKey(parent);
            handleSoftkeyLongpressChange();
        }

        private void handleSoftkeyLongpressChange() {
            if (Settings.System.getInt(mResolver,
                    EOSConstants.SYSTEMUI_NAVBAR_DISABLE_GESTURE,
                    EOSConstants.SYSTEMUI_NAVBAR_DISABLE_GESTURE_DEF) == 1) {
                loadSoftkeyActions();
            } else {
                unloadSoftkeyActions();
            }
        }

        // Dummy cheaters class to help keep organized
        private class SoftKeyObject {
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
                    if (!mSupportsLongPress)
                        key.setSupportsLongPress(true);
                }
            }

            public void unloadListener() {
                for (View v : mParent) {
                    KeyButtonView key = (KeyButtonView) v.findViewById(mId);
                    key.setOnLongClickListener(null);
                    key.disableLongPressIntercept(false);
                    if (key.getSupportsLongPress())
                        key.setSupportsLongPress(false);
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

        private class SoftkeyLongClickListener implements View.OnLongClickListener {
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

        @Override
        public void onFeatureStateChanged(int msg) {
            if (msg == MSG_SOFTKEY_LONGPRESS_ENABLED_SETTINGS) {
                handleSoftkeyLongpressChange();
                return;
            } else if (msg == MSG_SOFTKEY_LONGPRESS_BACK_SETTINGS
                    || msg == MSG_SOFTKEY_LONGPRESS_HOME_SETTINGS
                    || msg == MSG_SOFTKEY_LONGPRESS_RECENT_SETTINGS
                    || msg == MSG_SOFTKEY_LONGPRESS_MENU_SETTINGS) {
                loadSoftkeyActions();
                return;
            }
        }

        @Override
        public boolean handleAction(String action) {
            return true;
        }

        void log(String s) {
            if (DEBUG)
                Log.i(TAG, s);
        }
    }

}
