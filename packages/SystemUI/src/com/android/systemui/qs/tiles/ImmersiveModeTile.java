/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import java.util.ArrayList;

import org.meerkats.katkiss.KKC;
import org.meerkats.katkiss.KatUtils;
import org.meerkats.katkiss.CustomObserver;

import android.net.Uri;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Airplane mode **/
public class ImmersiveModeTile extends QSTile<QSTile.BooleanState> implements CustomObserver.ChangeNotification
{
	CustomObserver _observer = null;
	
    public ImmersiveModeTile(Host host) 
    {
        super(host);
    }

    @Override
    protected BooleanState newTileState() { return new BooleanState(); }

    @Override
    public void handleClick() { KatUtils.expandedDesktopSwitch(mContext);}

    @Override
    public void handleLongClick() {  }


    @Override
    protected void handleUpdateState(BooleanState state, Object arg) 
    {
        final boolean mode = KatUtils.isExpanded(mContext);
        state.value = mode;
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_immersive_mode_label);
        final int iconId =  mode ? R.drawable.ic_qs_immersive_on : R.drawable.ic_qs_immersive_off;
	state.icon = ResourceIcon.get(iconId);
    }

	@Override
	public void setListening(boolean listening) 
	{ 
		if(listening && _observer == null ) _observer = new CustomObserver(mContext, this);
	}

	@Override
	public ArrayList<Uri> getObservedUris() 
	{
	      ArrayList<Uri> uris = new  ArrayList<Uri>();
	      uris.add(Settings.System.getUriFor(KKC.S.USER_IMMERSIVE_MODE));
	      return uris;
	}

	@Override
	public void onChangeNotification(Uri uri) { refreshState(); }
}
