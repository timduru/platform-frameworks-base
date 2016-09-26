/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.systemui.tuner.katkiss;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService.Tunable;

import java.util.Set;

public class KatSwitch extends SwitchPreference {

    private ContentResolver mResolver;

    public KatSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
	mResolver = context.getContentResolver();
    }

    @Override
    public void onAttached() {
        super.onAttached();
    }

    @Override
    public void onDetached() {
        super.onDetached();
    }

    @Override
    protected boolean persistBoolean(boolean value) {
	Boolean val = (Boolean) value;
        Settings.System.putInt( mResolver, getKey(), val?1:0);
        return true;
    }

}
