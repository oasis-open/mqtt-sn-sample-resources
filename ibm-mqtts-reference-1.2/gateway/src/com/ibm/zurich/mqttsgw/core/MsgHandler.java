/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.core;

import com.ibm.zurich.mqttsgw.messages.control.ControlMessage;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttMessage;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage;

public abstract class MsgHandler {
		
	/**
	 * 
	 */
	public abstract void initialize();
	
	
	/**
	 * @param msg
	 */
	public abstract void handleMqttsMessage(MqttsMessage msg);
	
	
	/**
	 * @param msg
	 */
	public abstract void handleMqttMessage(MqttMessage msg);
	
	
	/**
	 * @param msg
	 */
	public abstract void handleControlMessage(ControlMessage msg);
}
