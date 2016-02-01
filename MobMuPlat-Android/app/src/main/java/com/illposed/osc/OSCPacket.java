package com.illposed.osc;

import com.illposed.osc.utility.*;

/**
 * OSCPacket is the abstract superclass for the various
 * kinds of OSC Messages. The actual packets are:
 * <ul>
 * <li>OSCMessage &mdash; simple OSC messages
 * <li>OSCBundle &mdash; OSC messages with timestamps and/or made up of multiple messages
 * </ul>
 *<p>
 * This implementation is based on <a href="http://www.emergent.de/Goodies/">Markus Gaelli</a> and
 * Iannis Zannos' OSC implementation in Squeak Smalltalk.
 */
public abstract class OSCPacket {

	protected boolean isByteArrayComputed;
	protected byte[] byteArray;

	/**
	 * Default constructor for the abstract class
	 */
	public OSCPacket() {
		super();
	}

	/**
	 * Generate a representation of this packet conforming to the
	 * the OSC byte stream specification. Used Internally.
	 */
	protected void computeByteArray() {
		OSCJavaToByteArrayConverter stream = new OSCJavaToByteArrayConverter();
		computeByteArray(stream);
	}

	/**
	 * Subclasses should implement this method to product a byte array
	 * formatted according to the OSC specification.
	 * @param stream OscPacketByteArrayConverter
	 */
	protected abstract void computeByteArray(OSCJavaToByteArrayConverter stream);

	/**
	 * Return the OSC byte stream for this packet.
	 * @return byte[]
	 */
	public byte[] getByteArray() {
		if (!isByteArrayComputed) 
			computeByteArray();
		return byteArray;
	}

	/**
	 * Run any post construction initialization. (By default, do nothing.) 
	 */ 
	protected void init() {
		
	}
}