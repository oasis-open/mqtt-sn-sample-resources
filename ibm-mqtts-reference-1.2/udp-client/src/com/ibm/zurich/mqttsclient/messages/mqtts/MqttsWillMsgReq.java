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
 * This object represents a Mqtts WILLMSGREQ message.
 * 
 *
 */
public class MqttsWillMsgReq extends MqttsMessage{

	/**
	 * MqttsWillMsgReq constructor.Sets the appropriate message type. 
	 */
	public MqttsWillMsgReq() {
		msgType = MqttsMessage.WILLMSGREQ;
	}
	
	/**
	 * MqttsWillMsgReq constructor.Sets the appropriate message type and constructs 
	 * a Mqtts WILLMSGREQ message from a received byte array.
	 * @param data: The buffer that contains the WILLMSGREQ message.
	 */
	public MqttsWillMsgReq(byte[] data){
		msgType = MqttsMessage.WILLMSGREQ;
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the WILLMSGREQ message as it should appear on the wire.
	 */
	public byte [] toBytes() {
		int length = 2;
		byte[] data = new byte[length];
		data[0] = (byte)length;
		data[1] = (byte)msgType;		
		return data;
	}
}