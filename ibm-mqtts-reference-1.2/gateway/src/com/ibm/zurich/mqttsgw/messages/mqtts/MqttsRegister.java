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

import java.io.UnsupportedEncodingException;
import com.ibm.zurich.mqttsgw.utils.Utils;

/**
 * This object represents a Mqtts REGISTER message.
 * 
 *
 */
public class MqttsRegister extends MqttsMessage {
	
	//Mqtts REGISTER fields
	private int topicId;
	private int msgId;
	private String topicName;
		
	/**
	 * MqttsRegister constructor.Sets the appropriate message type. 
	 */
	public MqttsRegister() {
		msgType = MqttsMessage.REGISTER;
	}
	
	/**
	 * MqttsRegister constructor.Sets the appropriate message type and constructs 
	 * a Mqtts REGISTER message from a received byte array.
	 * @param data: The buffer that contains the REGISTER message.
	 */	
	public MqttsRegister(byte[] data) {
		msgType = MqttsMessage.REGISTER;
		topicId = 0;//send by the client
		msgId = ((data[4] & 0xFF) << 8) + (data[5] & 0xFF);
		int tlen = (data[0] & 0xFF) - 6;
		byte[] byteTopicName = new byte[tlen];
		System.arraycopy(data, 6, byteTopicName, 0, tlen);
		try {
			topicName = new String(byteTopicName, Utils.STRING_ENCODING);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the REGISTER message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 6 + topicName.length();
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)((topicId >> 8) & 0xFF);
		data[3] = (byte)(topicId & 0xFF);
		data[4] = (byte)((msgId >> 8) & 0xFF);
		data[5] = (byte)(msgId & 0xFF);
		System.arraycopy(topicName.getBytes(), 0, data, 6, topicName.length());		
		return data;
	}
			
	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public int getTopicId() {
		return topicId;
	}

	public void setTopicId(int topicId) {
		this.topicId = topicId;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}
}
