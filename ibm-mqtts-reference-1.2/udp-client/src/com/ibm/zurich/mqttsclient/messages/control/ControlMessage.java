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


package com.ibm.zurich.mqttsclient.messages.control;

public class ControlMessage {

	//Control message types	
	public static final int ACK     		= 1;
	public static final int KEEP_ALIVE 		= 2;
	public static final int WAIT_SEARCHGW 	= 3;
	public static final int WAIT_GWINFO	    = 4;
	
	public ControlMessage(){}
	
	private int msgType;

	public int getMsgType() {
		return msgType;
	}

	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}	
}
