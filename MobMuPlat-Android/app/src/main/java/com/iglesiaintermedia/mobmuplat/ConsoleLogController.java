package com.iglesiaintermedia.mobmuplat;

import java.util.Observable;


public class ConsoleLogController extends Observable{
	private static ConsoleLogController mInstance = null;
	
	StringBuffer _consoleStringBuffer;
	
	//instance!
	public static ConsoleLogController getInstance(){
        if(mInstance == null)
        {
            mInstance = new ConsoleLogController();
        }
        return mInstance;
    }
	
	private ConsoleLogController() {
		super();
		_consoleStringBuffer = new StringBuffer();
	}

	public void append(String s) {
		_consoleStringBuffer.append(s);
		if (_consoleStringBuffer.length()>5000) {
			_consoleStringBuffer.delete(0, _consoleStringBuffer.length() - 5000);
		}
		setChanged();
	    notifyObservers();
	}
	
	public void clear(){
		_consoleStringBuffer.delete(0, _consoleStringBuffer.length());
		setChanged();
	    notifyObservers();
	} 
	
	public String toString() {
		//String s = _consoleStringBuffer.toString();
		//_consoleStringBuffer.delete(0, _consoleStringBuffer.length());
		return _consoleStringBuffer.toString();
	}
}
