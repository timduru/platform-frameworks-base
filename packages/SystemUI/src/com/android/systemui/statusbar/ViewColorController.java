
package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.statusbar.ActivityWatcher.ActivityListener;
import com.android.systemui.statusbar.EosObserver.FeatureListener;

import org.teameos.jellybean.settings.EOSConstants;

import java.util.ArrayList;

public abstract class ViewColorController implements ActivityListener {
    protected Context mContext;
    protected ContentResolver mResolver;
    protected boolean mGlassEnabled;

    private int mColor;
    private int mDefColor;
    private boolean mIsAnimating = false;

    private int MSG_VIEW_COLOR;
    private int MSG_GLASS_ENABLED_STATE;

    private static final int BAR_ANIM_TIME = 300;

    public ViewColorController(Context context, EosObserver observer) {
        mContext = context;
        mResolver = context.getContentResolver();
        observer.registerClass(new FeatureListener() {
            @Override
            public ArrayList<String> onRegisterClass() {
                ArrayList<String> uris = new ArrayList<String>();
                uris.add(getColorUri());
                uris.add(EOSConstants.SYSTEMUI_USE_GLASS);
                return uris;
            }

            @Override
            public void onSetMessage(String uri, int msg) {
                if (uri.equals(getColorUri())) {
                    MSG_VIEW_COLOR = msg;
                } else if (uri.equals(EOSConstants.SYSTEMUI_USE_GLASS)) {
                    MSG_GLASS_ENABLED_STATE = msg;
                }
            }

            @Override
            public void onFeatureStateChanged(int msg) {
                if (msg == MSG_VIEW_COLOR) {
                    updateBarColor();
                    handleColorChange(mColor);
                } else if (msg == MSG_GLASS_ENABLED_STATE) {
                    checkEnabledandUpdateView();
                }
            }
        });
    }

    protected abstract String getColorUri();

    protected abstract void handleColorChange(int color);

    protected abstract int getDefColor();

    protected abstract void prepareGlassEffect(String componentName);

    protected abstract void handleGlassChange();

    public int getBarColor() {
        return mColor;
    }

    protected void checkEnabledandUpdateView() {
        mGlassEnabled = Settings.System.getInt(mResolver,
                EOSConstants.SYSTEMUI_USE_GLASS,
                EOSConstants.SYSTEMUI_USE_GLASS_DEF) == 1;
        mDefColor = getDefColor();
        updateBarColor();
        if (mGlassEnabled) {
            handleGlassChange();
        } else {
            handleColorChange(mColor);
        }
    }

    private void updateBarColor() {
        mColor = Settings.System.getInt(mResolver, getColorUri(),
                mDefColor);
        if (mColor != -1) {
            mColor = removeAlphaFromColor(mColor);
        }
    }

    public static int removeAlphaFromColor(int color) {
        return color += 255 << 24;
    }

    public static int applyAlphaToColor(int alpha, int color) {
        return color += alpha << 24;
    }

    public static int glassLevelToColor(int glass) {
        return glass += glass << 24 + 0 << 16 + 0 << 8 + 0;
    }

    public void animateBarColor(View v, int colorFrom, int colorTo) {
        if (colorFrom == colorTo)
            return;
        //
        if (!mIsAnimating) {
            final View view = v;
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom,
                    colorTo);
            colorAnimation.addUpdateListener(new AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    view.setBackgroundColor((Integer) animator.getAnimatedValue());
                }

            });
            colorAnimation.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    mIsAnimating = true;

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsAnimating = false;

                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mIsAnimating = false;

                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    mIsAnimating = true;

                }
            });
            colorAnimation.setDuration(BAR_ANIM_TIME);
            colorAnimation.start();
        }
    }

    @Override
    public void onActivityChanged(String componentName) {
        if (mGlassEnabled)
            prepareGlassEffect(componentName);
    }
}
