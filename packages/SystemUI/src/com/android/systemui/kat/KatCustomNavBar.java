package com.android.systemui.kat;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.R;

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
        private int layoutID;

        public ButtonInfo(String name, int labelID, int layoutID)
        {
            this.name = name;
            this.labelID = labelID;
            this.layoutID = layoutID;
        }
    }

    public KatCustomNavBar() {
        String name = "";

        name = "switch_toprevious_task";
        _buttonsInfo.put(name,  new ButtonInfo(name, R.string.nav_btn_switch_label, R.layout.nav_btn_switch));
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

        return v;
    }

    public CharSequence getLabel(String name, Context context)
    {
        ButtonInfo info = _buttonsInfo.get(name);
        if(info != null) return context.getString(info.labelID);
        else return name;
    }

}
