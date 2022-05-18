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
 * This object represents a Mqtts UNSUBACK message.
 *
 */
public class MqttsUnsuback extends MqttsMessage{

	//Mqtts UNSUBACK fields
	private int msgId;
	
	
	/**
	 * MqttsUnsuback constructor.Sets the appropriate message type. 
	 */
	public MqttsUnsuback() {
		msgType = MqttsMessage.UNSUBACK;
	}
	
	/**
	 * MqttsUnsuback constructor.Sets the appropriate message type and constructs 
	 * a Mqtts UNSUBACK message from a received byte array.
	 * @param data: The buffer that contains the UNSUBACK message.
	 * (Don't needed in the GW)
	 */	
	public MqttsUnsuback(byte[] data){
		msgType = MqttsMessage.UNSUBACK;
		msgId = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the UNSUBACK message as it should appear on the wire.
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