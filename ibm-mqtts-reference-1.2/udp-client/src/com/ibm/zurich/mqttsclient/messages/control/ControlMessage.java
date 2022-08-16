/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
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
