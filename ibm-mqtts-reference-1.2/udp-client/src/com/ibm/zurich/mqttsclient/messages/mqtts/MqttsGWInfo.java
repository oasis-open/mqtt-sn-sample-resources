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
 * This object represents a Mqtts GWINFO message.
 * 
 *
 */
public class MqttsGWInfo extends MqttsMessage {
	
	//Mqtts GWINFO fields
	private int gwId;
	private byte[] gwAdd = null;

	/**
	 * MqttsGWInfo constructor.Sets the appropriate message type. 
	 */
	public MqttsGWInfo() {
		msgType = MqttsMessage.GWINFO;
	}
	
	/**
	 * MqttsGWInfo constructor.Sets the appropriate message type and constructs 
	 * a Mqtts GWINFO message from a received byte array.
	 * @param data: The buffer that contains the GWINFO message.
	 */
	public MqttsGWInfo(byte[] data) {
		msgType = MqttsMessage.GWINFO;
		gwId = (data[2] & 0xFF);
		if(data.length > 3)
			System.arraycopy(data, 3, gwAdd, 0, data.length - 3); 
	}


	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the GWINFO message as it should appear on the wire.
	 */	
	public byte[] toBytes() {
		int length = 3 + gwAdd.length;
		byte[] data = new byte[length];
		data[0] = (byte)length;
		data[1] = (byte)msgType;
		data[2] = (byte)gwId;
		System.arraycopy(gwAdd, 0, data, 3, gwAdd.length);
		return data;
	}
	
	public int getGwId() {
		return gwId;
	}

	public void setGwId(int gwId) {
		this.gwId = gwId;
	}

	public byte[] getGwAdd() {
		return gwAdd;
	}

	public void setGwAdd(byte[] gwAdd) {
		this.gwAdd = gwAdd;
	}
}