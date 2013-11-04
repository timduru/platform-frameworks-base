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
import android.database.ContentObserver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.util.Log;
import android.util.Slog;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import java.util.ArrayList;

import org.meerkats.katkiss.KKC;
import android.provider.Settings;
import org.meerkats.katkiss.CustomObserver;
import com.android.systemui.R;

public class BatteryController extends BroadcastReceiver implements CustomObserver.ChangeNotification {
    private static final String TAG = "StatusBar.BatteryController";

    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();
    protected int iconCharge, iconBattery;
    protected String levelIntentParam, statusIntentParam;

    private ArrayList<BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<BatteryStateChangeCallback>();

    private int mLastPercentage = 0;
    private boolean mCharging = false;
    protected boolean mBatteryAvailable = true;

    private boolean mShowIcon = true, mShowText = true, mShowTextPercent = true;

    private ContentObserver mPercentObserver;
 
    public interface BatteryStateChangeCallback {
        public void onBatteryLevelChanged(int level, boolean pluggedIn);
    }

    public BatteryController(Context context) {
        init(context);
    }

    protected void initResources() {
        iconCharge = R.drawable.stat_sys_battery_charge;
        iconBattery = R.drawable.stat_sys_battery;
        levelIntentParam = BatteryManager.EXTRA_LEVEL;
        statusIntentParam = BatteryManager.EXTRA_PLUGGED;
    }

    public void init(Context context) {
        mContext = context;
        initResources();
        refreshConf();
        new CustomObserver(context, this);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(this, filter);
/*        mPercentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateLabelPercent();
            }
        };
        EosUiController.registerObserverStateListener(mListener);
*/
    }
/*
    private void registerObservers() {
        mContext.getContentResolver().registerContentObserver( 
            Settings.System.getUriFor(EOSConstants.SYSTEMUI_BATTERY_PERCENT_VISIBLE), false, mPercentObserver);
    }

<<<<<<< HEAD
    private void unregisterObservers() {
        mContext.getContentResolver().unregisterContentObserver(mPercentObserver);
    }
*/

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
        updateLabel();
    }

    protected void updateViews() {
        final int icon = mCharging ? iconCharge : iconBattery;
        int N = mIconViews.size();
        for (int i = 0; i < N; i++) {
            ImageView v = mIconViews.get(i);
            boolean show =  mBatteryAvailable && mShowIcon;
            v.setVisibility(show? View.VISIBLE : View.GONE);
            
            if(show) {
              v.setImageResource(icon);
              v.setImageLevel(mLastPercentage);
              v.setContentDescription(mContext.getString(R.string.accessibility_battery_level, mLastPercentage));
            }
        }
    }

    public void updateLabel() {
        int N = mLabelViews.size();
        for (int i = 0; i < N; i++) {
            TextView v = mLabelViews.get(i);

            boolean show =  mBatteryAvailable && mShowText;
            v.setVisibility(show? View.VISIBLE : View.GONE);
            
            if(show) {              	
              String label = mContext.getString( R.string.status_bar_settings_battery_meter_format, mLastPercentage);            
              if (!mShowTextPercent) label = trimPercent(label);
              v.setText(label);
            }
            
        }
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        // refresh values immediately
        cb.onBatteryLevelChanged(mLastPercentage, mCharging);
        mChangeCallbacks.add(cb);
    }

    public void removeStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }


    private String trimPercent(String s) {
        if (s.contains("%"))
            return s.substring(0, s.indexOf("%"));
        else
            return s;
    }

    protected boolean isCharging(int status) {
        return (status != 0);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            final int level = intent.getIntExtra(levelIntentParam, 0);
            if (level !=0) mLastPercentage = level;
            final int status = intent.getIntExtra(statusIntentParam, 0);
            mCharging = isCharging(status);

            updateViews();
            updateLabel();

            for (BatteryStateChangeCallback cb : mChangeCallbacks) {
                cb.onBatteryLevelChanged(mLastPercentage, mCharging);
            }
        }
    }

    private void refreshConf() {
        mShowIcon = Settings.System.getInt(mContext.getContentResolver(), KKC.S.SYSTEMUI_BATTERY_ICON, 1) == 1;
        mShowText = Settings.System.getInt(mContext.getContentResolver(), KKC.S.SYSTEMUI_BATTERY_TEXT, 1) == 1;
        mShowTextPercent = Settings.System.getInt(mContext.getContentResolver(), KKC.S.SYSTEMUI_BATTERY_TEXT_PERCENT, 1) == 1;
    }
    
 // CustomObserver ChangeNotifications
    @Override
    public ArrayList<Uri> getObservedUris()
    {
      ArrayList<Uri> uris = new  ArrayList<Uri>();
      uris.add(Settings.System.getUriFor(KKC.S.SYSTEMUI_BATTERY_ICON));
      uris.add(Settings.System.getUriFor(KKC.S.SYSTEMUI_BATTERY_TEXT));
      uris.add(Settings.System.getUriFor(KKC.S.SYSTEMUI_BATTERY_TEXT_PERCENT));
      return uris;
    }

    @Override
    public void onChangeNotification(Uri uri)
    {
      Log.d(TAG, "onChangeNotification:" + uri);
      refreshConf();
      updateViews();
      updateLabel();
    }
    
}
