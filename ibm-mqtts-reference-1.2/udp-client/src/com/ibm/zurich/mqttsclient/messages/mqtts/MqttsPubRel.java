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
 * This object represents a Mqtts PUBREL message.
 * 
 *
 */
public class MqttsPubRel extends MqttsMessage {
	
	//Mqtts PUBREL fields
	private int msgId;

	/**
	 * MqttsPubrel constructor.Sets the appropriate message type. 
	 */
	public MqttsPubRel() {
		msgType = MqttsMessage.PUBREL;
	}
	
	/**
	 * MqttsPubrel constructor.Sets the appropriate message type and constructs 
	 * a Mqtts PUBREL message from a received byte array.
	 * @param data: The buffer that contains the PUBREL message.
	 */
	public MqttsPubRel(byte[] data) {
		msgType = MqttsMessage.PUBREL;
		msgId = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the PUBREL message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 4;
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)((msgId >> 8) & 0xFF);
		data[3] = (byte)(msgId & 0xFF);	
		return data;		
	}
	
	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
}