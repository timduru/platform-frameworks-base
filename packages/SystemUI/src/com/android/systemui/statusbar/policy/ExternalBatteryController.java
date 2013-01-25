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
import android.os.BatteryManager;
import com.android.systemui.R;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ExternalBatteryController extends BatteryController {

    public ExternalBatteryController(Context context) {
        super(context);

        mVisibilityOverride = true;
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        context.registerReceiver(this, intentFilter);
    }

    protected void initResources() {
        iconCharge = R.drawable.stat_sys_kb_battery_charge;
        iconBattery = R.drawable.stat_sys_kb_battery;
        levelIntentParam = BatteryManager.EXTRA_DOCK_LEVEL;
        statusIntentParam = BatteryManager.EXTRA_DOCK_STATUS;
    }

    protected boolean isCharging(int status) {
        return (status == 2);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_DOCK_EVENT)) {
            final int status = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);
            mVisibility = (status != Intent.EXTRA_DOCK_STATE_UNDOCKED);
            updateViews();
            updateLabelPercent();
        }
        else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            final int status = intent.getIntExtra(statusIntentParam, 0);
            mVisibility = (status != BatteryManager.DOCK_STATE_UNDOCKED);

            super.onReceive(context, intent);
        }
    }
}


