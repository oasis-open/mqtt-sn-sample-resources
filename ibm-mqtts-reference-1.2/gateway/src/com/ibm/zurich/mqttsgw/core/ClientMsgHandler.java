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

import com.ibm.zurich.mqttsgw.broker.tcp.TCPBrokerInterface;
import com.ibm.zurich.mqttsgw.client.ClientInterface;
import com.ibm.zurich.mqttsgw.exceptions.MqttsException;
import com.ibm.zurich.mqttsgw.messages.Message;
import com.ibm.zurich.mqttsgw.messages.control.ControlMessage;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttConnack;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttConnect;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttDisconnect;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttMessage;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttPingReq;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttPingResp;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttPubComp;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttPubRec;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttPubRel;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttPuback;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttPublish;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttSuback;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttSubscribe;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttUnsuback;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttUnsubscribe;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsConnack;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsConnect;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsDisconnect;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPingReq;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPingResp;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPubComp;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPubRec;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPubRel;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPuback;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPublish;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsRegack;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsRegister;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsSearchGW;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsSuback;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsSubscribe;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsUnsuback;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsUnsubscribe;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillMsg;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillMsgReq;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillMsgUpd;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillTopic;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillTopicReq;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillTopicUpd;
import com.ibm.zurich.mqttsgw.timer.TimerService;
import com.ibm.zurich.mqttsgw.utils.ClientAddress;
import com.ibm.zurich.mqttsgw.utils.GWParameters;
import com.ibm.zurich.mqttsgw.utils.GatewayAddress;
import com.ibm.zurich.mqttsgw.utils.GatewayLogger;
import com.ibm.zurich.mqttsgw.utils.Utils;

/**
 * This object implements the core functions of the protocol translation.
 * For each client there is one instance of this object.Every message (Mqtt, 
 * Mqtts or Control)
 * that corresponds to a certain client, is handled by this object.
 *
 */
public class ClientMsgHandler extends MsgHandler{

	//the unique address of the client that distinguishes this object
	private ClientAddress clientAddress = null;

	//the clientId of the client(the one that is sent in Mqtts CONNECT message)
	private String clientId = "...";

	//the ClientInterface (IP, Serial, etc.) in which this object should 
	//respond in case of sending a Mqtts message to the client 
	private ClientInterface clientInterface = null;

	//the BrokerInterface which represents an interface for communication with the broker
	private TCPBrokerInterface brokerInterface = null;

	//a timer service which is used for timeouts
	private TimerService timer = null;

	//a table that is used for mapping topic Ids with topic names
	private TopicMappingTable topicIdMappingTable = null;

	//a reference to Dispatcher object
	private Dispatcher dispatcher = null;

	//class that represents the state of the client at any given time
	private ClientState client = null;

	//class that represents the state of the gateway (actually this handler's state) at any given time
	private GatewayState gateway = null;

	//variable for checking the time of inactivity of this object 
	//in order to remove it from Dispatcher's mapping table
	private long timeout;

	//messages for storing the information while on a connection procedure  
	private MqttsConnect mqttsConnect = null;
	private MqttsWillTopic mqttsWillTopic = null;	

	//messages for storing information while on a subscribe/unsubscribe procedure 
	private MqttsSubscribe mqttsSubscribe = null;
	private MqttsUnsubscribe mqttsUnsubscribe = null;

	//message for storing information while on registration procedure initiated by the gateway
	private MqttsRegister mqttsRegister = null;

	//message for storing information (if necessary) when we receive a Mqtt PUBLISH message
	private MqttPublish mqttPublish = null;

	//message for storing information (if necessary) when we receive a Mqtts PUBLISH message
	private MqttsPublish mqttsPublish = null;


	//variables for handling Mqtts messages WILLTOPICUPD and WILLMSGUPD
	//private String willtopic = "";
	//private String willMessage = "";

	//variables for storing the msgId and topicId that are issued by the gateway
	private int msgId;
	private int topicId;


	/**
	 * Constructor of the ClientMsgHandler.
	 * 
	 * @param addr The address of the client.
	 */
	public ClientMsgHandler(ClientAddress addr){
		this.clientAddress = addr;
	}


	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.core.MsgHandler#initialize()
	 */
	public void initialize() {
		brokerInterface = new TCPBrokerInterface(this.clientAddress);
		brokerInterface.setClientId(clientId);
		timer = TimerService.getInstance();
		dispatcher = Dispatcher.getInstance();
		topicIdMappingTable = new TopicMappingTable();
		topicIdMappingTable.initialize();
		timeout = 0;
		client = new ClientState();
		gateway = new GatewayState();
		msgId = 1;
		topicId = GWParameters.getPredfTopicIdSize()+1;
	}



