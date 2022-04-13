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

import com.ibm.zurich.mqttsgw.utils.Utils;

/**
 * This object represents a Mqtt SUBSCRIBE message.
 * 
 *
 */
public class MqttSubscribe extends MqttMessage{

	//Mqtt SUBSCRIBE fields
	private boolean dup;
//	private final int qos = 1;//qos for the message itself is set to 1 
	private int msgId;
	public String topicName;//supports only one topic name at the time (as mqtts) not an array
	public int	requestedQoS;


	/**
	 * MqttSubscribe constructor.Sets the appropriate message type. 
	 */
	public MqttSubscribe() {
		msgType = MqttMessage.SUBSCRIBE;
	}

	/**
	 * MqttSubscribe constructor.Sets the appropriate message type and constructs 
	 * a Mqtt SUBSCRIBE message from a received byte array.
	 * @param data: The buffer that contains the SUBSCRIBE message.
	 * (Don't needed in the GW)
	 */
	public MqttSubscribe(byte[] data) {}


	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the SUBSCRIBE message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = topicName.length() + 6;//1st byte plus 2 bytes for utf encoding, 2 for msgId and 1 for requested qos
		byte[] data = new byte[length];
		data [0] = (byte)((msgType << 4) & 0xF0);	
		data [0] |= 0x02; //insert qos = 1
		data [1] = (byte)((msgId >> 8) & 0xFF);
		data [2] = (byte) (msgId & 0xFF);

		byte[] utfEncodedTopicName = Utils.StringToUTF(topicName);
		System.arraycopy(utfEncodedTopicName, 0, data, 3, utfEncodedTopicName.length);
		data[length-1] = (byte)(requestedQoS);//insert requested qos
		data = encodeMsgLength(data);	// add Remaining Length field
		return data;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public int getRequestedQoS() {
		return requestedQoS;
	}

	public void setRequestedQoS(int requestedQoS) {
		this.requestedQoS = requestedQoS;
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
}
