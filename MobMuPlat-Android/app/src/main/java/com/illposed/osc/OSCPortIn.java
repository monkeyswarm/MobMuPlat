package com.illposed.osc;

import java.net.*;
import java.io.IOException;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCPacketDispatcher;

/**
 * OSCPortIn is the class that listens for OSC messages.
 * <p>
 * An example based on com.illposed.osc.test.OSCPortTest::testReceiving() :
 * <pre>
 
	receiver = new OSCPortIn(OSCPort.defaultSCOSCPort());
	OSCListener listener = new OSCListener() {
		public void acceptMessage(java.util.Date time, OSCMessage message) {
			System.out.println("Message received!");
		}
	};
	receiver.addListener("/message/receiving", listener);
	receiver.startListening();

 * </pre>
 * <p>		
 * Then, using a program such as SuperCollider or sendOSC, send a message
 * to this computer, port 57110 (defaultSCOSCPort), with the address /message/receiving
 * <p>
 * Copyright (C) 2004-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 * <p>
 * See license.txt (or license.rtf) for license information.
 *
 * @author Chandrasekhar Ramakrishnan
 * @version 1.0
 */
public class OSCPortIn extends OSCPort implements Runnable {

	//DEI
	protected MulticastSocket socket;
	
	// state for listening
	protected boolean isListening;
	protected OSCByteArrayToJavaConverter converter = new OSCByteArrayToJavaConverter();
	protected OSCPacketDispatcher dispatcher = new OSCPacketDispatcher();
	
	/**
	 * Create an OSCPort that listens on the specified port.
	 * @param port UDP port to listen on.
	 * @throws IOException 
	 */
	public OSCPortIn(int port) throws IOException {
		socket = new MulticastSocket(port);//new DatagramSocket(port);
		InetAddress groupAddress = InetAddress.getByName("224.0.0.1");
		socket.joinGroup(groupAddress);
		this.port = port;
	}
	
	/**
	 * Run the loop that listens for OSC on a socket until isListening becomes false.
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
			// buffers were 1500 bytes in size, but this was
			// increased to 1536, as this is a common MTU
		byte[] buffer = new byte[1536];
		DatagramPacket packet = new DatagramPacket(buffer, 1536);
		while (isListening) {
			try {
				socket.receive(packet);
				OSCPacket oscPacket = converter.convert(buffer, packet.getLength());
				dispatcher.dispatchPacket(oscPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Start listening for incoming OSCPackets
	 */
	public void startListening() {
		isListening = true;
		Thread thread = new Thread(this);
		thread.start();
	}
	
	/**
	 * Stop listening for incoming OSCPackets
	 */
	public void stopListening() {
		isListening = false;
	}
	
	/**
	 * Am I listening for packets?
	 */
	public boolean isListening() {
		return isListening;
	}
	
	/**
	 * Register the listener for incoming OSCPackets addressed to an Address
	 * @param anAddress  the address to listen for
	 * @param listener   the object to invoke when a message comes in
	 */
	public void addListener(String anAddress, OSCListener listener) {
		dispatcher.addListener(anAddress, listener);
	}
	
	/**
	 * Close the socket and free-up resources. It's recommended that clients call
	 * this when they are done with the port.
	 */
	public void close() {
		socket.close();
	}
}