	/******************************************************************************************/
	/**                      HANDLING OF MQTTS MESSAGES FROM THE CLIENT                     **/
	/****************************************************************************************/

	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.core.MsgHandler#handleMqttsMessage(com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage)
	 */
	public void handleMqttsMessage(MqttsMessage receivedMsg){		
		//update this handler's timeout
		timeout = System.currentTimeMillis() + GWParameters.getHandlerTimeout()*1000;

		//get the type of the Mqtts message and handle the message according to that type	
		switch(receivedMsg.getMsgType()){
		case MqttsMessage.ADVERTISE:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.SEARCHGW:
			handleMqttsSearchGW((MqttsSearchGW) receivedMsg);
			break;

		case MqttsMessage.GWINFO:
			//we will never receive such a message from the client
			break;				

		case MqttsMessage.CONNECT:
			handleMqttsConnect((MqttsConnect) receivedMsg);
			break;

		case MqttsMessage.CONNACK:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLTOPICREQ:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLTOPIC:
			handleMqttsWillTopic((MqttsWillTopic)receivedMsg);
			break;

		case MqttsMessage.WILLMSGREQ:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLMSG:
			handleMqttsWillMsg((MqttsWillMsg) receivedMsg);
			break;

		case MqttsMessage.REGISTER:
			handleMqttsRegister((MqttsRegister)receivedMsg);
			break;

		case MqttsMessage.REGACK:
			handleMqttsRegack((MqttsRegack) receivedMsg);
			break;

		case MqttsMessage.PUBLISH:
			handleMqttsPublish((MqttsPublish) receivedMsg);
			break;

		case MqttsMessage.PUBACK:
			handleMqttsPuback((MqttsPuback) receivedMsg);
			break;

		case MqttsMessage.PUBCOMP:
			handleMqttsPubComp((MqttsPubComp) receivedMsg);
			break;

		case MqttsMessage.PUBREC:
			handleMqttsPubRec((MqttsPubRec) receivedMsg);
			break;

		case MqttsMessage.PUBREL:
			handleMqttsPubRel((MqttsPubRel) receivedMsg);
			break;

		case MqttsMessage.SUBSCRIBE:
			handleMqttsSubscribe((MqttsSubscribe) receivedMsg);
			break;

		case MqttsMessage.SUBACK:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.UNSUBSCRIBE:
			handleMqttsUnsubscribe((MqttsUnsubscribe) receivedMsg);
			break;

		case MqttsMessage.UNSUBACK:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.PINGREQ:
			handleMqttsPingReq((MqttsPingReq) receivedMsg);
			break;

		case MqttsMessage.PINGRESP:
			handleMqttsPingResp((MqttsPingResp) receivedMsg);
			break;			

		case MqttsMessage.DISCONNECT:
			handleMqttsDisconnect((MqttsDisconnect) receivedMsg);
			break;

		case MqttsMessage.WILLTOPICUPD:
			handleMqttsWillTopicUpd((MqttsWillTopicUpd) receivedMsg);
			break;

		case MqttsMessage.WILLTOPICRESP:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLMSGUPD:
			handleMqttsWillMsgUpd((MqttsWillMsgUpd) receivedMsg);
			break;

		case MqttsMessage.WILLMSGRESP:
			//we will never receive such a message from the client
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts message of unknown type \"" + receivedMsg.getMsgType()+"\" received.");
			break;
		}
	}	


	/**
	 * The method that handles a Mqtts SEARCHGW message.
	 *  
	 * @param receivedMsg The received MqttsSearchGW message.
	 */
	private void handleMqttsSearchGW(MqttsSearchGW receivedMsg) {		
		//		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts SEARCHGW message with \"Radius\" = \""+receivedMsg.getRadius()+"\" received. The message will be handled by GatewayMsgHandler.");

		//construct an "internal" message (see com.ibm.zurich.mqttsgw.messages.Message)
		//for the GatewayMsgHandler and put it to the dispatcher's queue 
		GatewayAddress gwAddress = GWParameters.getGatewayAddress();
		Message msg = new Message(gwAddress);

		msg.setType(Message.MQTTS_MSG);
		msg.setMqttsMessage(receivedMsg);
		dispatcher.putMessage(msg);
	}


	/**
	 * The method that handles a Mqtts CONNECT message.
	 * 
	 * @param receivedMsg The received MqttsConnect message.
	 */
	private void handleMqttsConnect(MqttsConnect receivedMsg) {		
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts CONNECT message with \"Will\" = \"" +receivedMsg.isWill()+"\" and \"CleanSession\" = \"" +receivedMsg.isCleanSession()+"\" received.");

		this.clientId = receivedMsg.getClientId();
		brokerInterface.setClientId(clientId);

		//if the client is already connected return a Mqtts CONNACK 
		if(client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is already connected. Mqtts CONNACK message will be send to the client.");
			MqttsConnack connack = new MqttsConnack();
			connack.setReturnCode(MqttsMessage.RETURN_CODE_ACCEPTED);
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts CONNACK message to the client.");
			clientInterface.sendMsg(this.clientAddress, connack);	
			return;
		}

		//if the gateway is already in process of establishing a connection with the client, drop the message 
		if(gateway.isEstablishingConnection()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is already establishing a connection. The received Mqtts CONNECT message cannot be processed.");
			return;
		}

		//if the will flag of the Mqtts CONNECT message is not set, 
		//construct a Mqtt CONNECT message, send it to the broker and return
		if(!receivedMsg.isWill()){			
			MqttConnect mqttConnect = new MqttConnect();
			mqttConnect.setProtocolName(receivedMsg.getProtocolName());
			mqttConnect.setProtocolVersion (receivedMsg.getProtocolVersion());
			mqttConnect.setWill (receivedMsg.isWill());	
			mqttConnect.setCleanStart (receivedMsg.isCleanSession());
			mqttConnect.setKeepAlive(receivedMsg.getDuration());
			mqttConnect.setClientId (receivedMsg.getClientId());

			//open a new TCP/IP connection with the broker 
			try {
				brokerInterface.initialize();
			} catch (MqttsException e) {
				e.printStackTrace();
				GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - An error occurred while TCP/IP connection setup with the broker.");
				return;
			}

			//send the Mqtt CONNECT message to the broker
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt CONNECT message to the broker.");
			try {
				brokerInterface.sendMsg(mqttConnect);
			} catch (MqttsException e) {
				e.printStackTrace();
				GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt CONNECT message to the broker.");
				return;
			}

			//set the state of the client as "Connected"
			client.setConnected();
			return;
		}

		//if the will flag is set, store the received Mqtts CONNECT message, construct a 
		//Mqtts WILTOPICREQ message, and send it to the client
		this.mqttsConnect = receivedMsg;
		MqttsWillTopicReq willTopicReq = new MqttsWillTopicReq();

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts WILLTOPICREQ message to the client.");
		clientInterface.sendMsg(this.clientAddress, willTopicReq);

		//set the gateway on "waitingWillTopic" state and increase 
		//the tries of sending Mqtts WILLTOPICREQ message to the client
		gateway.setWaitingWillTopic();
		gateway.increaseTriesSendingWillTopicReq();

		//set a timeout for waiting a Mqtts WILLTOPIC message from the client by registering to the timer
		timer.register(this.clientAddress, ControlMessage.WAITING_WILLTOPIC_TIMEOUT, GWParameters.getWaitingTime());
	}


	/**
	 * The method that handles a Mqtts WILLTOPIC message.
	 * 
	 * @param receivedMsg The received MqttsWillTopic message.
	 */
	private void handleMqttsWillTopic(MqttsWillTopic receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts WILLTOPIC message with \"WillTopic\" = \""+receivedMsg.getWillTopic()+"\" received.");
		//if the gateway is not expecting a Mqtts WILLTOPIC at this time, drop the received message and return
		if(!gateway.isWaitingWillTopic()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtts WILLTOPIC message from the client. The received message cannot be processed.");
			return;
		}

		//"reset" the "waitingWillTopic" state of the gateway, "reset" the tries of sending 
		//Mqtts WILLTOPICREQ message to the client and unregister from the timer
		gateway.resetWaitingWillTopic();
		gateway.resetTriesSendingWillTopicReq();
		timer.unregister(this.clientAddress,ControlMessage.WAITING_WILLTOPIC_TIMEOUT);

		//store the received Mqtts WILLTOPIC message, construct a Mqtts 
		//WILLMSGREQ message, and send it to the client
		this.mqttsWillTopic = receivedMsg;
		MqttsWillMsgReq willMsgReq = new MqttsWillMsgReq();

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts WILLMSGREQ message to the client.");
		clientInterface.sendMsg(this.clientAddress, willMsgReq);

		//set the gateway on "waitingWillMsg" state and increase 
		//the tries of sending Mqtts WILLMSGREQ message to the client
		gateway.setWaitingWillMsg();
		gateway.increaseTriesSendingWillMsgReq();

		//set a timeout for waiting a Mqtts WILLMSG message from the client by registering to the timer
		timer.register(this.clientAddress, ControlMessage.WAITING_WILLMSG_TIMEOUT, GWParameters.getWaitingTime());
	}

	/**
	 * The method that handles a Mqtts WILLMSG message.
	 * 
	 * @param receivedMsg The received MqttsWillMsg message.
	 */
	private void handleMqttsWillMsg(MqttsWillMsg receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts WILLMSG message with \"WillMsg\" = \""+receivedMsg.getWillMsg()+"\" received.");
		//if the gateway is not expecting a Mqtts WILLMSG at this time, drop the received message and return
		if(!gateway.isWaitingWillMsg()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtts WILLMSG message from the client.The received message cannot be processed.");
			return;
		}

		//"reset" the "waitingWillMsg" state of the gateway, "reset" the tries of sending 
		//Mqtts WILLMSGREQ message to the client and unregister from the timer
		gateway.resetWaitingWillMsg();
		gateway.resetTriesSendingWillMsgReq();
		timer.unregister(this.clientAddress, ControlMessage.WAITING_WILLMSG_TIMEOUT);

		//assure that the stored Mqtts CONNECT and Mqtts WILLTOPIC messages that we received before are not null
		//if one of them is null delete the other and return (debugging checks)
		if (this.mqttsConnect == null){				
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The stored Mqtts CONNECT message is null. The received Mqtts WILLMSG message cannot be processed.");
			this.mqttsWillTopic = null;
			return;
		}
		if (this.mqttsWillTopic == null){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The stored Mqtts WILLTOPIC message is null. The received Mqtts WILLMSG message cannot be processed.");
			this.mqttsConnect = null;
			return;
		}

		//construct a Mqtt CONNECT message		
		MqttConnect mqttConnect = new MqttConnect();

		//populate the Mqtt CONNECT message with the information of the stored Mqtts CONNECT
		//and WILLTOPIC messages and the information of the received Mqtts WILLMSG message 
		mqttConnect.setProtocolName(this.mqttsConnect.getProtocolName());
		mqttConnect.setProtocolVersion (this.mqttsConnect.getProtocolVersion());
		mqttConnect.setWillRetain (this.mqttsWillTopic.isRetain());
		mqttConnect.setWillQoS (this.mqttsWillTopic.getQos());
		mqttConnect.setWill (this.mqttsConnect.isWill());	
		mqttConnect.setCleanStart (this.mqttsConnect.isCleanSession());
		mqttConnect.setKeepAlive(this.mqttsConnect.getDuration());
		mqttConnect.setClientId (this.mqttsConnect.getClientId());
		mqttConnect.setWillTopic (this.mqttsWillTopic.getWillTopic());
		mqttConnect.setWillMessage (receivedMsg.getWillMsg());

		//open a new TCP/IP connection with the broker 
		try {
			brokerInterface.initialize();
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - An error occurred while TCP/IP connection setup with the broker.");
			return;
		}

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt CONNECT message to the broker.");
		//send the Mqtt CONNECT message to the broker
		try {
			brokerInterface.sendMsg(mqttConnect);
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt CONNECT message to the broker.");
			return;
		}

		//set the state of the client as "Connected"
		client.setConnected();

		//delete the stored Mqtts CONNECT and Mqtts WILLTOPIC messages
		this.mqttsConnect = null;
		this.mqttsWillTopic = null;
	}


	/**
	 * The method that handles a Mqtts REGISTER message.
	 * 
	 * @param receivedMsg The received MqttsRegister message.
	 */
	private void handleMqttsRegister(MqttsRegister receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts REGISTER message with \"TopicName\" = \"" +receivedMsg.getTopicName()+"\" received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts REGISTER message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		int topicId = topicIdMappingTable.getTopicId(receivedMsg.getTopicName());
		if (topicId == 0){				
			//assign a topicID to the received topicName
			topicId = getNewTopicId();
			topicIdMappingTable.assignTopicId(topicId, receivedMsg.getTopicName());
		}

		//construct a Mqtts REGACK message
		MqttsRegack regack = new MqttsRegack();
		regack.setTopicId(topicId);
		regack.setMsgId(receivedMsg.getMsgId());
		regack.setReturnCode(MqttsMessage.RETURN_CODE_ACCEPTED);

		//send the Mqtts REGACK message to the client	
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts REGACK message with \"TopicId\" = \"" +topicId+"\" to the client.");
		clientInterface.sendMsg(this.clientAddress, regack);
	}

	/**
	 * The method that handles a Mqtts REGACK message.
	 * 
	 * @param receivedMsg The received MqttsRegack message.
	 */
	private void handleMqttsRegack(MqttsRegack receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts REGACK message with \"TopicId\" = \"" +receivedMsg.getTopicId()+"\" received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts REGACK message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//if the gateway is not expecting a Mqtts REGACK at this time, drop the received message and return
		if(!gateway.isWaitingRegack()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtts REGACK message from the client. The received message cannot be processed.");
			return;
		}

		//assure that the stored Mqtts REGISTER and Mqtt PUBLISH messages are not null
		//if one of them is null delete the other and return (debugging checks)
		if (this.mqttsRegister == null){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The stored Mqtts REGISTER message is null. The received Mqtts REGACK message cannot be processed.");

			//"reset" the "waitingRegack" state of the gateway, "reset" the tries of sending 
			//Mqtts REGISTER message to the client and unregister from the timer
			gateway.resetWaitingRegack();
			gateway.resetTriesSendingRegister();
			timer.unregister(this.clientAddress, ControlMessage.WAITING_REGACK_TIMEOUT);

			this.mqttPublish = null;
			return;
		}
		if (this.mqttPublish == null){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The stored Mqtt PUBLISH message is null. The received Mqtts REGACK message cannot be processed.");

			//"reset" the "waitingRegack" state of the gateway, "reset" the tries of sending 
			//Mqtts REGISTER message to the client and unregister from the timer
			gateway.resetWaitingRegack();
			gateway.resetTriesSendingRegister();
			timer.unregister(this.clientAddress, ControlMessage.WAITING_REGACK_TIMEOUT);

			this.mqttsRegister = null;
			return;
		}

		//if the MsgId of the received Mqtt REGACK is not the same with MsgId of the stored Mqtts 
		//REGISTER message drop the received message and return (don't delete any stored message)
		if(receivedMsg.getMsgId() != this.mqttsRegister.getMsgId()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - MsgId (\""+receivedMsg.getMsgId()+"\") of the received Mqtts REGACK message does not match the MsgId (\""+this.mqttsRegister.getMsgId()+"\") of the stored Mqtts REGISTER message. The message cannot be processed.");
			return;
		}

		//assign the topicId of the Mqtts REGACK message to the received topicName of 
		//the Mqtt PUBLISH message (topicId is the same as in the stored Mqtts REGISTER message)

		topicIdMappingTable.assignTopicId(receivedMsg.getTopicId(), this.mqttPublish.getTopicName());

		//now we have a topicId, so construct a Mqtts PUBLISH message
		MqttsPublish publish = new MqttsPublish();

		//populate the Mqtts PUBLISH message with the information of the stored Mqtt PUBLISH message
		publish.setDup(mqttPublish.isDup());
		publish.setQos(mqttPublish.getQos());
		publish.setRetain(mqttPublish.isRetain());
		publish.setTopicIdType(MqttsMessage.NORMAL_TOPIC_ID);
		publish.setTopicId(receivedMsg.getTopicId());
		publish.setMsgId(mqttPublish.getMsgId());
		publish.setData(mqttPublish.getPayload());

		//send the Mqtts PUBLISH message to the client
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBLISH message with \"QoS\" = \""+mqttPublish.getQos()+"\" and \"TopicId\" = \"" +receivedMsg.getTopicId()+"\" to the client.");
		clientInterface.sendMsg(this.clientAddress, publish);

		//"reset" the "waitingRegack" state of the gateway, "reset" the tries of sending 
		//Mqtts REGISTER message to the client, unregister from the timer and delete
		//the stored Mqtts REGISTER and Mqtt PUBLISH messages		
		gateway.resetWaitingRegack();
		gateway.resetTriesSendingRegister();
		timer.unregister(this.clientAddress, ControlMessage.WAITING_REGACK_TIMEOUT);
		this.mqttsRegister = null;
		this.mqttPublish = null;		
	}


	/**
	 * The method that handles a Mqtts PUBLISH message.
	 * 
	 * @param receivedMsg The received MqttsPublish message.
	 */
	private void handleMqttsPublish(MqttsPublish receivedMsg) {
		if(receivedMsg.getTopicIdType() == MqttsMessage.NORMAL_TOPIC_ID)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" and \"TopicId\" = \""+receivedMsg.getTopicId()+"\" received.");
		else if (receivedMsg.getTopicIdType() == MqttsMessage.PREDIFINED_TOPIC_ID)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" and \"TopicId\" = \""+receivedMsg.getTopicId()+"\" (predefined topid Id) received.");
		else if (receivedMsg.getTopicIdType() == MqttsMessage.SHORT_TOPIC_NAME)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" and \"TopicId\" = \""+receivedMsg.getShortTopicName()+"\" (short topic name) received.");
		else{
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBLISH message with unknown topicIdType (\"" + receivedMsg.getTopicIdType()+"\") received. The message cannot be processed.");
			return;
		}


		//if Mqtts PUBLISH message has QoS = -1, construct an "internal" message (see com.ibm.zurich.mqttsgw.core.Message)
		//for the GatewayMsgHandler and put it to the dispatcher's queue 
		if(receivedMsg.getQos()== -1){
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The received Mqtts PUBLISH message with \"QoS\" = \"-1\" will be handled by GatewayMsgHandler.");

			Message msg = new Message(GWParameters.getGatewayAddress());

			msg.setType(Message.MQTTS_MSG);
			msg.setMqttsMessage(receivedMsg);
			dispatcher.putMessage(msg);
			return;
		}

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts PUBLISH message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//if there is already a publish procedure from the client with QoS 1 and the 
		//gateway is expecting a Mqtt PUBACK from the broker, then drop the message if it has QoS 1 
		if(gateway.isWaitingPuback() && receivedMsg.getQos() == 1){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is already in a publish procedure with \"QoS\" = \"1\". The received Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" cannot be processed.");
			return;
		}

		//else construct a Mqtt PUBLISH message
		MqttPublish publish = new MqttPublish();

		//check the TopicIdType in the received Mqtts PUBLISH message
		switch(receivedMsg.getTopicIdType()){

		//if the TopicIdType is a normal TopicId
		case MqttsMessage.NORMAL_TOPIC_ID:
			if(receivedMsg.getTopicId() <= GWParameters.getPredfTopicIdSize()){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - TopicId (\"" + receivedMsg.getTopicId() + "\") of the received Mqtts PUBLISH message is in the range of predefined topic Ids [1,"+GWParameters.getPredfTopicIdSize()+"]. The message cannot be processed. Mqtts PUBACK with rejection reason will be sent to the client.");

				//construct a Mqtts PUBACK message with ReturnCode = "Rejected:Invalid TopicId"
				MqttsPuback puback = new MqttsPuback();
				puback.setTopicId(receivedMsg.getTopicId());
				puback.setMsgId(receivedMsg.getMsgId());
				puback.setReturnCode(MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID);

				//send the Mqtts PUBACK message to the client	
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBACK message with \"TopicId\" = \"" +receivedMsg.getTopicId()+"\" and \"ReturnCode\" = \"Rejected: invalid TopicId\" to the client.");
				clientInterface.sendMsg(this.clientAddress, puback);
				return;
			}

			//get the TopicName by TopicId
			String topicName = topicIdMappingTable.getTopicName(receivedMsg.getTopicId());

			//if there is no such an entry
			if(topicName == null){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - TopicId (\"" + receivedMsg.getTopicId() + "\") of the received Mqtts PUBLISH message does not exist. The message cannot be processed. Mqtts PUBACK with rejection reason will be sent to the client.");

				//construct a Mqtts PUBACK message with ReturnCode = "Rejected:Invalid TopicId"
				MqttsPuback puback = new MqttsPuback();
				puback.setTopicId(receivedMsg.getTopicId());
				puback.setMsgId(receivedMsg.getMsgId());
				puback.setReturnCode(MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID);

				//send the Mqtts PUBACK message to the client	
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBACK message with \"TopicId\" = \"" +receivedMsg.getTopicId()+"\" and \"ReturnCode\" = \"Rejected: invalid TopicId\" to the client.");
				clientInterface.sendMsg(this.clientAddress, puback);
				return;
			}

			//we found a topicName corresponding to the received topicId
			publish.setTopicName(topicName);				
			break;

			//if the TopicIdType is a shortTopicName then simply copy it to the topicName field of the Mqtt PUBLISH message
		case MqttsMessage.SHORT_TOPIC_NAME:
			publish.setTopicName(receivedMsg.getShortTopicName());	
			break;

			//if the TopicIdType is a predifinedTopiId
		case MqttsMessage.PREDIFINED_TOPIC_ID:
			if(receivedMsg.getTopicId() > GWParameters.getPredfTopicIdSize()){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + receivedMsg.getTopicId() + "\") of the received Mqtts PUBLISH message is out of the range of predefined topic Ids [1,"+GWParameters.getPredfTopicIdSize()+"]. The message cannot be processed. Mqtts PUBACK with rejection reason will be sent to the client.");

				//construct a Mqtts PUBACK message with ReturnCode = "Rejected:Invalid TopicId"
				MqttsPuback puback = new MqttsPuback();
				puback.setTopicId(receivedMsg.getTopicId());
				puback.setMsgId(receivedMsg.getMsgId());
				puback.setReturnCode(MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID);

				//send the Mqtts PUBACK message to the client	
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBACK message with \"TopicId\" = \"" +receivedMsg.getTopicId()+"\" and \"ReturnCode\" = \"Rejected: invalid TopicId\" to the client.");
				clientInterface.sendMsg(this.clientAddress, puback);
				return;
			}

			//get the predefined topic name that corresponds to the received predefined topicId
			topicName = topicIdMappingTable.getTopicName(receivedMsg.getTopicId());

			//this should not happen as predefined topic ids are already stored
			if(topicName == null){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + receivedMsg.getTopicId() + "\") of the received Mqtts PUBLISH message does not exist. The message cannot be processed. Mqtts PUBACK with rejection reason will be sent to the client.");

				//construct a Mqtts PUBACK message with ReturnCode = "Rejected:Invalid TopicId"
				MqttsPuback puback = new MqttsPuback();
				puback.setTopicId(receivedMsg.getTopicId());
				puback.setMsgId(receivedMsg.getMsgId());
				puback.setReturnCode(MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID);

				//send the Mqtts PUBACK message to the client	
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBACK message with \"TopicId\" = \"" +receivedMsg.getTopicId()+"\" and \"ReturnCode\" = \"Rejected: invalid TopicId\" to the client.");
				clientInterface.sendMsg(this.clientAddress, puback);
				return;
			}

			publish.setTopicName(topicName);
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Unknown topicIdType (\"" + receivedMsg.getTopicIdType()+"\"). The received Mqtts PUBLISH message cannot be processed.");
			return;	
		}

		//populate the Mqtt PUBLISH message with the remaining information from Mqtts PUBLISH message
		publish.setDup(receivedMsg.isDup());
		publish.setQos(receivedMsg.getQos());
		publish.setRetain(receivedMsg.isRetain());
		publish.setMsgId(receivedMsg.getMsgId());
		publish.setPayload(receivedMsg.getData());		

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" and \"TopicName\" = \""+publish.getTopicName()+ "\" to the broker.");
		//send the Mqtt PUBLISH message to the broker
		try {
			brokerInterface.sendMsg(publish);
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt PUBLISH message to the broker.");
			connectionLost();
			return;
		}

		//TODO handle qos 2
		if(receivedMsg.getQos() == 1){
			gateway.setWaitingPuback();
			this.mqttsPublish = receivedMsg;
		}
		//if(receivedMsg.getQos() == 2)				
	}


	/**
	 * The method that handles a Mqtts PUBACK message.
	 * 
	 * @param receivedMsg The received MqttsPuback message.
	 */
	private void handleMqttsPuback(MqttsPuback receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBACK message received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts PUBACK message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//if the return code of the Mqtts PUBACK message is "Rejected: Invalid topic ID", then
		//delete this topicId(and the associate topic name)from the mapping table 
		if(receivedMsg.getReturnCode() == MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The received Mqtts PUBACK has \"ReturnCode\" = \"Rejected: invalid TopicId\". TopicId \"" +receivedMsg.getTopicId()+"\" will be deleted from mapping table.");
			topicIdMappingTable.removeTopicId(receivedMsg.getTopicId());
			return;
		}

		//else if everything is ok, construct a Mqtt PUBACK message  
		MqttPuback puback = new MqttPuback();
		puback.setMsgId(receivedMsg.getMsgId());

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PUBACK message to the broker.");
		//send the Mqtt PUBACK message to the broker
		try {
			brokerInterface.sendMsg(puback);
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt PUBACK message to the broker.");
			connectionLost();
		}
	}


	/**
	 * The method that handles a Mqtts PUBCOMP message.
	 * 
	 * @param receivedMsg The received MqttsPubComp message.
	 */
	private void handleMqttsPubComp(MqttsPubComp receivedMsg) {	
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBCOMP message received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts PUBCOMP message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//else construct a Mqtt PUBCOMP message
		MqttPubComp pubcomp = new MqttPubComp();
		pubcomp.setMsgId(receivedMsg.getMsgId());

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PUBCOMP message to the broker.");
		//send the Mqtt PUBCOMP message to the broker
		try {
			brokerInterface.sendMsg(pubcomp);
		} catch (MqttsException e) {
			e.printStackTrace();
			//if failed sending the message
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt PUBCOMP message to the broker.");
			connectionLost();
		}	
	}


	/**
	 * The method that handles a Mqtts PUBREC message.
	 * 
	 * @param receivedMsg The received MqttsPubRec message.
	 */
	private void handleMqttsPubRec(MqttsPubRec receivedMsg) {		
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBREC message received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts PUBREC message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//construct a Mqtt PUBREC message
		MqttPubRec pubrec = new MqttPubRec();
		pubrec.setMsgId(receivedMsg.getMsgId());

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PUBREC message to the broker.");
		//send the Mqtt PUBREC message to the broker	
		try {
			brokerInterface.sendMsg(pubrec);
		} catch (MqttsException e) {
			e.printStackTrace();
			//if failed sending the message
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt PUBREC message to the broker.");
			connectionLost();
		}
	}


	/**
	 * The method that handles a Mqtts PUBREL message.
	 * 
	 * @param receivedMsg The received MqttsPubRel message.
	 */
	private void handleMqttsPubRel(MqttsPubRel receivedMsg) {		
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBREL message received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts PUBREL message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//construct a Mqtt PUBREL message
		MqttPubRel pubrel = new MqttPubRel();
		pubrel.setMsgId(receivedMsg.getMsgId());

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PUBREL message to the broker.");
		//send the Mqtt PUBREL message to the broker
		try {
			brokerInterface.sendMsg(pubrel);
		} catch (MqttsException e) {
			e.printStackTrace();
			//if failed sending the message
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt PUBREL message to the broker.");
			connectionLost();
		}		
	}


	/**
	 * The method that handles a Mqtts SUBSCRIBE message.
	 * 
	 * @param receivedMsg The received MqttsSubscribe message.
	 */
	private void handleMqttsSubscribe(MqttsSubscribe receivedMsg) {
		if(receivedMsg.getTopicIdType() == MqttsMessage.TOPIC_NAME)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts SUBSCRIBE message with \"TopicName\" = \""+receivedMsg.getTopicName()+"\" received.");
		else if(receivedMsg.getTopicIdType() == MqttsMessage.PREDIFINED_TOPIC_ID)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts SUBSCRIBE message with \"TopicId\" = \""+receivedMsg.getPredefinedTopicId()+"\" (predefined topid Id) received.");
		else if(receivedMsg.getTopicIdType() == MqttsMessage.SHORT_TOPIC_NAME)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts SUBSCRIBE message with \"TopicId\" = \""+receivedMsg.getShortTopicName()+"\" (short topic name) received.");
		else{
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts SUBSCRIBE message with unknown topicIdType (\"" + receivedMsg.getTopicIdType()+"\") received. The message cannot be processed.");
			return;
		}


		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts SUBSCRIBE message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//if we are already in a subscription process, drop the received message and return
		if(gateway.isWaitingSuback()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is already in a subscription procedure. The received Mqtts SUBSCRIBE message cannot be processed.");
			return;
		}

		//else construct a Mqtt SUBSCRIBE message
		MqttSubscribe mqttSubscribe = new MqttSubscribe(); 

		//check the TopicIdType in the received Mqtts SUBSCRIBE message
		switch(receivedMsg.getTopicIdType()){

		//if the TopicIdType is a TopicName
		case MqttsMessage.TOPIC_NAME:
			mqttSubscribe.setTopicName(receivedMsg.getTopicName());
			break;

			//if the TopicIdType is a shortTopicName 
		case MqttsMessage.SHORT_TOPIC_NAME:
			mqttSubscribe.setTopicName(receivedMsg.getShortTopicName());	
			break;

			//if the TopicIdType is a predifinedTopiId
		case MqttsMessage.PREDIFINED_TOPIC_ID:
			if(receivedMsg.getPredefinedTopicId() > GWParameters.getPredfTopicIdSize()){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + + receivedMsg.getPredefinedTopicId() + "\") of the received Mqtts SUBSCRIBE message is out of the range of predefined topic Ids [1,"+GWParameters.getPredfTopicIdSize()+"]. The message cannot be processed. Mqtts SUBACK with rejection reason will be sent to the client.");

				//construct a Mqtts SUBACK message with ReturnCode = "Rejected:Invalid TopicId"
				MqttsSuback suback = new MqttsSuback();
				suback.setTopicIdType(MqttsMessage.PREDIFINED_TOPIC_ID);
				suback.setPredefinedTopicId(receivedMsg.getPredefinedTopicId());
				suback.setMsgId(receivedMsg.getMsgId());
				suback.setReturnCode(MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID);

				//send the Mqtts SUBACK message to the client	
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts SUBACK message with \"TopicId\" = \"" +receivedMsg.getPredefinedTopicId()+"\" and \"ReturnCode\" = \"Rejected: invalid TopicId\" to the client.");

				clientInterface.sendMsg(this.clientAddress, suback);			

				return;
			}

			//get the predefined topic name that corresponds to the predefined topicId
			String topicName = topicIdMappingTable.getTopicName(receivedMsg.getPredefinedTopicId());

			//this should not happen as predefined topic ids are already stored
			if(topicName == null){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + receivedMsg.getPredefinedTopicId() + "\") of the received Mqtts SUBSCRIBE message does not exist. The message cannot be processed. Mqtts SUBACK with rejection reason will be sent to the client.");

				//construct a Mqtts SUBACK message with ReturnCode = "Rejected:Invalid TopicId"
				MqttsSuback suback = new MqttsSuback();
				suback.setTopicIdType(MqttsMessage.PREDIFINED_TOPIC_ID);
				suback.setPredefinedTopicId(receivedMsg.getPredefinedTopicId());
				suback.setMsgId(receivedMsg.getMsgId());
				suback.setReturnCode(MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID);

				//send the Mqtts SUBACK message to the client	
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts SUBACK message with \"TopicId\" = \"" +receivedMsg.getPredefinedTopicId()+"\" and \"ReturnCode\" = \"Rejected: invalid TopicId\" to the client.");

				clientInterface.sendMsg(this.clientAddress, suback);					
				return;
			}
			mqttSubscribe.setTopicName(topicName);
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Unknown topicIdType (\"" + receivedMsg.getTopicIdType()+"\"). The received Mqtts SUBSCRIBE message cannot be processed.");
			return;				
		}

		//store the received Mqtts SUBSCRIBE message (for handling Mqtt SUBACK from the broker)
		this.mqttsSubscribe = receivedMsg;

		// populate the Mqtt SUBSCRIBE message with the remaining information from Mqtts SUBSCRIBE message
		mqttSubscribe.setDup(receivedMsg.isDup());

		mqttSubscribe.setMsgId(receivedMsg.getMsgId());

		//set the requested QoS for the specific topic name
		mqttSubscribe.setRequestedQoS(receivedMsg.getQos());

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt SUBSCRIBE message with \"TopicName\" = \""+mqttSubscribe.getTopicName()+ "\" to the broker.");
		//send the Mqtt SUBSCRIBE message to the broker
		try {
			brokerInterface.sendMsg(mqttSubscribe);
		} catch (MqttsException e) {
			e.printStackTrace();
			//if failed sending the message
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt SUBSCRIBE message to the broker.");
			connectionLost();
			return;
		}

		//set the gateway on "waitingSuback" state 
		gateway.setWaitingSuback();
	}


	/**
	 * The method that handles a Mqtts UNSUBSCRIBE message.
	 * 
	 * @param receivedMsg The received MqttsUnsubscribe message.
	 */
	private void handleMqttsUnsubscribe(MqttsUnsubscribe receivedMsg) {				
		if(receivedMsg.getTopicIdType() == MqttsMessage.TOPIC_NAME)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts UNSUBSCRIBE message with \"TopicName\" = \""+receivedMsg.getTopicName()+"\" received.");
		else if(receivedMsg.getTopicIdType() == MqttsMessage.PREDIFINED_TOPIC_ID)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts UNSUBSCRIBE message with \"TopicId\" = \""+receivedMsg.getPredefinedTopicId()+"\" (predefined topid Id) received.");
		else if(receivedMsg.getTopicIdType() == MqttsMessage.SHORT_TOPIC_NAME)
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts UNSUBSCRIBE message with \"TopicId\" = \""+receivedMsg.getShortTopicName()+"\" (short topic name) received.");
		else{
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts UNSUBSCRIBE message with unknown topicIdType (\"" + receivedMsg.getTopicIdType()+"\") received. The message cannot be processed.");
			return;
		}


		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts UNSUBSCRIBE message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//if we are already in an un-subscription process, drop the received message and return
		if(gateway.isWaitingUnsuback()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is already in a un-subscription procedure. The received Mqtts UNSUBSCRIBE message cannot be processed.");
			return;
		}

		//else construct a Mqtt UNSUBSCRIBE message
		MqttUnsubscribe mqttUnsubscribe = new MqttUnsubscribe(); 

		//check the TopicIdType in the received Mqtts UNSUBSCRIBE message
		switch(receivedMsg.getTopicIdType()){

		//if the TopicIdType is a TopicName
		case MqttsMessage.TOPIC_NAME:
			mqttUnsubscribe.setTopicName(receivedMsg.getTopicName());
			break;

			//if the TopicIdType is a shortTopicName 
		case MqttsMessage.SHORT_TOPIC_NAME:
			mqttUnsubscribe.setTopicName(receivedMsg.getShortTopicName());	
			break;

			//if the TopicIdType is a predifinedTopiId
		case MqttsMessage.PREDIFINED_TOPIC_ID:
			if(receivedMsg.getPredefinedTopicId() > GWParameters.getPredfTopicIdSize()){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + + receivedMsg.getPredefinedTopicId() + "\") of the received Mqtts UNSUBSCRIBE message is out of the range of predefined topic Ids [1,"+GWParameters.getPredfTopicIdSize()+"]. The message cannot be processed.");
				return;
			}				

			String topicName = topicIdMappingTable.getTopicName(receivedMsg.getPredefinedTopicId());

			//this should not happen
			if(topicName == null){
				GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + + receivedMsg.getPredefinedTopicId() + "\") does not exist. The received Mqtts UNSUBSCRIBE message cannot be processed.");
				return;
			}
			mqttUnsubscribe.setTopicName(topicName);
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Unknown topicIdType (\"" + receivedMsg.getTopicIdType()+"\"). The received Mqtts UNSUBSCRIBE message cannot be processed.");
			return;				
		}

		//store the received Mqtts UNSUBSCRIBE message (for handling Mqtt UNSUBACK)
		this.mqttsUnsubscribe = receivedMsg;

		// populate the Mqtt UNSUBSCRIBE message with the remaining information from Mqtts UNSUBSCRIBE message
		mqttUnsubscribe.setDup(receivedMsg.isDup());

		mqttUnsubscribe.setMsgId(receivedMsg.getMsgId());

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt UNSUBSCRIBE message with \"TopicName\" = \""+mqttUnsubscribe.getTopicName()+ "\" to the broker.");
		//send the Mqtt UNSUBSCRIBE message to the broker
		try {
			brokerInterface.sendMsg(mqttUnsubscribe);
		} catch (MqttsException e) {
			e.printStackTrace();
			//if failed sending the message
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt UNSUBSCRIBE message to the broker.");
			connectionLost();
			return;
		}

		//set the gateway on "waitingUnsuback" state 
		gateway.setWaitingUnsuback();
	}


	/**
	 * The method that handles a Mqtts PINGREQ message.
	 * 
	 * @param receivedMsg The received MqttsPingReq message.
	 */
	private void handleMqttsPingReq(MqttsPingReq receivedMsg) {		
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PINGREQ message received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts PINGREQ message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//construct a Mqtt PINGREQ message
		MqttPingReq pingreq = new MqttPingReq();

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PINGREQ message to the broker.");
		//send the Mqtt PINGREQ message to the broker
		try {
			brokerInterface.sendMsg(pingreq);
		} catch (MqttsException e) {
			e.printStackTrace();
			//if failed sending the message
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt PINGREQ message to the broker.");
			connectionLost();		
		}
	}


	/**
	 * The method that handles a Mqtts PINGRESP message.
	 * 
	 * @param receivedMsg The received MqttsPingResp message.
	 */
	private void handleMqttsPingResp(MqttsPingResp receivedMsg) {		
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts PINGRESP message received.");

		//if the client is not in state "Connected" send to it a Mqtts DISCONNECT message and return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts PINGRESP message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//construct a Mqtt PINGRESP message
		MqttPingResp pingresp = new MqttPingResp();

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PINGRESP message to the broker.");
		//send the Mqtt PINGRESP message to the broker
		try {
			brokerInterface.sendMsg(pingresp);
		} catch (MqttsException e) {
			e.printStackTrace();
			//if failed sending the message
			GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Failed sending Mqtt PINGRESP message to the broker.");
			connectionLost();
		}
	}


	/**
	 * The method that handles a Mqtts DISCONNECT message.
	 * 
	 * @param receivedMsg The received MqttsDisconnect message.
	 */
	private void handleMqttsDisconnect(MqttsDisconnect receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts DISCONNECT message received.");

		//if the client is not in state "Connected" return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtts DISCONNECT message cannot be processed.");
			return;
		}

		//else, stop the reading thread of the BrokerInterface
		//(this does not have any effect to the input and output streams which remain active)
		brokerInterface.setRunning(false);

		//construct a Mqtt DISCONNECT message
		MqttDisconnect mqttDisconnect = new MqttDisconnect();

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt DISCONNECT message to the broker.");
		//send the Mqtt DISCONNECT message to the broker
		//(no checks - don't bother if the sending of Mqtt DISCONNECT message to the broker was successful or not)
		try {
			brokerInterface.sendMsg(mqttDisconnect);
		} catch (MqttsException e) {
			//do nothing
		}		

		//call sendClientDisconnect method of this handler
		sendClientDisconnect();
	}


	/**
	 * The method that handles a Mqtts WILLTOPICUPD message.
	 * 
	 * @param receivedMsg The received MqttsWillTopicUpd message.
	 */
	private void handleMqttsWillTopicUpd(MqttsWillTopicUpd receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts WILLTOPICUPD message received.");
	}


	/**
	 * The method that handles a Mqtts WILLMSGUPD message.
	 * 
	 * @param receivedMsg The received MqttsWillMsgUpd message.
	 */
	private void handleMqttsWillMsgUpd(MqttsWillMsgUpd receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtts WILLMSGUPD received.");
	}



	/******************************************************************************************/
	/**                      HANDLING OF MQTT MESSAGES FROM THE BROKER                      **/
	/****************************************************************************************/

	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.core.MsgHandler#handleMqttMessage(com.ibm.zurich.mqttsgw.messages.mqtt.MqttMessage)
	 */
	public void handleMqttMessage(MqttMessage receivedMsg){
		//update this handler's timeout
		timeout = System.currentTimeMillis() + GWParameters.getHandlerTimeout()*1000;

		//get the type of the Mqtt message and handle the message according to that type	
		switch(receivedMsg.getMsgType()){
		case MqttMessage.CONNECT:
			//we will never receive such a message from the broker
			break;

		case MqttMessage.CONNACK:
			handleMqttConnack((MqttConnack) receivedMsg);
			break;

		case MqttMessage.PUBLISH:
			handleMqttPublish((MqttPublish) receivedMsg);
			break;

		case MqttMessage.PUBACK:
			handleMqttPuback((MqttPuback)receivedMsg);
			break;

		case MqttMessage.PUBREC:
			handleMqttPubRec((MqttPubRec) receivedMsg);
			break;

		case MqttMessage.PUBREL:
			handleMqttPubRel((MqttPubRel)receivedMsg);
			break;

		case MqttMessage.PUBCOMP:
			handleMqttPubComp((MqttPubComp) receivedMsg);
			break;

		case MqttMessage.SUBSCRIBE:
			//we will never receive such a message from the broker
			break;

		case MqttMessage.SUBACK:
			handleMqttSuback((MqttSuback) receivedMsg);
			break;

		case MqttMessage.UNSUBSCRIBE:
			//we will never receive such a message from the broker
			break;

		case MqttMessage.UNSUBACK:
			handleMqttUnsuback((MqttUnsuback) receivedMsg);
			break;

		case MqttMessage.PINGREQ:
			handleMqttPingReq((MqttPingReq) receivedMsg);
			break;

		case MqttMessage.PINGRESP:
			handleMqttPingResp((MqttPingResp) receivedMsg);
			break;

		case MqttMessage.DISCONNECT:
			//we will never receive such a message from the broker
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt message of unknown type \"" + receivedMsg.getMsgType()+"\" received.");
			break;
		}
	}


	/**
	 * The method that handles a Mqtt CONNACK message.
	 * 
	 * @param receivedMsg The received MqttConnack message.
	 */
	private void handleMqttConnack(MqttConnack receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt CONNACK message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt CONNACK message cannot be processed.");
			return;
		}

		//if the return code of the Mqtt CONNACK message is not "Connection Accepted", drop the message
		if (receivedMsg.getReturnCode() != MqttMessage.RETURN_CODE_CONNECTION_ACCEPTED){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Return Code of Mqtt CONNACK message it is not \"Connection Accepted\". The received Mqtt CONNACK message cannot be processed.");
			sendClientDisconnect();
			return;
		}

		//else construct a Mqtts CONNACK message
		MqttsConnack msg = new MqttsConnack();
		msg.setReturnCode(MqttsMessage.RETURN_CODE_ACCEPTED);

		//send the Mqtts CONNACK message to the client	
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts CONNACK message to the client.");
		clientInterface.sendMsg(this.clientAddress, msg);	
	}


	/**
	 * The method that handles a Mqtt PUBLISH message.
	 * 
	 * @param receivedMsg The received MqttPublish message
	 */
	private void handleMqttPublish(MqttPublish receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+ "\" and \"TopicName\" = \""+receivedMsg.getTopicName()+"\" received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt PUBLISH message cannot be processed.");
			return;
		}

		//if the data is too long to fit into a Mqtts PUBLISH message or the topic name is too
		//long to fit into a Mqtts REGISTER message then drop the received message 
		if (receivedMsg.getPayload().length > GWParameters.getMaxMqttsLength() - 7){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The payload in the received Mqtt PUBLISH message does not fit into a Mqtts PUBLISH message (payload length = "+receivedMsg.getPayload().length+ ". The message cannot be processed.");
			return;
		}

		if (receivedMsg.getTopicName().length() > GWParameters.getMaxMqttsLength() - 6){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The topic name in the received Mqtt PUBLISH message does not fit into a Mqtts REGISTER message (topic name length = "+receivedMsg.getTopicName().length()+ ". The message cannot be processed.");
			return;
		}


		//get the corresponding topicId from the topicName of the Mqtt PUBLISH message		
		int topicId = topicIdMappingTable.getTopicId(receivedMsg.getTopicName());

		//construct a Mqtts PUBLISH message
		MqttsPublish publish = new MqttsPublish();

		//if topicId exists then accept the Mqtt PUBLISH and send a Mqtts PUBLISH to the client
		if(topicId != 0){			
			//populate the Mqtts PUBLISH message with the information from the Mqtt PUBLISH message
			publish.setDup(receivedMsg.isDup());
			publish.setQos(receivedMsg.getQos());
			publish.setRetain(receivedMsg.isRetain());
			publish.setMsgId(receivedMsg.getMsgId());
			publish.setData(receivedMsg.getPayload());


			//check if the retrieved topicID is associated with a normal topicName 
			if(topicId > GWParameters.getPredfTopicIdSize()){
				publish.setTopicIdType(MqttsMessage.NORMAL_TOPIC_ID);
				publish.setTopicId(topicId);
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+ "\" and \"TopicId\" = \"" + topicId + "\" to the client.");

			}
			//or a predefined topic Id
			else if (topicId>0 && topicId<=GWParameters.getPredfTopicIdSize()){
				publish.setTopicIdType(MqttsMessage.PREDIFINED_TOPIC_ID);
				publish.setTopicId(topicId);
				GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+ "\" and \"TopicId\" = \"" + topicId + "\" to the client.");

			}
			//send the Mqtts PUBLISH message to the client
			clientInterface.sendMsg(this.clientAddress, publish);
			return;
		}		

		//handle the case of short topic names
		if (topicId == 0 && receivedMsg.getTopicName().length() == 2){
			publish.setTopicIdType(MqttsMessage.SHORT_TOPIC_NAME);
			publish.setShortTopicName(receivedMsg.getTopicName());
			publish.setDup(receivedMsg.isDup());
			publish.setQos(receivedMsg.getQos());
			publish.setRetain(receivedMsg.isRetain());
			publish.setMsgId(receivedMsg.getMsgId());
			publish.setData(receivedMsg.getPayload());
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+ "\" and \"TopicId\" = \"" + receivedMsg.getTopicName() + "\" (short topic name) to the client.");
			return;
		}

		//if topicId doesn't exist and we are not already in a register procedure initiated by 
		//the gateway, then store the Mqtts PUBLISH and send a Mqtts REGISTER to the client
		if (topicId == 0 && !gateway.isWaitingRegack()){
			//store the Mqtt PUBLISH message
			this.mqttPublish = receivedMsg;

			//get a new topicId (don't assign it until we get the REGACK message!)
			topicId = getNewTopicId();

			//construct a Mqtts REGISTER message and store it (for comparing later the MsgId)
			this.mqttsRegister = new MqttsRegister();
			this.mqttsRegister.setTopicId(topicId);
			this.mqttsRegister.setMsgId(getNewMsgId());
			this.mqttsRegister.setTopicName(receivedMsg.getTopicName());

			//send the Mqtts REGISTER message to the client
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts REGISTER message with \"TopicId\" = \"" +topicId+"\"  and \"TopicName\" = \"" +receivedMsg.getTopicName()+"\" to the client.");
			clientInterface.sendMsg(this.clientAddress, mqttsRegister);

			//set the gateway on "waitingRegack" state and increase 
			//the tries of sending Mqtts REGISTER message to the client
			gateway.setWaitingRegack();
			gateway.increaseTriesSendingRegister();

			//set a timeout for waiting a Mqtts REGACK message from the client by registering to the timer
			timer.register(this.clientAddress, ControlMessage.WAITING_REGACK_TIMEOUT, GWParameters.getWaitingTime());
			return;
		}

		//if topicId doesn't exist and we are already in a register procedure initiated by 
		//the gateway, then drop the received Mqtt PUBLISH message
		if (topicId == 0 && gateway.isWaitingRegack()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Topic name (\"" +receivedMsg.getTopicName()+"\") does not exist in the mapping table and the gateway is waiting a Mqtts REGACK message from the client. The received Mqtt PUBLISH message cannot be processed.");
			return;	
		}		
	}


	/**
	 * The method that handles a Mqtt PUBACK message.
	 * 
	 * @param receivedMsg The received MqttPuback message.
	 */
	private void handleMqttPuback(MqttPuback receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt PUBACK message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt PUBACK message cannot be processed.");
			return;
		}		

		//if the gateway is not expecting a Mqtt PUBACK at this time, drop the received message and return
		if(!gateway.isWaitingPuback()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtt PUBACK message from the broker.The received message cannot be processed.");
			return;
		}

		//else, assure that the stored Mqtts PUBLISH is not null (debugging checks)
		if (this.mqttsPublish == null){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The stored Mqtts PUBLISH message is null.The received Mqtt PUBACK message cannot be processed.");

			//"reset" the "waitingPuback" state of the gateway
			gateway.resetWaitingPuback();
			return;
		} 

		//if the MsgId of the received Mqtt PUBACK is not the same with MsgId of the stored 
		//Mqtts PUBLISH message, drop the received message and return (don't delete any stored message)
		if(receivedMsg.getMsgId() != this.mqttsPublish.getMsgId()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Message ID of the received Mqtt PUBACK does not match the message ID of the stored Mqtts PUBLISH message.The message cannot be processed.");
			return;
		}		

		//construct a Mqtts PUBACK message
		MqttsPuback puback = new MqttsPuback();

		puback.setMsgId(receivedMsg.getMsgId());
		puback.setReturnCode(MqttsMessage.RETURN_CODE_ACCEPTED);


		//check the TopicIdType in the stored Mqtts PUBLISH message
		switch(this.mqttsPublish.getTopicIdType()){

		//if the TopicIdType is a normal TopicId
		case MqttsMessage.NORMAL_TOPIC_ID:
			puback.setTopicId(this.mqttsPublish.getTopicId());
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBACK message with \"TopicId\" = \"" + puback.getTopicId() + "\" to the client.");
			break;

			//if the TopicIdType is a shortTopicName 
		case MqttsMessage.SHORT_TOPIC_NAME:
			puback.setShortTopicName(this.mqttsPublish.getShortTopicName());
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBACK message with \"TopicId\" = \"" + puback.getShortTopicName() + "\" (short topic name) to the client.");

			break;

			//if the TopicIdType is a predifinedTopiId
		case MqttsMessage.PREDIFINED_TOPIC_ID:
			puback.setTopicId(this.mqttsPublish.getTopicId());
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBACK message with \"TopicId\" = \"" + puback.getTopicId() + "\" to the client.");

			break;

			//should never reach here because topicIdType was checked 
			//already when we received the Mqtts PUBLISH message 
		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Unknown topicIdType of the stored Mqtts PUBLISH message: " + this.mqttsPublish.getTopicIdType()+". The received Mqtt PUBACK message cannot be processed.");
			return;				
		}		

		//send the Mqtts PUBACK message to the client
		clientInterface.sendMsg(this.clientAddress, puback);

		//"reset" the "waitingPuback" state of the gateway and delete Mqtts PUBLISH message
		gateway.resetWaitingPuback();
		this.mqttsPublish = null;
	}


	/**
	 * The method that handles a Mqtt PUBREC message.
	 * 
	 * @param receivedMsg The received MqttPubRec message.
	 */
	private void handleMqttPubRec(MqttPubRec receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt PUBREC message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt PUBREC message cannot be processed.");
			return;
		}

		//construct a Mqtts PUBREC message
		MqttsPubRec msg = new MqttsPubRec();
		msg.setMsgId(receivedMsg.getMsgId());

		//send the Mqtts PUBREC message to the client	
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBREC message to the client.");
		clientInterface.sendMsg(this.clientAddress, msg);
	}


	/**
	 * The method that handles a Mqtt PUBREL message.
	 * 
	 * @param receivedMsg The received MqttPubRel message.
	 */
	private void handleMqttPubRel(MqttPubRel receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt PUBREL message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt PUBREL message cannot be processed.");
			return;
		}

		//construct a Mqtts PUBREL message
		MqttsPubRel msg = new MqttsPubRel();
		msg.setMsgId(receivedMsg.getMsgId());

		//send the Mqtts PUBREL message to the client
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBREL message to the client.");
		clientInterface.sendMsg(this.clientAddress, msg);

	}


	/**
	 * The method that handles a Mqtt PUBCOMP message.
	 * 
	 * @param receivedMsg The received MqttPubComp message.
	 */
	private void handleMqttPubComp(MqttPubComp receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt PUBCOMP message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt PUBCOMP message cannot be processed.");
			return;
		}

		//construct a Mqtts PUBCOMP message
		MqttsPubComp msg = new MqttsPubComp();
		msg.setMsgId(receivedMsg.getMsgId());

		//send the Mqtts PUBCOMP message to the client
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PUBCOMP message to the client.");
		clientInterface.sendMsg(this.clientAddress, msg);		
	}


	/**
	 * The method that handles a Mqtt SUBACK message.
	 * 
	 * @param receivedMsg The received MqttSuback message.
	 */
	private void handleMqttSuback(MqttSuback receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt SUBACK message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt SUBACK message cannot be processed.");
			return;
		}

		//if the gateway is not expecting a Mqtt SUBACK at this time, drop the received message and return
		if(!gateway.isWaitingSuback()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtt SUBACK message from the broker. The received message cannot be processed.");
			return;
		}

		//else, assure that the stored Mqtts SUBSCRIBE is not null (debugging checks)
		if (this.mqttsSubscribe == null){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The stored Mqtts SUBSCRIBE is null. The received Mqtt SUBACK message cannot be processed.");

			//"reset" the "waitingSuback" state of the gateway
			gateway.resetWaitingSuback();
			return;
		} 

		//if the MsgId of the received Mqtt SUBACK is not the same with MsgId of the stored 
		//Mqtts SUBSCRIBE message, drop the received message and return (don't delete any stored message)
		if(receivedMsg.getMsgId() != this.mqttsSubscribe.getMsgId()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - MsgId (\""+receivedMsg.getMsgId()+"\") of the received Mqtts SUBACK message does not match the MsgId (\""+this.mqttsSubscribe.getMsgId()+"\") of the stored Mqtts SUBSCRIBE message. The message cannot be processed.");
			return;
		}

		//else construct a Mqtts SUBACK message
		MqttsSuback suback = new MqttsSuback();

		//populate the Mqtts SUBACK message with the information from the Mqtt SUBACK message
		suback.setGrantedQoS(receivedMsg.getGrantedQoS());
		suback.setMsgId(receivedMsg.getMsgId());
		suback.setReturnCode(MqttsMessage.RETURN_CODE_ACCEPTED);


		//check the TopicIdType in the stored Mqtts SUBSCRIBE message
		switch(this.mqttsSubscribe.getTopicIdType()){

		//if the TopicIdType is a TopicName
		case MqttsMessage.TOPIC_NAME:
			suback.setTopicIdType(MqttsMessage.NORMAL_TOPIC_ID);

			//if contains wildcard characters
			if(this.mqttsSubscribe.getTopicName().equals("#") 
					|| this.mqttsSubscribe.getTopicName().equals("+")
					|| this.mqttsSubscribe.getTopicName().indexOf("/#/") != -1 
					|| this.mqttsSubscribe.getTopicName().indexOf("/+/") != -1
					|| this.mqttsSubscribe.getTopicName().endsWith("/#")
					|| this.mqttsSubscribe.getTopicName().endsWith("/+")
					|| this.mqttsSubscribe.getTopicName().startsWith("#/")
					|| this.mqttsSubscribe.getTopicName().startsWith("+/")){
				//set the topicId of the Mqtts SUBACK message to 0x0000
				suback.setTopicId(0);					
			}else if(topicIdMappingTable.getTopicId(this.mqttsSubscribe.getTopicName()) != 0)
				//if topic id already exists
				suback.setTopicId(topicIdMappingTable.getTopicId(this.mqttsSubscribe.getTopicName()));
			else{
				//assign a new topicID to the topic name
				int topicId = getNewTopicId();
				topicIdMappingTable.assignTopicId(topicId, this.mqttsSubscribe.getTopicName());
				suback.setTopicId(topicId);
			}
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts SUBACK message with \"TopicId\" = \"" + suback.getTopicId() + "\" to the client.");

			break;

			//if the TopicIdType is a shortTopicName 
		case MqttsMessage.SHORT_TOPIC_NAME:
			suback.setTopicIdType(MqttsMessage.SHORT_TOPIC_NAME);
			suback.setShortTopicName(this.mqttsSubscribe.getShortTopicName());
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts SUBACK message with \"TopicId\" = \"" + suback.getShortTopicName() + "\" (short topic name) to the client.");

			break;

			//if the TopicIdType is a predifinedTopiId
		case MqttsMessage.PREDIFINED_TOPIC_ID:
			suback.setTopicIdType(MqttsMessage.PREDIFINED_TOPIC_ID);
			suback.setPredefinedTopicId(this.mqttsSubscribe.getPredefinedTopicId());
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts SUBACK message with \"TopicId\" = \"" + suback.getPredefinedTopicId() + "\" to the client.");

			break;

			//should never reach here because topicIdType was checked 
			//already when we received the Mqtts SUBSCRIBE message 
		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - UnknownTopicId type of the stored Mqtts SUBSCRIBE message: " + this.mqttsSubscribe.getTopicIdType()+". The received Mqtt SUBACK message cannot be processed.");
			return;				
		}

		//send the Mqtts SUBACK message to the client
		clientInterface.sendMsg(this.clientAddress, suback);

		//"reset" the "waitingSuback" state of the gateway and delete Mqtts SUBSCRIBE message
		gateway.resetWaitingSuback();
		this.mqttsSubscribe = null;
	}


	/**
	 * The method that handles a Mqtt UNSUBACK message.
	 * 
	 * @param receivedMsg The received MqttUnsuback message.
	 */
	private void handleMqttUnsuback(MqttUnsuback receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt UNSUBACK message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt UNSUBACK message cannot be processed.");
			return;
		}

		//if the gateway is not expecting a Mqtt UNSUBACK at this time, drop the received message and return
		if(!gateway.isWaitingUnsuback()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtt UNSUBACK message from the broker.The received message cannot be processed.");
			return;
		}

		//else, assure that the stored Mqtts UNSUBSCRIBE is not null (debugging checks)
		if (this.mqttsUnsubscribe == null){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - The stored Mqtts UNSUBSCRIBE is null.The received Mqtt UNSUBACK message cannot be processed.");

			//"reset" the "waitingUnsuback" state of the gateway
			gateway.resetWaitingUnsuback();
			return;
		} 

		//if the MsgId of the received Mqtt UNSUBACK is not the same with MsgId of the stored 
		//Mqtts UNSUBSCRIBE message, drop the received message and return (don't delete any stored message)
		if(receivedMsg.getMsgId() != this.mqttsUnsubscribe.getMsgId()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - MsgId (\""+receivedMsg.getMsgId()+"\") of the received Mqtts UNSUBACK message does not match the MsgId (\""+this.mqttsUnsubscribe.getMsgId()+"\") of the stored Mqtts UNSUBSCRIBE message. The message cannot be processed.");
			return;
		}

		//else remove the associated topicId from the mapping table
		if(!(this.mqttsUnsubscribe.getTopicIdType() == MqttsMessage.SHORT_TOPIC_NAME || this.mqttsUnsubscribe.getTopicIdType() == MqttsMessage.PREDIFINED_TOPIC_ID))
			topicIdMappingTable.removeTopicId(this.mqttsUnsubscribe.getTopicName());

		//construct a Mqtts UNSUBACK message
		MqttsUnsuback unsuback = new MqttsUnsuback();

		//set the msgId of Mqtts UNSUBACK message with the information from the Mqtt UNSUBACK message
		unsuback.setMsgId(receivedMsg.getMsgId());

		//send the Mqtts SUBACK message to the client
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts UNSUBACK message to the client.");
		clientInterface.sendMsg(this.clientAddress, unsuback);

		//"reset" the "waitingUnsuback" state of the gateway and delete Mqtts UNSUBSCRIBE message
		gateway.resetWaitingUnsuback();
		mqttsUnsubscribe = null;
	}


	/**
	 * The method that handles a Mqtt PINGREQ message.
	 * 
	 * @param receivedMsg The received MqttPingReq message.
	 */
	private void handleMqttPingReq(MqttPingReq receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt PINGREQ message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt PINGREQ message cannot be processed.");
			return;
		}

		//construct a Mqtts PINGREQ message
		MqttsPingReq msg = new MqttsPingReq();

		//send the Mqtts PINGREQ message to the client
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PINGREQ message to the client.");
		clientInterface.sendMsg(this.clientAddress, msg);
	}


	/**
	 * The method that handles a Mqtt PINGRESP message.
	 * 
	 * @param receivedMsg The received MqttPingResp message.
	 */
	private void handleMqttPingResp(MqttPingResp receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Mqtt PINGRESP message received.");

		//if the client is not in state "Connected" drop the received message
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Mqtt PINGRESP message cannot be processed.");
			return;
		}

		//construct a Mqtts PINGRESP message
		MqttsPingResp msg = new MqttsPingResp();

		//send the Mqtts PINGRESP message to the client
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts PINGRESP message to the client.");
		clientInterface.sendMsg(this.clientAddress, msg);	
	}



	/******************************************************************************************/
	/**                        HANDLING OF CONTROL MESSAGES AND TIMEOUTS	                **/
	/****************************************************************************************/

	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.core.MsgHandler#handleControlMessage(com.ibm.zurich.mqttsgw.messages.control.ControlMessage)
	 */
	public void handleControlMessage(ControlMessage receivedMsg){
		//get the type of the Control message and handle the message according to that type	
		switch(receivedMsg.getMsgType()){
		case ControlMessage.CONNECTION_LOST:
			connectionLost();
			break;

		case ControlMessage.WAITING_WILLTOPIC_TIMEOUT:
			handleWaitingWillTopicTimeout();
			break;

		case ControlMessage.WAITING_WILLMSG_TIMEOUT:
			handleWaitingWillMsgTimeout();
			break;

		case ControlMessage.WAITING_REGACK_TIMEOUT:
			handleWaitingRegackTimeout();
			break;

		case ControlMessage.CHECK_INACTIVITY:
			handleCheckInactivity();
			break;

		case ControlMessage.SEND_KEEP_ALIVE_MSG:
			//we will never receive such a message 
			break;				

		case ControlMessage.SHUT_DOWN:
			shutDown();
			break;			

		default:
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Control message of unknown type \"" + receivedMsg.getMsgType()+"\" received.");
			break;
		}
	}


	/**
	 * The method that is invoked when the TCP/IP connection with the broker was lost.
	 */
	private void connectionLost() {
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Control CONNECTION_LOST message received.");

		//if the client is not in state "Connected" return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The call on connectionLost() method has no effect.");
			return;
		}

		GatewayLogger.log(GatewayLogger.ERROR, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - TCP/IP connection with the broker was lost.");

		//call the sendClientDisconnect method of this handler
		sendClientDisconnect();
	}


	/**
	 * The method that is invoked when waiting for a Mqtts WILLTOPIC message from
	 * the client has timeout.
	 */
	private void handleWaitingWillTopicTimeout(){
		GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Control WAITING_WILLTOPIC_TIMEOUT message received.");

		//check if the gateway is still in state of waiting for a WILLTOPIC message from the client
		if(!gateway.isWaitingWillTopic()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtts WILLTOPIC message from the client. The received control WAITING_WILLTOPIC_TIMEOUT message cannot be processed.");
			return;
		}

		//if we have reached the maximum tries of sending Mqtts WILLTOPICREQ message
		if(gateway.getTriesSendingWillTopicReq() > GWParameters.getMaxRetries()){
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Maximum retries of sending Mqtts WILLTOPICREQ message to the client were reached. The message will not be sent again.");		

			//"reset" the "waitingWillTopic" state of the gateway, "reset" the tries of sending 
			//Mqtts WILLTOPICREQ message to the client, unregister from the timer and and delete 
			//the stored Mqtts CONNECT message
			gateway.resetWaitingWillTopic();
			gateway.resetTriesSendingWillTopicReq();
			timer.unregister(this.clientAddress, ControlMessage.WAITING_WILLTOPIC_TIMEOUT);
			this.mqttsConnect = null;

			//else construct a Mqtts WILTOPICREQ, and send it to the client
		}else{			
			MqttsWillTopicReq willTopicReq = new MqttsWillTopicReq();

			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Re-sending Mqtts WILLTOPICREQ message to the client. Retry: "+gateway.getTriesSendingWillTopicReq()+".");		
			clientInterface.sendMsg(this.clientAddress, willTopicReq);	

			//increase the tries of sending Mqtts WILLTOPICREQ message to the client
			gateway.increaseTriesSendingWillTopicReq();
		}
	}


	/**
	 * The method that is invoked when waiting for a Mqtts WILLMSG message from
	 * the client has timeout.
	 */
	private void handleWaitingWillMsgTimeout(){
		GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Control WAITING_WILLMSG_TIMEOUT message received.");

		//check if the gateway is still in state of waiting for a WILLMSG message from the client
		if(!gateway.isWaitingWillMsg()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not waiting a Mqtts WILLMSG message from the client. The received control WAITING_WILLMSG_TIMEOUT message cannot be processed.");
			return;
		}

		//if we have reached the maximum tries of sending Mqtts WILLMSGREQ message
		if(gateway.getTriesSendingWillMsgReq() > GWParameters.getMaxRetries()){
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Maximum retries of sending Mqtts WILLMSGREQ message to the client were reached. The message will not be sent again.");
			//"reset" the "waitingWillMsg" state of the gateway, "reset" the tries of sending
			//Mqtts WILLMSGREQ message to the client, unregister from the timer and delete 
			//the stored Mqtts CONNECT and Mqtts WILLTOPIC messages
			gateway.resetWaitingWillMsg();
			gateway.resetTriesSendingWillMsgReq();
			timer.unregister(this.clientAddress, ControlMessage.WAITING_WILLMSG_TIMEOUT);
			this.mqttsConnect = null;
			this.mqttsWillTopic = null;

			//else construct a Mqtts WILMSGREQ and send it to the client
		}else{			
			MqttsWillMsgReq willMsgReq = new MqttsWillMsgReq();

			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Re-sending Mqtts WILLMSGREQ message to the client. Retry: "+gateway.getTriesSendingWillMsgReq()+".");
			clientInterface.sendMsg(this.clientAddress, willMsgReq);

			//increase the tries of sending Mqtts WILLMSGREQ message to the client
			gateway.increaseTriesSendingWillMsgReq();
		}
	}


	/**
	 * The method that is invoked when waiting for a Mqtts REGACK message from
	 * the client has timeout.
	 */
	private void handleWaitingRegackTimeout(){
		GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Control WAITING_REGACK_TIMEOUT message received.");

		//check if the gateway is still in state of waiting for a REGACK message from the client
		if(!gateway.isWaitingRegack()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Gateway is not in state of waiting a Mqtts REGACK message from the client. The received control REGACK_TIMEOUT message cannot be processed.");
			return;
		}

		//if we have reached the maximum tries of sending the Mqtts REGISTER message
		if(gateway.getTriesSendingRegister() > GWParameters.getMaxRetries()){
			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Maximum retries of sending Mqtts REGISTER message to the client were reached. The message will not be sent again.");
			//"reset" the 'waitingRegack" state of the gateway, "reset" the tries of sending
			//Mqtts REGISTER message to the client, unregister from the timer and delete
			//the stored Mqtt PUBLISH and Mqtts REGISTER messages
			gateway.resetWaitingRegack();
			gateway.resetTriesSendingRegister();
			timer.unregister(this.clientAddress, ControlMessage.WAITING_REGACK_TIMEOUT);
			this.mqttPublish = null;
			this.mqttsRegister = null;
		}	

		//else modify the MsgId (get a new one) of the stored Mqtts 
		//REGISTER message, and send it to the client
		else{			
			this.mqttsRegister.setMsgId(getNewMsgId());

			GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Re-sending Mqtts REGISTER message to the client. Retry: "+gateway.getTriesSendingRegister()+".");
			clientInterface.sendMsg(this.clientAddress, this.mqttsRegister);
			gateway.increaseTriesSendingRegister();
		}
	}


	/**
	 * This method is invoked in regular intervals to check the inactivity of this handler
	 * in order to remove it from Dispatcher's mapping table.
	 */
	private void handleCheckInactivity() {
		GatewayLogger.log(GatewayLogger.INFO, 
				"ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+
				"]/["+clientId+"] - Control CHECK_INACTIVITY message received.");

		if(System.currentTimeMillis() > this.timeout){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is inactive for more than "+ GWParameters.getHandlerTimeout()/60+ " minutes. The associated ClientMsgHandler will be removed from Dispatcher's mapping table.");

			//close broker connection (if any)
			brokerInterface.disconnect();

			dispatcher.removeHandler(this.clientAddress);
		}
	}

	/**
	 * This method is invoked when the gateway is shutting down.
	 */
	private void shutDown(){
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Control SHUT_DOWN message received.");

		//if the client is not in state "Connected" return
		if(!client.isConnected()){
			GatewayLogger.log(GatewayLogger.WARN, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Client is not connected. The received Control SHUT_DOWN message cannot be processed.");
			return;
		}

		//stop the reading thread of the BrokerInterface
		//(this does not have any effect to the input and output streams which remain active)
		brokerInterface.setRunning(false);

		//construct a Mqtt DISCONNECT message
		MqttDisconnect mqttDisconnect = new MqttDisconnect();

		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt DISCONNECT message to the broker.");
		//send the Mqtt DISCONNECT message to the broker
		//(don't bother if the sending of Mqtt DISCONNECT message to the broker was successful or not)
		try {
			brokerInterface.sendMsg(mqttDisconnect);
		} catch (MqttsException e) {
			// do nothing
		}		
	}


	/******************************************************************************************/
	/**                                   OTHER METHODS AND CLASSES       	                **/
	/****************************************************************************************/


	/**
	 * The method that sets the Client interface in which this handler should respond in case 
	 * of sending a Mqtts message to the client.
	 * 
	 * @param clientInterface
	 */
	public void setClientInterface(ClientInterface clientInterface) {
		this.clientInterface = clientInterface;
	}


	/**
	 * This method sends a Mqtts DISCONNECT message to the client.
	 */
	private void sendClientDisconnect() {
		//construct a Mqtts DISCONNECT message
		MqttsDisconnect mqttsDisconnect = new MqttsDisconnect();

		//send the Mqtts DISCONNECT message to the client	
		GatewayLogger.log(GatewayLogger.INFO, "ClientMsgHandler ["+Utils.hexString(this.clientAddress.getAddress())+"]/["+clientId+"] - Sending Mqtts DISCONNECT message to the client.");
		clientInterface.sendMsg(this.clientAddress, mqttsDisconnect);

		//set the state of the client to "Disconnected"
		client.setDisconnected();

		//remove timer registrations (if any)
		timer.unregister(this.clientAddress);

		//"reset" all the states and the retry counters of the gateway
		gateway.reset();

		//close the connection with the broker (if any)
		brokerInterface.disconnect();
		//topicIdMappingTable.printContent();
	}


	/**
	 * This method generates and return a unique message ID.
	 * 
	 * @return The message ID
	 */
	private int getNewMsgId(){
		return msgId ++;
	}

	/**
	 *This method generates and return a unique topic ID.
	 * 
	 * @return A unique topic ID
	 */
	private int getNewTopicId(){
		return topicId ++;
	}


	/**
	 * The class that represents the state of the client at any given time.
	 *
	 */
	private class ClientState {

		private final int NOT_CONNECTED = 1;
		private final int CONNECTED 	= 2;
		private final int DISCONNECTED 	= 3;

		private int state;

		public ClientState(){
			state = NOT_CONNECTED;
		}

//		public boolean isNotConnected() {
//			return (state == NOT_CONNECTED);
//		}

//		public void setNotConnected() {
//			state = NOT_CONNECTED;
//		}

		public boolean isConnected() {
			return (state == CONNECTED);
		}

		public void setConnected() {
			state = CONNECTED;
		}

//		public boolean isDisconnected() {
//			return (state == DISCONNECTED);
//		}

		public void setDisconnected() {
			state = DISCONNECTED;
		}
	}


	/**
	 * The class that represents the state of the gateway at any given time.
	 *
	 */
	private class GatewayState {

		//waiting message from the client
		private boolean waitingWillTopic;
		private boolean waitingWillMsg;
		private boolean waitingRegack;

		//waiting message from the broker
		private boolean waitingSuback;
		private boolean waitingUnsuback;
		private boolean waitingPuback;

		//counters
		private int triesSendingWillTopicReq;
		private int triesSendingWillMsgReq;
		private int triesSendingRegister;

		public GatewayState(){
			this.waitingWillTopic = false;
			this.waitingWillMsg = false;		
			this.waitingRegack = false;

			this.waitingSuback = false;
			this.waitingUnsuback = false;
			this.waitingPuback = false;

			this.triesSendingWillTopicReq = 0;
			this.triesSendingWillMsgReq = 0;
			this.triesSendingRegister = 0;
		}


		public void reset(){
			this.waitingWillTopic = false;
			this.waitingWillMsg = false;		
			this.waitingRegack = false;

			this.waitingSuback = false;
			this.waitingUnsuback = false;
			this.waitingPuback = false;


			this.triesSendingWillTopicReq = 0;
			this.triesSendingWillMsgReq = 0;
			this.triesSendingRegister = 0;

			//delete also all stored messages (if any)  
			mqttsConnect = null;
			mqttsWillTopic = null;	
			mqttsSubscribe = null;
			mqttsUnsubscribe = null;
			mqttsRegister = null;
			mqttsPublish = null;
			mqttPublish = null;
		}


		public boolean isEstablishingConnection() {
			return (isWaitingWillTopic() || isWaitingWillMsg());
		}


		public boolean isWaitingWillTopic() {
			return this.waitingWillTopic;
		}

		public void setWaitingWillTopic() {
			this.waitingWillTopic = true;
		}

		public void resetWaitingWillTopic() {
			this.waitingWillTopic = false;
		}


		public boolean isWaitingWillMsg() {
			return this.waitingWillMsg;
		}

		public void setWaitingWillMsg() {
			this.waitingWillMsg = true;
		}

		public void resetWaitingWillMsg() {
			this.waitingWillMsg = false;
		}


		public boolean isWaitingRegack() {
			return this.waitingRegack;
		}

		public void setWaitingRegack() {
			this.waitingRegack = true;
		}

		public void resetWaitingRegack() {
			this.waitingRegack = false;
		}


		public boolean isWaitingSuback() {
			return this.waitingSuback;
		}

		public void setWaitingSuback() {
			this.waitingSuback = true;
		}

		public void resetWaitingSuback() {
			this.waitingSuback = false;
		}


		public boolean isWaitingUnsuback() {
			return this.waitingUnsuback;
		}

		public void setWaitingUnsuback() {
			this.waitingUnsuback = true;
		}

		public void resetWaitingUnsuback() {
			this.waitingUnsuback = false;
		}


		public boolean isWaitingPuback() {
			return this.waitingPuback;
		}

		public void setWaitingPuback() {
			this.waitingPuback = true;
		}

		public void resetWaitingPuback() {
			this.waitingPuback = false;
		}



		public int getTriesSendingWillTopicReq() {
			return this.triesSendingWillTopicReq;
		}

		public void increaseTriesSendingWillTopicReq() {
			this.triesSendingWillTopicReq ++;
		}

		public void resetTriesSendingWillTopicReq() {
			this.triesSendingWillTopicReq = 0;
		}



		public int getTriesSendingWillMsgReq() {
			return this.triesSendingWillMsgReq;
		}

		public void increaseTriesSendingWillMsgReq() {
			this.triesSendingWillMsgReq ++;
		}

		public void resetTriesSendingWillMsgReq() {
			this.triesSendingWillMsgReq = 0;
		}



		public int getTriesSendingRegister() {
			return this.triesSendingRegister;
		}

		public void increaseTriesSendingRegister() {
			this.triesSendingRegister ++;
		}	

		public void resetTriesSendingRegister() {
			this.triesSendingRegister = 0;
		}
	}

	/*	private void printState(String string) {
		System.out.println(string);
		System.out.println("client.isNotDisconnected = "+client.isNotConnected());
		System.out.println("client.isConnected = "+client.isConnected());
		System.out.println("client.isDisconnected = "+client.isDisconnected());	

		System.out.println("gateway.waitingWillTopic = "+gateway.isWaitingWillTopic());
		System.out.println("gateway.waitingWillMsg = "+gateway.isWaitingWillMsg());
		System.out.println("gateway.triesSendingWillTopicReq = "+gateway.getTriesSendingWillTopicReq());
		System.out.println("gateway.triesSendingWillMsgReq = "+gateway.getTriesSendingWillMsgReq());
		System.out.println("gateway.waitingRegack = "+gateway.isWaitingRegack());
		System.out.println("gateway.triesSendingRegister = "+gateway.getTriesSendingRegister());
		System.out.println("gateway.waitingSuback = "+gateway.isWaitingSuback());
		System.out.println("gateway.waitingUnsuback = "+gateway.isWaitingUnsuback());

		System.out.println("handler.mqttsConnect = "+mqttsConnect);
		System.out.println("handler.mqttsWillTopic = "+mqttsWillTopic);
		System.out.println("handler.mqttsRegister = "+mqttsRegister);
		System.out.println("handler.mqttPublish = "+mqttPublish);
		System.out.println("handler.mqttsSubscribe = "+mqttsSubscribe);
		System.out.println("handler.mqttsUnsubscribe  = "+mqttsUnsubscribe );
	}*/
}