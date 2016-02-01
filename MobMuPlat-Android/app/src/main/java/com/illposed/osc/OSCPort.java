package com.illposed.osc;

import java.net.*;
import java.io.IOException;

/**
 * OSCPort is an abstract superclass. To send OSC messages, use @see OSCPortOut. 
 * To listen for OSC messages, use @see OSCPortIn.
 * <p>
 * Copyright (C) 2003-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 * <p>
 * See license.txt (or license.rtf) for license information.
 *
 * @author Chandrasekhar Ramakrishnan
 * @version 1.0
 */
public abstract class OSCPort {

	protected DatagramSocket socket;
	protected int port;

	/**
	 * The port that the SuperCollider <b>synth</b> engine ususally listens to &mdash; 57110.
	 */
	public static int defaultSCOSCPort() {
		return 57110;
	}
	
	/**
	 * The port that the SuperCollider <b>language</b> engine ususally listens to &mdash; 57120.
	 */
	public static int defaultSCLangOSCPort() {
		return 57120;
	}
	
	/**
	 * Close the socket if this hasn't already happened.
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		super.finalize();
		socket.close();
	}
	
	/**
	 * Close the socket and free-up resources. It's recommended that clients call
	 * this when they are done with the port.
	 */
	public void close() {
		socket.close();
	}

}
