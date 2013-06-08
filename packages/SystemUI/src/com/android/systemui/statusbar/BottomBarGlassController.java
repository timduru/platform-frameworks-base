
package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.statusbar.EosObserver.FeatureListener;

import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;

public class BottomBarGlassController extends ViewColorController {

    private static final int NAVBAR_DEF = EOSConstants.SYSTEMUI_NAVBAR_GLASS_PRESET;
    private static final int KEYGUARD_NAV = Color.parseColor("#73000000");
    private static final int RECENT_NAV = Color.parseColor("#e0000000");

    private int MSG_GLASS_NAVBAR_SETTINGS;
    private int MSG_GLASS_NAVBAR_PRESET;

    private View mBottomBar;
    private int mNavbarGlassLevel;
    private int mLastColor;
    private boolean mGlassNavbarPreset;

    public BottomBarGlassController(Context context, EosObserver observer, View bottomBar) {
        super(context, observer);
        mBottomBar = bottomBar;
        observer.registerClass(new FeatureListener() {

            @Override
            public ArrayList<String> onRegisterClass() {
                ArrayList<String> uris = new ArrayList<String>();
                uris.add(EOSConstants.SYSTEMUI_NAVBAR_GLASS_LEVEL);
                uris.add(EOSConstants.SYSTEMUI_NAVBAR_GLASS_DEFAULT_ENABLED);
                return uris;
            }

            @Override
            public void onSetMessage(String uri, int msg) {
                if (uri.equals(EOSConstants.SYSTEMUI_NAVBAR_GLASS_LEVEL)) {
                    MSG_GLASS_NAVBAR_SETTINGS = msg;
                } else if (uri.equals(EOSConstants.SYSTEMUI_NAVBAR_GLASS_DEFAULT_ENABLED)) {
                    MSG_GLASS_NAVBAR_PRESET = msg;
                }
            }

            @Override
            public void onFeatureStateChanged(int msg) {
                if ( msg == MSG_GLASS_NAVBAR_SETTINGS
                        || msg == MSG_GLASS_NAVBAR_PRESET) {
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
        return EOSConstants.SYSTEMUI_NAVBAR_COLOR;
    }

    @Override
    protected void handleColorChange(int color) {
        mBottomBar.setBackgroundColor(color == -1 ? getDefColor() : color);
    }

    @Override
    protected int getDefColor() {
        return 0xff000000;
    }

    protected void handleGlassChange() {
        mNavbarGlassLevel = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_NAVBAR_GLASS_LEVEL, NAVBAR_DEF);
        mNavbarGlassLevel = glassLevelToColor(mNavbarGlassLevel);
        mGlassNavbarPreset = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_NAVBAR_GLASS_DEFAULT_ENABLED, 0) == 1;

        if (mGlassNavbarPreset)
            mNavbarGlassLevel = glassLevelToColor(NAVBAR_DEF);
    }

    public void prepareGlassEffect(String mTopApp) {
        int newColor;
        if (ActivityWatcher.WINDOW_TYPE_KEYGUARD.equals(mTopApp)) {
            newColor = KEYGUARD_NAV;
        } else if (ActivityWatcher.WINDOW_TYPE_HOME.equals(mTopApp)) {
            newColor = mNavbarGlassLevel;
        } else if (ActivityWatcher.WINDOW_TYPE_RECENT.equals(mTopApp)) {
            newColor = RECENT_NAV;
        } else {
            newColor = getBarColor();
            if (newColor == -1) newColor = getDefColor();
        }
        if (mLastColor != newColor) {
            animateBarColor(mBottomBar, mLastColor, newColor);
        }
        mLastColor = newColor;
    }
}
