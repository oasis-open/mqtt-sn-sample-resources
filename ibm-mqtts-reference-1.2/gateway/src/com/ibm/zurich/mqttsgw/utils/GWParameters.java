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
import java.util.Hashtable;
import java.util.Vector;

import com.ibm.zurich.mqttsgw.client.ClientInterface;

public class GWParameters {
	
	//the ID of the gateway (from 0 up to 255)
	private static int gwId;
	
	//the address of the gateway
	private static GatewayAddress gatewayAddress; 
	
	//the period (in seconds) of broadcasting the Mqtts ADVERTISE message to the network
	public static long advPeriod; 

	//the period (in seconds) of sending the Mqtt PINGRESP message to the broker
	public static int keepAlivePeriod; 

	//maximum retries of sending a message to the client
	public static int maxRetries;
	
	//maximum time (in seconds) waiting for a message from the client
	public static int waitingTime;
	
	//the maximum number of predefined topic ids
	private static int predfTopicIdSize;
	
	//the maximum length of the Mqtts message
	private static int maxMqttsLength;

	//the minimum length of the Mqtts message
	private static int minMqttsLength;
	
	//the IP address of the gateway
	private static InetAddress ipAddress;
	
	//the URL of the gateway
	private static String gatewayURL;
	
	//the UDP port that will be used for the UDP socket of the ClientIPInterface
	private static int udpPort;
	
	//the URL of the broker
	private static String brokerURL;
	
	//the TCP port where broker listens
	private static int brokerTcpPort;
	
	//serial port parameters
	private static String serialPortURL;
	
	//the time (in seconds) that a ClientMsgHandler can remain inactive
	private static long handlerTimeout;
	
	//the time (in seconds) that a Forwarder can remain inactive (we don't have any message)
	private static long forwarderTimeout;
	
	//the period (in seconds) that a control message is sent to all ClientMsgHandlers for removing 
	//themselves from Dispatcher's mapping table if they are inactive for at least handlerTimeout seconds
	private static long ckeckingPeriod;
	
	//a String for storing the names of all available client interfaces
	private static String clientIntString;
	
	//a vector for storing all available client interfaces
	private static Vector<ClientInterface> clientInterfacesVector;
	
	//a hashtable for storing predefined topic ids
	private static Hashtable<?, ?> predefTopicIdTable;

	
	//other parameters of the Mqtt CONNECT message that GatewayMsgHandler sends to the broker
	private static String protocolName;
	private static int protocolVersion;
	private static boolean retain;
	private static int willQoS;
	private static boolean willFlag;	
	private static boolean cleanSession;
	private static String willTopic; 
	private static String willMessage;
		
	/**
	 * 
	 */	
	
	public static int getGwId() {
		return gwId;
	}

	public static void setGwId(int gwId) {
		GWParameters.gwId = gwId;
	}

	public static long getAdvPeriod() {
		return advPeriod;
	}

	public static void setAdvPeriod(long advPeriod) {
		GWParameters.advPeriod = advPeriod;
	}

	public static int getMaxRetries() {
		return maxRetries;
	}

	public static void setMaxRetries(int maxRetries) {
		GWParameters.maxRetries = maxRetries;
	}

	public static int getWaitingTime() {
		return waitingTime;
	}

	public static void setWaitingTime(int waitingTime) {
		GWParameters.waitingTime = waitingTime;
	}

	public static int getPredfTopicIdSize() {
		return predfTopicIdSize;
	}

	public static void setPredfTopicIdSize(int predfTopicIdSize) {
		GWParameters.predfTopicIdSize = predfTopicIdSize;
	}

	public static int getUdpPort() {
		return udpPort;
	}

	public static void setUdpPort(int udpPort) {
		GWParameters.udpPort = udpPort;
	}

	public static String getBrokerURL() {
		return brokerURL;
	}

	public static void setBrokerURL(String brokerURL) {
		GWParameters.brokerURL = brokerURL;
	}

	public static int getBrokerTcpPort() {
		return brokerTcpPort;
	}

	public static void setBrokerTcpPort(int brokerTcpPort) {
		GWParameters.brokerTcpPort = brokerTcpPort;
	}

