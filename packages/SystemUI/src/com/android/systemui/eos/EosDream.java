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

package com.android.systemui.eos;

import android.provider.Settings;
import android.service.dreams.DreamService;

import com.android.systemui.eos.EosDreamEngine.Dream;

public class EosDream extends DreamService {

    private static boolean mIsInteractiveEnabled;
    private Dream mDream;

    @Override
    public void onCreate() {
        super.onCreate();
        // we need to catch this here for initial service start
        mIsInteractiveEnabled = (Settings.System.getInt(getContentResolver(),
                EosDreamSettings.INTERACTIVE, 1) == 1 ? true : false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(mIsInteractiveEnabled);
        setFullscreen(true);
        mDream = new Dream(this, null);
        setContentView(mDream);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        mDream.startAnimation();
    }

    @Override
    public void onDreamingStopped() {
        mDream.stopAnimation();
        super.onDreamingStopped();
    }

    public static void putInteractive(boolean value) {
        mIsInteractiveEnabled = value;
    }
}
