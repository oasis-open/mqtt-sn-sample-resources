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
 * This object represents a Mqtt PUBLISH message.
 * 
 *
 */
public class MqttPublish extends MqttMessage{

	//Mqtt PUBLISH fields
	private boolean dup;
	private int qos;
	private boolean retain;
	private String topicName;
	private int msgId;
	private byte[] payload;

	/**
	 * MqttPublish constructor.Sets the appropriate message type. 
	 */
	public MqttPublish() {
		msgType = MqttMessage.PUBLISH;	
	}

	/**
	 * MqttPublish constructor.Sets the appropriate message type and constructs
	 * a Mqtt PUBLISH message from a received byte array.
	 * @param data: The buffer that contains the PUBLISH message.
	 */
	public MqttPublish(byte[] data) {
		msgType = MqttMessage.PUBLISH;	
		dup = ((data[0] & 0x08) >> 3 != 0);
		qos = (data[0] & 0x06) >> 1;
		retain = ((data[0] & 0x01) != 0);		

		long remainingBytes = decodeMsgLength(data);//the number of remaining bytes after the fixed header
		int fixedHeaderLength = (int) (data.length - remainingBytes);//the length of the fixed header
		topicName = Utils.UTFToString(data, fixedHeaderLength);

		if (qos > 0) {
			msgId = ((data[fixedHeaderLength + 2 + topicName.length()] & 0xFF) << 8) + (data[fixedHeaderLength + 2 + topicName.length() + 1] & 0xFF);
			payload = Utils.SliceByteArray(data, fixedHeaderLength + topicName.length() + 4, data.length - (fixedHeaderLength + topicName.length() + 4));
		} else {
			payload = Utils.SliceByteArray(data, fixedHeaderLength + topicName.length() + 2, data.length - (fixedHeaderLength + topicName.length() + 2));
		}
	}

	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the PUBLISH message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		byte[] data;
		byte[] byteString = Utils.StringToUTF(topicName);
		if ( qos > 0 ) {
			data = new byte[byteString.length + 3 + payload.length];
		} else {
			// No message id in a QoS 0 message
			data = new byte[byteString.length + 1 + payload.length];
		}

		data[0] = (byte)((this.msgType << 4) & 0xF0);//msg type
		byte bdup = (byte) ((dup) ? 0x08 : 0x00);//dup flag
		byte bqos = (byte) ((qos & 0x03) << 1);//qos
		byte bret = (byte) ((retain) ? 0x01 : 0x00);//retain

		data[0] = (byte) (data[0] | bqos | bret | bdup);//1st byte completed

		int pos = 1;	
		System.arraycopy(byteString,0,data,pos,byteString.length);//attach the topic name
		pos+=byteString.length;

		if (qos > 0) {
			int msgId = getMsgId();
			data[pos++] = (byte) (msgId / 256); // MSB
			data[pos++] = (byte) (msgId % 256); // LSB
		}  
		System.arraycopy(payload,0,data,pos,payload.length);//attach the payload
		data = encodeMsgLength(data);	// add Remaining Length field
		return data;
	}

	public boolean isDup() {
		return dup;
	}

	public void setDup(boolean dup) {
		this.dup = dup;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public boolean isRetain() {
		return retain;
	}

	public void setRetain(boolean retain) {
		this.retain = retain;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}	
}
