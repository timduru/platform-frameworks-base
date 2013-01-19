package com.android.systemui.eos;

import com.android.systemui.R;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.InputType;

public class EosDreamSettings extends PreferenceActivity implements OnPreferenceChangeListener {

    public static final String INTERACTIVE = "eos_dream_interactive";
    public static final String ROTATION = "eos_dream_rotation";
    public static final String COUNT = "eos_dream_count";
    public static final String DEBUG_MODE = "eos_dream_debug";
    public static final String SPEED = "eos_dream_speed";
    public static final String TRANSPARENT = "eos_dream_transparent";
    public static final int DEFAULT_NUM_MOB = 15;
    public static final int MAX_NUM_MOB = 50;
    public static final int MIN_NUM_MOB = 1;
    public static final int DEFAULT_SPEED = 500;

    private CheckBoxPreference mInteractivePref, mRotationPref, mDebugModePref, mTransparentPref;
    private SelectableEditTextPreference mCountPref;
    private ListPreference mSpeedPref;

    private boolean mIsInteractiveEnabled, mIsRotationEnabled, mDebugMode, mIsTransparent;
    private int mNumberOfMobs;
    private int mSpeedOfMobs;
    private static String mCurrentMobSize;
    private Context mContext;

    // This setting determines whether or not the Debug Mode option is available
    private boolean DEBUG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.eos_dream_settings);

        mContext = (Context) this;
        mCurrentMobSize = getString(R.string.eos_dream_current_mob_size);

        // let's initialize our values here to be on the safe side
        mIsInteractiveEnabled = Settings.System.getInt(mContext.getContentResolver(),
                INTERACTIVE, 1) == 1 ? true : false;
        mIsRotationEnabled = Settings.System.getInt(mContext.getContentResolver(),
                ROTATION, 1) == 1 ? true : false;
        mDebugMode = Settings.System.getInt(mContext.getContentResolver(),
                DEBUG_MODE, 0) == 1 ? true : false;
        mIsTransparent = Settings.System.getInt(mContext.getContentResolver(),
                TRANSPARENT, 0) == 1 ? true : false;
        mNumberOfMobs = Settings.System.getInt(mContext.getContentResolver(),
                COUNT, DEFAULT_NUM_MOB);
        mSpeedOfMobs = Settings.System.getInt(mContext.getContentResolver(),
                SPEED, DEFAULT_SPEED);

        mInteractivePref = (CheckBoxPreference) findPreference(INTERACTIVE);
        mInteractivePref.setChecked(mIsInteractiveEnabled);
        mInteractivePref.setOnPreferenceChangeListener(this);

        mRotationPref = (CheckBoxPreference) findPreference(ROTATION);
        mRotationPref.setChecked(mIsRotationEnabled);
        mRotationPref.setOnPreferenceChangeListener(this);
        
        mCountPref = (SelectableEditTextPreference)findPreference(COUNT);
        mCountPref.setDialogTitle("Enter a value betwen 1 and 50");
        mCountPref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        mCountPref.setInitialSelectionMode(SelectableEditTextPreference.SELECTION_SELECT_ALL);
        mCountPref.setDefaultValue(String.valueOf(mNumberOfMobs));
        mCountPref.setText(String.valueOf(mNumberOfMobs));
        updateCountSummary(String.valueOf(mNumberOfMobs));
        mCountPref.setOnPreferenceChangeListener(this);

        if (DEBUG) {
            mDebugModePref = (CheckBoxPreference) findPreference(DEBUG_MODE);
            mDebugModePref.setChecked(mDebugMode);
            mDebugModePref.setOnPreferenceChangeListener(this);
        } else {
            final PreferenceScreen prefScreen = getPreferenceScreen();
            prefScreen.removePreference(getPreferenceScreen().findPreference(DEBUG_MODE));
        }

        mSpeedPref = (ListPreference) findPreference(SPEED);
        mSpeedPref.setDialogTitle(getString(R.string.eos_dream_speed_dialog_title));
        mSpeedPref.setValue(String.valueOf(mSpeedOfMobs));
        updateSpeedSummary(String.valueOf(mSpeedOfMobs));
        mSpeedPref.setOnPreferenceChangeListener(this);

        mTransparentPref = (CheckBoxPreference) findPreference(TRANSPARENT);
        mTransparentPref.setChecked(mIsTransparent);
        mTransparentPref.setOnPreferenceChangeListener(this);
    }

    private void updateCountSummary(String value) {
        StringBuilder b = new StringBuilder();
        b.append(mCurrentMobSize)
                .append(" ")
                .append(value);
        mCountPref.setSummary(b.toString());
    }

    private void updateSpeedSummary(String value) {
        String summary = "Entry not found";
        CharSequence[] entries = mSpeedPref.getEntries();
        CharSequence[] values = mSpeedPref.getEntryValues();
        for (int i = 0; i < entries.length; i++) {
            if (values[i].toString().equals(value)) {
                summary = entries[i].toString();
            }
        }
        StringBuilder b = new StringBuilder();
        b.append(mCurrentMobSize)
                .append(" ")
                .append(summary);
        mSpeedPref.setSummary(b.toString());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mInteractivePref)) {
            mIsInteractiveEnabled = (Boolean)newValue;
            Settings.System.putInt(mContext.getContentResolver(), INTERACTIVE,
                    mIsInteractiveEnabled ? 1 : 0);
            EosDream.putInteractive(mIsInteractiveEnabled);
        } else if (preference.equals(mRotationPref)) {
            mIsRotationEnabled = (Boolean)newValue;
            Settings.System.putInt(mContext.getContentResolver(), ROTATION, mIsRotationEnabled ? 1 : 0);
            EosDreamEngine.putRotation(mIsRotationEnabled);
        } else if (preference.equals(mCountPref)) {
            String count = (String)newValue;
            if (count.equals ("")) {
                mCountPref.getEditText().setError("Value cannot be blank");
                mCountPref.show();
                return false;
            } else {
                mNumberOfMobs = (Integer.parseInt((String)newValue));
                if (mNumberOfMobs > MAX_NUM_MOB || mNumberOfMobs < MIN_NUM_MOB) {
                    mCountPref.getEditText().setError(getString(R.string.eos_dream_mob_bad_value));
                    mCountPref.show();
                    return false;
                }
                Settings.System.putInt(mContext.getContentResolver(), COUNT, mNumberOfMobs);
                EosDreamEngine.putMobCount(mNumberOfMobs);
                updateCountSummary(String.valueOf(mNumberOfMobs));
            }
        } else if (preference.equals(mDebugModePref)) {
            mDebugMode = (Boolean)newValue;
            Settings.System.putInt(mContext.getContentResolver(), DEBUG_MODE, mDebugMode ? 1 : 0);
            EosDreamEngine.putDebugMode(mDebugMode);
        } else if (preference.equals(mSpeedPref)) {
            mSpeedOfMobs = Integer.parseInt(((String)newValue));
            Settings.System.putInt(mContext.getContentResolver(), SPEED, mSpeedOfMobs);
            EosDreamEngine.putMobSpeed(mSpeedOfMobs);
            updateSpeedSummary(String.valueOf(mSpeedOfMobs));
        } else if (preference.equals(mTransparentPref)) {
            mIsTransparent = (Boolean)newValue;
            Settings.System.putInt(mContext.getContentResolver(), TRANSPARENT, mIsTransparent ? 1 : 0);
            EosDreamEngine.putTransparent(mIsTransparent);
        }
        return true;
    }
}