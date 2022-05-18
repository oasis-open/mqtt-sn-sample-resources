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
 * This object represents a Mqtts REGACK message.
 * 
 *
 */
public class MqttsRegack extends MqttsMessage {

	//Mqtts REGACK fields
	private int msgId;
	private int returnCode;
	private int topicId;
		
	/**
	 * MqttsRegack constructor.Sets the appropriate message type. 
	 */
	public MqttsRegack() {
		msgType = MqttsMessage.REGACK;
	}
	
	/**
	 * MqttsRegack constructor.Sets the appropriate message type and constructs 
	 * a Mqtts REGACK message from a received byte array.
	 * @param data: The buffer that contains the REGACK message.
	 */	
	public MqttsRegack(byte[] data) {
		msgType = MqttsMessage.REGACK;
		topicId = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
		msgId = ((data[4] & 0xFF) << 8) + (data[5] & 0xFF);
		returnCode = (data[6] & 0xFF);
	}

	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the REGACK message as it should appear on the wire.
	 */
	public byte[] toBytes(){
		int length = 7;
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)((topicId >> 8) & 0xFF);
		data[3] = (byte)(topicId & 0xFF);
		data[4] = (byte)((msgId >> 8) & 0xFF);
		data[5] = (byte)(msgId & 0xFF);
		data[6] = (byte)(returnCode);		
		return data;		
	}
	
	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public int getTopicId() {
		return topicId;
	}

	public void setTopicId(int topicId) {
		this.topicId = topicId;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
}