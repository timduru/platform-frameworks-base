
package com.android.systemui.statusbar.tablet;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.BarUiController;
import com.android.systemui.statusbar.EosGlassController;
import com.android.systemui.statusbar.EosObserver.FeatureListener;

import org.teameos.jellybean.settings.EOSConstants;
import org.teameos.jellybean.settings.EOSUtils;

public class TabletUiController extends BarUiController {
    private static final int max_notification_normal_ui = 5;
    private static final int max_notification_tablets_on_tablet_ui_port = 4;
    private static final int max_notification_phones_on_tablet_ui_port = 2;
    private static final int max_notification_phones_on_tablet_ui_land = 3;

    private View mIndicatorView;
    private EosGlassController mGlass;

    public TabletUiController(Context context) {
        super(context);
    }

    @Override
    protected ImageView getBatteryIconView() {
        return (ImageView) mIndicatorView.findViewById(R.id.notificationArea).findViewById(
                R.id.battery);
    }

    @Override
    protected TextView getBatteryTextView() {
        return (TextView) mIndicatorView.findViewById(R.id.notificationArea).findViewById(
                R.id.battery_text);
    }

    @Override
    protected TextView getClockCenterView() {
        return (TextView) mIndicatorView.findViewById(R.id.clock_center);
    }

    @Override
    protected TextView getClockClusterView() {
        return (TextView) mIndicatorView.findViewById(R.id.notificationArea).findViewById(
                R.id.clock);
    }

    public int getNotificationPanelMinHeight() {
        return R.dimen.notification_panel_min_height;
    }

    public int getNotificationPanelWidth() {
        return isNormalScreen() ? R.dimen.notification_panel_width_tablet_mode
                : R.dimen.notification_panel_width;
    }

    public int getNotificationIconSize() {
        switch (mCurrentBarSizeMode) {
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_NORMAL:
                return R.dimen.system_bar_icon_size_normal;
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_SLIM:
                return R.dimen.system_bar_icon_size_slim;
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_TINY:
                return R.dimen.system_bar_icon_size_tiny;
            default:
                return R.dimen.system_bar_icon_size_normal;
        }
    }

    public int getTickerIconSize() {
        switch (mCurrentBarSizeMode) {
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_NORMAL:
                return R.dimen.notification_large_icon_height;
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_SLIM:
                return R.dimen.notification_large_icon_height_slim;
            case EOSConstants.SYSTEMUI_BARSIZE_MODE_TINY:
                return R.dimen.notification_large_icon_height_tiny;
            default:
                return R.dimen.notification_large_icon_height;
        }
    }

    public int getNotificationHeightMax() {
        return getNavbarHeightResource();
    }

    public int getNotificationHeightMin() {
        return getNavbarHeightResource();
    }

    public int getNavigationKeyWidth() {
        return isNormalScreen() ? R.dimen.navigation_key_width_tablet_mode_on_phones
                : R.dimen.navigation_key_width_tablet_mode_on_tablets;
    }

    public int getMenuKeyWidth() {
        return isNormalScreen() ? R.dimen.navigation_menu_key_width_tablet_mode_on_phones
                : R.dimen.navigation_menu_key_width_tablet_mode_on_tablets;
    }

    public int getMaxNotificationIcons() {
        final boolean isPortrait = !EOSUtils.isLandscape(mContext);
        if (isNormalScreen()) {
            if (isPortrait) {
                return max_notification_phones_on_tablet_ui_port;
            } else {
                return max_notification_phones_on_tablet_ui_land;
            }
        } else if (isLargeScreen()) {
            if (isPortrait) {
                return max_notification_tablets_on_tablet_ui_port;
            } else {
                return max_notification_normal_ui;
            }
        } else {
            return max_notification_normal_ui;
        }
    }

    @Override
    protected void registerIndicatorView(View v) {
        mIndicatorView = v;
        mGlass = new EosGlassController(mContext);
        mGlass.setNavigationBar(v);
        mObserver.registerClass((FeatureListener) mGlass);
        notifyIndicatorViewRegistered();
    }

    @Override
    protected EosGlassController getGlass() {
        return mGlass;
    }
}
