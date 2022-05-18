/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsclient.messages.mqtts;

import java.io.UnsupportedEncodingException;
import com.ibm.zurich.mqttsclient.utils.Utils;

/**
 * This object represents a Mqtts WILLTOPIC message.
 * 
 *
 */
public class MqttsWillTopic extends MqttsMessage {

	//Mqtts WILLTOPIC fields
	private int qos;
	private boolean retain = false;
	private String willTopic ="";
	
	/**
	 * MqttsWillTopic constructor.Sets the appropriate message type. 
	 */
	public MqttsWillTopic() {
		msgType = MqttsMessage.WILLTOPIC;
	}
	
	/** 
	 * MqttsWillTopic constructor.Sets the appropriate message type and constructs 
	 * a Mqtts WILLTOPIC message from a received byte array.
	 * @param data: The buffer that contains the WILLTOPIC message.
	 */
	public MqttsWillTopic(byte[] data) {
		msgType = MqttsMessage.WILLTOPIC;
		if (data.length > 3){ //non empty WILLTOPIC message
			qos = (data[2] & 0x60) >> 5;
			retain = ((data[2] & 0x10) >> 4 != 0);
			try {
				willTopic = new String(data, 3, data[0] - 3, Utils.STRING_ENCODING);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the WILLTOPIC message as it should appear on the wire.
	 * (Don't needed in the GW)
	 */
	public byte[] toBytes(){
		int length = 3 + willTopic.length();
		byte[] data = new byte[length];
		int flags = 0;
		if(qos == -1) {
			flags |= 0x60; 
		} else if(qos == 0) {
		
		} else if(qos == 1) {
			flags |= 0x20;
		} else if(qos == 2) {
			flags |= 0x40;
		} else {
			throw new IllegalArgumentException("Unknown QoS value: " + qos);
		}
		if(retain) {
			flags |= 0x10;
		}

		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)flags;
		System.arraycopy(willTopic.getBytes(), 0, data, 3, willTopic.length());	
		return data;
	}

	public int getQos() {
		return qos;
	}
	public void setQos(int qoS) {
		this.qos = qoS;
	}
	public boolean isRetain() {
		return retain;
	}
	public void setRetain(boolean retain) {
		this.retain = retain;
	}
	public String getWillTopic() {
		return willTopic;
	}
	public void setWillTopic(String willTopic) {
		this.willTopic = willTopic;
	}
}