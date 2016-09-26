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

package com.android.systemui.tuner.katkiss;

import org.meerkats.katkiss.KKC;
import org.meerkats.katkiss.KatUtils;
import org.meerkats.katkiss.WMController;

import android.support.v14.preference.PreferenceFragment;
import com.android.systemui.R;

import android.support.v7.preference.CheckBoxPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;

import android.provider.Settings;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.app.Dialog;
import com.android.internal.logging.MetricsLogger;


public class UISettings extends PreferenceFragment implements Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String KEY_UI_MODE = "kk_ui_mode";
    private static final String KEY_UI_BARSIZE = "kk_ui_barsize";
    private ContentResolver mResolver; 

    private ListPreference _uiModeList, _uiBarSizeList, _uiImmersiveModeType;
    private CheckBoxPreference _inputNotification, _batteryIcon, _batteryText, _batteryTextOnIcon, _batteryTextPercent ;
    private CheckBoxPreference _clockTime, _clockDate;
    private CheckBoxPreference _recentsKillall, _recentsMem, _recentsMultiWindowIcons;
    private CheckBoxPreference _btnSwitchToPrevious, _btnSplitViewAuto, _btnSplitViewAuto3, _btnSplitViewAuto4, _btnRelaunchFloating;
    private CheckBoxPreference _immersiveMode, _autoExpanded;
    private CheckBoxPreference _enablePanelsDropShadow;
    private CheckBoxPreference _test;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mResolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.kk_ui_settings);
        _uiModeList = (ListPreference) findPreference(KEY_UI_MODE);
        _uiImmersiveModeType = (ListPreference) findPreference(KKC.S.USER_IMMERSIVE_MODE_TYPE);
        _uiBarSizeList = (ListPreference) findPreference(KEY_UI_BARSIZE);
        
        _inputNotification = (CheckBoxPreference) findPreference(KKC.S.INPUTMETHOD_SHOWNOTIFICATION);
        _batteryIcon = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BATTERY_ICON);
        _batteryText = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BATTERY_TEXT);
        _batteryTextOnIcon = (CheckBoxPreference) findPreference(KKC.S.NATIVE_BATTERY_TEXT_ON_ICON);
        _batteryTextPercent = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BATTERY_TEXT_PERCENT);
        _clockTime = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_CLOCK_TIME);
        _clockDate = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_CLOCK_DATE);
        _recentsKillall =  (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_RECENTS_KILLALL_BUTTON);
        _recentsMem = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_RECENTS_MEM_DISPLAY);
        _recentsMultiWindowIcons = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_RECENTS_MULTIWINDOW_ICONS);

        _btnSwitchToPrevious = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BTN_SWITCH_TOPREVIOUS);
        _btnSplitViewAuto = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BTN_SPLITVIEW_AUTO);
        _btnSplitViewAuto3 = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BTN_SPLITVIEW_AUTO + "3");
        _btnSplitViewAuto4 = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BTN_SPLITVIEW_AUTO + "4");
        _btnRelaunchFloating = (CheckBoxPreference) findPreference(KKC.S.SYSTEMUI_BTN_RELAUNCH_FLOATING);

        _autoExpanded = (CheckBoxPreference) findPreference(KKC.S.AUTO_EXPANDED_DESKTOP_ONDOCK);
        _immersiveMode = (CheckBoxPreference) findPreference(KKC.S.USER_IMMERSIVE_MODE);
        _enablePanelsDropShadow = (CheckBoxPreference) findPreference(KKC.S.ENABLE_PANELS_DROPSHADOW);
        _test = (CheckBoxPreference) findPreference("test");

        refreshState();

        if(_uiModeList != null) _uiModeList.setOnPreferenceChangeListener(this);
        if(_uiImmersiveModeType != null) _uiImmersiveModeType.setOnPreferenceChangeListener(this);
        if(_uiBarSizeList != null) _uiBarSizeList.setOnPreferenceChangeListener(this);
        if(_inputNotification != null) _inputNotification.setOnPreferenceChangeListener(this);
        if(_batteryIcon != null) _batteryIcon.setOnPreferenceChangeListener(this);
        if(_batteryText != null) _batteryText.setOnPreferenceChangeListener(this);
        if(_batteryTextOnIcon != null) _batteryTextOnIcon.setOnPreferenceChangeListener(this);
        if(_batteryTextPercent != null) _batteryTextPercent.setOnPreferenceChangeListener(this);

        if(_clockTime != null) _clockTime.setOnPreferenceChangeListener(this);
        
        if(_recentsKillall != null) _recentsKillall.setOnPreferenceChangeListener(this);
        if(_recentsMem != null) _recentsMem.setOnPreferenceChangeListener(this);
        if(_recentsMultiWindowIcons != null) _recentsMultiWindowIcons.setOnPreferenceChangeListener(this);

        if(_btnSwitchToPrevious != null) _btnSwitchToPrevious.setOnPreferenceChangeListener(this);
        if(_btnSplitViewAuto != null) _btnSplitViewAuto.setOnPreferenceChangeListener(this);
        if(_btnSplitViewAuto3 != null) _btnSplitViewAuto3.setOnPreferenceChangeListener(this);
        if(_btnSplitViewAuto4 != null) _btnSplitViewAuto4.setOnPreferenceChangeListener(this);
        if(_btnRelaunchFloating !=null) _btnRelaunchFloating.setOnPreferenceChangeListener(this);
        
        if(_immersiveMode != null) _immersiveMode.setOnPreferenceChangeListener(this);
        if(_autoExpanded != null) _autoExpanded.setOnPreferenceChangeListener(this);
        if(_enablePanelsDropShadow != null) _enablePanelsDropShadow.setOnPreferenceChangeListener(this);
        if(_test != null) _test.setOnPreferenceChangeListener(this);
    }


    private void refreshState() {
        int valInt;
      if(_uiModeList != null) 
      {
        valInt =  Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_UI_MODE, KKC.S.SYSTEMUI_UI_MODE_NAVBAR_BALANCED );
        _uiModeList.setDefaultValue(String.valueOf(valInt));
        _uiModeList.setValue(String.valueOf(valInt));
      }

      if(_uiImmersiveModeType != null) 
      {
        valInt =  Settings.System.getInt(mResolver, KKC.S.USER_IMMERSIVE_MODE_TYPE, 0 );
        _uiImmersiveModeType.setDefaultValue(String.valueOf(valInt));
        _uiImmersiveModeType.setValue(String.valueOf(valInt));
      }

      if(_uiBarSizeList != null)
      {
        valInt =  Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_UI_BARSIZE, KKC.S.SYSTEMUI_BARSIZE_MODE_SLIM );
        _uiBarSizeList.setDefaultValue(String.valueOf(valInt));
        _uiBarSizeList.setValue(String.valueOf(valInt));
       } 

      if(_inputNotification != null) _inputNotification.setChecked(Settings.System.getInt(mResolver, KKC.S.INPUTMETHOD_SHOWNOTIFICATION, 0) == 1);

      if(_batteryIcon != null) _batteryIcon.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BATTERY_ICON, 1) == 1);

      if(_batteryText != null)
        _batteryText.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BATTERY_TEXT, 1) == 1);
      if(_batteryTextOnIcon != null)
        _batteryTextOnIcon.setChecked(Settings.System.getInt(mResolver, KKC.S.NATIVE_BATTERY_TEXT_ON_ICON, 0) == 1);
      if(_batteryTextPercent != null)
        _batteryTextPercent.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BATTERY_TEXT_PERCENT, 1) == 1);

      if(_clockTime != null) _clockTime.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_CLOCK_TIME, 1) == 1);
        //_clockDate.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_CLOCK_DATE, 0) == 1);
        
      if(_recentsKillall != null) _recentsKillall.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_RECENTS_KILLALL_BUTTON, 1) == 1);
      if(_recentsMem != null) _recentsMem.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_RECENTS_MEM_DISPLAY, 0) == 1);        
      if(_recentsMultiWindowIcons != null) _recentsMultiWindowIcons.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_RECENTS_MULTIWINDOW_ICONS, 1) == 1);        

      if(_btnSwitchToPrevious != null) _btnSwitchToPrevious.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BTN_SWITCH_TOPREVIOUS, 1) == 1);        
      if(_btnSplitViewAuto != null)  _btnSplitViewAuto.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BTN_SPLITVIEW_AUTO, 1) == 1);        
      if(_btnSplitViewAuto3 != null)  _btnSplitViewAuto3.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BTN_SPLITVIEW_AUTO + "3", 1) == 1);        
      if(_btnSplitViewAuto4 != null)  _btnSplitViewAuto4.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BTN_SPLITVIEW_AUTO + "4", 1) == 1);        
      if(_btnRelaunchFloating != null)  _btnRelaunchFloating.setChecked(Settings.System.getInt(mResolver, KKC.S.SYSTEMUI_BTN_RELAUNCH_FLOATING, 1) == 1);        

      if(_immersiveMode != null)  _immersiveMode.setChecked(Settings.System.getInt(mResolver, KKC.S.USER_IMMERSIVE_MODE, 0) == 1);
      if(_autoExpanded != null)  _autoExpanded.setChecked(Settings.System.getInt(mResolver, KKC.S.AUTO_EXPANDED_DESKTOP_ONDOCK, 0) == 1);
      if(_enablePanelsDropShadow != null)   _enablePanelsDropShadow.setChecked(Settings.System.getInt(mResolver, KKC.S.ENABLE_PANELS_DROPSHADOW, 0) == 1);
    }
    
    @Override
    public void onResume() {
        super.onResume();

        refreshState();
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
	if(key == null) return true;

	    if (key.equals(KEY_UI_MODE)) 
	    {
	      int mode = Integer.parseInt((String) objValue);
	
	      Settings.System.putInt(mResolver, KKC.S.SYSTEMUI_UI_MODE, mode);
	      KatUtils.sendIntentToWindowManager(getActivity(), KKC.I.UI_CHANGED, KKC.I.CMD_BARTYPE_CHANGED, true);
	    }
	    if (key.equals(KKC.S.USER_IMMERSIVE_MODE_TYPE)) 
	    {
	      int mode = Integer.parseInt((String) objValue);
	      Settings.System.putInt(mResolver, KKC.S.USER_IMMERSIVE_MODE_TYPE, mode);
	    }
        else if(key.equals(KEY_UI_BARSIZE))
        {
          int size = Integer.parseInt((String) objValue);
          Settings.System.putInt(mResolver, KKC.S.SYSTEMUI_UI_BARSIZE, size);
          KatUtils.sendIntentToWindowManager(getActivity(), KKC.I.UI_CHANGED, KKC.I.CMD_BARSIZE_CHANGED, true);
        }
        else if(key.equals(KKC.S.USER_IMMERSIVE_MODE))
        {
            Boolean val = (Boolean) objValue;
       	  	KatUtils.expandedDesktop(getActivity(), val);
        }
        else if (key.equals("test"))
        {
          Boolean val = (Boolean) objValue;
          Settings.System.putInt(mResolver, KKC.S.DEVICE_SETTINGS_RIGHTCLICK_MODE, val?1:0);
        }
        // other CheckBox
        else if (preference instanceof CheckBoxPreference)
        {
          Boolean val = (Boolean) objValue;
          Settings.System.putInt(mResolver, key, val?1:0);
        }

        return true;
    }

    

    @Override
    public boolean onPreferenceClick(Preference preference) {
/*        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
*/
        return false;
    }

}
