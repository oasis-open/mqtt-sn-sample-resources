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
 * This object represents a Mqtt PUBCOMP message.
 * 
 *
 */
public class MqttPubComp extends MqttMessage{

	//Mqtt PUBCOMP fields
	private int msgId;

	/**
	 * MqttPubcomp constructor.Sets the appropriate message type. 
	 */
	public MqttPubComp() {
		msgType = MqttMessage.PUBCOMP;
	}

	/**
	 * MqttPubcomp constructor.Sets the appropriate message type and constructs
	 * a Mqtt PUBCOMP message from a received byte array.
	 * @param data: The buffer that contains the PUBCOMP message.
	 */
	public MqttPubComp(byte[] data) {
		msgType = MqttMessage.PUBCOMP;
		msgId = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
	}

	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the PUBCOMP message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 4;
		byte[] data = new byte[length];
		data [0] = (byte)((msgType << 4) & 0xF0);
		data [1] = (byte)0x02;//add Remaining length fields
		data [2] = (byte)((msgId >> 8) & 0xFF);
		data [3] = (byte) (msgId & 0xFF);			
		return data;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
}