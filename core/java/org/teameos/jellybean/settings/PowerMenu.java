/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.teameos.jellybean.settings;

import com.android.internal.R;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Borrow code from global actions to make a quick reboot menu
 */
class PowerMenu implements DialogInterface.OnClickListener {

    private final Context mContext;
    private ArrayList<Action> mItems;
    private AlertDialog mDialog;
    private MyAdapter mAdapter;

    /**
     * @param context everything needs a context :(
     */
    public PowerMenu(Context context) {
        mContext = context;

        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(mBroadcastReceiver, filter);
        showDialog();
    }

    public void showDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
            // Show delayed, so that the dismiss of the previous dialog
            // completes
            mHandler.sendEmptyMessage(MESSAGE_SHOW);
        } else {
            handleShow();
        }
    }

    private void handleShow() {
        mDialog = createDialog();
        prepareDialog();

        mDialog.show();
        mDialog.getWindow().getDecorView().setSystemUiVisibility(View.STATUS_BAR_DISABLE_EXPAND);
    }

    /**
     * Create the global actions dialog.
     * 
     * @return A new dialog.
     */
    private AlertDialog createDialog() {
        createRebootMenuItems();

        mAdapter = new MyAdapter();

        final AlertDialog.Builder ab = new AlertDialog.Builder(mContext);

        ab.setAdapter(mAdapter, this)
                .setInverseBackgroundForced(true);
        ab.setTitle("Reboot Menu");

        final AlertDialog dialog = ab.create();
        dialog.getListView().setItemsCanFocus(true);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);

        return dialog;
    }

    private void createRebootMenuItems() {
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mItems = new ArrayList<Action>();

        // first: power off
        mItems.add(
                new SinglePressAction(
                        com.android.internal.R.drawable.ic_lock_reboot,
                        R.string.eos_globalactions_reboot) {

                    public void onPress() {
                        pm.reboot("");
                    }

                    public boolean showDuringKeyguard() {
                        return true;
                    }

                    public boolean showBeforeProvisioning() {
                        return true;
                    }
                });

        mItems.add(
                new SinglePressAction(
                        com.android.internal.R.drawable.ic_lock_reboot_bootloader,
                        R.string.eos_globalactions_reboot_bootloader) {

                    public void onPress() {
                        pm.reboot("bootloader");
                    }

                    public boolean showDuringKeyguard() {
                        return true;
                    }

                    public boolean showBeforeProvisioning() {
                        return true;
                    }
                });

        mItems.add(
                new SinglePressAction(
                        com.android.internal.R.drawable.ic_lock_reboot_recovery,
                        R.string.eos_globalactions_reboot_recovery) {

                    public void onPress() {
                        pm.reboot("recovery");
                    }

                    public boolean showDuringKeyguard() {
                        return true;
                    }

                    public boolean showBeforeProvisioning() {
                        return true;
                    }
                });
    }

    private void prepareDialog() {
        mAdapter.notifyDataSetChanged();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);

    }

    /** {@inheritDoc} */
    public void onClick(DialogInterface dialog, int which) {
        mAdapter.getItem(which).onPress();
    }

    /**
     * The adapter used for the list within the global actions dialog, taking
     * into account whether the keyguard is showing via
     * {@link GlobalActions#mKeyguardShowing} and whether the device is
     * provisioned via {@link GlobalActions#mDeviceProvisioned}.
     */
    private class MyAdapter extends BaseAdapter {

        public int getCount() {
            int count = 0;

            for (int i = 0; i < mItems.size(); i++) {
                final Action action = mItems.get(i);
                count++;
            }
            return count;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        public Action getItem(int position) {

            int filteredPos = 0;
            for (int i = 0; i < mItems.size(); i++) {
                final Action action = mItems.get(i);
                if (filteredPos == position) {
                    return action;
                }
                filteredPos++;
            }

            throw new IllegalArgumentException("position " + position
                    + " out of range of showable actions"
                    + ", filtered count=" + getCount());
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Action action = getItem(position);
            final Context context = mContext;
            return action.create(context, convertView, parent, LayoutInflater.from(context));
        }
    }

    // note: the scheme below made more sense when we were planning on having
    // 8 different things in the global actions dialog. seems overkill with
    // only 3 items now, but may as well keep this flexible approach so it will
    // be easy should someone decide at the last minute to include something
    // else, such as 'enable wifi', or 'enable bluetooth'

    /**
     * What each item in the global actions dialog must be able to support.
     */
    private interface Action {
        View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater);

        void onPress();

        public boolean onLongPress();

        /**
         * @return whether this action should appear in the dialog when the
         *         keygaurd is showing.
         */
        boolean showDuringKeyguard();

        /**
         * @return whether this action should appear in the dialog before the
         *         device is provisioned.
         */
        boolean showBeforeProvisioning();

        boolean isEnabled();
    }

    /**
     * A single press action maintains no state, just responds to a press and
     * takes an action.
     */
    private static abstract class SinglePressAction implements Action {
        private final int mIconResId;
        private final int mMessageResId;
        private final CharSequence mMessage;

        protected SinglePressAction(int iconResId, int messageResId) {
            mIconResId = iconResId;
            mMessageResId = messageResId;
            mMessage = null;
        }

        public boolean isEnabled() {
            return true;
        }

        abstract public void onPress();

        public boolean onLongPress() {
            return false;
        }

        public View create(
                Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            View v = inflater.inflate(R.layout.global_actions_item, parent, false);

            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            TextView messageView = (TextView) v.findViewById(R.id.message);

            v.findViewById(R.id.status).setVisibility(View.GONE);

            icon.setImageDrawable(context.getResources().getDrawable(mIconResId));
            if (mMessage != null) {
                messageView.setText(mMessage);
            } else {
                messageView.setText(mMessageResId);
            }

            return v;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)
                    || Intent.ACTION_SCREEN_OFF.equals(action)) {
                mHandler.sendEmptyMessage(MESSAGE_DISMISS);
            }
        }
    };

    private static final int MESSAGE_DISMISS = 0;
    private static final int MESSAGE_REFRESH = 1;
    private static final int MESSAGE_SHOW = 2;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DISMISS:
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                    break;
                case MESSAGE_REFRESH:
                    mAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_SHOW:
                    handleShow();
                    break;
            }
        }
    };
}
