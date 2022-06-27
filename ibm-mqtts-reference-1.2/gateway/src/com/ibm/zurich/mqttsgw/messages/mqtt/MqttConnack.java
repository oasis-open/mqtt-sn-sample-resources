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
 * This object represents a Mqtt CONNACK message.
 * 
 *
 */
public class MqttConnack extends MqttMessage{

	//Mqtt CONNACK fields
	private boolean	topicNameCompression = false; //not used
	private int returnCode;

	/**
	 * MqttConack constructor.Sets the appropriate message type. 
	 */
	public MqttConnack() {
		msgType = MqttMessage.CONNACK;
	}

	/**
	 * MqttConack constructor.Sets the appropriate message type and constructs 
	 * a Mqtt CONNACK message from a received byte array.
	 * @param data: The buffer that contains the CONNACK message.
	 */
	public MqttConnack(byte[] data) {
		msgType = MqttMessage.CONNACK;
		returnCode = (data[3] & 0xFF);
	}

	/**
	 * Method to convert this message to byte array for transmission.
	 * (Don't needed in the GW)
	 */
	public byte[] toBytes() {
		int length = 4;
		byte[] data = new byte[length];
		data [0] = (byte)((msgType << 4) & 0xF0);
		data [1] = (byte)0x02;//add Remaining length fields
		data [2] = ((topicNameCompression) ? (byte)0x01 : (byte)0x00);
		data [3] = (byte) returnCode;
		return data;
	}


	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
}