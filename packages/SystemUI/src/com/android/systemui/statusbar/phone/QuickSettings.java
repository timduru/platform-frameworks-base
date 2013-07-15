/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import com.android.internal.view.RotationPolicy;
import com.android.systemui.R;

import com.android.systemui.statusbar.phone.QuickSettingsModel.BluetoothState;
import com.android.systemui.statusbar.phone.QuickSettingsModel.LteState;
import com.android.systemui.statusbar.phone.QuickSettingsModel.RSSIState;
import com.android.systemui.statusbar.phone.QuickSettingsModel.State;
import com.android.systemui.statusbar.phone.QuickSettingsModel.UserState;
import com.android.systemui.statusbar.phone.QuickSettingsModel.WifiState;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BrightnessController;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.ToggleSlider;
import com.android.systemui.statusbar.tablet.NotificationPanel;
import com.android.systemui.statusbar.tablet.TabletStatusBar;

import org.teameos.jellybean.settings.EOSConstants;
import org.teameos.jellybean.settings.EOSUtils;

import javax.microedition.khronos.opengles.GL10;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.LevelListDrawable;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.location.LocationManager;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Profile;
import android.provider.Settings;
import android.net.ConnectivityManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class QuickSettings {
    private static final String TAG = "QuickSettings";
    public static final boolean SHOW_IME_TILE = false;

    public static final boolean LONG_PRESS_TOGGLES = true;

    private Context mContext;
    private PanelBar mBar;
    private TabletStatusBar mTabletBar;
    private QuickSettingsModel mModel;
    private ViewGroup mContainerView;

    private DisplayManager mDisplayManager;
    private WifiDisplayStatus mWifiDisplayStatus;
    private PhoneStatusBar mStatusBarService;
    private BluetoothState mBluetoothState;

    private BrightnessController mBrightnessController;

    private Dialog mBrightnessDialog;
    private int mBrightnessDialogShortTimeout;
    private int mBrightnessDialogLongTimeout;

    private AsyncTask<Void, Void, Pair<String, Drawable>> mUserInfoTask;

    private LevelListDrawable mBatteryLevels;
    private LevelListDrawable mChargingBatteryLevels;

    boolean mTilesSetUp = false;
    boolean mIsTabletUi = false;

    private int mQsTileRes;
    private int mQsSlimTileRes;
    private int mQsSlimmerTileRes;
    private int mQsHeight_slim;
    private int mQsHeight_more_slim;

    private Handler mHandler;

    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;
    private WakeLock mWakeLock;
    private boolean mTorchEnabled = false;
    public static int mVolumeStream = AudioManager.STREAM_MUSIC;
    private List<String> mEnabledTiles;
    private int mSeekbarSpan;
    private Boolean mUserEnabled = true;

    private final String AVATAR = EOSConstants.SYSTEMUI_PANEL_USER_TILE;
    private final String SETTINGS = EOSConstants.SYSTEMUI_PANEL_SETTINGS_TILE;
    private final String SEEKBAR = EOSConstants.SYSTEMUI_PANEL_SEEKBAR_TILE;
    private final String BRIGHT_SEEKBAR = EOSConstants.SYSTEMUI_PANEL_BRIGHT_SEEKBAR_TILE;
    private final String VOL_SEEKBAR = EOSConstants.SYSTEMUI_PANEL_VOL_SEEKBAR_TILE;
    private final String BATTERY = EOSConstants.SYSTEMUI_PANEL_BATTERY_TILE;
    private final String ROTATION = EOSConstants.SYSTEMUI_PANEL_ROTATION_TILE;
    private final String AIRPLANE = EOSConstants.SYSTEMUI_PANEL_AIRPLANE_TILE;
    private final String WIFI = EOSConstants.SYSTEMUI_PANEL_WIFI_TILE;
    private final String DATA = EOSConstants.SYSTEMUI_PANEL_DATA_TILE;
    private final String BT = EOSConstants.SYSTEMUI_PANEL_BT_TILE;
    private final String SCREEN = EOSConstants.SYSTEMUI_PANEL_SCREENOFF_TILE;
    private final String LOCATION = EOSConstants.SYSTEMUI_PANEL_LOCATION_TILE;
    private final String RINGER = EOSConstants.SYSTEMUI_PANEL_RINGER_TILE;
    private final String WIFIAP = EOSConstants.SYSTEMUI_PANEL_WIFIAP_TILE;
    private final String TORCH = EOSConstants.SYSTEMUI_PANEL_TORCH_TILE;
    private final String LTE = EOSConstants.SYSTEMUI_PANEL_LTE_TILE;
    private final String TWOGEEZ = EOSConstants.SYSTEMUI_PANEL_2G3G_TILE;
    private final String SYNC = EOSConstants.SYSTEMUI_PANEL_SYNC_TILE;
    private final String EXPANDED_DESKTOP = EOSConstants.SYSTEMUI_PANEL_EXPANDED_DESKTOP_TILE;
    private final String BRIGHTNESS = EOSConstants.SYSTEMUI_PANEL_BRIGHTNESS_TILE;
    private final String INTENT_UPDATE_TORCH_TILE = EOSConstants.SYSTEMUI_PANEL_TORCH_INTENT;
    private final String INTENT_UPDATE_VOLUME_OBSERVER_STREAM = EOSConstants.SYSTEMUI_PANEL_VOLUME_OBSERVER_STREAM_INTENT;

    // The set of QuickSettingsTiles that have dynamic spans (and need to be
    // updated on
    // configuration change)
    private final ArrayList<QuickSettingsTileView> mDynamicSpannedTiles =
            new ArrayList<QuickSettingsTileView>();

    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    mModel.onRotationLockChanged();
                }
            };

    public QuickSettings(Context context, QuickSettingsContainerView container) {
        mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        mContext = context;
        mContainerView = container;

        // get our resources accordingly
        mIsTabletUi = EOSUtils.hasSystemBar(context);
        mQsHeight_slim = mIsTabletUi ? R.dimen.quick_settings_slim_cell_height_tablet : R.dimen.quick_settings_slim_cell_height;
        mQsHeight_more_slim = mIsTabletUi ? R.dimen.quick_settings_more_slim_cell_height_tablet : R.dimen.quick_settings_more_slim_cell_height;
        mQsTileRes = mIsTabletUi ? R.layout.quick_settings_tablet_tile : R.layout.quick_settings_tile;
        mQsSlimTileRes = mIsTabletUi ? R.layout.quick_settings_tile_slim_tablet : R.layout.quick_settings_tile_slim;
        mQsSlimmerTileRes = mIsTabletUi ? R.layout.quick_settings_tile_more_slim_tablet : R.layout.quick_settings_tile_more_slim;
    }

    public void setEnabledTiles(List<String> tiles) {
        mEnabledTiles = tiles;
        mModel = new QuickSettingsModel(mContext, mEnabledTiles);
        mWifiDisplayStatus = new WifiDisplayStatus();
        mBluetoothState = new QuickSettingsModel.BluetoothState();

        mHandler = new Handler();

        Resources r = mContext.getResources();
        mBatteryLevels = (LevelListDrawable) r.getDrawable(R.drawable.qs_sys_battery);
        mChargingBatteryLevels =
                (LevelListDrawable) r.getDrawable(R.drawable.qs_sys_battery_charging);
        mBrightnessDialogLongTimeout =
                r.getInteger(R.integer.quick_settings_brightness_dialog_long_timeout);
        mBrightnessDialogShortTimeout =
                r.getInteger(R.integer.quick_settings_brightness_dialog_short_timeout);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(mReceiver, filter);

        IntentFilter profileFilter = new IntentFilter();
        profileFilter.addAction(ContactsContract.Intents.ACTION_PROFILE_CHANGED);
        profileFilter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        mContext.registerReceiverAsUser(mProfileReceiver, UserHandle.ALL, profileFilter,
                null, null);

        mSeekbarSpan = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_PANEL_COLUMN_COUNT,
                EOSConstants.SYSTEMUI_PANEL_COLUMNS_DEF);
    }

    public void removeReceivers() {
        mContext.unregisterReceiver(mReceiver);
        mContext.unregisterReceiver(mProfileReceiver);
        if (mModel != null)
            mModel.removeReceivers();
    }

    void setBar(PanelBar bar) {
        mBar = bar;
    }

    public void setTabletPanel(TabletStatusBar tabletBar) {
        mTabletBar = tabletBar;
    }

    public void setService(PhoneStatusBar phoneStatusBar) {
        mStatusBarService = phoneStatusBar;
    }

    public PhoneStatusBar getService() {
        return mStatusBarService;
    }

    public void setImeWindowStatus(boolean visible) {
        mModel.onImeWindowStatusChanged(visible);
    }

    private boolean isDeviceProvisioned() {
        if (mStatusBarService != null) {
            return mStatusBarService.isDeviceProvisioned();
        } else if (mTabletBar != null) {
            return mTabletBar.isDeviceProvisioned();
        } else {
            return true;
        }
    }

    private void collapsePanels() {
        if (mBar != null) {
            mBar.collapseAllPanels(true);
        } else {
            if (mTabletBar != null) {
                mTabletBar.animateCollapsePanels();
            }
        }
    }

    public void setup(NetworkController networkController, BluetoothController bluetoothController,
            BatteryController batteryController, LocationController locationController) {

        setupQuickSettings();
        updateWifiDisplayStatus();
        updateResources();

        networkController.addNetworkSignalChangedCallback(mModel);
        bluetoothController.addStateChangedCallback(mModel);
        batteryController.addStateChangedCallback(mModel);
        locationController.addStateChangedCallback(mModel);
        RotationPolicy.registerRotationPolicyListener(mContext, mRotationPolicyListener,
                UserHandle.USER_ALL);
    }

    private void queryForUserInformation() {
        Context currentUserContext = null;
        UserInfo userInfo = null;
        try {
            userInfo = ActivityManagerNative.getDefault().getCurrentUser();
            currentUserContext = mContext.createPackageContextAsUser("android", 0,
                    new UserHandle(userInfo.id));
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Couldn't create user context", e);
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't get user info", e);
        }
        final int userId = userInfo.id;
        final String userName = userInfo.name;

        final Context context = currentUserContext;
        mUserInfoTask = new AsyncTask<Void, Void, Pair<String, Drawable>>() {
            @Override
            protected Pair<String, Drawable> doInBackground(Void... params) {
                final UserManager um =
                        (UserManager) mContext.getSystemService(Context.USER_SERVICE);

                // Fall back to the UserManager nickname if we can't read the
                // name from the local
                // profile below.
                String name = userName;
                Drawable avatar = null;
                Bitmap rawAvatar = um.getUserIcon(userId);
                if (rawAvatar != null) {
                    avatar = new BitmapDrawable(mContext.getResources(), rawAvatar);
                } else {
                    avatar = mContext.getResources().getDrawable(R.drawable.ic_qs_default_user);
                }

                // If it's a single-user device, get the profile name, since the
                // nickname is not
                // usually valid
                if (um.getUsers().size() <= 1) {
                    // Try and read the display name from the local profile
                    final Cursor cursor = context.getContentResolver().query(
                            Profile.CONTENT_URI, new String[] {
                                    Phone._ID, Phone.DISPLAY_NAME
                            },
                            null, null, null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                name = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                }
                return new Pair<String, Drawable>(name, avatar);
            }

            @Override
            protected void onPostExecute(Pair<String, Drawable> result) {
                super.onPostExecute(result);
                mModel.setUserTileInfo(result.first, result.second);
                mUserInfoTask = null;
            }
        };
        mUserInfoTask.execute();
    }

    private void setupQuickSettings() {
        // Setup the tiles that we are going to be showing (including the
        // temporary ones)
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mUserEnabled = mModel.isToggleEnabled(AVATAR);

        addSeekbarTiles(mContainerView, inflater);
        addSystemTiles(mContainerView, inflater);
        addTemporaryTiles(mContainerView, inflater);

        if (mUserEnabled)
            queryForUserInformation();
        mTilesSetUp = true;
    }

    private void startSettingsActivity(String action) {
        Intent intent = new Intent(action);
        startSettingsActivity(intent);
    }

    private void startSettingsActivity(Intent intent) {
        startSettingsActivity(intent, true);
    }

    private void startSettingsActivity(Intent intent, boolean onlyProvisioned) {
        if (onlyProvisioned && !isDeviceProvisioned())
            return;
        try {
            // Dismiss the lock screen when Settings starts.
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        collapsePanels();
    }

    private void addUserTiles(ViewGroup parent, LayoutInflater inflater) {
        // User
        QuickSettingsTileView userTile = (QuickSettingsTileView)
                inflater.inflate(mQsTileRes, parent, false);
        userTile.setContent(R.layout.quick_settings_tile_user, inflater);
        userTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapsePanels();
                final UserManager um =
                        (UserManager) mContext.getSystemService(Context.USER_SERVICE);
                if (um.getUsers(true).size() > 1) {
                    try {
                        WindowManagerGlobal.getWindowManagerService().lockNow(null);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Couldn't show user switcher", e);
                    }
                } else {
                    Intent intent = ContactsContract.QuickContact.composeQuickContactsIntent(
                            mContext, v, ContactsContract.Profile.CONTENT_URI,
                            ContactsContract.QuickContact.MODE_LARGE, null);
                    mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
                }
            }
        });
        mModel.addUserTile(userTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State state) {
                UserState us = (UserState) state;
                ImageView iv = (ImageView) view.findViewById(R.id.user_imageview);
                TextView tv = (TextView) view.findViewById(R.id.user_textview);
                tv.setText(state.label);
                iv.setImageDrawable(us.avatar);
                view.setContentDescription(mContext.getString(
                        R.string.accessibility_quick_settings_user, state.label));
            }
        });
        parent.addView(userTile);
        // mDynamicSpannedTiles.add(userTile);

        // Time tile
        /*
         * QuickSettingsTileView timeTile = (QuickSettingsTileView)
         * inflater.inflate(R.layout.quick_settings_tile, parent, false);
         * timeTile.setContent(R.layout.quick_settings_tile_time, inflater);
         * timeTile.setOnClickListener(new View.OnClickListener() {
         * @Override public void onClick(View v) { // Quick. Clock. Quick.
         * Clock. Quick. Clock.
         * startSettingsActivity(Intent.ACTION_QUICK_CLOCK); } });
         * mModel.addTimeTile(timeTile, new QuickSettingsModel.RefreshCallback()
         * {
         * @Override public void refreshView(QuickSettingsTileView view, State
         * alarmState) {} }); parent.addView(timeTile);
         * mDynamicSpannedTiles.add(timeTile);
         */
    }

    private void addSystemTiles(ViewGroup parent, LayoutInflater inflater) {
        List<String> mTilesOrderedList = getTilesOrderedList();
        for (String tile : mTilesOrderedList) {
            loadTile(tile, parent, inflater);
        }
    }

    private void addSeekbarTiles(ViewGroup parent, LayoutInflater inflater) {
        List<String> mTilesOrderedList = getTilesOrderedList();
        for (String tile : mTilesOrderedList) {
            if (tile.equals(SEEKBAR)) {
                addSliderTile(mContainerView, inflater);
            } else if (tile.equals(VOL_SEEKBAR)) {
                addVolSliderTile(mContainerView, inflater);
            } else if (tile.equals(BRIGHT_SEEKBAR)) {
                addBrightSliderTile(mContainerView, inflater);
            }
        }
    }

    private void addTemporaryTiles(final ViewGroup parent, final LayoutInflater inflater) {
        // Alarm tile
        QuickSettingsTileView alarmTile = (QuickSettingsTileView)
                inflater.inflate(mQsTileRes, parent, false);
        alarmTile.setContent(R.layout.quick_settings_tile_alarm, inflater);
        alarmTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettingsActivity(new Intent(AlarmClock.ACTION_SET_ALARM));
            }
        });

        mModel.addAlarmTile(alarmTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State alarmState) {
                TextView tv = (TextView) view.findViewById(R.id.alarm_textview);
                tv.setText(alarmState.label);
                view.setVisibility(alarmState.enabled ? View.VISIBLE : View.GONE);
                view.setContentDescription(mContext.getString(
                        R.string.accessibility_quick_settings_alarm, alarmState.label));
            }
        });
        parent.addView(alarmTile);

        // Wifi Display
        QuickSettingsTileView wifiDisplayTile = (QuickSettingsTileView)
                inflater.inflate(mQsTileRes, parent, false);
        wifiDisplayTile.setContent(R.layout.quick_settings_tile_wifi_display, inflater);
        wifiDisplayTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettingsActivity(android.provider.Settings.ACTION_WIFI_DISPLAY_SETTINGS);
            }
        });
        mModel.addWifiDisplayTile(wifiDisplayTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State state) {
                TextView tv = (TextView) view.findViewById(R.id.wifi_display_textview);
                tv.setText(state.label);
                tv.setCompoundDrawablesWithIntrinsicBounds(0, state.iconId, 0, 0);
                view.setVisibility(state.enabled ? View.VISIBLE : View.GONE);
            }
        });
        parent.addView(wifiDisplayTile);

        if (SHOW_IME_TILE) {
            // IME
            QuickSettingsTileView imeTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            imeTile.setContent(R.layout.quick_settings_tile_ime, inflater);
            imeTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        collapsePanels();
                        Intent intent = new Intent(Settings.ACTION_SHOW_INPUT_METHOD_PICKER);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0,
                                intent, 0);
                        pendingIntent.send();
                    } catch (Exception e) {
                    }
                }
            });
            mModel.addImeTile(imeTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.ime_textview);
                    if (state.label != null) {
                        tv.setText(state.label);
                    }
                    view.setVisibility(state.enabled ? View.VISIBLE : View.GONE);
                }
            });
            parent.addView(imeTile);
        }

        // Bug reports
        QuickSettingsTileView bugreportTile = (QuickSettingsTileView)
                inflater.inflate(mQsTileRes, parent, false);
        bugreportTile.setContent(R.layout.quick_settings_tile_bugreport, inflater);
        bugreportTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapsePanels();
                showBugreportDialog();
            }
        });
        mModel.addBugreportTile(bugreportTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State state) {
                view.setVisibility(state.enabled ? View.VISIBLE : View.GONE);
            }
        });
        parent.addView(bugreportTile);
        /*
         * QuickSettingsTileView mediaTile = (QuickSettingsTileView)
         * inflater.inflate(R.layout.quick_settings_tile, parent, false);
         * mediaTile.setContent(R.layout.quick_settings_tile_media, inflater);
         * parent.addView(mediaTile); QuickSettingsTileView imeTile =
         * (QuickSettingsTileView)
         * inflater.inflate(R.layout.quick_settings_tile, parent, false);
         * imeTile.setContent(R.layout.quick_settings_tile_ime, inflater);
         * imeTile.setOnClickListener(new View.OnClickListener() {
         * @Override public void onClick(View v) { parent.removeViewAt(0); } });
         * parent.addView(imeTile);
         */
    }

    public void updateResources() {
        Resources r = mContext.getResources();

        // Update the model
        mModel.updateResources();

        // Update the User, Time, and Settings tiles spans, and reset everything
        // else
        int span = r.getInteger(R.integer.quick_settings_user_time_settings_tile_span);
        for (QuickSettingsTileView v : mDynamicSpannedTiles) {
            v.setColumnSpan(span);
        }
        ((QuickSettingsContainerView) mContainerView).updateResources();
        mContainerView.requestLayout();

        // Reset the dialog
        boolean isBrightnessDialogVisible = false;
        if (mBrightnessDialog != null) {
            removeAllBrightnessDialogCallbacks();

            isBrightnessDialogVisible = mBrightnessDialog.isShowing();
            mBrightnessDialog.dismiss();
        }
        mBrightnessDialog = null;
        if (isBrightnessDialogVisible) {
            showBrightnessDialog();
        }
    }

    private void removeAllBrightnessDialogCallbacks() {
        mHandler.removeCallbacks(mDismissBrightnessDialogRunnable);
    }

    private Runnable mDismissBrightnessDialogRunnable = new Runnable() {
        public void run() {
            if (mBrightnessDialog != null && mBrightnessDialog.isShowing()) {
                mBrightnessDialog.dismiss();
            }
            removeAllBrightnessDialogCallbacks();
        };
    };

    private void showBrightnessDialog() {
        if (mBrightnessDialog == null) {
            mBrightnessDialog = new Dialog(mContext);
            mBrightnessDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mBrightnessDialog.setContentView(R.layout.quick_settings_brightness_dialog);
            mBrightnessDialog.setCanceledOnTouchOutside(true);

            mBrightnessController = new BrightnessController(mContext,
                    (ImageView) mBrightnessDialog.findViewById(R.id.brightness_icon),
                    (ToggleSlider) mBrightnessDialog.findViewById(R.id.brightness_slider));
            mBrightnessController.addStateChangedCallback(mModel);
            mBrightnessDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mBrightnessController = null;
                }
            });

            mBrightnessDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mBrightnessDialog.getWindow().getAttributes().privateFlags |=
                    WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
            mBrightnessDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        if (!mBrightnessDialog.isShowing()) {
            try {
                WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
            } catch (RemoteException e) {
            }
            mBrightnessDialog.show();
            dismissBrightnessDialog(mBrightnessDialogLongTimeout);
        }
    }

    private void dismissBrightnessDialog(int timeout) {
        removeAllBrightnessDialogCallbacks();
        if (mBrightnessDialog != null) {
            mHandler.postDelayed(mDismissBrightnessDialogRunnable, timeout);
        }
    }

    private void showBugreportDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setPositiveButton(com.android.internal.R.string.report, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    // Add a little delay before executing, to give the
                    // dialog a chance to go away before it takes a
                    // screenshot.
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ActivityManagerNative.getDefault()
                                        .requestBugReport();
                            } catch (RemoteException e) {
                            }
                        }
                    }, 500);
                }
            }
        });
        builder.setMessage(com.android.internal.R.string.bugreport_message);
        builder.setTitle(com.android.internal.R.string.bugreport_title);
        builder.setCancelable(true);
        final Dialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        try {
            WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
        } catch (RemoteException e) {
        }
        dialog.show();
    }

    private void updateWifiDisplayStatus() {
        mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();
        applyWifiDisplayStatus();
    }

    private void applyWifiDisplayStatus() {
        mModel.onWifiDisplayStateChanged(mWifiDisplayStatus);
    }

    private void applyBluetoothStatus() {
        mModel.onBluetoothStateChange(mBluetoothState);
    }

    void reloadUserInfo() {
        if (mUserInfoTask != null) {
            mUserInfoTask.cancel(false);
            mUserInfoTask = null;
        }
        if (mTilesSetUp && mUserEnabled) {
            queryForUserInformation();
        }
    }

    private void addVolSliderTile(ViewGroup parent, LayoutInflater inflater) {
        // Seekbar
        QuickSettingsTileView seekbarTile = (QuickSettingsTileView)
                inflater.inflate(mQsSlimmerTileRes, parent, false);
        seekbarTile.setColumnSpan(mSeekbarSpan);
        seekbarTile.setCustomHeight(mContext.getResources().getDimension(mQsHeight_more_slim));
        seekbarTile.setContent(R.layout.quick_settings_tile_vol_seekbar, inflater);

        final SeekBar sbVolume = (SeekBar) seekbarTile.findViewById(R.id.volume_seekbar);
        final ImageView ivVolume = (ImageView) seekbarTile.findViewById(R.id.sound_icon);

        final AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mModel.addVolSeekbarTile(seekbarTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State state) {
                sbVolume.setMax(am.getStreamMaxVolume(mVolumeStream));
                sbVolume.setProgress(am.getStreamVolume(mVolumeStream));
                if (mVolumeStream == AudioManager.STREAM_MUSIC) {
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_vol);
                } else {
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_ring_notif);
                }
            }
        });
        parent.addView(seekbarTile);

        ivVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVolumeStream == AudioManager.STREAM_RING) {
                    mVolumeStream = AudioManager.STREAM_MUSIC;
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_vol);
                } else {
                    mVolumeStream = AudioManager.STREAM_RING;
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_ring_notif);
                }
                mContext.sendBroadcast(new Intent(INTENT_UPDATE_VOLUME_OBSERVER_STREAM));
            }
        });

        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    am.setStreamVolume(mVolumeStream, progress, 0);
                } else {
                    sbVolume.setProgress(am.getStreamVolume(mVolumeStream));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void addBrightSliderTile(ViewGroup parent, LayoutInflater inflater) {
        // Seekbar
        QuickSettingsTileView seekbarTile = (QuickSettingsTileView)
                inflater.inflate(mQsSlimmerTileRes, parent, false);
        seekbarTile.setColumnSpan(mSeekbarSpan);
        seekbarTile.setCustomHeight(mContext.getResources().getDimension(mQsHeight_more_slim));
        seekbarTile.setContent(R.layout.quick_settings_tile_bright_seekbar, inflater);

        final SeekBar sbBrightness = (SeekBar) seekbarTile.findViewById(R.id.brightness_seekbar);
        final CheckBox cbBrightness = (CheckBox) seekbarTile.findViewById(R.id.brightness_switch);

        cbBrightness.setBackgroundResource(R.drawable.status_bar_toggle_button);

        final IPowerManager ipm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        final int min = pm.getMinimumScreenBrightnessSetting();
        final int max = pm.getMaximumScreenBrightnessSetting();

        mModel.addBrightSeekbarTile(seekbarTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State state) {
                int automatic = 0;
                int value = max;
                try {
                    automatic = Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE);
                    value = Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS);
                } catch (Exception e) {
                }

                cbBrightness.setChecked(automatic == 1);

                sbBrightness.setMax(max - min);
                sbBrightness.setProgress(value - min);
            }
        });
        parent.addView(seekbarTile);

        cbBrightness.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
                    } else {
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                        // restore value set by user
                        int progress = sbBrightness.getProgress();
                        ipm.setTemporaryScreenBrightnessSettingOverride(progress + min);
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS, progress + min);
                    }
                } catch (Exception e) {
                }
            }
        });

        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    // disable autobright if enabled first
                    if (cbBrightness.isChecked()) {
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                        cbBrightness.setChecked(false);
                    }
                    ipm.setTemporaryScreenBrightnessSettingOverride(progress + min);
                    Settings.System.putInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, progress + min);
                } catch (Exception e) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void addSliderTile(ViewGroup parent, LayoutInflater inflater) {
        // Seekbar
        QuickSettingsTileView seekbarTile = (QuickSettingsTileView)
                inflater.inflate(mQsSlimTileRes, parent, false);
        seekbarTile.setColumnSpan(mSeekbarSpan);
        seekbarTile.setCustomHeight(mContext.getResources().getDimension(mQsHeight_slim));
        seekbarTile.setContent(R.layout.quick_settings_tile_seekbar, inflater);

        final SeekBar sbVolume = (SeekBar) seekbarTile.findViewById(R.id.volume_seekbar);
        final ImageView ivVolume = (ImageView) seekbarTile.findViewById(R.id.sound_icon);
        final SeekBar sbBrightness = (SeekBar) seekbarTile.findViewById(R.id.brightness_seekbar);
        final CheckBox cbBrightness = (CheckBox) seekbarTile.findViewById(R.id.brightness_switch);     

        cbBrightness.setBackgroundResource(R.drawable.status_bar_toggle_button);
        
        final AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        final IPowerManager ipm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        final int min = pm.getMinimumScreenBrightnessSetting();
        final int max = pm.getMaximumScreenBrightnessSetting();

        mModel.addSeekbarTile(seekbarTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State state) {
                sbVolume.setMax(am.getStreamMaxVolume(mVolumeStream));
                sbVolume.setProgress(am.getStreamVolume(mVolumeStream));
                if (mVolumeStream == AudioManager.STREAM_MUSIC) {
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_vol);
                } else {
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_ring_notif);
                }

                int automatic = 0;
                int value = max;
                try {
                    automatic = Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE);
                    value = Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS);
                } catch (Exception e) {
                }

                cbBrightness.setChecked(automatic == 1);

                sbBrightness.setMax(max - min);
                sbBrightness.setProgress(value - min);
            }
        });
        parent.addView(seekbarTile);

        ivVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVolumeStream == AudioManager.STREAM_RING) {
                    mVolumeStream = AudioManager.STREAM_MUSIC;
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_vol);
                } else {
                    mVolumeStream = AudioManager.STREAM_RING;
                    ivVolume.setImageResource(com.android.internal.R.drawable.ic_audio_ring_notif);
                }
                mContext.sendBroadcast(new Intent(INTENT_UPDATE_VOLUME_OBSERVER_STREAM));
            }
        });

        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    am.setStreamVolume(mVolumeStream, progress, 0);
                } else {
                    sbVolume.setProgress(am.getStreamVolume(mVolumeStream));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        cbBrightness.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
                    } else {
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                        // restore value set by user
                        int progress = sbBrightness.getProgress();
                        ipm.setTemporaryScreenBrightnessSettingOverride(progress + min);
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS, progress + min);
                    }
                } catch (Exception e) {
                }
            }
        });

        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    // disable autobright if enabled first
                    if (cbBrightness.isChecked()) {
                        Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                        cbBrightness.setChecked(false);
                    }
                    ipm.setTemporaryScreenBrightnessSettingOverride(progress + min);
                    Settings.System.putInt(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, progress + min);
                } catch (Exception e) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void loadTile(String tile, ViewGroup parent, LayoutInflater inflater) {
        if (tile.equals(AVATAR)) {
            if (mUserEnabled)
                addUserTiles(mContainerView, inflater);
        } else if (tile.equals(SETTINGS)) {
            // Settings
            QuickSettingsTileView settingsTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            settingsTile.setContent(R.layout.quick_settings_tile_settings, inflater);
            settingsTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startSettingsActivity(android.provider.Settings.ACTION_SETTINGS);
                }
            });
            settingsTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent controlPanelIntent = new Intent();
                    controlPanelIntent.setClassName("org.kat.controlcenter",
                            "org.kat.controlcenter.Main");
                    controlPanelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(controlPanelIntent);

                    collapsePanels();
                    return true;
                }
            });
            mModel.addSettingsTile(settingsTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.settings_tileview);
                    tv.setText(state.label);
                }
            });
            parent.addView(settingsTile);
            // mDynamicSpannedTiles.add(settingsTile);
        } else if (tile.equals(RINGER)) {
            // Ringer
            QuickSettingsTileView ringerTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            ringerTile.setContent(R.layout.quick_settings_tile_ringer, inflater);
            ringerTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioManager am = (AudioManager) mContext
                            .getSystemService(Context.AUDIO_SERVICE);
                    switch (am.getRingerMode()) {
                        case AudioManager.RINGER_MODE_NORMAL:
                            am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                            break;
                        case AudioManager.RINGER_MODE_VIBRATE:
                            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            break;
                        case AudioManager.RINGER_MODE_SILENT:
                            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            break;
                    }
                }
            });
            ringerTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity(android.provider.Settings.ACTION_SOUND_SETTINGS);
                    return true;
                }
            });
            mModel.addRingerTile(ringerTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.ringer_textview);
                    ImageView iv = (ImageView) view.findViewById(R.id.ringer_overlay_image);

                    AudioManager am = (AudioManager) mContext
                            .getSystemService(Context.AUDIO_SERVICE);
                    switch (am.getRingerMode()) {
                        case AudioManager.RINGER_MODE_NORMAL:
                            tv.setText("Normal");
                            iv.setImageResource(R.drawable.ic_qs_ring_on);
                            break;
                        case AudioManager.RINGER_MODE_VIBRATE:
                            tv.setText("Vibrate");
                            iv.setImageResource(R.drawable.ic_qs_vibrate_on);
                            break;
                        case AudioManager.RINGER_MODE_SILENT:
                            tv.setText("Silent");
                            iv.setImageResource(R.drawable.ic_qs_ring_off);
                            break;
                    }
                }
            });
            parent.addView(ringerTile);
        } else if (tile.equals(LTE)) {
            QuickSettingsTileView lteTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            lteTile.setContent(R.layout.quick_settings_tile_lte, inflater);
            lteTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int currentMode = Settings.Secure.getInt(mContext.getContentResolver(),
                            Settings.Global.PREFERRED_NETWORK_MODE,
                            LteState.DEFAULT_MODE);
                    int newMode = LteState.DEFAULT_MODE;
                    switch (currentMode) {
                        case LteState.LTE_CDMA:
                            newMode = LteState.CDMA_ONLY;
                            break;
                        case LteState.CDMA_ONLY:
                            newMode = LteState.LTE_CDMA;
                            break;
                    }
                    Intent intent = new Intent(LteState.EOS_TELEPHONY_INTENT);
                    intent.putExtra(LteState.EOS_TELEPHONY_MODE_KEY, newMode);
                    mContext.sendBroadcast(intent);
                }
            });
            lteTile.setOnLongClickListener(new View.OnLongClickListener() {                
                @Override
                public boolean onLongClick(View v) {
                    Intent intent = new Intent("com.android.phone.MobileNetworkSettings");
                    intent.addCategory(Intent.ACTION_MAIN);
                    intent.setClassName("com.android.phone", "com.android.phone.MobileNetworkSettings");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    collapsePanels();
                    return true;
                }
            });
            mModel.addLteTile(lteTile, new QuickSettingsModel.RefreshCallback() {                
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    int newMode = ((LteState)state).settingsNetworkMode;
                    TextView tv = (TextView) view.findViewById(R.id.lte_textview);
                    ImageView iv = (ImageView) view.findViewById(R.id.lte_image);
                    int stringRes = (newMode == LteState.LTE_CDMA
                            ? R.string.quick_settings_lte_on_label
                            : R.string.quick_settings_lte_off_label);
                    int iconRes = (newMode == LteState.LTE_CDMA
                            ? R.drawable.ic_qs_lte_on
                            : R.drawable.ic_qs_lte_off);
                    iv.setImageResource(iconRes);
                    tv.setText(stringRes);                    
                }
            });
            parent.addView(lteTile);
        } else if (tile.equals(TWOGEEZ)) {
            QuickSettingsTileView twoGeezTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            twoGeezTile.setContent(R.layout.quick_settings_tile_2g3g, inflater);
            twoGeezTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // keeps me from adding telephony-common
                    // default gsm mode Phone.NT_MODE_WCDMA_PREF = 0;
                    // gsm only Phone.NT_MODE_GSM_ONLY = 1;
                    final int GSM_DEFAULT = 0;
                    final int GSM_ONLY = 1;
                    int currentMode = android.provider.Settings.Secure.getInt(
                            mContext.getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE, GSM_DEFAULT);
                    int newMode = (currentMode == GSM_DEFAULT ? GSM_ONLY : GSM_DEFAULT);
                    Intent intent = new Intent(EOSConstants.INTENT_TELEPHONY_2G3G_TOGGLE);
                    intent.putExtra(EOSConstants.INTENT_TELEPHONY_2G3G_TOGGLE_KEY, newMode);
                    mContext.sendBroadcast(intent);
                }
            });
            twoGeezTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent intent = new Intent("com.android.phone.MobileNetworkSettings");
                    intent.addCategory(Intent.ACTION_MAIN);
                    intent.setClassName("com.android.phone", "com.android.phone.MobileNetworkSettings");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    collapsePanels();
                    return true;
                }
            });
            mModel.add2g3gTile(twoGeezTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    final int GSM_DEFAULT = 0;
                    final int GSM_ONLY = 1;
                    int newMode = state.enabled ? GSM_ONLY : GSM_DEFAULT;
                    TextView tv = (TextView) view.findViewById(R.id.twogeez_textview);
                    ImageView iv = (ImageView) view.findViewById(R.id.twogeez_image);
                    int stringRes = (state.enabled ? R.string.quick_settings_2g_on_label : R.string.quick_settings_2g_off_label);
                    int iconRes = (state.enabled ? R.drawable.ic_qs_2g_on : R.drawable.ic_qs_2g_off);
                    iv.setImageResource(iconRes);
                    tv.setText(stringRes);
                }
            });
            parent.addView(twoGeezTile);
        } else if (tile.equals(SYNC)) {
            QuickSettingsTileView syncTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            syncTile.setContent(R.layout.quick_settings_tile_sync, inflater);
            syncTile.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    final boolean isSyncEnabled = ContentResolver.getMasterSyncAutomatically();
                    ContentResolver.setMasterSyncAutomatically(!isSyncEnabled);
                    mModel.refreshSyncTile();

                }
            });
            syncTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity(android.provider.Settings.ACTION_SYNC_SETTINGS);
                    return true;
                }
            });
            mModel.addSyncTile(syncTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView syncText = (TextView) view.findViewById(R.id.sync_textview);
                    ImageView syncImage = (ImageView) view.findViewById(R.id.sync_image);
                    syncText.setText(mContext
                            .getString((state.enabled) ? R.string.quick_settings_sync_on_label
                                    : R.string.quick_settings_sync_off_label));
                    syncImage.setImageResource((state.enabled) ? R.drawable.ic_qs_sync_on
                            : R.drawable.ic_qs_sync_off);

                }
            });
            parent.addView(syncTile);
        } else if (tile.equals(BATTERY)) {
            // Battery
            QuickSettingsTileView batteryTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            batteryTile.setContent(R.layout.quick_settings_tile_battery, inflater);
            batteryTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startSettingsActivity(Intent.ACTION_POWER_USAGE_SUMMARY);
                }
            });
            mModel.addBatteryTile(batteryTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    QuickSettingsModel.BatteryState batteryState =
                            (QuickSettingsModel.BatteryState) state;
                    TextView tv = (TextView) view.findViewById(R.id.battery_textview);
                    ImageView iv = (ImageView) view.findViewById(R.id.battery_image);
                    Drawable d = batteryState.pluggedIn
                            ? mChargingBatteryLevels
                            : mBatteryLevels;
                    String t;
                    if (batteryState.batteryLevel == 100) {
                        t = mContext.getString(R.string.quick_settings_battery_charged_label);
                    } else {
                        t = batteryState.pluggedIn
                                ? mContext.getString(
                                        R.string.quick_settings_battery_charging_label,
                                        batteryState.batteryLevel)
                                : mContext.getString(
                                        R.string.status_bar_settings_battery_meter_format,
                                        batteryState.batteryLevel);
                    }
                    iv.setImageDrawable(d);
                    iv.setImageLevel(batteryState.batteryLevel);
                    tv.setText(t);
                    view.setContentDescription(
                            mContext.getString(R.string.accessibility_quick_settings_battery, t));
                }
            });
            parent.addView(batteryTile);
        } else if (tile.equals(ROTATION)) {
            // Rotation Lock
            QuickSettingsTileView rotationLockTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            rotationLockTile.setContent(R.layout.quick_settings_tile_rotation_lock, inflater);
            rotationLockTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean locked = RotationPolicy.isRotationLocked(mContext);
                    RotationPolicy.setRotationLock(mContext, !locked);
                }
            });
            rotationLockTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity("android.settings.DISPLAY_SETTINGS");
                    return true;
                }
            });
            mModel.addRotationLockTile(rotationLockTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.rotation_lock_textview);
                    tv.setCompoundDrawablesWithIntrinsicBounds(0, state.iconId, 0, 0);
                    tv.setText(state.label);
                }
            });
            parent.addView(rotationLockTile);
        } else if (tile.equals(AIRPLANE)) {
            // Airplane
            QuickSettingsTileView airplaneTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            airplaneTile.setContent(R.layout.quick_settings_tile_airplane, inflater);
            airplaneTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity("android.settings.AIRPLANE_MODE_SETTINGS");
                    return true;
                }
            });
            mModel.addAirplaneModeTile(airplaneTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.airplane_mode_textview);
                    tv.setCompoundDrawablesWithIntrinsicBounds(0, state.iconId, 0, 0);

                    String airplaneState = mContext.getString(
                            (state.enabled) ? R.string.accessibility_desc_on
                                    : R.string.accessibility_desc_off);
                    view.setContentDescription(
                            mContext.getString(R.string.accessibility_quick_settings_airplane,
                                    airplaneState));
                    tv.setText(state.label);
                }
            });
            parent.addView(airplaneTile);
        } else if (tile.equals(WIFI)) {
            // Wi-fi
            QuickSettingsTileView wifiTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            wifiTile.setContent(R.layout.quick_settings_tile_wifi, inflater);
            wifiTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    wm.setWifiEnabled(!wm.isWifiEnabled());
                }
            });
            wifiTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity(android.provider.Settings.ACTION_WIFI_SETTINGS);
                    return true;
                }
            });
            mModel.addWifiTile(wifiTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    WifiState wifiState = (WifiState) state;
                    TextView tv = (TextView) view.findViewById(R.id.wifi_textview);
                    tv.setCompoundDrawablesWithIntrinsicBounds(0, wifiState.iconId, 0, 0);
                    tv.setText(wifiState.label);
                    view.setContentDescription(mContext.getString(
                            R.string.accessibility_quick_settings_wifi,
                            wifiState.signalContentDescription,
                            (wifiState.connected) ? wifiState.label : ""));
                }
            });
            parent.addView(wifiTile);
        } else if (mModel.deviceSupportsTelephony() && tile.equals(DATA)) {
            // RSSI
            QuickSettingsTileView rssiTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            rssiTile.setContent(R.layout.quick_settings_tile_rssi, inflater);
            rssiTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConnectivityManager cm = (ConnectivityManager) mContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    cm.setMobileDataEnabled(!cm.getMobileDataEnabled());
                    mModel.refreshRSSI();
                }
            });
            rssiTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    Intent roamingMenuIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                    roamingMenuIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(roamingMenuIntent);

                    collapsePanels();
                    return true;
                }
            });
            mModel.addRSSITile(rssiTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    RSSIState rssiState = (RSSIState) state;
                    ImageView iv = (ImageView) view.findViewById(R.id.rssi_image);
                    ImageView iov = (ImageView) view.findViewById(R.id.rssi_overlay_image);
                    TextView tv = (TextView) view.findViewById(R.id.rssi_textview);
                    iv.setImageResource(rssiState.signalIconId);

                    if (rssiState.dataTypeIconId > 0) {
                        iov.setImageResource(rssiState.dataTypeIconId);
                    } else {
                        iov.setImageDrawable(null);
                    }
                    view.setContentDescription(mContext.getResources().getString(
                            R.string.accessibility_quick_settings_mobile,
                            rssiState.signalContentDescription, rssiState.dataContentDescription,
                            state.label));

                    tv.setText(rssiState.enabledByUser ? state.label : "Disabled");
                }
            });
            parent.addView(rssiTile);
        } else if (mModel.deviceSupportsBluetooth() && tile.equals(BT)) {
            // Bluetooth
            QuickSettingsTileView bluetoothTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            bluetoothTile.setContent(R.layout.quick_settings_tile_bluetooth, inflater);
            bluetoothTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
                    if (ba.isEnabled()) {
                        ba.disable();
                    } else {
                        ba.enable();
                    }
                }
            });
            bluetoothTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    return true;
                }
            });
            mModel.addBluetoothTile(bluetoothTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    BluetoothState bluetoothState = (BluetoothState) state;
                    TextView tv = (TextView) view.findViewById(R.id.bluetooth_textview);
                    tv.setCompoundDrawablesWithIntrinsicBounds(0, state.iconId, 0, 0);
                    String label = state.label;
                    /*
                     * //TODO: Show connected bluetooth device label
                     * Set<BluetoothDevice> btDevices =
                     * mBluetoothController.getBondedBluetoothDevices(); if
                     * (btDevices.size() == 1) { // Show the name of the
                     * bluetooth device you are connected to label =
                     * btDevices.iterator().next().getName(); } else if
                     * (btDevices.size() > 1) { // Show a generic label about
                     * the number of bluetooth devices label =
                     * r.getString(R.string
                     * .quick_settings_bluetooth_multiple_devices_label,
                     * btDevices.size()); }
                     */
                    view.setContentDescription(mContext.getString(
                            R.string.accessibility_quick_settings_bluetooth,
                            bluetoothState.stateContentDescription));
                    tv.setText(label);
                }
            });
            parent.addView(bluetoothTile);
        } else if (tile.equals(LOCATION)) {
            // Location
            QuickSettingsTileView locationTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            locationTile.setContent(R.layout.quick_settings_tile_location, inflater);
            locationTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean locationEnabled = Settings.Secure.isLocationProviderEnabled(
                            mContext.getContentResolver(),
                            LocationManager.GPS_PROVIDER);

                    Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
                            LocationManager.GPS_PROVIDER, !locationEnabled);
                }
            });
            locationTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    return true;
                }
            });
            mModel.addLocationTile(locationTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    boolean locationEnabled = Settings.Secure.isLocationProviderEnabled(
                            mContext.getContentResolver(),
                            LocationManager.GPS_PROVIDER);

                    TextView tv = (TextView) view.findViewById(R.id.location_textview);
                    ImageView iv = (ImageView) view.findViewById(R.id.location_image);
                    if (locationEnabled) {
                        tv.setText("GPS ON");
                        iv.setImageResource(R.drawable.ic_qs_gps_on);
                    } else {
                        tv.setText("GPS OFF");
                        iv.setImageResource(R.drawable.ic_qs_gps_off);
                    }
                }
            });
            parent.addView(locationTile);
        } else if (tile.equals(WIFIAP)) {
            // Wifi AP
            QuickSettingsTileView WifiApTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            WifiApTile.setContent(R.layout.quick_settings_tile_wifiap, inflater);
            WifiApTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final WifiManager wifiManager = (WifiManager) mContext
                            .getSystemService(Context.WIFI_SERVICE);
                    int WifiApState = wifiManager.getWifiApState();

                    if (WifiApState == WifiManager.WIFI_AP_STATE_DISABLED
                            || WifiApState == WifiManager.WIFI_AP_STATE_DISABLING) {
                        wifiManager.setWifiEnabled(false);
                        wifiManager.setWifiApEnabled(null, true);
                    } else if (WifiApState == WifiManager.WIFI_AP_STATE_ENABLED
                            || WifiApState == WifiManager.WIFI_AP_STATE_ENABLING) {
                        wifiManager.setWifiApEnabled(null, false);
                    }
                }
            });
            WifiApTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity("android.settings.TETHER_SETTINGS");
                    return true;
                }
            });
            mModel.addWifiApTile(WifiApTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.wifiap_textview);
                    ImageView iv = (ImageView) view.findViewById(R.id.wifiap_image);

                    final WifiManager wifiManager = (WifiManager) mContext
                            .getSystemService(Context.WIFI_SERVICE);
                    int WifiApState = wifiManager.getWifiApState();

                    if (WifiApState == WifiManager.WIFI_AP_STATE_DISABLED
                            || WifiApState == WifiManager.WIFI_AP_STATE_DISABLING) {
                        tv.setText("Wifi AP Off");
                        iv.setImageResource(R.drawable.ic_qs_wifi_ap_off);
                    } else if (WifiApState == WifiManager.WIFI_AP_STATE_ENABLED
                            || WifiApState == WifiManager.WIFI_AP_STATE_ENABLING) {
                        tv.setText("Wifi AP On");
                        iv.setImageResource(R.drawable.ic_qs_wifi_ap_on);
                    }
                }
            });
            parent.addView(WifiApTile);
        } else if (tile.equals(TORCH)) {
            // Torch
            QuickSettingsTileView torchTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            torchTile.setContent(R.layout.quick_settings_tile_torch, inflater);
            torchTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTorchEnabled = !mTorchEnabled;
                    setTorchEnabled(mTorchEnabled);

                    mContext.sendBroadcast(new Intent(INTENT_UPDATE_TORCH_TILE));
                }
            });
            mModel.addTorchTile(torchTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.torch_textview);
                    ImageView iv = (ImageView) view.findViewById(R.id.torch_image);

                    if (mTorchEnabled) {
                        tv.setText("Torch On");
                        iv.setImageResource(R.drawable.ic_qs_torch_on);
                    } else {
                        tv.setText("Torch Off");
                        iv.setImageResource(R.drawable.ic_qs_torch_off);
                    }
                }
            });
            parent.addView(torchTile);
        } else if (tile.equals(SCREEN)) {
            // Screen
            QuickSettingsTileView screenTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            screenTile.setContent(R.layout.quick_settings_tile_screen, inflater);
            screenTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PowerManager pm = (PowerManager) mContext
                            .getSystemService(Context.POWER_SERVICE);
                    pm.goToSleep(SystemClock.uptimeMillis());
                }
            });
            mModel.addScreenTile(screenTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.screen_tileview);
                    tv.setText("Screen Off");
                }
            });
            parent.addView(screenTile);
        } else if (tile.equals(BRIGHTNESS)) {
            QuickSettingsTileView brightnessTile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            brightnessTile.setContent(R.layout.quick_settings_tile_brightness, inflater);
            brightnessTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    collapsePanels();
                    showBrightnessDialog();
                }
            });
            mModel.addBrightnessTile(brightnessTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {
                    TextView tv = (TextView) view.findViewById(R.id.brightness_textview);
                    tv.setCompoundDrawablesWithIntrinsicBounds(0, state.iconId, 0, 0);
                    tv.setText(state.label);
                    dismissBrightnessDialog(mBrightnessDialogShortTimeout);
                }
            });
            parent.addView(brightnessTile);   
        } else if (tile.equals(EXPANDED_DESKTOP)) {
            QuickSettingsTileView expanded_desktop_Tile = (QuickSettingsTileView)
                    inflater.inflate(mQsTileRes, parent, false);
            expanded_desktop_Tile.setContent(R.layout.quick_settings_tile_expanded_desktop,
                    inflater);
            expanded_desktop_Tile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.EXPANDED_DESKTOP_STATE, 0) == 1;
                    Settings.System.putInt(mContext.getContentResolver(),
                            Settings.System.EXPANDED_DESKTOP_STATE, enabled ? 0 : 1);
                }
            });
            expanded_desktop_Tile.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    startSettingsActivity(new Intent()
                            .setAction(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .setClassName("org.kat.controlcenter", "org.kat.controlcenter.Main")
                            .putExtra("eos_incoming_last_frag_viewed", 1));
                    return true;
                }
            });
            mModel.addExpandedDesktopTile(expanded_desktop_Tile,
                    new QuickSettingsModel.RefreshCallback() {
                        @Override
                        public void refreshView(QuickSettingsTileView view, State state) {
                            TextView tv = (TextView) view
                                    .findViewById(R.id.expanded_desktop_textview);
                            ImageView iv = (ImageView) view
                                    .findViewById(R.id.expanded_desktop_image);
                            if (state.enabled) {
                                iv.setImageResource(R.drawable.ic_qs_expanded_desktop_on);
                                tv.setText(mContext
                                        .getString(R.string.quick_settings_expanded_desktop));
                                // collapse panel if tablet ui or statusbar hides
                                // otherwise we have a lingering panel without a bar
                                if (EOSUtils.hasSystemBar(mContext)
                                        || Settings.System.getInt(mContext.getContentResolver(),
                                                Settings.System.EXPANDED_DESKTOP_STYLE, 0) == 2) {
                                    collapsePanels();
                                }
                            } else {
                                iv.setImageResource(R.drawable.ic_qs_expanded_desktop_off);
                                tv.setText(mContext
                                        .getString(R.string.quick_settings_expanded_desktop_off));
                            }
                        }
                    });
            parent.addView(expanded_desktop_Tile);
        }
    }

    private List<String> getTilesOrderedList() {
        List<String> tiles;
        String tempTilesString = Settings.System.getString(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_PANEL_ENABLED_TILES);
        if (tempTilesString != null) {
            tiles = Arrays.asList(tempTilesString.split("\\|"));
        } else {
            tiles = Arrays.asList(EOSConstants.SYSTEMUI_PANEL_DEFAULTS);
        }
        return tiles;
    }

    private void setTorchEnabled(boolean enabled) {

        if (mCamera == null) {
            mCamera = Camera.open();
        }

        if (enabled) {
            if (mSurfaceTexture == null) {
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        textures[0]);
                GLES20.glTexParameterf(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
                GLES20.glTexParameterf(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
                GLES20.glTexParameteri(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(
                        GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

                mSurfaceTexture = new SurfaceTexture(textures[0]);
                try {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                } catch (Exception e) {
                }
                mCamera.startPreview();
            }

            Camera.Parameters mParams = mCamera.getParameters();
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mParams);

            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            if (mWakeLock == null) {
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QS_Torch");
            }
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        } else {
            Camera.Parameters mParams = mCamera.getParameters();
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mParams);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mSurfaceTexture = null;

            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED.equals(action)) {
                WifiDisplayStatus status = (WifiDisplayStatus) intent.getParcelableExtra(
                        DisplayManager.EXTRA_WIFI_DISPLAY_STATUS);
                mWifiDisplayStatus = status;
                applyWifiDisplayStatus();
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                mBluetoothState.enabled = (state == BluetoothAdapter.STATE_ON);
                applyBluetoothStatus();
            } else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int status = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                        BluetoothAdapter.STATE_DISCONNECTED);
                mBluetoothState.connected = (status == BluetoothAdapter.STATE_CONNECTED);
                applyBluetoothStatus();
            } else if (Intent.ACTION_USER_SWITCHED.equals(action)) {
                reloadUserInfo();
            }
        }
    };

    private final BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ContactsContract.Intents.ACTION_PROFILE_CHANGED.equals(action) ||
                    Intent.ACTION_USER_INFO_CHANGED.equals(action)) {
                try {
                    final int userId = ActivityManagerNative.getDefault().getCurrentUser().id;
                    if (getSendingUserId() == userId) {
                        reloadUserInfo();
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Couldn't get current user id for profile change", e);
                }
            }

        }
    };
}
