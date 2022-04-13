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

package com.ibm.zurich.mqttsclient.utils;

import java.net.InetAddress;


public class ClientParameters {

	//the address of the gateway
	private  InetAddress gatewayAddress;
	
	//the UDP port of the gateway
	private int gatewayPort;
	
	//the broadcast radius of the SEARCHGW message
	//private int searchGWBroadcastRadius = 1;
	
	//the maximum length of the Mqtts message
	private int maxMqttsLength;

	//the minimum length of the Mqtts message
	private int minMqttsLength;
	
	//the keep alive period (in seconds) 
	private int keepAlivePeriod; 

	//maximum retries of sending a message 
	private int maxRetries;
	
	//maximum time (in seconds) waiting for a message 
	private int waitingTime;

		
	
	
	

	public InetAddress getGatewayAddress() {
		return gatewayAddress;
	}

	public void setGatewayAddress(InetAddress gatewayAddress) {
		this.gatewayAddress = gatewayAddress;
	}

	public int getGatewayPort() {
		return gatewayPort;
	}

	public void setGatewayPort(int gatewayPort) {
		this.gatewayPort = gatewayPort;
	}

//	public int getSearchGWBroadcastRadius() {
//		return searchGWBroadcastRadius;
//	}
//
//	public void setSearchGWBroadcastRadius(int searchGWBroadcastRadius) {
//		this.searchGWBroadcastRadius = searchGWBroadcastRadius;
//	}

	public int getMaxMqttsLength() {
		return maxMqttsLength;
	}

	public void setMaxMqttsLength(int maxMqttsLength) {
		this.maxMqttsLength = maxMqttsLength;
	}

	public int getMinMqttsLength() {
		return minMqttsLength;
	}

	public void setMinMqttsLength(int minMqttsLength) {
		this.minMqttsLength = minMqttsLength;
	}

	public int getKeepAlivePeriod() {
		return keepAlivePeriod;
	}

	public void setKeepAlivePeriod(int keepAlivePeriod) {
		this.keepAlivePeriod = keepAlivePeriod;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getWaitingTime() {
		return waitingTime;
	}

	public void setWaitingTime(int waitingTime) {
		this.waitingTime = waitingTime;
	}
}