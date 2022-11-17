/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.zurich.mqttsgw.core.Dispatcher;
import com.ibm.zurich.mqttsgw.core.GatewayMsgHandler;
import com.ibm.zurich.mqttsgw.exceptions.MqttsException;
import com.ibm.zurich.mqttsgw.messages.Message;
import com.ibm.zurich.mqttsgw.messages.control.ControlMessage;
import com.ibm.zurich.mqttsgw.timer.TimerService;
import com.ibm.zurich.mqttsgw.utils.ConfigurationParser;
import com.ibm.zurich.mqttsgw.utils.GWParameters;
import com.ibm.zurich.mqttsgw.utils.GatewayAddress;
import com.ibm.zurich.mqttsgw.utils.GatewayLogger;

/**
 * This is the entry point of the MQTT-SN Gateway.
 * 
 */
public class Gateway {
	private static Dispatcher dispatcher;
	private static ShutDownHook shutdHook;



	public void start(String fileName){
		DateFormat dFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");

		System.out.println();
		System.out.println(dFormat.format(new Date())+ 
				"  INFO:  -------- MQTT-SN Gateway starting --------");

		//load the gateway parameters from a file		
		System.out.println(dFormat.format(new Date())+ 
				"  INFO:  Loading MQTT-SN Gateway parameters from " + fileName + " ... ");
		try {
			ConfigurationParser.parseFile(fileName);
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.error("Failed to load Gateway parameters. Gateway cannot start.");
			System.exit(1);
		}
		GatewayLogger.info("Gateway paremeters loaded.");

		//instantiate the timer service
		TimerService.getInstance();

		//instantiate the dispatcher
		dispatcher = Dispatcher.getInstance();

		//initialize the dispatcher
		dispatcher.initialize();		

		//create the address of the gateway itself(see com.ibm.zurich.mqttsgw.utils.GatewayAdress)
		int len = 1;
		byte[] addr = new byte[len];
		addr[0] = (byte)GWParameters.getGwId();

		InetAddress ip = null;

		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {			
			e.printStackTrace();
			GatewayLogger.error("Failed to create the address of the Gateway.Gateway cannot start.");
			System.exit(1);
		}

		int port = GWParameters.getUdpPort();

		GatewayAddress gatewayAddress = new GatewayAddress(addr,ip,port);
		GWParameters.setGatewayAddress(gatewayAddress);		


		//create a new GatewayMsgHandler (for the connection of the gateway itself)		
		GatewayMsgHandler gatewayHandler = new GatewayMsgHandler(GWParameters.getGatewayAddress());

		//insert this handler to the Dispatcher's mapping table
		dispatcher.putHandler(GWParameters.getGatewayAddress(), gatewayHandler);

		//initialize the GatewayMsgHandler
		gatewayHandler.initialize();

		//connect to the broker
		gatewayHandler.connect();

		//add a "listener" for catching shutdown events (Ctrl+C,etc.)
		shutdHook = new ShutDownHook(); 
		Runtime.getRuntime().addShutdownHook(shutdHook);		
	}

	/**
	 * 
	 */
	public static void shutDown(){
		//generate a control message 
		ControlMessage controlMsg = new ControlMessage();
		controlMsg.setMsgType(ControlMessage.SHUT_DOWN);

		//construct an "internal" message and put it to dispatcher's queue
		//@see com.ibm.zurich.mqttsgw.core.Message
		Message msg = new Message(null);
		msg.setType(Message.CONTROL_MSG);
		msg.setControlMessage(controlMsg);
		dispatcher.putMessage(msg);
	}


	/**
	 *
	 */
	private class ShutDownHook extends Thread{
		public void run(){
			shutDown();	
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = "gateway.properties";
		if (args.length > 0) fileName = args[0];
		Gateway gateway = new Gateway();
		gateway.start(fileName);		
	}

	public static void removeShutDownHook() {
		Runtime.getRuntime().removeShutdownHook(shutdHook);		
	}	
}