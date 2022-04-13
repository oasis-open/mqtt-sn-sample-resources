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

import java.io.UnsupportedEncodingException;

import com.ibm.zurich.mqttsgw.exceptions.MqttsException;
//import com.ibm.zurich.mqttsgw.utils.GatewayLogger;
import com.ibm.zurich.mqttsgw.utils.Utils;

/**
 * This object represents a Mqtts SUBSCRIBE message.
 * 
 */
public class MqttsSubscribe extends MqttsMessage {
	
	//Mqtts SUBSCRIBE message
	private boolean  dup;
	private int      qos;//the requested qos
	private int topicIdType;
		
	private int   msgId = 0;
	private byte[] byteTopicId;
	
	//The form of TopicName(or TopicID) that depends on TopicIdType.
	//Maybe either an int or a String.
	private String topicName = "";
	private int predefinedTopicId = 0;
	private String shortTopicName = "";
	
	/**
	 * MqttsSubscribe constructor.Sets the appropriate message type. 
	 */
	public MqttsSubscribe() {
		msgType = MqttsMessage.SUBSCRIBE;
	}
	
	/**
	 * MqttsSubscribe constructor.Sets the appropriate message type and constructs 
	 * a Mqtts SUBSCRIBE message from a received byte array.
	 * @param data: The buffer that contains the SUBSCRIBE message.
	 * @throws MqttsException 
	 */	
	public MqttsSubscribe(byte[] data) throws MqttsException {
		msgType = MqttsMessage.SUBSCRIBE;
		dup = ((data[2] & 0x80) >> 7 != 0);
		qos = (data[2] & 0x60) >> 5;
		if(qos == 4) qos = -1;
		topicIdType = (data[2] & 0x03);		
		msgId   = ((data[3] & 0xFF) << 8) + (data[4] & 0xFF);
		
		int length = (data[0] & 0xFF)-5;
		byteTopicId = new byte[length];

		try {
			switch (topicIdType){
				case MqttsMessage.TOPIC_NAME:
					System.arraycopy(data, 5, byteTopicId, 0, length);
					topicName = new String(byteTopicId,Utils.STRING_ENCODING);
					break;
					
				case MqttsMessage.PREDIFINED_TOPIC_ID:
					if(length != 2){
						throw new MqttsException("Wrong format. Predefined topic id must be 2 bytes long.");
					}
					byteTopicId[0] = data[5];
					byteTopicId[1] = data[6];
					predefinedTopicId = ((byteTopicId[0] & 0xFF) << 8) + (byteTopicId[1] & 0xFF);
					break;
				case MqttsMessage.SHORT_TOPIC_NAME:
					if(length != 2)
						throw new MqttsException("Wrong format. Short topic name must be 2 bytes long.");
					System.arraycopy(data, 5, byteTopicId, 0, byteTopicId.length);
					shortTopicName = new String(byteTopicId,Utils.STRING_ENCODING);
					break;
				
				default:
					throw new MqttsException("Unknown topic id type: " + topicIdType);
			}
		
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the SUBSCRIBE message as it should appear on the wire.
	 * (Don't needed in the GW)
	 */	
	public byte[] toBytes(){
		int flags = 0;
		if(dup) {
			flags |= 0x80;
		}		
		if(qos == -1) {
			flags |= 0x60; //TODO check if this is right
		} else if(qos == 0) {
		
		} else if(qos == 1) {
			flags |= 0x20;
		} else if(qos == 2) {
			flags |= 0x40;
		} else {
			throw new IllegalArgumentException("Unknown QoS value: " + qos);
		}
		if(topicIdType == MqttsMessage.TOPIC_NAME){
			byteTopicId = new byte[topicName.length()];
			System.arraycopy(topicName.getBytes(), 0, byteTopicId, 0, byteTopicId.length);
		}else if (topicIdType == MqttsMessage.PREDIFINED_TOPIC_ID){
			flags |= 0x01;
			byteTopicId = new byte[2];
			byteTopicId [0] = (byte)((predefinedTopicId >> 8) & 0xFF);
			byteTopicId [1] = (byte) (predefinedTopicId & 0xFF);
		}else if (topicIdType == MqttsMessage.SHORT_TOPIC_NAME){
			flags |= 0x02;
			byteTopicId = new byte[2];
			System.arraycopy(shortTopicName.getBytes(), 0, byteTopicId, 0, byteTopicId.length);
		}else {
			throw new IllegalArgumentException("Unknown topic id type: " + topicIdType);
		}
		
		int length = 5 + byteTopicId.length;	
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;
		data[2] = (byte)flags; 
		data[3] = (byte)((msgId >> 8) & 0xFF);
		data[4] = (byte) (msgId & 0xFF);
		System.arraycopy(byteTopicId, 0, data, 5, byteTopicId.length);		
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

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public int getTopicIdType() {
		return topicIdType;
	}

	public void setTopicIdType(int topicIdType) {
		this.topicIdType = topicIdType;
	}

	public byte[] getByteTopicId() {
		return byteTopicId;
	}

	public void setByteTopicId(byte[] byteTopicId) {
		this.byteTopicId = byteTopicId;
	}

	public int getPredefinedTopicId() {
		return predefinedTopicId;
	}

	public void setPredefinedTopicId(int predefinedTopicId) {
		this.predefinedTopicId = predefinedTopicId;
	}

	public String getShortTopicName() {
		return shortTopicName;
	}

	public void setShortTopicName(String shortTopicName) {
		this.shortTopicName = shortTopicName;
	}
}