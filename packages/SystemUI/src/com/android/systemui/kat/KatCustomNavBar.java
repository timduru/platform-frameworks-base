package com.android.systemui.kat;


import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import com.android.systemui.statusbar.phone.NavigationBarInflaterView;


public class KatCustomNavBar
{
    private HashMap<String, ButtonInfo> _buttonsInfo = new HashMap<>();

    public class ButtonInfo
    {
        public String name;
        public int labelID;
        private int layoutID = R.layout.kk_nav_btn_custom;
        private int imageID;

        public ButtonInfo(String name, int labelID, int imageID, int layoutID)
        {
            this.name = name;
            this.labelID = labelID;
            this.imageID = imageID;
            if(layoutID != 0) this.layoutID = layoutID;
        }
    }

    public KatCustomNavBar() {
        String name = "";

// Native Buttons
        name = NavigationBarInflaterView.BACK;
        _buttonsInfo.put(name,  new ButtonInfo(null, R.string.accessibility_back, 0, R.layout.back));
        name = NavigationBarInflaterView.HOME;
        _buttonsInfo.put(name,  new ButtonInfo(null, R.string.accessibility_home, 0, R.layout.home));
        name = NavigationBarInflaterView.RECENT;
        _buttonsInfo.put(name,  new ButtonInfo(null, R.string.accessibility_recent, 0, R.layout.recent_apps));
        name = NavigationBarInflaterView.MENU_IME;
        _buttonsInfo.put(name,  new ButtonInfo(null, R.string.menu_ime, 0, R.layout.menu_ime));
        name = NavigationBarInflaterView.CLIPBOARD;
        _buttonsInfo.put(name,  new ButtonInfo(null, R.string.clipboard, 0, R.layout.clipboard));
        name = ButtonSpec.KEY;
        _buttonsInfo.put(name,  new ButtonInfo(null, R.string.keycode, 0, R.layout.custom_key));

//KK Buttons
        name = "switch_toprevious_task";
        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.nav_btn_switch_label, R.drawable.ic_sysbar_switch_toprevious_task, 0));
        name = "expanded_desktop";
        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.quick_settings_immersive_mode_label, R.drawable.ic_navbar_immersive, 0));
        name = "killcurrent";
        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.nav_btn_kill_process_label, R.drawable.ic_close_white, 0));
//        name = "relaunch_floating";
//        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.nav_btn_relaunch_floating_label, R.drawable.ic_sysbar_splitview_auto, 0));
    }

    public String[] getButtonList(String[] baseList)
    {
        HashSet<String> fullList = new HashSet<>();
        Collections.addAll(fullList, baseList);
        fullList.addAll(_buttonsInfo.keySet());
         return fullList.toArray(new String[fullList.size()]);
    }

    public View inflateButton(String name, LayoutInflater inflater, String extra, ViewGroup parent, boolean landscape)
    {
        View v = null;

        ButtonInfo info = null;
        if(name.startsWith(ButtonSpec.KEY)) info = _buttonsInfo.get(ButtonSpec.KEY);
        else info = _buttonsInfo.get(name);

        if(info == null) return null; 

        v = inflater.inflate(info.layoutID, parent, false);

        if(v instanceof KeyButtonView)
        {
            KeyButtonView btn = ((KeyButtonView) v);
            if(info.imageID != 0) btn.setImageResource(info.imageID);
            if(info.name != null) btn.setClickAction(info.name);
            if(extra != null && !extra.equals("")) btn.setLongPressAction(extra);
        }

        return v;
    }

    public CharSequence getLabel(String name, Context context)
    {
        ButtonInfo info = _buttonsInfo.get(name);
        if(info != null) return context.getString(info.labelID);
        else return name;
    }
}
