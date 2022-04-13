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

package com.ibm.zurich.mqttsgw.utils;

import java.net.InetAddress;


/**
* This class represents the address of the gateway.It includes also the IP address of the
* gateway.
* 
* Parts of this code were imported from com.ibm.zurich.mqttz_gw.util.SAaddress.java
* 
*/
public class GatewayAddress extends Address{
	
	private byte[] gatewayAddress = null;
	private InetAddress ipAddress = null;
	private int port = 0;

	
	public GatewayAddress(byte[] addr) {
		this.gatewayAddress = addr;
		this.ipAddress = null;
		this.port = 0;
	}
	
	public GatewayAddress(byte[] addr, InetAddress ipAddr, int port) {
		this.gatewayAddress = addr;
		this.ipAddress = ipAddr;
		this.port   = port;		
	}
	
	public byte[] getAddress() {
		return this.gatewayAddress;
	}
	
	public InetAddress getIPaddress() {
		return this.ipAddress;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public void setIPaddress(Address addr) {
		GatewayAddress gatewayAddr = (GatewayAddress) addr;
		this.ipAddress = gatewayAddr.ipAddress;
		this.port   = gatewayAddr.port;
	}

	public boolean equal(Object o) {
		if(o == null) return false;
		if(!(o instanceof GatewayAddress)) return false;
		if(o == this) return true;
		
		GatewayAddress ga = (GatewayAddress)o;
		if(gatewayAddress == null) {
			if(ga.gatewayAddress == null) {
				return true;
			} else {
				return false;
			}
		} else {
			if(ga.gatewayAddress == null || gatewayAddress.length != ga.gatewayAddress.length) 
				return false;
			boolean ok = true;
			for(int i = 0; i < gatewayAddress.length; i++) {
				if(gatewayAddress[i] != ga.gatewayAddress[i]) {
					ok = false;
					break;
				}
			}
			if(!(ipAddress.equals(ga.getIPaddress()) && port == ga.getPort()))
				ok = false;
			return ok;
		}
	}
}