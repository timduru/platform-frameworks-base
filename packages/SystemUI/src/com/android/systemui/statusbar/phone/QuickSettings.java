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
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.R;

import com.android.systemui.statusbar.phone.QuickSettingsModel.BluetoothState;
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

import org.teameos.jellybean.settings.EOSConstants;

import javax.microedition.khronos.opengles.GL10;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.LevelListDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.media.AudioManager;
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
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Profile;
import android.provider.Settings;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import android.util.Log;
import android.util.Pair;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

/**
 *
 */
class QuickSettings {
    private static final String TAG = "QuickSettings";
    public static final boolean SHOW_IME_TILE = false;

    private Context mContext;
    private PanelBar mBar;
    private QuickSettingsModel mModel;
    private ViewGroup mContainerView;

    private DisplayManager mDisplayManager;
    private WifiDisplayStatus mWifiDisplayStatus;
    private PhoneStatusBar mStatusBarService;
    private BluetoothState mBluetoothState;

    private BrightnessController mBrightnessController;
    private BluetoothController mBluetoothController;

    private Dialog mBrightnessDialog;
    private int mBrightnessDialogShortTimeout;
    private int mBrightnessDialogLongTimeout;

    private AsyncTask<Void, Void, Pair<String, Drawable>> mUserInfoTask;

    private LevelListDrawable mBatteryLevels;
    private LevelListDrawable mChargingBatteryLevels;

    boolean mTilesSetUp = false;

    private Handler mHandler;
	
	private Camera mCamera;
	private SurfaceTexture mSurfaceTexture;
	private WakeLock mWakeLock;
	private boolean mTorchEnabled = false;
	public static int mVolumeStream = AudioManager.STREAM_MUSIC;
    private List<String> mEnabledTiles;
    private int mSeekbarSpan;
    private Boolean mUserEnabled = true;
	
    private String AVATAR = EOSConstants.SYSTEMUI_PANEL_USER_TILE;
	private String SETTINGS = EOSConstants.SYSTEMUI_PANEL_SETTINGS_TILE;
	private String SEEKBAR = EOSConstants.SYSTEMUI_PANEL_SEEKBAR_TILE;
	private String BATTERY = EOSConstants.SYSTEMUI_PANEL_BATTERY_TILE;
	private String ROTATION = EOSConstants.SYSTEMUI_PANEL_ROTATION_TILE;
	private String AIRPLANE = EOSConstants.SYSTEMUI_PANEL_AIRPLANE_TILE;
	private String WIFI = EOSConstants.SYSTEMUI_PANEL_WIFI_TILE;
	private String DATA = EOSConstants.SYSTEMUI_PANEL_DATA_TILE;
	private String BT = EOSConstants.SYSTEMUI_PANEL_BT_TILE;
	private String SCREEN = EOSConstants.SYSTEMUI_PANEL_SCREENOFF_TILE;
	private String LOCATION = EOSConstants.SYSTEMUI_PANEL_LOCATION_TILE;
	private String RINGER = EOSConstants.SYSTEMUI_PANEL_RINGER_TILE;
	private String WIFIAP = EOSConstants.SYSTEMUI_PANEL_WIFIAP_TILE;
	private String TORCH = EOSConstants.SYSTEMUI_PANEL_TORCH_TILE;
	private String INTENT_UPDATE_TORCH_TILE = EOSConstants.SYSTEMUI_PANEL_TORCH_INTENT;
    private String INTENT_UPDATE_VOLUME_OBSERVER_STREAM = EOSConstants.SYSTEMUI_PANEL_VOLUME_OBSERVER_STREAM_INTENT;
	
