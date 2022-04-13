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

package com.ibm.zurich.mqttsgw.client;

import com.ibm.zurich.mqttsgw.exceptions.MqttsException;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage;
import com.ibm.zurich.mqttsgw.utils.ClientAddress;


/**
 * 
 * This class represents the interface over which the clients send 
 * Mqtts messages to the gateway and vice versa.
 * 
 */
public interface ClientInterface {	
		
	/**
	 * The method that initializes the client interface.
	 */
	public void initialize() throws MqttsException;
	
	
	/**
	 * The method that sends a Mqtts message to the specified SA client (unicast).
	 * @param address The unique address of the SA client.
	 * @param msg The Mqtts message to be sent.
	 */
	public void sendMsg(ClientAddress address, MqttsMessage msg);
	
	
	/**
	 * The method that broadcasts a Mqtts Message to the network.
	 * @param msg The Mqtt message to be broadcasted.
	 */
	public void broadcastMsg(MqttsMessage msg);
	
	
	/**
	 * The method that broadcasts a Mqtts message to the network within a specific radius.
	 * @param radius The broadcast radius of the Mqtt message.
	 * @param msg The Mqtt message to be broadcasted.
	 */
	public void broadcastMsg(int radius, MqttsMessage msg);
}