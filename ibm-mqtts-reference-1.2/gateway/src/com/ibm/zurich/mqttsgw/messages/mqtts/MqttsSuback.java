/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.messages.mqtts;

import java.io.UnsupportedEncodingException;
import com.ibm.zurich.mqttsgw.utils.Utils;

/**
 * This object represents a Mqtts SUBACK message.
 * 
 */
public class MqttsSuback extends MqttsMessage{
	
	//Mqtts SUBACK fields
	private int grantedQoS;	
	private int topicIdType;	
	private int   msgId = 0;	
	private int returnCode;
	
	private byte[] byteTopicId;
	
	//The form of TopicID that depends on TopicIdType.
	//Maybe either an int or a String.
	private int topicId = 0;
	private int predefinedTopicId = 0;
	private String shortTopicName = "";
	
	/**
	 * MqttsSuback constructor.Sets the appropriate message type. 
	 */
	public MqttsSuback() {
		msgType = MqttsMessage.SUBACK;
	}
	
	/**
	 * MqttsSuback constructor.Sets the appropriate message type and constructs 
	 * a Mqtts SUBACK message from a received byte array.
	 * @param data: The buffer that contains the SUBACK message.
	 * (Don't needed in the GW)
	 */	
	public MqttsSuback(byte[] data){
		msgType = MqttsMessage.SUBACK;
		grantedQoS = (data[2] & 0x60) >> 5;
		if(grantedQoS == 4) grantedQoS = -1;
		topicIdType = (data[2] & 0x03);		
		byteTopicId = new byte[2];
		try {
			if (topicIdType == MqttsMessage.NORMAL_TOPIC_ID){
				byteTopicId [0] = data[3];
				byteTopicId [1] = data[4];
				topicId = ((byteTopicId[0] & 0xFF) << 8) + (byteTopicId[1] & 0xFF);
			}else if(topicIdType == MqttsMessage.PREDIFINED_TOPIC_ID){
				byteTopicId [0] = data[3];
				byteTopicId [1] = data[4];
				predefinedTopicId = ((byteTopicId[0] & 0xFF) << 8) + (byteTopicId[1] & 0xFF);
			}else if(topicIdType == MqttsMessage.SHORT_TOPIC_NAME){
				System.arraycopy(data, 3, byteTopicId, 0, byteTopicId.length);
				shortTopicName = new String(byteTopicId,Utils.STRING_ENCODING);
			}else
				throw new IllegalArgumentException("Unknown topic id type: " + topicIdType);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		msgId   = ((data[5] & 0xFF) << 8) + (data[6] & 0xFF);
		returnCode = (data[7] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the SUBACK message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int flags = 0;
		if(grantedQoS == -1) {
			flags |= 0x60; 
		} else if(grantedQoS == 0) {
		
		} else if(grantedQoS == 1) {
			flags |= 0x20;
		} else if(grantedQoS == 2) {
			flags |= 0x40;
		} else {
			throw new IllegalArgumentException("Unknown QoS value: " + grantedQoS);
		}
		byteTopicId = new byte[2];
		if(topicIdType == MqttsMessage.NORMAL_TOPIC_ID){
			byteTopicId [0] = (byte)((topicId >> 8) & 0xFF);
			byteTopicId [1] = (byte) (topicId & 0xFF);
		}else if (topicIdType == MqttsMessage.PREDIFINED_TOPIC_ID){
			flags |= 0x01;
			byteTopicId [0] = (byte)((predefinedTopicId >> 8) & 0xFF);
			byteTopicId [1] = (byte) (predefinedTopicId & 0xFF);
		}else if (topicIdType == MqttsMessage.SHORT_TOPIC_NAME){
			flags |= 0x02;
			System.arraycopy(shortTopicName.getBytes(), 0, byteTopicId, 0, byteTopicId.length);
		}else {
			throw new IllegalArgumentException("Unknown topic id type: " + topicIdType);
		}
		
		int length = 8;	
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;
		data[2] = (byte)flags; 
		data[3] = byteTopicId[0];
		data[4] = byteTopicId[1];
		data[5] = (byte)((msgId >> 8) & 0xFF);
		data[6] = (byte) (msgId & 0xFF);
		data[7] = (byte)returnCode;		
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

	public byte[] getByteTopicId() {
		return byteTopicId;
	}

	public void setByteTopicId(byte[] byteTopicId) {
		this.byteTopicId = byteTopicId;
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

	public int getTopicIdType() {
		return topicIdType;
	}

	public void setTopicIdType(int topicIdType) {
		this.topicIdType = topicIdType;
	}

	public String getShortTopicName() {
		return shortTopicName;
	}

	public void setShortTopicName(String shortTopicName) {
		this.shortTopicName = shortTopicName;
	}

	public int getPredefinedTopicId() {
		return predefinedTopicId;
	}

	public void setPredefinedTopicId(int predefinedTopicId) {
		this.predefinedTopicId = predefinedTopicId;
	}
}