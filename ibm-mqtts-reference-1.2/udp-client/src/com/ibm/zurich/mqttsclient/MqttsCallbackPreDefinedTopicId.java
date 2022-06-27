/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/


package com.ibm.zurich.mqttsclient;

public interface MqttsCallbackPreDefinedTopicId extends MqttsCallback {
	public int publishArrivedPreDefinedTopicId(boolean retain, int QoS, int topicId, byte[] thisPayload);
}
