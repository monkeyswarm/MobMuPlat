package com.illposed.osc;

import java.math.BigInteger;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Vector;

import com.illposed.osc.utility.*;

/**
 * A bundle represents a collection of osc packets (either messages or other bundles) and
 * has a timetag which can be used by a scheduler to execute a bundle in the future instead
 * of immediately (OSCMessages are executed immediately). Bundles should be used if you want
 * to send multiple messages to be executed atomically together, or you want to schedule one
 * or more messages to be executed in the future.
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
public class OSCBundle extends OSCPacket {

	/**
	 * 2208988800 seconds -- includes 17 leap years
	 */
	public static final BigInteger SECONDS_FROM_1900_to_1970 =
		new BigInteger("2208988800");
		
	/**
	 * The Java representation of an OSC timestamp with the semantics of "immediately"
	 */		
	public static final Date TIMESTAMP_IMMEDIATE = new Date(0);

	protected Date timestamp;
	protected Vector packets;

	/**
	 * Create a new empty OSCBundle with a timestamp of immediately.
	 * You can add packets to the bundle with addPacket()
	 */
	public OSCBundle() {
		this(null, TIMESTAMP_IMMEDIATE);
	}
	
	/**
	 * Create an OSCBundle with the specified timestamp.
	 * @param timestamp the time to execute the bundle
	 */
	public OSCBundle(Date timestamp) {
		this(null, timestamp);
	}

	/**
	 * Create an OSCBundle made up of the given packets with a timestamp of now.
	 * @param packets array of OSCPackets to initialize this object with
	 */
	public OSCBundle(OSCPacket[] packets) {
		this(packets, TIMESTAMP_IMMEDIATE);
	}

	/**
	 * Create an OSCBundle, specifying the packets and timestamp.
	 * @param packets the packets that make up the bundle
	 * @param timestamp the time to execute the bundle
	 */
	public OSCBundle(OSCPacket[] packets, Date timestamp) {
		super();
		if (null != packets) {
			this.packets = new Vector(packets.length);
			for (int i = 0; i < packets.length; i++) {
				this.packets.add(packets[i]);
			}
		} else
			this.packets = new Vector();
		this.timestamp = timestamp;
		init();
	}
	
	/**
	 * Return the time the bundle will execute.
	 * @return a Date
	 */
	public Date getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Set the time the bundle will execute.
	 * @param timestamp Date
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Add a packet to the list of packets in this bundle.
	 * @param packet OSCMessage or OSCBundle
	 */
	public void addPacket(OSCPacket packet) {
		packets.add(packet);
	}
	
	/**
	 * Get the packets contained in this bundle.
	 * @return an array of packets
	 */
	public OSCPacket[] getPackets() {
		OSCPacket[] packetArray = new OSCPacket[packets.size()];
		packets.toArray(packetArray);
		return packetArray;
	}

	/**
	 * Convert the timetag (a Java Date) into the OSC byte stream. Used Internally.
	 */
	protected void computeTimeTagByteArray(OSCJavaToByteArrayConverter stream) {
		if ((null == timestamp) || (timestamp == TIMESTAMP_IMMEDIATE)) {
			stream.write((int) 0);
			stream.write((int) 1);
			return;
		}
		
		long millisecs = timestamp.getTime();
		long secsSince1970 = (long) (millisecs / 1000);
		long secs = secsSince1970 + SECONDS_FROM_1900_to_1970.longValue();
			// the next line was cribbed from jakarta commons-net's NTP TimeStamp code
		long fraction = ((millisecs % 1000) * 0x100000000L) / 1000;
		
		stream.write((int) secs);
		stream.write((int) fraction);
	}

	/**
	 * Compute the OSC byte stream representation of the bundle. Used Internally.
	 * @param stream OscPacketByteArrayConverter
	 */
	protected void computeByteArray(OSCJavaToByteArrayConverter stream) {
		stream.write("#bundle");
		computeTimeTagByteArray(stream);
		Enumeration myenum = packets.elements();
		OSCPacket nextElement;
		byte[] packetBytes;
		while (myenum.hasMoreElements()) {
			nextElement = (OSCPacket) myenum.nextElement();
			packetBytes = nextElement.getByteArray();
			stream.write(packetBytes.length);
			stream.write(packetBytes);
		}
		byteArray = stream.toByteArray();
	}

}