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

public abstract class Address {
	
	public abstract boolean equal(Object o);

	public abstract void setIPaddress(Address addr);
	
	public abstract byte[] getAddress() ;
	
	public abstract InetAddress getIPaddress() ;
	
	public abstract int getPort();	
}
