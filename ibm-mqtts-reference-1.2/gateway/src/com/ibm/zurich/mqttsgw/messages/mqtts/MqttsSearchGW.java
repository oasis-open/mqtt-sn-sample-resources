/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.messages.mqtts;

/**
 * This object represents a Mqtts SEARCHGW message.
 * 
 *
 */
public class MqttsSearchGW extends MqttsMessage{
	
	//Mqtts SEARCHGW fields
	private int radius;

	/**
	 * MqttsSearchGW constructor.Sets the appropriate message type. 
	 */
	public MqttsSearchGW() {
		msgType = MqttsMessage.SEARCHGW;
	}
	
	/**
	 * MqttsSearchGW constructor.Sets the appropriate message type and constructs 
	 * a Mqtts SEARCHGW message from a received byte array.
	 * @param data: The buffer that contains the SEARCHGW message.
	 */	
	public MqttsSearchGW(byte[] data) {
		msgType = MqttsMessage.SEARCHGW;
		radius = (data[2] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the SEARCHGW message as it should appear on the wire.
	 * (Dont't needed in the GW)
	 */
	public byte[] toBytes(){
		int length = 3;
		byte[] data = new byte[length];
		data[0] = (byte)length;
		data[1] = (byte)msgType;
		data[2] = (byte)radius;	
		return data;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}	
}