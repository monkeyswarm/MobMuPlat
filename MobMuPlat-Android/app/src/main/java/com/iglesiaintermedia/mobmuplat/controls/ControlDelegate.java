package com.iglesiaintermedia.mobmuplat.controls;

import java.util.List;

import android.graphics.Color;

public interface ControlDelegate {
	public void sendGUIMessageArray(List<Object> msgArray);
	public Color getPatchBackgroundColor();
	public void launchMenuFragment(MMPMenu menu);
	public void refreshMenuFragment(MMPMenu menu);
}
