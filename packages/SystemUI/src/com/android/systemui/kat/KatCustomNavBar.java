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


public class KatCustomNavBar
{
    private HashMap<String, ButtonInfo> _buttonsInfo = new HashMap<>();

    class ButtonInfo
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

        name = "switch_toprevious_task";
        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.nav_btn_switch_label, R.drawable.ic_sysbar_switch_toprevious_task, 0));
        name = "expanded_desktop";
        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.quick_settings_immersive_mode_label, R.drawable.ic_navbar_immersive, 0));
        name = "killcurrent";
        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.kk_ui_kill_process_title, R.drawable.ic_close_white, 0));
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

    public View inflateButton(String name, LayoutInflater inflater, String buttonSpec, ViewGroup parent, boolean landscape, int indexInParent)
    {
        View v = null;

        ButtonInfo info = _buttonsInfo.get(name);
        if(info != null) v = inflater.inflate(info.layoutID, parent, false);

        if(v instanceof KeyButtonView)
        {
            KeyButtonView btn = ((KeyButtonView) v);
            if(info.imageID != 0) btn.setImageResource(info.imageID);
            btn.setClickAction(info.name);
            //btn.setLongPressAction("killcurrent");
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
