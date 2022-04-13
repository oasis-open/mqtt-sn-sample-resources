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

import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage;

/**
 * This object represents a Mqtts CONNACK message.
 * 
 *
 */
public class MqttsConnack extends MqttsMessage {

	//Mqtts CONNACK fields
	private int returnCode;
	
	/**
	 * MqttsConack constructor.Sets the appropriate message type. 
	 */
	public MqttsConnack() {
		msgType = MqttsMessage.CONNACK;
	}
	
	/**
	 * MqttsConack constructor.Sets the appropriate message type and constructs
	 * a Mqtts CONNACK message from a received byte array.
	 * @param data: The buffer that contains the CONNACK message.
	 * (Don't needed in the GW)
	 */
	public MqttsConnack(byte[] data){
		msgType = MqttsMessage.CONNACK;
		returnCode = (data[2] & 0xFF);
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the CONNACK message as it should appear on the wire.
	 */
	public byte[] toBytes() {
		int length = 3;
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)returnCode;  	
		return data;
	}
		
	public int getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
}