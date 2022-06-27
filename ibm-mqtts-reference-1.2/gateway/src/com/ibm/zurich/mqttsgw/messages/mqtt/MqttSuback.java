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
 * This object represents a Mqtt SUBACK message.
 * 
 */
public class MqttSuback extends MqttMessage {

	//Mqtt SUBACK fields
	private int msgId;
	private int grantedQoS; //supports only one topic name at the time (as with mqtts) not an array


	/**
	 * MqttSuback constructor.Sets the appropriate message type. 
	 */
	public MqttSuback() {
		msgType = MqttMessage.SUBACK;
	}

	/**
	 * MqttSuback constructor.Sets the appropriate message type and constructs 
	 * a Mqtt SUBACK message from a received byte array.
	 * @param data: The buffer that contains the SUBACK message.
	 */
	public MqttSuback(byte[] data) {
		msgType = MqttMessage.SUBACK;
		msgId = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
		grantedQoS = (data[4] & 0x03);
	}

	/**
	 * Method to convert this message to byte array for transmission.
	 * @return A byte array containing the UNSUBACK message as it should appear on the wire.
	 * (Don't needed in the GW)
	 */
	public byte[] toBytes() {
		int length = 5;
		byte[] data = new byte[length];
		data [0] = (byte)((msgType << 4) & 0xF0);
		data [1] = (byte)0x03;//add Remaining length fields
		data [2] = (byte)((msgId >> 8) & 0xFF);
		data [3] = (byte) (msgId & 0xFF);
		data [4] = (byte)(grantedQoS & 0xFF);		
		return data;
	}


	public int getGrantedQoS() {
		return grantedQoS;
	}

	public void setGrantedQoS(int grantedQoS) {
		this.grantedQoS = grantedQoS;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
}
