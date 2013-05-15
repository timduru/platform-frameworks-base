/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.systemui.R;

import org.teameos.jellybean.settings.EOSConstants;
import org.teameos.jellybean.settings.EOSUtils;

/**
 *
 */
public class QuickSettingsContainerView extends FrameLayout {

    // The number of columns in the QuickSettings grid
    private int mNumColumns;

    // The gap between tiles in the QuickSettings grid
    private float mCellGap;

    // we need a context
    Context mContext;
    
    private boolean mIsTabletUi;

    public QuickSettingsContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mIsTabletUi = EOSUtils.hasSystemBar(getContext());
        updateResources();
    }

    public void updateResources() {
        Resources r = getContext().getResources();
        mNumColumns = Settings.System.getInt(mContext.getContentResolver(),
                EOSConstants.SYSTEMUI_PANEL_COLUMN_COUNT,
                EOSConstants.SYSTEMUI_PANEL_COLUMNS_DEF);
        mCellGap = r.getDimension(mIsTabletUi ? R.dimen.quick_settings_cell_gap_tablet
                : R.dimen.quick_settings_cell_gap);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate the cell width dynamically
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = (int) (width - getPaddingLeft() - getPaddingRight() -
                (mNumColumns - 1) * mCellGap);
        float cellWidth = (float) Math.ceil(((float) availableWidth) / mNumColumns);

        // Update each of the children's widths accordingly to the cell width
        int N = getChildCount();
        int cellHeight = 0;
        int cursor = 0;
        int numCustomRows = 0;
        float customHeight = 0;
        for (int i = 0; i < N; ++i) {
            // Update the child's width
            QuickSettingsTileView v = (QuickSettingsTileView) getChildAt(i);
            boolean isCustomHeight = false;
            if (v.getVisibility() != View.GONE) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v
                        .getLayoutParams();
                if (v.getCustomHeight() > 0) {
                    customHeight += v.getCustomHeight();
                    numCustomRows += 1;
                    isCustomHeight = true;
                }
                int colSpan = v.getColumnSpan();
                lp.width = (int) ((colSpan * cellWidth) + (colSpan - 1) * mCellGap);

                // Measure the child
                int newWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
                int newHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                v.measure(newWidthSpec, newHeightSpec);

                // Save the cell height
                // When this value is registered, it must be the height of a normal tile
                if (cellHeight <= 0 && !isCustomHeight) {
                    cellHeight = v.getMeasuredHeight();
                }
                cursor += colSpan;
            }
        }

        // Set the measured dimensions. We always fill the tray width, but wrap
        // to the height of
        // all the tiles.
        int numRows = (int) Math.ceil((float) cursor / mNumColumns);
        // compensate for seekbar height variance
        // then shave the difference from newHeight
        int seekbarDiff = (int) Math.ceil((numCustomRows * cellHeight) - customHeight - mCellGap);

        int newHeight = (int) ((numRows * cellHeight) + ((numRows - 1) * mCellGap)) +
                getPaddingTop() + getPaddingBottom() - seekbarDiff;
        setMeasuredDimension(width, newHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int N = getChildCount();
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int cursor = 0;
        for (int i = 0; i < N; ++i) {
            QuickSettingsTileView v = (QuickSettingsTileView) getChildAt(i);
            ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) v.getLayoutParams();
            if (v.getVisibility() != GONE) {
                int col = cursor % mNumColumns;
                int colSpan = v.getColumnSpan();
                int row = (int) (cursor / mNumColumns);

                // Push the item to the next row if it can't fit on this one
                if ((col + colSpan) > mNumColumns) {
                    x = getPaddingLeft();
                    y += lp.height + mCellGap;
                    row++;
                }

                // Layout the container
                v.layout(x, y, x + lp.width, y + lp.height);

                // Offset the position by the cell gap or reset the position and
                // cursor when we
                // reach the end of the row
                cursor += v.getColumnSpan();
                if (cursor < (((row + 1) * mNumColumns))) {
                    x += lp.width + mCellGap;
                } else {
                    x = getPaddingLeft();
                    y += lp.height + mCellGap;
                }
            }
        }
    }
}
