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

package com.ibm.zurich.mqttsgw.messages.control;

public class ControlMessage {

	//Control message types	
	public static final int CONNECTION_LOST     		= 1;
	public static final int WAITING_WILLTOPIC_TIMEOUT 	= 2;
	public static final int WAITING_WILLMSG_TIMEOUT 	= 3;
	public static final int WAITING_REGACK_TIMEOUT 	    = 4;
	public static final int CHECK_INACTIVITY			= 5;
	public static final int SEND_KEEP_ALIVE_MSG			= 6;
	public static final int SHUT_DOWN					= 7;

	public ControlMessage(){}

	private int msgType;

	public int getMsgType() {
		return msgType;
	}

	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}


}
