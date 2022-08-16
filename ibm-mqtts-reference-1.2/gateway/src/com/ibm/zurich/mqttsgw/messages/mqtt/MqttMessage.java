/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.messages.mqtt;

/**
 * This object represents a Mqtt message. It is subclassed
 * to create the appropriate Mqtt Message.
 * 
 * 
 * Parts of code on Mqtt message classes were imported from the similar classes on
 * com.ibm.mqttclient.ia92 package
 *
 */
public abstract class MqttMessage {

	//Mqtt messages types
	public final static int CONNECT 			= 1;
	public final static int CONNACK 			= 2;
	public final static int PUBLISH 			= 3;
	public final static int PUBACK 				= 4;
	public final static int PUBREC 				= 5;
	public final static int PUBREL 				= 6;
	public final static int PUBCOMP 			= 7;
	public final static int SUBSCRIBE 			= 8;
	public final static int SUBACK 				= 9;
	public final static int UNSUBSCRIBE 	 	= 10;
	public final static int UNSUBACK 			= 11;
	public final static int PINGREQ 			= 12;
	public final static int PINGRESP 			= 13;
	public final static int DISCONNECT 			= 14;
	
	// Mqtt message type
	protected int msgType;

	public final static int RETURN_CODE_CONNECTION_ACCEPTED = 0;

	// client id restriction
	public static final int    MAX_CLIENT_ID_LENGTH = 23;
	
	
	/**
	 * MqttMessage default constructor.
	 */
	public MqttMessage() {}
	
	
	/**
	 * This method calculates the length of a Mqtt message and encodes it 
	 * in the fixed header.
	 */	
	protected byte[] encodeMsgLength(byte[] data) {		
		int size = data.length - 1; 
		int pos = 0;				
		byte[] tmp = new byte[4]; 
		// Encode remaining length field in tmp[]
		do {
			int digit = size % 128;
			size = size / 128;
			if (size > 0) {
				digit = digit | 0x80;
			}
			tmp[pos++]=(byte) digit;
		} while (size > 0);
		byte[] buffer = new byte[data.length + pos];
		buffer[0] = data[0];								// Fixed Hdr
		System.arraycopy(tmp, 0, buffer, 1, pos);			// add the MsgLength bytes
		System.arraycopy(data,1,buffer,pos+1,data.length-1);// add the rest of Message
		data = buffer;
		return data;
	}
	
	/**
	 * This method decodes the Remaining length field of a Mqtt message and 
	 * returns the number of the remaining bytes of the message.
	 */	
	protected long decodeMsgLength(byte[] data) {		
		byte digit;
		long msgLength = 0;
		int multiplier = 1;
		int offset = 1;
		do {
			// Now read msgLength bytes
			digit = (byte)data[offset];
			msgLength += ((digit & 0x7F) * multiplier);
			multiplier *= 128;
			offset++;
		} while ((digit & 0x80) != 0);		
		return msgLength;
	}


	/**
	 * This method is implemented in subclasses.
	 */
	public abstract byte[] toBytes ();		
	
	
	public int getMsgType() {
		return msgType;
	}


	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}	
}