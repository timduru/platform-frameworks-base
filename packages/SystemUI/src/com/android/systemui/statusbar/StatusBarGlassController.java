
package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.EosObserver.FeatureListener;

import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;

public class StatusBarGlassController extends ViewColorController {
    private static final int STATUSBAR_DEF = EOSConstants.SYSTEMUI_STATUSBAR_GLASS_PRESET;;
    private static final int KEYGUARD_STAT = Color.parseColor("#73000000");
    private static final int RECENT_STAT = Color.parseColor("#99000000");

    View mStatusBarView;
    View mStatusBarWindow;
    private int mBarGlassLevel;
    private int mLastColor;
    private boolean mGlassBarPreset;

    private int MSG_GLASS_STATUSBAR_SETTINGS;
    private int MSG_GLASS_STATUSBAR_PRESET;

    public StatusBarGlassController(Context context, EosObserver observer, View statusbar,
            View window) {
        super(context, observer);
        mStatusBarView = statusbar;
        mStatusBarWindow = window;
        observer.registerClass(new FeatureListener() {

            @Override
            public ArrayList<String> onRegisterClass() {
                ArrayList<String> uris = new ArrayList<String>();
                uris.add(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_LEVEL);
                uris.add(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_DEFAULT_ENABLED);
                return uris;
            }

            @Override
            public void onSetMessage(String uri, int msg) {
                if (uri.equals(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_LEVEL)) {
                    MSG_GLASS_STATUSBAR_SETTINGS = msg;
                } else if (uri.equals(EOSConstants.SYSTEMUI_STATUSBAR_GLASS_DEFAULT_ENABLED)) {
                    MSG_GLASS_STATUSBAR_PRESET = msg;
                }
            }

            @Override
            public void onFeatureStateChanged(int msg) {
                if (msg == MSG_GLASS_STATUSBAR_SETTINGS
                        || msg == MSG_GLASS_STATUSBAR_PRESET) {
                    handleGlassChange();
                    return;
                }
            }
        });
        mLastColor = getDefColor();
        checkEnabledandUpdateView();
    }

    @Override
    protected String getColorUri() {
        return EOSConstants.SYSTEMUI_STATUSBAR_COLOR;
    }

    @Override
    protected void handleColorChange(int color) {
        if (color == -1) {
            mStatusBarView.setBackground(mContext.getResources().getDrawable(
                    R.drawable.status_bar_background));
            return;
        } else {
            mStatusBarWindow.setBackground(null);
            mStatusBarView.setBackgroundColor(removeAlphaFromColor(color));
        }
    }

    @Override
    protected int getDefColor() {
        return -1;
    }

    protected void handleGlassChange() {
        mBarGlassLevel = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_STATUSBAR_GLASS_LEVEL, STATUSBAR_DEF);
        mBarGlassLevel = glassLevelToColor(mBarGlassLevel);
        mGlassBarPreset = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_STATUSBAR_GLASS_DEFAULT_ENABLED, 0) == 1;
        if (mGlassBarPreset)
            mBarGlassLevel = glassLevelToColor(STATUSBAR_DEF);
    }

    protected void prepareGlassEffect(String mTopApp) {
        int newColor;
        if (ActivityWatcher.WINDOW_TYPE_KEYGUARD.equals(mTopApp)) {
            newColor = KEYGUARD_STAT;
        } else if (ActivityWatcher.WINDOW_TYPE_HOME.equals(mTopApp)) {
            newColor = mBarGlassLevel;
        } else if (ActivityWatcher.WINDOW_TYPE_RECENT.equals(mTopApp)) {
            newColor = RECENT_STAT;
        } else {
            newColor = getBarColor();
            if (newColor == -1)
                newColor = 0xff000000;
        }
        if (mLastColor != newColor) {
            animateBarColor(mStatusBarView, mLastColor, newColor);
        }
        mLastColor = newColor;
    }
}
