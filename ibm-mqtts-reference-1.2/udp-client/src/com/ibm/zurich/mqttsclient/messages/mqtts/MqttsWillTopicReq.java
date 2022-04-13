/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corp.
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

package com.ibm.zurich.mqttsclient.messages.mqtts;

/**
 * This object represents a Mqtts WILLTOPICREQ message.
 * 
 */
public class MqttsWillTopicReq extends MqttsMessage{
	
	/**
	 * MqttsWillTopicReq constructor.Sets the appropriate message type. 
	 */
	public MqttsWillTopicReq() {
		msgType = MqttsMessage.WILLTOPICREQ;
	}
	
	/** 
	 * MqttsWillTopicReq constructor.Sets the appropriate message type and constructs 
	 * a Mqtts WILLTOPICREQ message from a received byte array.
	 * @param data: The buffer that contains the WILLTOPICREQ message.
	 */
	public MqttsWillTopicReq(byte[] data){
		msgType = MqttsMessage.WILLTOPICREQ;
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the WILLTOPICREQ message as it should appear on the wire.
	 */
	public byte [] toBytes() {
		int length = 2;
		byte[] data = new byte[length];
		data[0] = (byte)length;
		data[1] = (byte)msgType;		
		return data;
	}
}