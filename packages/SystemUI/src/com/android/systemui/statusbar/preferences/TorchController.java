package com.android.systemui.statusbar.preferences;

import org.teameos.jellybean.settings.EOSConstants;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.Torch;

public class TorchController extends SettingsController {
    Context mContext;

    public TorchController(Context context, View button) {
        super(context, button);
        mContext = context;
        getIcons(R.drawable.toggle_torch_off, R.drawable.toggle_torch);
        updateController();
    }

    protected int getPreferenceStatus() {
        return Torch.getTorch() == null ? 0 : 1;
    }

    protected void setPreferenceStatus(int status) {
        if (status == 1) {
            if (Torch.getTorch() == null) {
                mContext.startActivity(new Intent(mContext, Torch.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } else {
            mContext.sendBroadcast(new Intent().setAction(EOSConstants.ACTION_TORCH_OFF));
        }
    }
}
