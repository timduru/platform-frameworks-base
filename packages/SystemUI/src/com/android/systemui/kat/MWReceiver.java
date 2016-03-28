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
                
				if(mMgr == null) mMgr = new MWManager(context);
                mMgr.refresh(context, 2);
				mMgr.switchTopTask();
                
/*             RunningTaskInfo topTask =  mProxy.getTopMostTask();
    Log.d(TAG, "RunningTaskInfo, top: " + topTask.baseActivity);
                
             
                int focusedStack = mProxy.getFocusedStack();
                List<RecentTaskInfo> taskList = mProxy.getRecentTasks(10, UserHandle.myUserId(), true);


                RecentTaskInfo topInfo = taskList.get(0); //FIXME
                Log.i(TAG, "cmd:" + cmd + " top="+ topInfo.baseIntent +" top.stackId="+ topInfo.stackId);
                Log.i(TAG, " focusedStack=" + focusedStack);
                
				if(topInfo == null || topInfo.id == -1) return;
							
				Rect usableScreenRect = mProxy.getWindowRect();

				//int focusedStack = topInfo.stackId ; //mProxy.getFocusedStack();
				
				Rect focusedStackRect = mProxy.getTaskBounds(focusedStack);
				boolean fullscreen = focusedStackRect.equals(usableScreenRect);

				Rect newRect = new Rect();
				newRect.set(usableScreenRect);

				Log.i(TAG, "fullscreen" + fullscreen + "focusedStackRect" + focusedStackRect + "usableScreenRect" + usableScreenRect);

				if(fullscreen)
					newRect.right = usableScreenRect.centerX();

				mProxy.resizeTask(topInfo.id, newRect);

				am.moveTaskToFront(topInfo.id, 0, null);
            * */
                break;
            }
		}
    }
    
    
}
