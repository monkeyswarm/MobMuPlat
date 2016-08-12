package com.iglesiaintermedia.mobmuplat;

public interface AudioDelegate {
	//public int getBlockSize();
	
	//public int setTicksPerBuffer(int newTicks); //returns actual ticks
	//public int getTicksPerBuffer();
	//public int getBufferSizeSamples();
	//public float getBufferSizeMS();
	//public int setSampleRate(int newSampleRate); //returns actual rate
	public int getSampleRate();
	public void setBackgroundAudioAndNetworkEnabled(boolean backgroundAudioAndNetworkEnabled);
	
}
