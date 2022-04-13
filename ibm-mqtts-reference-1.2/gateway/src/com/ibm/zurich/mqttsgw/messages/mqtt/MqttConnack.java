/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Ian Craggs - initial API and implementation and/or initial documentation
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