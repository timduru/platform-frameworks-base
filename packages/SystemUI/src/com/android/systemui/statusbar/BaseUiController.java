package com.android.systemui.statusbar;

import android.content.Context;

import com.android.systemui.statusbar.ActivityWatcher.ActivityListener;

/**
 * All UI modes will extend this
 * @author bigrushdog
 *
 */

public abstract class BaseUiController implements ActivityListener {

    protected Context mContext;
    protected ActivityWatcher mActivityWatcher;
    protected EosObserver mObserver;

    public BaseUiController(Context context) {
        mContext = context;
        mActivityWatcher = new ActivityWatcher(context);
        mActivityWatcher.setActivityListener((ActivityListener)this);
        mObserver = new EosObserver(context);
    }

    public void notifyTopAppChanged() {
        mActivityWatcher.notifyTopAppChanged();
    }

    public EosObserver getObserver() {
        return mObserver;
    }

    public void wakeObserver(boolean wake) {
        mObserver.setEnabled(wake);
    }

    @Override
    public void onActivityChanged(String componentName) {
        mObserver.setEnabled(componentName.contains("org.kat.controlcenter")
                || componentName.contains("com.android.settings"));
    }
}
