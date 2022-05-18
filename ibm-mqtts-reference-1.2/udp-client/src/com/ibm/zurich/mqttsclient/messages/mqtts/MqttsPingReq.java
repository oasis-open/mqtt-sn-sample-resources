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

/**
 * This object represents a Mqtts PINGREQ message.
 * 
 *
 */
public class MqttsPingReq extends MqttsMessage {

	/**
	 * MqttsPingReq constructor.Sets the appropriate message type. 
	 */
	public MqttsPingReq() {
		msgType = MqttsMessage.PINGREQ;
	}
	
	/**
	 * MqttsPingReq constructor.Sets the appropriate message type and constructs 
	 * a Mqtts PINGREQ message from a received byte array.
	 * @param data: The buffer that contains the PINGREQ message.
	 */
	public MqttsPingReq(byte[] data) {
		msgType = MqttsMessage.PINGREQ;
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the PINGREQ message as it should appear on the wire.
	 */	
	public byte[] toBytes() {
		int length = 2;
		byte[] data = new byte[length];
		data[0] = (byte)length;
		data[1] = (byte)msgType;		
		return data;
	}
}