    // The set of QuickSettingsTiles that have dynamic spans (and need to be updated on
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
        if (mModel != null) mModel.removeReceivers();
    }

    void setBar(PanelBar bar) {
        mBar = bar;
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

    void setup(NetworkController networkController, BluetoothController bluetoothController,
            BatteryController batteryController, LocationController locationController) {
        mBluetoothController = bluetoothController;

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

                // Fall back to the UserManager nickname if we can't read the name from the local
                // profile below.
                String name = userName;
                Drawable avatar = null;
                Bitmap rawAvatar = um.getUserIcon(userId);
                if (rawAvatar != null) {
                    avatar = new BitmapDrawable(mContext.getResources(), rawAvatar);
                } else {
                    avatar = mContext.getResources().getDrawable(R.drawable.ic_qs_default_user);
                }

                // If it's a single-user device, get the profile name, since the nickname is not
                // usually valid
                if (um.getUsers().size() <= 1) {
                    // Try and read the display name from the local profile
                    final Cursor cursor = context.getContentResolver().query(
                            Profile.CONTENT_URI, new String[] {Phone._ID, Phone.DISPLAY_NAME},
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
        // Setup the tiles that we are going to be showing (including the temporary ones)
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mUserEnabled = mModel.isToggleEnabled(AVATAR);

        addSystemTiles(mContainerView, inflater);
        addTemporaryTiles(mContainerView, inflater);

        if (mUserEnabled) queryForUserInformation();
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
        if (onlyProvisioned && !getService().isDeviceProvisioned()) return;
        try {
            // Dismiss the lock screen when Settings starts.
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        getService().animateCollapsePanels();
    }

    private void addUserTiles(ViewGroup parent, LayoutInflater inflater) {
		// User
		QuickSettingsTileView userTile = (QuickSettingsTileView)
				inflater.inflate(R.layout.quick_settings_tile, parent, false);
		userTile.setContent(R.layout.quick_settings_tile_user, inflater);
		userTile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mBar.collapseAllPanels(true);
				final UserManager um =
						(UserManager) mContext.getSystemService(Context.USER_SERVICE);
				if (um.getUsers(true).size() > 1) {
					try {
						WindowManagerGlobal.getWindowManagerService().lockNow(
								LockPatternUtils.USER_SWITCH_LOCK_OPTIONS);
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
        QuickSettingsTileView timeTile = (QuickSettingsTileView)
                inflater.inflate(R.layout.quick_settings_tile, parent, false);
        timeTile.setContent(R.layout.quick_settings_tile_time, inflater);
        timeTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quick. Clock. Quick. Clock. Quick. Clock.
                startSettingsActivity(Intent.ACTION_QUICK_CLOCK);
            }
        });
        mModel.addTimeTile(timeTile, new QuickSettingsModel.RefreshCallback() {
            @Override
            public void refreshView(QuickSettingsTileView view, State alarmState) {}
        });
        parent.addView(timeTile);
        mDynamicSpannedTiles.add(timeTile);
        */
    }

    private void addSystemTiles(ViewGroup parent, LayoutInflater inflater) {
        List<String> mGesturesOrderedList = getGesturesOrderedList();
        for (int i = 0; i < mGesturesOrderedList.size(); i++) {
			loadTile(mGesturesOrderedList.get(i), parent, inflater);
		}
    }

    private void addTemporaryTiles(final ViewGroup parent, final LayoutInflater inflater) {
        // Alarm tile
        QuickSettingsTileView alarmTile = (QuickSettingsTileView)
                inflater.inflate(R.layout.quick_settings_tile, parent, false);
        alarmTile.setContent(R.layout.quick_settings_tile_alarm, inflater);
        alarmTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Jump into the alarm application
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.google.android.deskclock",
                        "com.android.deskclock.AlarmClock"));
                startSettingsActivity(intent);
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
                inflater.inflate(R.layout.quick_settings_tile, parent, false);
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
                    inflater.inflate(R.layout.quick_settings_tile, parent, false);
            imeTile.setContent(R.layout.quick_settings_tile_ime, inflater);
            imeTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mBar.collapseAllPanels(true);
                        Intent intent = new Intent(Settings.ACTION_SHOW_INPUT_METHOD_PICKER);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
                        pendingIntent.send();
                    } catch (Exception e) {}
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
                inflater.inflate(R.layout.quick_settings_tile, parent, false);
        bugreportTile.setContent(R.layout.quick_settings_tile_bugreport, inflater);
        bugreportTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBar.collapseAllPanels(true);
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
        QuickSettingsTileView mediaTile = (QuickSettingsTileView)
                inflater.inflate(R.layout.quick_settings_tile, parent, false);
        mediaTile.setContent(R.layout.quick_settings_tile_media, inflater);
        parent.addView(mediaTile);
        QuickSettingsTileView imeTile = (QuickSettingsTileView)
                inflater.inflate(R.layout.quick_settings_tile, parent, false);
        imeTile.setContent(R.layout.quick_settings_tile_ime, inflater);
        imeTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.removeViewAt(0);
            }
        });
        parent.addView(imeTile);
        */
    }

    void updateResources() {
        Resources r = mContext.getResources();

        // Update the model
        mModel.updateResources();

        // Update the User, Time, and Settings tiles spans, and reset everything else
        int span = r.getInteger(R.integer.quick_settings_user_time_settings_tile_span);
        for (QuickSettingsTileView v : mDynamicSpannedTiles) {
            v.setColumnSpan(span);
        }
        ((QuickSettingsContainerView)mContainerView).updateResources();
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
                        @Override public void run() {
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
	
	private void loadTile(String tile, ViewGroup parent, LayoutInflater inflater) {
        if (tile.equals(AVATAR)) {
            if (mUserEnabled) addUserTiles(mContainerView, inflater);
        } else if (tile.equals(SETTINGS)) {
		// Settings
			QuickSettingsTileView settingsTile = (QuickSettingsTileView)
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
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
					controlPanelIntent.setClassName("org.eos.controlcenter", "org.eos.controlcenter.Main"); 
					controlPanelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(controlPanelIntent);
					
					mBar.collapseAllPanels(true);
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
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
			ringerTile.setContent(R.layout.quick_settings_tile_ringer, inflater);
			ringerTile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);				
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
					
					AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);				
					switch (am.getRingerMode()) {
						case AudioManager.RINGER_MODE_NORMAL:
							tv.setText("Normal");
							iv.setImageResource(R.drawable.stat_ring_on);
							break;
						case AudioManager.RINGER_MODE_VIBRATE:
							tv.setText("Vibrate");
							iv.setImageResource(R.drawable.stat_ring_vibrate);
							break;	
						case AudioManager.RINGER_MODE_SILENT:
							tv.setText("Silent");
							iv.setImageResource(R.drawable.stat_ring_off);
							break;	
					}					
				}
			});
			parent.addView(ringerTile);
			// mDynamicSpannedTiles.add(ringerTile);
		} else if (tile.equals(SEEKBAR)) {
        // Seekbar
            QuickSettingsTileView seekbarTile = (QuickSettingsTileView)
                    inflater.inflate(R.layout.quick_settings_tile, parent, false);
            seekbarTile.setContent(R.layout.quick_settings_tile_seekbar, inflater);
            seekbarTile.setColumnSpan(mSeekbarSpan);
            
            final Switch swView = (Switch) seekbarTile.findViewById(R.id.view_switch);
            final SeekBar sbVolume = (SeekBar) seekbarTile.findViewById(R.id.volume_seekbar);
            final CheckBox cbVolume = (CheckBox) seekbarTile.findViewById(R.id.volume_switch);	
            final SeekBar sbBrightness = (SeekBar) seekbarTile.findViewById(R.id.brightness_seekbar);
            final CheckBox cbBrightness = (CheckBox) seekbarTile.findViewById(R.id.brightness_switch);
            final LinearLayout brightnessView = (LinearLayout) seekbarTile.findViewById(R.id.brightness_view);
            final LinearLayout volumeView = (LinearLayout) seekbarTile.findViewById(R.id.volume_view);            
            
            final AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
            final IPowerManager ipm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
			final PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
			final int min = pm.getMinimumScreenBrightnessSetting();
			final int max = pm.getMaximumScreenBrightnessSetting();
                    
            mModel.addSeekbarTile(seekbarTile, new QuickSettingsModel.RefreshCallback() {
                @Override
                public void refreshView(QuickSettingsTileView view, State state) {	
                    sbVolume.setMax(am.getStreamMaxVolume(mVolumeStream));
                    sbVolume.setProgress(am.getStreamVolume(mVolumeStream));
                    if (mVolumeStream == AudioManager.STREAM_MUSIC) {
                        cbVolume.setChecked(true);
                        cbVolume.setText("Media");
                    } else {
                        cbVolume.setChecked(false);
                        cbVolume.setText("Ring");
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

                    if (automatic == 1) {
                        cbBrightness.setChecked(true);
                        cbBrightness.setText("Auto");
                        sbBrightness.setEnabled(false);
                    } else {
                        cbBrightness.setChecked(false);
                        cbBrightness.setText("Manual");
                        sbBrightness.setEnabled(true);
                    }
                    sbBrightness.setMax(max - min);
                    sbBrightness.setProgress(value - min);
                }
            });
            parent.addView(seekbarTile);
            
            swView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Animation slideInFromLeft = AnimationUtils.loadAnimation(mContext, R.anim.in_from_left);
                        slideInFromLeft.setAnimationListener(new AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                volumeView.setVisibility(View.VISIBLE);
                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                            @Override
                            public void onAnimationEnd(Animation animation) {                             
                            }
                        });
                        volumeView.startAnimation(slideInFromLeft);

                        Animation slideOutToRight = AnimationUtils.loadAnimation(mContext, R.anim.out_to_right);
                        slideOutToRight.setAnimationListener(new AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                            @Override
                            public void onAnimationEnd(Animation animation) {   
                                brightnessView.setVisibility(View.GONE);
                            }
                        });
                        brightnessView.startAnimation(slideOutToRight);                      
                    } else {
                        Animation slideInFromRight = AnimationUtils.loadAnimation(mContext, R.anim.in_from_right);
                        slideInFromRight.setAnimationListener(new AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                brightnessView.setVisibility(View.VISIBLE);
                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                            @Override
                            public void onAnimationEnd(Animation animation) {                             
                            }
                        });
                        brightnessView.startAnimation(slideInFromRight);

                        Animation slideOutToLeft = AnimationUtils.loadAnimation(mContext, R.anim.out_to_left);
                        slideOutToLeft.setAnimationListener(new AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                            @Override
                            public void onAnimationEnd(Animation animation) {   
                                volumeView.setVisibility(View.GONE);
                            }
                        });
                        volumeView.startAnimation(slideOutToLeft); 
                    }
                }
            });
            
            cbVolume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mVolumeStream = AudioManager.STREAM_MUSIC;     
                        cbVolume.setText("Media");
                    } else {
                        mVolumeStream = AudioManager.STREAM_RING;
                        cbVolume.setText("Ring");
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
                                sbBrightness.setEnabled(false);
                                cbBrightness.setText("Auto");
                        } else {
                            Settings.System.putInt(mContext.getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS_MODE, 0);	
                            sbBrightness.setEnabled(true);
                            cbBrightness.setText("Manual");
                        }
                    } catch (Exception e) {
                    }
                }
            });
            
            sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    try {
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
        } else if (tile.equals(BATTERY)) {
        // Battery
            QuickSettingsTileView batteryTile = (QuickSettingsTileView)
                    inflater.inflate(R.layout.quick_settings_tile, parent, false);
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
                            ? mContext.getString(R.string.quick_settings_battery_charging_label,
                                    batteryState.batteryLevel)
                            : mContext.getString(R.string.status_bar_settings_battery_meter_format,
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
                    inflater.inflate(R.layout.quick_settings_tile, parent, false);
            rotationLockTile.setContent(R.layout.quick_settings_tile_rotation_lock, inflater);
            rotationLockTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean locked = RotationPolicy.isRotationLocked(mContext);
                    RotationPolicy.setRotationLock(mContext, !locked);
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
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
			airplaneTile.setContent(R.layout.quick_settings_tile_airplane, inflater);
			mModel.addAirplaneModeTile(airplaneTile, new QuickSettingsModel.RefreshCallback() {
				@Override
				public void refreshView(QuickSettingsTileView view, State state) {
					TextView tv = (TextView) view.findViewById(R.id.airplane_mode_textview);
					tv.setCompoundDrawablesWithIntrinsicBounds(0, state.iconId, 0, 0);

					String airplaneState = mContext.getString(
							(state.enabled) ? R.string.accessibility_desc_on
									: R.string.accessibility_desc_off);
					view.setContentDescription(
						mContext.getString(R.string.accessibility_quick_settings_airplane, airplaneState));
					tv.setText(state.label);
				}
			});
			parent.addView(airplaneTile);
		} else if (tile.equals(WIFI)) {
		// Wi-fi
			QuickSettingsTileView wifiTile = (QuickSettingsTileView)
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
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
                    inflater.inflate(R.layout.quick_settings_tile, parent, false);
            rssiTile.setContent(R.layout.quick_settings_tile_rssi, inflater);
            rssiTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
					cm.setMobileDataEnabled(!cm.getMobileDataEnabled());
                }
            });
			rssiTile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
					
					Intent roamingMenuIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
					roamingMenuIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(roamingMenuIntent);
					
					mBar.collapseAllPanels(true);
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
                    tv.setText(state.label);
                    view.setContentDescription(mContext.getResources().getString(
                            R.string.accessibility_quick_settings_mobile,
                            rssiState.signalContentDescription, rssiState.dataContentDescription,
                            state.label));
                }
            });
            parent.addView(rssiTile);
        } else if (mModel.deviceSupportsBluetooth() && tile.equals(BT)) {
		// Bluetooth
            QuickSettingsTileView bluetoothTile = (QuickSettingsTileView)
                    inflater.inflate(R.layout.quick_settings_tile, parent, false);
            bluetoothTile.setContent(R.layout.quick_settings_tile_bluetooth, inflater);
            bluetoothTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
					if(ba.isEnabled()) {
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

                    Resources r = mContext.getResources();
                    String label = state.label;
                    /*
                    //TODO: Show connected bluetooth device label
                    Set<BluetoothDevice> btDevices =
                            mBluetoothController.getBondedBluetoothDevices();
                    if (btDevices.size() == 1) {
                        // Show the name of the bluetooth device you are connected to
                        label = btDevices.iterator().next().getName();
                    } else if (btDevices.size() > 1) {
                        // Show a generic label about the number of bluetooth devices
                        label = r.getString(R.string.quick_settings_bluetooth_multiple_devices_label,
                                btDevices.size());
                    }
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
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
			locationTile.setContent(R.layout.quick_settings_tile_location, inflater);
			locationTile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean locationEnabled = Settings.Secure.isLocationProviderEnabled(mContext.getContentResolver(),
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
					boolean locationEnabled = Settings.Secure.isLocationProviderEnabled(mContext.getContentResolver(),
							LocationManager.GPS_PROVIDER);
							
					TextView tv = (TextView) view.findViewById(R.id.location_textview);
					ImageView iv = (ImageView) view.findViewById(R.id.location_image);
					if (locationEnabled) {
						tv.setText("GPS ON");
						iv.setImageResource(R.drawable.ic_qs_location_on);
					} else {
						tv.setText("GPS OFF");
						iv.setImageResource(R.drawable.ic_qs_location_off);
					}
				}
			}); 
			parent.addView(locationTile);
		} else if (tile.equals(WIFIAP)) {
		// Wifi AP
			QuickSettingsTileView WifiApTile = (QuickSettingsTileView)
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
			WifiApTile.setContent(R.layout.quick_settings_tile_wifiap, inflater);
			WifiApTile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);				
					int WifiApState = wifiManager.getWifiApState();
					
					if (WifiApState == WifiManager.WIFI_AP_STATE_DISABLED || WifiApState == WifiManager.WIFI_AP_STATE_DISABLING) {
						wifiManager.setWifiEnabled(false);
						wifiManager.setWifiApEnabled(null, true);
					} else if (WifiApState == WifiManager.WIFI_AP_STATE_ENABLED || WifiApState == WifiManager.WIFI_AP_STATE_ENABLING) {
						wifiManager.setWifiApEnabled(null, false);
					}
				}
			});
			mModel.addWifiApTile(WifiApTile, new QuickSettingsModel.RefreshCallback() {
				@Override
				public void refreshView(QuickSettingsTileView view, State state) {
					TextView tv = (TextView) view.findViewById(R.id.wifiap_textview);
					ImageView iv = (ImageView) view.findViewById(R.id.wifiap_image);
					
					final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);				
					int WifiApState = wifiManager.getWifiApState();
					
					if (WifiApState == WifiManager.WIFI_AP_STATE_DISABLED || WifiApState == WifiManager.WIFI_AP_STATE_DISABLING) {
						tv.setText("Wifi AP Off");
						iv.setImageResource(R.drawable.stat_wifi_ap_off);
					} else if (WifiApState == WifiManager.WIFI_AP_STATE_ENABLED || WifiApState == WifiManager.WIFI_AP_STATE_ENABLING) {
						tv.setText("Wifi AP On");
						iv.setImageResource(R.drawable.stat_wifi_ap_on);
					}				
				}
			});
			parent.addView(WifiApTile);
		} if (tile.equals(TORCH)) {
		// Torch
			QuickSettingsTileView torchTile = (QuickSettingsTileView)
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
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
						iv.setImageResource(R.drawable.stat_flashlight_on);
					} else {
						tv.setText("Torch Off");
						iv.setImageResource(R.drawable.stat_flashlight_off);
					}
				}
			});
			parent.addView(torchTile);
		} else if (tile.equals(SCREEN)) {
		// Screen
			QuickSettingsTileView screenTile = (QuickSettingsTileView)
					inflater.inflate(R.layout.quick_settings_tile, parent, false);
			screenTile.setContent(R.layout.quick_settings_tile_screen, inflater);
			screenTile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
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
		}
	}

    private List<String> getGesturesOrderedList() {
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
                WifiDisplayStatus status = (WifiDisplayStatus)intent.getParcelableExtra(
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
