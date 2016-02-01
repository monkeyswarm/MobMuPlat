package com.iglesiaintermedia.mobmuplat;

public interface AsyncExceptionListener {
	public void receiveException(Exception e, String message, String elementKey);
}
