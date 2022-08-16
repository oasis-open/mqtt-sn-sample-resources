/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.core;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

//import com.ibm.zurich.mqttsgw.Gateway;
import com.ibm.zurich.mqttsgw.messages.Message;
import com.ibm.zurich.mqttsgw.messages.control.ControlMessage;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttMessage;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage;
import com.ibm.zurich.mqttsgw.utils.Address;
import com.ibm.zurich.mqttsgw.utils.ClientAddress;
//import com.ibm.zurich.mqttsgw.utils.GWParameters;
import com.ibm.zurich.mqttsgw.utils.GatewayLogger;
import com.ibm.zurich.mqttsgw.utils.MsgQueue;

/**
 * This object dispatches messages to the appropriate MsgHandler according to the
 * client address they carry.
 * 
 *
 */
public class Dispatcher implements Runnable{

	private static Dispatcher instance = null;

	private MsgQueue dataQueue; 
	private Hashtable<Address, MsgHandler> handlerTable;
	private volatile boolean running;
	private Thread readingThread;


	/**
	 * Initialization method.
	 */
	public void initialize(){
		dataQueue = new MsgQueue();
		handlerTable = new Hashtable<Address, MsgHandler>();				
		this.running = true;
		this.readingThread = new Thread(this,"Dispatcher");
		this.readingThread.start();
	}


	/**
	 * This method returns the instance of this object.If there no such an instance
	 * a new object is created.
	 * 
	 * @return The instance of this object.
	 */
	public static synchronized Dispatcher getInstance() {
		if (instance == null) {
			instance = new Dispatcher();
		}
		return instance;
	}


