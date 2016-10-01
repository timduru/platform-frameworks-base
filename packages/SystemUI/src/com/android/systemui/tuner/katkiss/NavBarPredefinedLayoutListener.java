package com.android.systemui.tuner.katkiss;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.android.systemui.R;
import com.android.systemui.tuner.NavBarTuner;

import static com.android.systemui.statusbar.phone.NavigationBarInflaterView.NAV_BAR_VIEWS;


public class NavBarPredefinedLayoutListener implements AdapterView.OnItemSelectedListener
{
    private final String[] _values;
    private NavBarTuner _tuner;

    public NavBarPredefinedLayoutListener(NavBarTuner tuner)
    {
        _tuner = tuner;
        _values = tuner.getResources().getStringArray(R.array.kk_ui_mode_values);
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
    {
        if(i==0) return;
        String val  = _values[i];
        Log.d("TTT", val);
        Settings.Secure.putString(_tuner.getActivity().getContentResolver(), NAV_BAR_VIEWS, val);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
