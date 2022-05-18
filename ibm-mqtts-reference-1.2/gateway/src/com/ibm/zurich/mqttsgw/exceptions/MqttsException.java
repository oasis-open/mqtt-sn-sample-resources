/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.exceptions;

public class MqttsException extends Exception{
	private static final long serialVersionUID = 1L;

	private Throwable cause = null; 

	public MqttsException() {
		super();
	}

	public MqttsException(String s) {
		super(s);
	}

	public MqttsException(Throwable cause) {
		super( (cause==null)? null : cause.toString() );
		this.cause = cause;
	}

	public Throwable getCause(){
		return this.cause;
	}
}
