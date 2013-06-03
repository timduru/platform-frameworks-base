
package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.statusbar.EosObserver.FeatureListener;

import org.teameos.jellybean.settings.EOSConstants;
import org.teameos.jellybean.settings.EOSUtils;

import java.util.ArrayList;

/**
 * Common behavior of all "bar" ui modes Mostly common indicator controls
 *
 * @author bigrushdog
 */

public abstract class BarUiController extends BaseUiController implements FeatureListener {
    private int MSG_BATTERY_ICON_SETTINGS;
    private int MSG_BATTERY_TEXT_SETTINGS;
    private int MSG_BATTERY_TEXT_COLOR_SETTINGS;
    private int MSG_CLOCK_VISIBLE_SETTINGS;
    private int MSG_CLOCK_COLOR_SETTINGS;

    private boolean mIsClockVisible = true;
    protected int mCurrentBarSizeMode;
    private boolean mIsNormalScreen;
    private boolean mIsLargeScreen;
    private boolean mIsTabletUi;

    protected ArrayList<View> mBatteryList = new ArrayList<View>();

    private View mCurrentClockView;
    protected ContentResolver mResolver;
    protected EosGlassController mGlass;

    public BarUiController(Context context) {
        super(context);
        mResolver = mContext.getContentResolver();
        mIsNormalScreen = EOSUtils.isNormalScreen();
        mIsLargeScreen = EOSUtils.isLargeScreen();
        mIsTabletUi = EOSUtils.hasSystemBar(context);
        updateBarSizeMode();
    }

    protected abstract ImageView getBatteryIconView();

    protected abstract TextView getBatteryTextView();

    protected abstract TextView getClockCenterView();

    protected abstract TextView getClockClusterView();

    protected abstract void registerIndicatorView(View v);

    protected abstract EosGlassController getGlass();

    public boolean isNormalScreen() {
        return mIsNormalScreen;
    }

    public boolean isTabletUi() {
        return mIsTabletUi;
    }

    public boolean isLargeScreen() {
        return mIsLargeScreen;
    }

    protected void notifyIndicatorViewRegistered() {
        getBatteryTextView().setTag(EOSConstants.SYSTEMUI_BATTERY_PERCENT_TAG);
        mBatteryList.add(getBatteryIconView());
        mBatteryList.add(getBatteryTextView());
        mObserver.registerClass((FeatureListener) getClockClusterView());
        mObserver.registerClass((FeatureListener) getClockCenterView());
        mObserver.registerClass((FeatureListener) BarUiController.this);
        handleBatteryChange();
        handleClockChange();
    }

    @Override
    public ArrayList<String> onRegisterClass() {
        ArrayList<String> uris = new ArrayList<String>();
        uris.add(EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE);
        uris.add(EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE);
        uris.add(EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR);
        uris.add(EOSConstants.SYSTEMUI_CLOCK_VISIBLE);
        uris.add(EOSConstants.SYSTEMUI_CLOCK_COLOR);
        return uris;
    }

