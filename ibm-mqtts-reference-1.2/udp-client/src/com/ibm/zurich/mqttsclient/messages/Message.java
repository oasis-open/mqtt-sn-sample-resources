/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/


package com.ibm.zurich.mqttsclient.messages;

import com.ibm.zurich.mqttsclient.messages.control.ControlMessage;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsMessage;


public class Message {	
	public static final int MQTTS_MSG = 1;
	public static final int CONTROL_MSG = 2;
	
	
	private int type;
	
	private MqttsMessage mqttsMessage = null;
	private ControlMessage controlMessage = null;
	

	public Message() {}


	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public MqttsMessage getMqttsMessage() {
		return mqttsMessage;
	}

	public void setMqttsMessage(MqttsMessage mqttsMessage) {
		this.mqttsMessage = mqttsMessage;
	}

	
	public ControlMessage getControlMessage() {
		return controlMessage;
	}

	public void setControlMessage(ControlMessage controlMessage) {
		this.controlMessage = controlMessage;
	}

}