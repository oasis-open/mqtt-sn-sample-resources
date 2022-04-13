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