package com.android.systemui.statusbar.preferences;

import android.content.*;
import android.view.*;
import android.widget.*;
import android.media.AudioManager;
import android.os.Vibrator;

import com.android.systemui.R;

public class AudioController extends MultipleStateController {

    AudioManager             mAudioManager;
    private ImageView        mIcon;
    private boolean          mHasVibrate = false;

    private static final int STATE_VIBRATE = 0;
    private static final int STATE_SILENT  = 1;
    private static final int STATE_NORMAL  = 2;

    public AudioController(Context context, View button) {
        super(context, button);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Vibrator mVibratorService = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mHasVibrate = mVibratorService.hasVibrator();
        mIcon = ((ImageView) button.findViewById(R.id.eos_settings_icon));
        updateController();
    }

    protected int getPreferenceStatus() {
        int ringerMode = mAudioManager.getRingerMode();
        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE)
            return STATE_VIBRATE;
        if (ringerMode == AudioManager.RINGER_MODE_SILENT)
            return STATE_SILENT;
        return STATE_NORMAL;
    }

    protected void setPreferenceStatus(final int status) {
        switch (status){
        case STATE_VIBRATE:
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            break;
        case STATE_SILENT:
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            break;
        case STATE_NORMAL:
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            break;
        }
    }

    protected void handleBroadcast(Intent intent) {
        mPreferenceState = getPreferenceStatus();
    }

    protected String getSettingsIntent() {
        return "android.settings.SOUND_SETTINGS";
    }

    protected IntentFilter getBroadcastIntents() {
        IntentFilter intents = new IntentFilter();
        intents.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);

        return intents;
    }

    @Override
    protected int getStateType(int state) {
        return MultipleStateController.STATE_TYPE_ENABLED;
    }

    @Override
    public int[] getStateTransitions() {
        if (mHasVibrate) {
            return new int[] { 1, 2, 0 };
        } else {
            return new int[] { 0, 2, 1 };
        }
    }

    @Override
    public void updateController() {
        mPreferenceState = getPreferenceStatus();
        updateControllerDrawable(mPreferenceState);
        updateControllerImage(mPreferenceState);
        controlWidget.invalidate();
    }

    private void updateControllerImage(int preferenceState) {
        switch (preferenceState) {
        case STATE_VIBRATE:
            mIcon.setImageResource(R.drawable.toggle_vibrate);
            break;
        case STATE_SILENT:
            mIcon.setImageResource(R.drawable.toggle_silence);
            break;
        case STATE_NORMAL:
            mIcon.setImageResource(R.drawable.toggle_silence_off);
        }
    }
}