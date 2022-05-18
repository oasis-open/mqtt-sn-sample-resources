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

import com.ibm.zurich.mqttsgw.utils.Utils;


/**
 * This object represents a Mqtt UNSUBSCRIBE message.
 * 
 */
public class MqttUnsubscribe extends MqttMessage{
	

	// Mqtt UNSUBSCRIBE fields
	private boolean dup;
//	private int qos = 1;//qos for the message itself is set to 1 
	private int msgId;
	private String topicName;//supports only one topic name at the time (as mqtts) not an array
	
	/**
	 * MqttUnsubscribe constructor.Sets the appropriate message type. 
	 */
	public MqttUnsubscribe() {
		msgType = MqttMessage.UNSUBSCRIBE;
	}
	
	/**
	 * MqttUnsubscribe constructor.Sets the appropriate message type and constructs 
	 * a Mqtt UNSUBSCRIBE message from a received byte array.
	 * @param data: The buffer that contains the UNSUBSCRIBE message.
	 * (Don't needed in the GW)
	 */
	public MqttUnsubscribe(byte[] data, int offset) {}
	
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the UNSUBSCRIBE message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = topicName.length() + 5;//1st byte plus 2 bytes for utf encoding and 2 for msgId
		byte[] data = new byte[length];
		data [0] = (byte)((msgType << 4) & 0xF0);	
		data [0] |= 0x02; //insert qos = 1
		data [1] = (byte)((msgId >> 8) & 0xFF);
		data [2] = (byte) (msgId & 0xFF);
		
		byte[] utfEncodedTopicName = Utils.StringToUTF(topicName);
		System.arraycopy(utfEncodedTopicName, 0, data, 3, utfEncodedTopicName.length);
		data = encodeMsgLength(data);	// add Remaining Length field
		return data;
	}

	public boolean isDup() {
		return dup;
	}

	public void setDup(boolean dup) {
		this.dup = dup;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}
}
