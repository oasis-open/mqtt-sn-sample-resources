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
 * This object represents a Mqtt DISCONNECT message.
 * 
 *
 */
public class MqttDisconnect extends MqttMessage{

	/**
	 * MqttDisconnect constructor.Sets the appropriate message type. 
	 */
	public MqttDisconnect() {
		msgType = MqttMessage.DISCONNECT;
	}

	/**
	 * This method is not needed in the GW
	 */
	public MqttDisconnect(byte[] data) {}

	/**
	 * Method to convert this message to byte array for transmission.
	 * @return A byte array containing the DISCONNECT message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 2;
		byte[] data = new byte[length];
		data [0] = (byte)((msgType << 4) & 0xF0);
		data [1] = (byte)0x00;//add Remaining length field
		return data;
	}
}