	/**
	 * The method that reads a message {@link com.ibm.zurich.mqttsgw.messages.Message} from 
	 * the queue and dispatches it according to its type (Mqtts, Mqtt or Control message).
	 */
	private void dispatch() {
		//read the next available Message from queue

		Message msg = null;
		try {
			msg = (Message)dataQueue.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//get the type of the message that "internal" message carries
		int type = msg.getType();
		switch(type){		
		case Message.MQTTS_MSG:
			dispatchMqtts(msg);
			break;

		case Message.MQTT_MSG:
			dispatchMqtt(msg);
			break;

		case Message.CONTROL_MSG:
			dispatchControl(msg);
			break;

		default:
			GatewayLogger.warn("Dispatcher - Message of unknown type \"" + msg.getType()+"\" received.");
			break;
		}
	}


	/**
	 * The method that handles a Mqtts message.According to its address is dispatched
	 * to the appropriate MsgHandler.
	 * 
	 * @param msg
	 */
	private void dispatchMqtts(Message msg) {
		//		GatewayLogger.log(GatewayLogger.INFO, "Dispatcher - New Mqtts message arrived at queue.");

		//get the address of the client from this message
		Address address = msg.getAddress(); 

		//get the Mqtts message itself
		MqttsMessage mqttsMsg = msg.getMqttsMessage();

		if(mqttsMsg == null){
			GatewayLogger.log(GatewayLogger.WARN, 
					"Dispatcher - The received Mqtts message is null. The message cannot be processed.");
			return;
		}

		//get the appropriate handler (GatewayMsgHandler or ClientMsgHandler)for this message 
		//according to the unique address of the client (or the gateway)
		MsgHandler handler = getHandler(address);

		if (handler == null){
			//there is no such a handler, create a new one (applies only for the case of ClientMsgHandler
			//because GatewayMsgHandler was inserted in the hashtable at the startup of the gateway)
			ClientAddress clientAddress = (ClientAddress) address;	
			handler = new ClientMsgHandler(clientAddress);
			putHandler(clientAddress,handler);
			handler.initialize();
		}

		//update the client interface of the MsgHandler (if the handler is a ClientMsgHandler)
		if(handler instanceof ClientMsgHandler && msg.getClientInterface() != null){
			ClientMsgHandler clientHandler = (ClientMsgHandler) handler;
			clientHandler.setClientInterface(msg.getClientInterface());
		}

		//pass the message to the handler
		handler.handleMqttsMessage(mqttsMsg);		
	}


	/**
	 * The method that handles a Mqtt message.According to its address is dispatched
	 * to the appropriate MsgHandler.
	 * 
	 * @param msg
	 */
	private void dispatchMqtt(Message msg) {
		//		GatewayLogger.log(GatewayLogger.INFO, "Dispatcher - New Mqtt message arrived at queue.");

		//get the address of the client from this message
		Address address = msg.getAddress(); 

		//get the Mqtt message itself
		MqttMessage mqttMsg = msg.getMqttMessage();

		if(mqttMsg == null){
			GatewayLogger.log(GatewayLogger.WARN, 
					"Dispatcher - The received Mqtt message is null. The message cannot be processed.");
			return;
		}

		//get the appropriate handler (GatewayMsgHandler or ClientMsgHandler)for this message 
		//according to the unique address of the client (or the gateway)
		MsgHandler handler = getHandler(address);

		if (handler == null){
			//there is no such a handler, create a new one (applies only for the case of ClientMsgHandler
			//because GatewayMsgHandler was inserted in the hashtable when Dispatcher was created)
			ClientAddress clientAddress = (ClientAddress) address;	
			handler = new ClientMsgHandler(clientAddress);
			putHandler(clientAddress,handler);
			handler.initialize();
		}	

		//pass the message to the handler
		handler.handleMqttMessage(mqttMsg);		
	}


	/**
	 * The method that handles a Control message.According to its address is dispatched
	 * to the appropriate MsgHandler.
	 * 
	 * @param msg
	 */
	private void dispatchControl(Message msg) {
		//		GatewayLogger.log(GatewayLogger.INFO, "Dispatcher - New Control message arrived at queue.");

		//get the address of the client from this message
		Address address = msg.getAddress(); 


		//get the Control message itself
		ControlMessage controlMsg = msg.getControlMessage();

		if(controlMsg == null){
			GatewayLogger.log(GatewayLogger.WARN, 
					"Dispatcher - The received Control message is null. The message cannot be processed.");
			return;
		}

		if(address == null){
			//this message applies to all message handlers
			//			GatewayLogger.log(GatewayLogger.INFO, "Dispatcher - The received Control message is addressed to all handlers.");
			deliverMessageToAll(controlMsg);
			return;
		}

		//get the appropriate handler (GatewayMsgHandler or ClientMsgHandler)for this message 
		//according to the unique address of the client (or the gateway)
		MsgHandler handler = getHandler(address);

		if (handler == null){
			//there is no such a handler, create a new one (applies only for the case of ClientMsgHandler
			//because GatewayMsgHandler was inserted in the hashtable when Dispatcher was created)
			ClientAddress clientAddress = (ClientAddress) address;	
			handler = new ClientMsgHandler(clientAddress);
			putHandler(clientAddress,handler);
			handler.initialize();
		}

		//pass the message to the handler
		handler.handleControlMessage(controlMsg);			
	}


	/**
	 * This method delivers a message to all MsgHandlers.
	 */
	private void deliverMessageToAll(ControlMessage msg) {
		Enumeration<MsgHandler> values;
		MsgHandler handler;

		if(msg.getMsgType() == ControlMessage.SHUT_DOWN){			
			GatewayLogger.info("-------- Mqtts Gateway shutting down --------");
		}
		for(values = handlerTable.elements(); values.hasMoreElements();){
			handler = (MsgHandler)values.nextElement();
			handler.handleControlMessage(msg);
		}

		if(msg.getMsgType() == ControlMessage.SHUT_DOWN){
			GatewayLogger.info("-------- Mqtts Gateway stopped --------");
			System.exit(0);
		}
	}


	/**
	 * The method that puts a new created MsgHandler to the mapping table.
	 * 
	 * @param addr The address of the handler
	 * @param handler The new created handler object
	 */
	public void putHandler(Address addr, MsgHandler handler) {
		this.handlerTable.put(addr, handler);		
	}


	/**
	 * 
	 * The method that gets a MsgHandler from the mapping table according to its address.
	 * 
	 * @param addr The address of the handler
	 * @return The handler object
	 */
	private MsgHandler getHandler(Address addr) {
		MsgHandler ret = null;
		Iterator<Address> iter = handlerTable.keySet().iterator();
		while (iter.hasNext()) {
			Address currentAddress = (Address)(iter.next());
			if(currentAddress.equal(addr) && addr.equal(currentAddress)) {
				currentAddress.setIPaddress(addr);
				ret = (MsgHandler)handlerTable.get(currentAddress);
				break;
			}
		}
		return ret;
	}	


	/**
	 * The method that removes an MsgHandler from the mapping table.
	 * 
	 * @param addr The address of the handler
	 */
	public void removeHandler(Address address) {
		Iterator<Address> iter  =  handlerTable.keySet().iterator();
		while (iter.hasNext()) {	
			Address currentAddress = (Address)(iter.next());
			if(currentAddress.equal(address)){
				iter.remove();
				break;
			}
		}		
	}	


	/**
	 * The method that puts a message {@link com.ibm.zurich.mqttsgw.messages.Message}
	 * to the queue.
	 * 
	 * @param msg
	 */
	public void putMessage(Message msg) {
		if(msg.getType() == Message.CONTROL_MSG)
			dataQueue.addFirst(msg);
		else
			dataQueue.addLast(msg);
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(running){
			dispatch();
		}		
	}
}
