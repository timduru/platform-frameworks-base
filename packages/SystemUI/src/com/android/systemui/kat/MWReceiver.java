package com.android.systemui.kat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.systemui.recents.Recents;
import org.meerkats.katkiss.KKC;

// FIXME do not declare in AndroidManifest to avoid being recreated everytime
 
public class MWReceiver extends BroadcastReceiver {

	private MWManager mMgr;
	
	
    @Override
    public void onReceive(Context context, Intent intent) {
        Recents recents = Recents.getInstanceAndStartIfNeeded(context);


        switch (intent.getAction())  
        {
            case KKC.I.MW_SWITCH: 
            {
                String cmd = intent.getStringExtra(KKC.I.CMD);
                int numapps = intent.getIntExtra("numapps", 2);
                
				if(mMgr == null) mMgr = new MWManager(context, numapps);
				mMgr.refresh(context, numapps);
				mMgr.switchTopTask();
                
                break;
            }
		}
    }
}
