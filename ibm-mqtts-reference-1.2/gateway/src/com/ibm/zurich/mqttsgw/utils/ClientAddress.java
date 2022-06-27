/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.utils;

import java.net.InetAddress;


/**
* This class represents the address of a client.It includes also the IP address and the 
* port of the forwarder with which this client is connected.
* 
* Parts of this code were imported from com.ibm.zurich.mqttz_gw.util.SAaddress.java
* 
*/
public class ClientAddress extends Address{
	
	private byte[] clientAddress = null;
	private InetAddress ipAddress = null;
	private int port = 0;
	private boolean isEncaps;  //whether fw-encapsulation is used by this client or not
	private byte[] encaps;

	
	public ClientAddress(byte[] addr) {
		this.clientAddress = addr;
		this.ipAddress = null;
		this.port = 0;
		this.isEncaps = true;
	}
	
	public ClientAddress(byte[] addr, InetAddress ipAddr, int port, boolean isencaps, byte[] encaps) {
		this.clientAddress = addr;
		this.ipAddress = ipAddr;
		this.port   = port;
		this.isEncaps = isencaps;
		this.encaps = encaps;
	}
	
	public boolean isEncaps() {
		return isEncaps;
	}
	public byte[] getEncaps() {
		return encaps;
	}
	
	public byte[] getAddress() {
		return this.clientAddress;
	}
	
	public InetAddress getIPaddress() {
		return this.ipAddress;
	}
	
	public int getPort() {
		return this.port;
	}
		
	public void setIPaddress(Address addr) {
		ClientAddress clientAddr = (ClientAddress) addr;
		this.ipAddress = clientAddr.ipAddress;
		this.port   = clientAddr.port;
	}

	public boolean equal(Object o) {
		if(o == null) return false;
		if(!(o instanceof ClientAddress)) return false;
		if(o == this) return true;
		
		ClientAddress ca = (ClientAddress)o;
		if(clientAddress == null) {
			if(ca.clientAddress == null) {
				return true;
			} else {
				return false;
			}
		} else {
			if(ca.clientAddress == null || clientAddress.length != ca.clientAddress.length) 
				return false;
			boolean ok = true;
			for(int i = 0; i < clientAddress.length; i++) {
				if(clientAddress[i] != ca.clientAddress[i]) {
					ok = false;
					break;
				}
			}
			return ok;
		}
	}
}