    @Override
    public void onSetMessage(String uri, int msg) {
        if (uri.equals(EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE)) {
            MSG_BATTERY_ICON_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE)) {
            MSG_BATTERY_TEXT_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR)) {
            MSG_BATTERY_TEXT_COLOR_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_CLOCK_VISIBLE)) {
            MSG_CLOCK_VISIBLE_SETTINGS = msg;
        } else if (uri.equals(EOSConstants.SYSTEMUI_CLOCK_COLOR)) {
            MSG_CLOCK_COLOR_SETTINGS = msg;
        }
    }

    @Override
    public void onFeatureStateChanged(int msg) {
        if (msg == MSG_BATTERY_ICON_SETTINGS
                || msg == MSG_BATTERY_TEXT_SETTINGS
                || msg == MSG_BATTERY_TEXT_COLOR_SETTINGS) {
            handleBatteryChange();
            return;
        } else if (msg == MSG_CLOCK_VISIBLE_SETTINGS
                || msg == MSG_CLOCK_COLOR_SETTINGS) {
            handleClockChange();
            return;
        }
    }

    private void handleBatteryChange() {
        int icon_visible = (Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_ICON_VISIBLE_DEF) == 1) ? View.VISIBLE : View.GONE;

        int text_visible = (Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_VISIBLE_DEF) == 1) ? View.VISIBLE : View.GONE;

        int color = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR,
                EOSConstants.SYSTEMUI_BATTERY_TEXT_COLOR_DEF);
        if (color == -1) {
            color = mContext.getResources()
                    .getColor(android.R.color.holo_blue_light);
        }
        for (View v : mBatteryList) {
            if (v.getTag() != null
                    && v.getTag().equals(EOSConstants.SYSTEMUI_BATTERY_PERCENT_TAG)) {
                // this is our text view
                ((TextView) v).setTextColor(color);
                v.setVisibility(text_visible);
            } else {
                // this works for now as we are only controlling
                // two views at any time
                v.setVisibility(icon_visible);
            }
        }
    }

    public void showClock(boolean show) {
        final View clock = mCurrentClockView;
        if (clock != null) {
            if (mIsClockVisible) {
                clock.setVisibility(show ? View.VISIBLE : View.GONE);
            } else {
                clock.setVisibility(View.GONE);
            }
        }
    }

    public void updateGlass() {
        if (getGlass() != null && getGlass().isGlassEnabled()) {
            getGlass().applyGlassEffect();
        }
    }

    public void updateBarSizeMode() {
        mCurrentBarSizeMode = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_BAR_SIZE_MODE, EOSConstants.SYSTEMUI_BARSIZE_MODE_NORMAL);
    }

    protected int getBarSizeMode() {
        return mCurrentBarSizeMode;
    }

    public int getNavbarHeightResource() {
        switch (mCurrentBarSizeMode) {
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_NORMAL:
                return com.android.internal.R.dimen.navigation_bar_height;
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_SLIM:
                return com.android.internal.R.dimen.navigation_bar_height_low_profile;
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_TINY:
                return com.android.internal.R.dimen.navigation_bar_height_tiny_profile;
            default:
                return com.android.internal.R.dimen.navigation_bar_height;
        }
    }

    private void handleClockChange() {
        if (mCurrentClockView == null)
            mCurrentClockView = getClockClusterView();

        int clock_state = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_CLOCK_VISIBLE,
                EOSConstants.SYSTEMUI_CLOCK_CLUSTER);

        switch (clock_state) {
            case EOSConstants.SYSTEMUI_CLOCK_GONE:
                mIsClockVisible = false;
                getClockCenterView().setVisibility(View.GONE);
                getClockClusterView().setVisibility(View.GONE);
                break;
            case EOSConstants.SYSTEMUI_CLOCK_CLUSTER:
                mIsClockVisible = true;
                getClockCenterView().setVisibility(View.GONE);
                getClockClusterView().setVisibility(View.VISIBLE);
                mCurrentClockView = getClockClusterView();
                break;
            case EOSConstants.SYSTEMUI_CLOCK_CENTER:
                mIsClockVisible = true;
                getClockClusterView().setVisibility(View.GONE);
                getClockCenterView().setVisibility(View.VISIBLE);
                mCurrentClockView = getClockCenterView();
                break;
        }

        int color = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_CLOCK_COLOR,
                EOSConstants.SYSTEMUI_CLOCK_COLOR_DEF);
        if (color == -1) {
            color = mContext.getResources()
                    .getColor(android.R.color.holo_blue_light);
        }
        getClockClusterView().setTextColor(color);
        getClockCenterView().setTextColor(color);
    }

    /* utility to iterate a viewgroup and return a list of child views */
    public ArrayList<View> getAllChildren(View v) {

        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<View>();

        ViewGroup vg = (ViewGroup) v;
        for (int i = 0; i < vg.getChildCount(); i++) {

            View child = vg.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            result.addAll(viewArrayList);
        }
        return result;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
