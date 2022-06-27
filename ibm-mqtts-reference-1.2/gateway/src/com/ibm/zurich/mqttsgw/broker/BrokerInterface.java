/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.broker;

import com.ibm.zurich.mqttsgw.exceptions.MqttsException;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttMessage;

/**
 * 
 * This class represents the interface over which the gateway send 
 * Mqtt messages to the the broker and vice versa.
 * 
 */
public interface BrokerInterface {


	/**
	 * The method that initializes the broker interface.
	 */
	public void initialize() throws MqttsException;


	/**
	 * The method that reads a Mqtt Message from the broker.
	 */
	public void readMsg();


	/**
	 * The method that sends a Mqtt message to the broker.
	 * @param msg The Mqtt message to be sent.
	 */
	public void sendMsg(MqttMessage msg)throws MqttsException;


	/**
	 * The method that disconnects from the broker.
	 */
	public void disconnect();
}