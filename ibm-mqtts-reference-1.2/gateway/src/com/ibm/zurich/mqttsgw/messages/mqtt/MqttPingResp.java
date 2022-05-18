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
 * This object represents a Mqtt PINGRESP message.
 * 
 *
 */
public class MqttPingResp extends MqttMessage{

	/**
	 * MqttPingResp constructor.Sets the appropriate message type. 
	 */
	public MqttPingResp() {
		msgType = MqttMessage.PINGRESP;
	}
	
	
	/**
	 * MqttPingResp constructor.Sets the appropriate message type. 
	 */
	public MqttPingResp(byte[] data) {
		msgType = MqttMessage.PINGRESP;
	}
	
	/**
	 * Method to convert this message to a byte array for transmission
	 * @return A byte array containing the PINGRESP message as it should appear on the wire
	 */
	public byte[] toBytes() {
		int length = 2;
		byte[] data = new byte[length];
		data [0] = (byte)((msgType << 4) & 0xF0);
		data [1] = (byte)0x00;//add Remaining length fields
		return data;
	}
}