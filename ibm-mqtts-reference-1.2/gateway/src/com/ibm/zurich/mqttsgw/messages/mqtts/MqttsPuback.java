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

package com.ibm.zurich.mqttsgw.messages.mqtts;

/**
 * This object represents a Mqtts PUBACK message.
 * 
 *
 */
public class MqttsPuback extends MqttsMessage {
	
	//Mqtts PUBACK fields
	private int msgId;
	private int returnCode;
	private byte[] byteTopicId;
	
	
	//The form of TopicId maybe either an int or a String.
	private int topicId = 0;
	private String shortTopicName = null;

	/**
	 * MqttsPuback constructor.Sets the appropriate message type. 
	 */
	public MqttsPuback() {
		msgType = MqttsMessage.PUBACK;//check the conversion to bytes
	}

	/**
	 * MqttsPuback constructor.Sets the appropriate message type and constructs 
	 * a Mqtts PUBACK message from a received byte array.
	 * @param data: The buffer that contains the PUBACK message.
	 */
	public MqttsPuback(byte[] data) {
		msgType = MqttsMessage.PUBACK;		
		msgId = ((data[4] & 0xFF) << 8) + (data[5] & 0xFF);
		returnCode = (data[6] & 0xFF);
		if (returnCode == MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID);
			topicId = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the PUBACK message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 7;
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;
		
		byteTopicId = new byte[2];
		if (this.topicId != 0){
			byteTopicId[0] = (byte)((topicId >> 8) & 0xFF);
			byteTopicId[1] = (byte) (topicId & 0xFF);
		}else if(this.shortTopicName != null)
			byteTopicId = shortTopicName.getBytes();
		
		System.arraycopy(byteTopicId, 0, data, 2, byteTopicId.length);	
		data[4] = (byte)((msgId >> 8) & 0xFF);
		data[5] = (byte) (msgId & 0xFF);
		data[6] = (byte)returnCode;		
		return data;
	}
	

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public int getTopicId() {
		return topicId;
	}

	public void setTopicId(int topicId) {
		this.topicId = topicId;
	}

	public String getShortTopicName() {
		return shortTopicName;
	}

	public void setShortTopicName(String shortTopicName) {
		this.shortTopicName = shortTopicName;
	}

	public byte[] getByteTopicId() {
		return byteTopicId;
	}

	public void setByteTopicId(byte[] byteTopicId) {
		this.byteTopicId = byteTopicId;
	}
}