
package com.android.systemui.statusbar.preferences;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;

import org.teameos.jellybean.settings.EOSConstants;

public class EosIndicatorController {
    Context mContext;
    int mPressedColor;
    public static final int STATE_UNPRESSED = 0;
    public static final int STATE_PRESSED = 1;
    public static final int STATE_TRANS = 2;
    static final int mUnpressed = 0x99333333;

    public EosIndicatorController(Context context) {
        mContext = context;
        mPressedColor = mContext.getResources().getColor(
                com.android.internal.R.color.holo_blue_light);
    }

    public Drawable getIndicator(int state) {
        switch (state) {
            case (STATE_UNPRESSED):
                return getIndicatorIcon(mUnpressed, true);
            case (STATE_PRESSED):
                return getIndicatorIcon(mPressedColor, false);
            case (STATE_TRANS):
                return getIndicatorIcon(mPressedColor, true);
            default:
                return getIndicatorIcon(mUnpressed, true);
        }
    }

    public void setColor(int color) {
        if (color == EOSConstants.SYSTEMUI_SETTINGS_INDICATOR_COLOR_DEF) {
            mPressedColor = mContext.getResources().getColor(
                    com.android.internal.R.color.holo_blue_light);
        } else {
            mPressedColor = color;
        }
    }

    private Drawable getIndicatorIcon(int color, boolean trans) {
        return new IndicatorDrawable(new RectShape(), color, trans);
    }

    private class IndicatorDrawable extends ShapeDrawable {
        Paint mPaint;

        public IndicatorDrawable(Shape s, int color, boolean trans) {
            super(s);
            mPaint = new Paint(this.getPaint());
            mPaint.setColor(color);
            if (trans) {
                mPaint.setAlpha(127);
            } else {
                mPaint.setAlpha(255);
            }
            mPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
            shape.draw(canvas, mPaint);
        }
    }
}
