package com.illposed.osc;

import java.util.Date;

/**
 * Interface for things that listen for incoming OSC Messages
 * <p>
 * Copyright (C) 2003-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 * <p>
 * See license.txt (or license.rtf) for license information.
 *
 * @author Chandrasekhar Ramakrishnan
 * @version 1.0
 */
public interface OSCListener {
	
	/**
	 * Accept an incoming OSCMessage
	 * @param time     The time this message is to be executed. null means execute now
	 * @param message  The message to execute.
	 */
	public void acceptMessage(Date time, OSCMessage message);

}
