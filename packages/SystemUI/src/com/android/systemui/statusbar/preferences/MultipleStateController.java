
package com.android.systemui.statusbar.preferences;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.R;

public abstract class MultipleStateController extends SettingsController {

    public static final int STATE_TYPE_DISABLED = 0;
    public static final int STATE_TYPE_ENABLED = 1;
    public static final int STATE_TYPE_TRANSITION = 2;
    protected Drawable[] mIndicators = new Drawable[3];

    public MultipleStateController(Context context, View controlWidget) {
        super(context, controlWidget);
        mIndicatorController = new EosIndicatorController(mContext);
        mIndicators[STATE_OFF] = mIndicatorController
                .getIndicator(EosIndicatorController.STATE_UNPRESSED);
        mIndicators[STATE_ON] = mIndicatorController
                .getIndicator(EosIndicatorController.STATE_PRESSED);
        mIndicators[STATE_TYPE_TRANSITION] = mIndicatorController
                .getIndicator(EosIndicatorController.STATE_TRANS);
    }

    abstract protected int getStateType(int state);

    abstract public int[] getStateTransitions();

    public void updateControllerDrawable(int state) {
        if (getStateType(state) == STATE_TYPE_DISABLED) {
            controlWidget.findViewById(R.id.eos_settings_status).setBackgroundDrawable(
                    mIndicators[STATE_OFF]);
            controlWidget.findViewById(R.id.eos_settings_main).setBackgroundResource(
                    R.drawable.eos_settings_widget_main_unpressed);
            controlWidget.findViewById(R.id.eos_settings_icon).setBackgroundDrawable(
                    mIcons[STATE_OFF]);
        } else if (getStateType(state) == STATE_TYPE_ENABLED) {
            controlWidget.findViewById(R.id.eos_settings_status).setBackgroundDrawable(
                    mIndicators[STATE_ON]);
            controlWidget.findViewById(R.id.eos_settings_main).setBackgroundResource(
                    R.drawable.eos_settings_widget_main_pressed);
            controlWidget.findViewById(R.id.eos_settings_icon).setBackgroundDrawable(
                    mIcons[STATE_ON]);
        } else if (getStateType(state) == STATE_TYPE_TRANSITION) {
            controlWidget.findViewById(R.id.eos_settings_status).setBackgroundDrawable(
                    mIndicators[STATE_TYPE_TRANSITION]);
        }

        controlWidget.invalidate();
    }

    public void updateIndicator(int color) {
        mIndicatorController.setColor(color);
        mIndicators[STATE_OFF] = mIndicatorController
                .getIndicator(EosIndicatorController.STATE_UNPRESSED);
        mIndicators[STATE_ON] = mIndicatorController
                .getIndicator(EosIndicatorController.STATE_PRESSED);
        mIndicators[STATE_TYPE_TRANSITION] = mIndicatorController
                .getIndicator(EosIndicatorController.STATE_TRANS);
        updateController();
    }

    public void onClick(View view) {
        mPreferenceState = getStateTransitions()[mPreferenceState];
        updateControllerDrawable(mPreferenceState);
        setPreferenceStatus(mPreferenceState);

        try {
            if (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED) == 1) {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator.hasVibrator())
                    vibrator.vibrate(10);
            }
        } catch (android.provider.Settings.SettingNotFoundException e) {
        }
    }
}