	public static long getHandlerTimeout() {
		return handlerTimeout;
	}

	public static void setHandlerTimeout(long handlerTimeout) {
		GWParameters.handlerTimeout = handlerTimeout;
	}

	public static int getKeepAlivePeriod() {
		return keepAlivePeriod;
	}

	public static void setKeepAlivePeriod(int keepAlivePeriod) {
		GWParameters.keepAlivePeriod = keepAlivePeriod;
	}

	public static long getCkeckingPeriod() {
		return ckeckingPeriod;
	}

	public static void setCkeckingPeriod(long ckeckingPeriod) {
		GWParameters.ckeckingPeriod = ckeckingPeriod;
	}

	public static GatewayAddress getGatewayAddress() {
		return gatewayAddress;
	}

	public static void setGatewayAddress(GatewayAddress gatewayAddress) {
		GWParameters.gatewayAddress = gatewayAddress;
	}

	public static InetAddress getIpAddress() {
		return ipAddress;
	}

	public static void setIpAddress(InetAddress ipAddr) {
		ipAddress = ipAddr;
	}

	public static String getGatewayURL() {
		return gatewayURL;
	}

	public static void setGatewayURL(String gatewayURL) {
		GWParameters.gatewayURL = gatewayURL;
	}
	
	public static String getClientIntString() {
		return clientIntString;
	}

	public static void setClientIntString(String clientIntString) {
		GWParameters.clientIntString = clientIntString;
	}
	
	public static Vector<ClientInterface> getClientInterfaces() {
		return clientInterfacesVector;
	}

	public static void setClientInterfacesVector(Vector<ClientInterface> clientInterfacesVector) {
		GWParameters.clientInterfacesVector = clientInterfacesVector;
	}

	public static String getProtocolName() {
		return protocolName;
	}

	public static void setProtocolName(String protocolName) {
		GWParameters.protocolName = protocolName;
	}

	public static int getProtocolVersion() {
		return protocolVersion;
	}

	public static void setProtocolVersion(int protocolVersion) {
		GWParameters.protocolVersion = protocolVersion;
	}

	public static boolean isRetain() {
		return retain;
	}

	public static void setRetain(boolean retain) {
		GWParameters.retain = retain;
	}

	public static int getWillQoS() {
		return willQoS;
	}

	public static void setWillQoS(int willQoS) {
		GWParameters.willQoS = willQoS;
	}

	public static boolean isWillFlag() {
		return willFlag;
	}

	public static void setWillFlag(boolean willFlag) {
		GWParameters.willFlag = willFlag;
	}

	public static boolean isCleanSession() {
		return cleanSession;
	}

	public static void setCleanSession(boolean cleanSession) {
		GWParameters.cleanSession = cleanSession;
	}

	public static String getWillTopic() {
		return willTopic;
	}

	public static void setWillTopic(String willTopic) {
		GWParameters.willTopic = willTopic;
	}

	public static String getWillMessage() {
		return willMessage;
	}

	public static void setWillMessage(String willMessage) {
		GWParameters.willMessage = willMessage;
	}

	public static long getForwarderTimeout() {
		return forwarderTimeout;
	}

	public static void setForwarderTimeout(long forwarderTimeout) {
		GWParameters.forwarderTimeout = forwarderTimeout;
	}

	public static int getMaxMqttsLength() {
		return maxMqttsLength;
	}

	public static void setMaxMqttsLength(int maxMqttsLength) {
		GWParameters.maxMqttsLength = maxMqttsLength;
	}

	public static int getMinMqttsLength() {
		return minMqttsLength;
	}

	public static void setMinMqttsLength(int minMqttsLength) {
		GWParameters.minMqttsLength = minMqttsLength;
	}

	public static Hashtable<?, ?> getPredefTopicIdTable() {
		return predefTopicIdTable;
	}

	public static void setPredefTopicIdTable(Hashtable<?, ?> predefTopicIdTable) {
		GWParameters.predefTopicIdTable = predefTopicIdTable;
	}

	public static String getSerialPortURL() {
		return serialPortURL;
	}

	public static void setSerialPortURL(String serialPortURL) {
		GWParameters.serialPortURL = serialPortURL;
	}
}