package com.illposed.osc;

import java.util.Enumeration;
import java.util.Vector;

import com.illposed.osc.utility.*;

/**
 * An simple (non-bundle) OSC message. An OSC message is made up of 
 * an address (the receiver of the message) and arguments 
 * (the content of the message).
 * <p>
 * Internally, I use Vector to maintain jdk1.1 compatability
 * <p>
 * Copyright (C) 2003-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 * <p>
 * See license.txt (or license.rtf) for license information.
 *
 * @author Chandrasekhar Ramakrishnan
 * @version 1.0
 */
public class OSCMessage extends OSCPacket {

	protected String address;
	protected Vector arguments;

	/**
	 * Create an empty OSC Message.
	 * In order to send this osc message, you need to set the address
	 * and, perhaps, some arguments.
	 */
	public OSCMessage() {
		super();
		arguments = new Vector();
	}

	/**
	 * Create an OSCMessage with an address already initialized.
	 * @param newAddress the recepient of this OSC message
	 */
	public OSCMessage(String newAddress) {
		this(newAddress, null);
	}

	/**
	 * Create an OSCMessage with an address and arguments already initialized.
	 * @param newAddress    the recepient of this OSC message
	 * @param newArguments  the data sent to the receiver
	 */
	public OSCMessage(String newAddress, Object[] newArguments) {
		super();
		address = newAddress;
		if (null != newArguments) {
			arguments = new Vector(newArguments.length);
			for (int i = 0; i < newArguments.length; i++) {
				arguments.add(newArguments[i]);
			}
		} else
			arguments = new Vector();
		init();
	}
	
	/**
	 * The receiver of this message.
	 * @return the receiver of this OSC Message
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * Set the address of this messsage.
	 * @param anAddress the receiver of the message
	 */
	public void setAddress(String anAddress) {
		address = anAddress;
	}
	
	/**
	 * Add an argument to the list of arguments.
	 * @param argument a Float, String, Integer, BigInteger, Boolean or array of these
	 */	
	public void addArgument(Object argument) {
		arguments.add(argument);
	}
	
	/**
	 * The arguments of this message.
	 * @return the arguments to this message
	 */		
	public Object[] getArguments() {
		return arguments.toArray();
	}

	/**
	 * Convert the address into a byte array. Used internally.
	 * @param stream OscPacketByteArrayConverter
	 */
	protected void computeAddressByteArray(OSCJavaToByteArrayConverter stream) {
		stream.write(address);
	}

	/**
 	 * Convert the arguments into a byte array. Used internally.
	 * @param stream OscPacketByteArrayConverter
	 */
	protected void computeArgumentsByteArray(OSCJavaToByteArrayConverter stream) {
		stream.write(',');
		if (null == arguments)
			return;
		stream.writeTypes(arguments);
		Enumeration myenum = arguments.elements();
		while (myenum.hasMoreElements()) {
			stream.write(myenum.nextElement());
		}
	}

	/**
	 * Convert the message into a byte array. Used internally.
	 * @param stream OscPacketByteArrayConverter
	 */
	protected void computeByteArray(OSCJavaToByteArrayConverter stream) {
		computeAddressByteArray(stream);
		computeArgumentsByteArray(stream);
		byteArray = stream.toByteArray();
	}

}