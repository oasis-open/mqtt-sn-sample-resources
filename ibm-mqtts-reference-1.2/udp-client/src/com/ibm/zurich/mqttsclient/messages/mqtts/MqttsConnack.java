/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsclient.messages.mqtts;

import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsMessage;

/**
 * This object represents a Mqtts CONNACK message.
 * 

 *
 */
public class MqttsConnack extends MqttsMessage {
	//Mqtts CONNACK fields
	private int returnCode;
	
	/**
	 * MqttsConack constructor.Sets the appropriate message type. 
	 */
	public MqttsConnack() {
		msgType = MqttsMessage.CONNACK;
	}
	
	/**
	 * MqttsConack constructor.Sets the appropriate message type and constructs
	 * a Mqtts CONNACK message from a received byte array.
	 * @param data: The buffer that contains the CONNACK message.
	 * (Don't needed in the GW)
	 */
	public MqttsConnack(byte[] data){
		msgType = MqttsMessage.CONNACK;
		returnCode = (data[2] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the CONNACK message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 3;
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)returnCode;  	
		return data;
	}
		
	public int getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
}