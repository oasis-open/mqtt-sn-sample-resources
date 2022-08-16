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
 * This object represents a Mqtts CONNECT message.
 * 
 *
 */
public class MqttsConnect extends MqttsMessage{
	
	//Mqtts CONNECT fields
	private boolean will;
	private boolean cleanSession;	
	private String protocolId;
	private int duration;	
	private String clientId;
	
	//Protocol name and protocol version are embedded in the "protocolId" variable.
	//Mqtts protocol does not use them separately.
	private String protocolName;
	private int protocolVersion;	
	
	/**
	 * MqttsConnect constructor.Sets the appropriate message type. 
	 */
	public MqttsConnect() {
		msgType = MqttsMessage.CONNECT;
	}
	
	/**
	 * MqttsConnect constructor.Sets the appropriate message type and constructs 
	 * a Mqtts CONNECT message from a received byte array.
	 * @param data: The buffer that contains the CONNECT message.
	 */
	public MqttsConnect(byte[] data) {
		msgType = MqttsMessage.CONNECT;		
		will = ((data[2] & 0x08) >> 3 != 0);
		cleanSession = ((data[2] & 0x04) >> 2 !=0);
		duration = ((data[4] & 0xFF) << 8) + (data[5] & 0xFF);
	
	//  TODO handle this fields
		protocolName = "MQIsdp";
		protocolVersion = 3;
	
		byte[] byteClientId = new byte[data[0] - 6];
		System.arraycopy(data, 6, byteClientId, 0, byteClientId.length);
		try {
			clientId = new String(byteClientId, Utils.STRING_ENCODING);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to convert this message to a byte array for transmission.
	 * @return A byte array containing the CONNECT message as it should appear on the wire.
	 * (Don't needed in the GW)
	 */	
	public byte[] toBytes(){
		int length = 6 + clientId.length();
		byte[] data = new byte[length];
		data[0] = (byte)length;   
		data[1] = (byte)msgType;  
		data[2] = (byte)0x00;  
		if(will)  data[2] |= 0x08;
		if(cleanSession) data[2] |= 0x04;
		data[3] = (byte)0x01;  // TODO handle this fields
		data[4] = (byte)((duration >> 8) & 0xFF);
		data[5] = (byte)(duration & 0xFF);
		System.arraycopy(clientId.getBytes(), 0, data, 6, clientId.length());		
		return data;
	}
	
	public boolean isWill() {
		return will;
	}

	public void setWill(boolean will) {
		this.will = will;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public String getProtocolId() {
		return protocolId;
	}

	public void setProtocolId(String protocolId) {
		this.protocolId = protocolId;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getProtocolName() {
		return protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.protocolName = protocolName;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
}