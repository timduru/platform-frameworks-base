package com.android.systemui.kat;

import java.util.List;
import java.util.ArrayList;
import android.graphics.Rect;

class MWPositions
{

	private Rect fullLand;
	private Rect fullPort;
	private List<Rect> land = new ArrayList<Rect>();
	private List<Rect> port = new ArrayList<Rect>();
	private boolean mLand = true;
	
	public MWPositions(Rect fullscreen, int numTasks)
	{
		fullLand = new Rect(fullscreen);
		fullPort = new Rect(0,0, fullscreen.bottom, fullscreen.right);
		
		if(fullscreen.width() < fullscreen.height())
		{
			Rect tmp = new Rect(fullLand);
			fullLand = fullPort;
			fullPort = tmp;			
		}


		Rect r;
		switch(numTasks)
		{
			case 2:
				r = new Rect(fullLand); r.right = r.centerX(); // left
				land.add(r);						
				r = new Rect(fullLand); r.left = r.centerX(); // right
				land.add(r);
				
				r = new Rect(fullPort); r.bottom = r.centerY(); // top
				port.add(r);						
				r = new Rect(fullPort); r.top = r.centerY(); // bottom
				port.add(r);
			break;
		}
	}
	
	public void setOrientation(boolean land) { mLand = land; }
	public List<Rect> getRects()		{ return mLand?land:port; }
	public Rect getFull()		{ return mLand?fullLand:fullPort; }
	
	boolean isFullScreen(Rect size) 
	{ 
		return getFull().equals(size); 
	}
}

