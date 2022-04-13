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
 * This object represents a Mqtts PUBREC message.
 * 
 *
 */
public class MqttsPubRec extends MqttsMessage {
	
	
	//Mqtts PUBREC fields
	private int msgId;

	/**
	 * MqttsPubrec constructor.Sets the appropriate message type. 
	 */
	public MqttsPubRec() {
		msgType = MqttsMessage.PUBREC;
	}
	
	/**
	 * MqttsPubrec constructor.Sets the appropriate message type and constructs 
	 * a Mqtts PUBREC message from a received byte array.
	 * @param data: The buffer that contains the PUBREC message.
	 */
	public MqttsPubRec(byte[] data) {
		msgType = MqttsMessage.PUBREC;
		msgId = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the PUBREC message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 4;
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)((msgId >> 8) & 0xFF);
		data[3] = (byte)(msgId & 0xFF);	
		return data;		
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}
}