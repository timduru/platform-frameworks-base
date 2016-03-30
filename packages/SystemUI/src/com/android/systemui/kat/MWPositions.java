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
				land = splitH(fullLand);
				port = splitV(fullPort);			
			break;

			case 3:
				int third = 2*fullLand.right/3;
				
				r = new Rect(fullLand); r.right = third; // 2/3 left
				land.add(r);
				r = new Rect(fullLand); r.left = third; // 1/3 right part 
				land.addAll(splitV(r));						
				
				
				third = 2*fullPort.bottom/3;
				r = new Rect(fullPort); r.bottom = third; // 2/3 top
				port.add(r);
				r = new Rect(fullPort); r.top = third; // 1/3 bottom
				port.addAll(splitH(r));				

			break;
			
			case 4:
				land = get4Zones(fullLand);
				port = get4Zones(fullPort);
			break;
		}
	}

	private List<Rect> splitH (Rect zone)
	{
		List<Rect> list = new ArrayList<Rect>();
		
		Rect r;
		r = new Rect(zone); r.right = r.centerX(); // left
		list.add(r);						
		r = new Rect(zone); r.left = r.centerX(); // right
		list.add(r);
		
		return list;
	}	

	private List<Rect> splitV (Rect zone)
	{
		List<Rect> list = new ArrayList<Rect>();
		
		Rect r;
		r = new Rect(zone); r.bottom = r.centerY(); // top
		list.add(r);						
		r = new Rect(zone); r.top = r.centerY(); // bottom
		list.add(r);
		
		return list;
	}	

	private List<Rect> get4Zones (Rect zone)
	{
		List<Rect> list = new ArrayList<Rect>();
		Rect r;
		
		Rect top = new Rect(zone); top.bottom = zone.centerY();
		Rect bottom = new Rect(zone); bottom.top = zone.centerY();
		
		list.addAll(splitH(top));
		list.addAll(splitH(bottom));

		return list;
	}
	
	
	public void setOrientation(boolean land) { mLand = land; }
	public List<Rect> getRects()		{ return mLand?land:port; }
	public Rect getFull()		{ return mLand?fullLand:fullPort; }
	
	boolean isFullScreen(Rect size) 
	{ 
		return getFull().equals(size); 
	}



}

