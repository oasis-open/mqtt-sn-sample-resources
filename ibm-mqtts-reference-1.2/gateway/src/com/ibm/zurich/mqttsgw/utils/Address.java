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

public abstract class Address {
	
	public abstract boolean equal(Object o);

	public abstract void setIPaddress(Address addr);
	
	public abstract byte[] getAddress() ;
	
	public abstract InetAddress getIPaddress() ;
	
	public abstract int getPort();	
}
