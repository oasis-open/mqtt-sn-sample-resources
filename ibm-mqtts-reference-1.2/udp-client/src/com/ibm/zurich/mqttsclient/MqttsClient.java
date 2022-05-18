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


import java.net.InetAddress;
import java.net.UnknownHostException;
//import java.util.Hashtable;

import com.ibm.zurich.mqttsclient.exceptions.MqttsException;
import com.ibm.zurich.mqttsclient.messages.control.ControlMessage;
//import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsAdvertise;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsConnack;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsConnect;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsDisconnect;
//import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsGWInfo;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsMessage;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsPingReq;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsPingResp;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsPubComp;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsPubRec;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsPubRel;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsPuback;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsPublish;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsRegack;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsRegister;
//import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsSearchGW;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsSuback;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsSubscribe;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsUnsuback;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsUnsubscribe;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillMsg;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillMsgReq;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillMsgResp;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillTopic;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillTopicReq;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillTopicResp;
import com.ibm.zurich.mqttsclient.timer.TimerService;
import com.ibm.zurich.mqttsclient.udp.UDPInterface;
import com.ibm.zurich.mqttsclient.utils.ClientLogger;
import com.ibm.zurich.mqttsclient.utils.ClientParameters;
import com.ibm.zurich.mqttsclient.utils.MsgQueue;
import com.ibm.zurich.mqttsclient.utils.Utils;
import com.ibm.zurich.mqttsclient.messages.Message;


public class MqttsClient implements Runnable{

	public static final String version="140217";

	private static final int MQTTS_BACKUP_MESSAGE = 0x00;
	private static final int MQTTS_BACKUP_SEND_MESSAGE = 0x01;

	private MqttsMessage message = null;

	private int msgId;
	private ClientState clState; 
	private volatile boolean running;

	private boolean lostGw = false;
	private MqttsCallback callback = null;
	private UDPInterface udpInterface = null;
	private MsgQueue queue = null;
	private Thread readThread = null;
	private TimerService timer = null;
	private ClientParameters clientParms = null;

	private String clientid;
	private boolean will;
	private boolean cleanstart;
	private int keepalive;
	private String willtopic;
	private int willQoS;
	private String willmsg;
	private boolean willretain;

	private boolean autoReconnect = false;

	int ackMissedCounter = 0;


	public MqttsClient(String gatewayAddress, int gatewayPort, int maxMqttsMsgLength, int minMqttsMsgLength, 
			int maxRetries, int ackWaitingTime, boolean autoReconnect){
		
		InetAddress adr = null;
		try {
			adr = InetAddress.getByName(gatewayAddress);
			clientParms = new ClientParameters();
			clientParms.setGatewayAddress(adr);
			clientParms.setGatewayPort(gatewayPort);
			
			clientParms.setMaxMqttsLength(maxMqttsMsgLength);
			clientParms.setMinMqttsLength(minMqttsMsgLength);
			clientParms.setMaxRetries(maxRetries);
			clientParms.setWaitingTime(ackWaitingTime);

			this.clState = ClientState.NOT_ACTIVE;
			this.msgId = 1;
			this.autoReconnect= autoReconnect;

			queue = new MsgQueue();
			timer = new TimerService(queue);

			udpInterface = new UDPInterface();
			udpInterface.initialize(queue, clientParms);	

			//create thread for reading
			this.readThread = new Thread (this, "MqttsClient");
			this.running = true;
			this.readThread.start();
			
			String s = null;
			if (UDPInterface.ENCAPS) s=" with "; else s=" without ";
			ClientLogger.log(ClientLogger.INFO, "MQTT-S client version "+
					version + s + ("encapsulation started ..."));
			System.out.println("MQTT-S client version "+ version + s + ("encapsulation started ..."));

		} catch (MqttsException e) {
			ClientLogger.log(ClientLogger.ERROR, ""+e); 
			e.printStackTrace();
		} catch (UnknownHostException e) {
			ClientLogger.log(ClientLogger.ERROR, ""+e); 
			e.printStackTrace();
		}
	}

	public MqttsClient(String gatewayAddress, int gatewayPort,
			int maxMqttsMsgLength, int minMqttsMsgLength, 
			int maxRetries, int ackWaitingTime) {
		
		this(gatewayAddress,gatewayPort,maxMqttsMsgLength,minMqttsMsgLength,
				maxRetries,ackWaitingTime,false);

	}
	
	public MqttsClient(String gatewayAddress, int gatewayPort) {
		this(gatewayAddress,gatewayPort,
				256, 		//max mqtts message length
				2,			//min mqtts message length
				2,			//max number retries
				5,			//ack waiting time
				false);		//auto reconnect
	}
	
	public MqttsClient(String gatewayAddress, int gatewayPort, boolean auto) {
		this(gatewayAddress,gatewayPort,
				256, 		//max mqtts message length
				2,			//min mqtts message length
				2,			//max number retries
				5,			//ack waiting time
				auto);		//auto reconnect
	}
	
	
	
	/**
	 * Registers the callback handler of the application.
	 * This handler is used to notify the app about the completion of async events.
	 */
	public void registerHandler(MqttsCallback handler) {
		this.callback = handler;
		this.clState = ClientState.WAITING_CONNECT;
	}

	public boolean connect(String clientid, boolean cleanstart, int keepalive, 
			String willtopic, int willQoS, String willmsg, boolean willretain) {
	
		if (this.clState != ClientState.WAITING_CONNECT) {
			ClientLogger.log(ClientLogger.WARN, "connect() ignored!");
			//System.out.println("mqttsClient>> connect ignored");
			callback.disconnected(MqttsCallback.MQTTS_ERR_STACK_NOT_READY);
			return false;
		}
	
		this.clientid = clientid;
		this.cleanstart = cleanstart;
		this.keepalive = keepalive;
		this.will = true;
		this.willtopic = willtopic;
		this.willQoS = willQoS;
		this.willmsg = willmsg;
		this.willretain = willretain;
	
		clState=ClientState.CONNECTING_TO_GW;
		mqtts_connecting(this.clientid, this.will, this.cleanstart, this.keepalive);
		return true;
	}

	public boolean connect(String clientid, boolean cleanstart, short keepalive){
	
		if (this.clState != ClientState.WAITING_CONNECT) {
			ClientLogger.log(ClientLogger.WARN, "connect() ignored!");
			callback.disconnected(MqttsCallback.MQTTS_ERR_STACK_NOT_READY);
			return false;
		}
	
		this.clientid = clientid;
		this.cleanstart = cleanstart;
		this.keepalive = keepalive;
		this.will = false;
	
		this.clState= ClientState.CONNECTING_TO_GW;
		mqtts_connecting(this.clientid, this.will, this.cleanstart, this.keepalive);
		return true;
	}

	public boolean disconnect(){
	
		MqttsDisconnect msg = new MqttsDisconnect();
	
		switch (this.clState) {
		case NOT_ACTIVE:
		case WAITING_CONNECT:
			ClientLogger.log(ClientLogger.WARN,"already disconnected, disconnect() ignored");
			break;
	
		case CONNECTING_TO_GW:
		case SEARCHING_GW:
			// no need for sending a DISC since we are not connected 
			this.clState = ClientState.WAITING_CONNECT;
			timer.unregisterAll();
			callback.disconnected(MqttsCallback.MQTTS_OK);
			break;				
	
		case READY:	  		
		case WAITING_ACK:
			// send DISC to gw and wait for DISC
			mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
			timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
			timer.unregister(ControlMessage.KEEP_ALIVE);
			this.clState = ClientState.DISCONNECTING;
			ClientLogger.log(ClientLogger.INFO,"DISCONNECT sent: "+ Utils.hexString(msg.toBytes()));
			udpInterface.sendMsg(msg);
			
			break;		
	
		default:
			break;
		}  	
		return true;
	}

	public boolean publish(int topicId, byte[] message, int qos, boolean retain){
		return publish(0, topicId, message, qos, retain);
	}
	public boolean publish(int topicIdType, int topicId, byte[] message, int qos, boolean retain){
	
		if (this.clState != ClientState.READY) {
			ClientLogger.log(ClientLogger.WARN, "client not ready, publish() ignored! "+"Client state = "+ this.clState);
			return false;
		}
	
		MqttsPublish msg = new MqttsPublish();
	
		switch (qos)   {
		case 2:
			msg.setQos(qos);
			int id = getNewMsgId();
			msg.setMsgId(id);
			break;
	
		case 1:
			msg.setQos(qos);
			id = getNewMsgId();
			msg.setMsgId(id);
			break;
	
		default:
			msg.setQos(qos);
		msg.setMsgId(0);
		break;
		}
	
		msg.setRetain(retain);
		msg.setTopicId(topicId);
		msg.setTopicIdType(topicIdType);
		msg.setData(message);
	
	
		if(qos !=0){  
			/* backup the message for the ack */
			mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
	
			/* timers */
			timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
			timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
			clState= ClientState.WAITING_ACK;
		}
		/* Send the message */
		ClientLogger.log(ClientLogger.INFO, "Send PUBLISH to gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);
		return true;
	}

	public boolean subscribe(String topicName, int qos, int topicIdType){
		if (this.clState != ClientState.READY) {
			ClientLogger.log(ClientLogger.WARN, "client not ready, subscribe() ignored!");
			return false;
		}
	
		MqttsSubscribe msg = new MqttsSubscribe();
		msg.setQos(qos);
		if (topicIdType == MqttsMessage.TOPIC_NAME){
			msg.setTopicIdType(MqttsMessage.TOPIC_NAME);
			msg.setTopicName(topicName);
		}			
		if (topicIdType == MqttsMessage.SHORT_TOPIC_NAME){
			msg.setTopicIdType(MqttsMessage.SHORT_TOPIC_NAME);
			msg.setShortTopicName(topicName);
		}
	
		int id = getNewMsgId();
		msg.setMsgId(id);
	
		/* backup the message for the ack */
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
	
		/* timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
	
		clState= ClientState.WAITING_ACK;
		/* Send the message */
		ClientLogger.log(ClientLogger.INFO, "Send SUBSCRIBE to gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);
		return true;
	}

	public boolean subscribe(int topicId, int qos){
		if (this.clState != ClientState.READY) {
			ClientLogger.log(ClientLogger.WARN, "client not ready, subscribe2() ignored!");
			return false;
		}
	
		MqttsSubscribe msg = new MqttsSubscribe();
		msg.setQos(qos);
		msg.setTopicIdType(MqttsMessage.PREDIFINED_TOPIC_ID);
		msg.setPredefinedTopicId(topicId);
		int id = getNewMsgId();
		msg.setMsgId(id);
	
		/* backup the message for the ack */
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
	
		/* timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
	
		clState= ClientState.WAITING_ACK;
		/* Send the message */
		ClientLogger.log(ClientLogger.INFO, "Send SUBSCRIBE to gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);
		return true;
	}

	public boolean unSubscribe(String topicName, int topicIdType){
		if (this.clState != ClientState.READY) {
			ClientLogger.log(ClientLogger.WARN, "client not ready, unsubscribe1() ignored!");
			return false;
		}
	
		MqttsUnsubscribe msg= new MqttsUnsubscribe();
	
		if (topicIdType == MqttsMessage.TOPIC_NAME){
			msg.setTopicIdType(MqttsMessage.TOPIC_NAME);
			msg.setTopicName(topicName);
		}			
		if (topicIdType == MqttsMessage.SHORT_TOPIC_NAME){
			msg.setTopicIdType(MqttsMessage.SHORT_TOPIC_NAME);
			msg.setShortTopicName(topicName);
		}
	
		int id = getNewMsgId();
		msg.setMsgId(id);
	
		/* backup the message for the ack */
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
	
		/* timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
	
		clState= ClientState.WAITING_ACK;	
		/* Send the message */
		ClientLogger.log(ClientLogger.INFO, "Send UNSUBSCRIBE to gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);
		return true;
	}

	public boolean unSubscribe(int topicId){
		if (this.clState != ClientState.READY) {
			ClientLogger.log(ClientLogger.WARN, "client not ready, subscribe2() ignored!");
			return false;
		}
	
		MqttsUnsubscribe msg= new MqttsUnsubscribe();
	
		msg.setTopicIdType(MqttsMessage.PREDIFINED_TOPIC_ID);
		msg.setPredefinedTopicId(topicId);
		int id = getNewMsgId();
		msg.setMsgId(id);
		/* backup the message for the ack */
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
	
		/* timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
	
		clState= ClientState.WAITING_ACK;
		/* Send the message */
		ClientLogger.log(ClientLogger.INFO, "Send UNSUBSCRIBE to gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);
		return true;
	}

	public boolean register(String topicName){
		if (this.clState != ClientState.READY) {
			ClientLogger.log(ClientLogger.WARN, "client not ready, register() ignored! "+this.clState);
			return false;
		}
	
		MqttsRegister msg = new MqttsRegister();
		msg.setTopicId(0);
		int msgId = getNewMsgId();
		msg.setMsgId(msgId);
		msg.setTopicName(topicName);
	
		/* backup the message for the ack */
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
	
		/* timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
	
		/* we waiting for REGACK */
		clState= ClientState.WAITING_ACK;
		/* Send the message */
		ClientLogger.log(ClientLogger.INFO, "Send REGISTER to gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);
		return true;
	}

	public void terminate(){
		// terminate udp reader
		ClientLogger.log(ClientLogger.INFO, "Closing UDP ...");
		this.udpInterface.terminate();
		// unregister all timers
		ClientLogger.log(ClientLogger.INFO, "Stopping all timers ...");
		this.timer.terminate();
		// stop mqtts client
		ClientLogger.log(ClientLogger.INFO, "Stopping client and closing queue ...");
		this.running = false;
		this.queue.close();
		// wait until thread is terminated.
		try {
			this.readThread.join();
			ClientLogger.log(ClientLogger.INFO, "Client terminated ...");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ClientParameters getClientParameters() {
		return clientParms;
	}

	public void setLogfile(String filename) {
		try {
			ClientLogger.setLogFile(filename);
		} catch (MqttsException e) {
			System.err.println("MqttsClient: cannot set log filename");
			e.printStackTrace();
		}
	}

	public void setLogLevel(int level) {
		ClientLogger.setLogLevel(level);
	}

	public void setWaitingTime(int t) {
		clientParms.setWaitingTime(t);
	}
	
	public int getLocalUDPPort() {
		return udpInterface.getUdpPort();
	}

	/******************************************************************************************/
	/**                      HANDLING OF MQTTS MESSAGES                                     **/
	/****************************************************************************************/

	private void handleMqttsMessage(MqttsMessage receivedMsg){	
		
		if (clState == ClientState.NOT_ACTIVE) {
			ClientLogger.log(ClientLogger.WARN, "Client not started yet, received msg ingored."); 
			return;
		}
		
		//get the type of the Mqtts message and handle the message according to that type	
		switch(receivedMsg.getMsgType()){
		case MqttsMessage.ADVERTISE:
			//handleMqttsAdvertise((MqttsAdvertise) receivedMsg);
			break;

		case MqttsMessage.SEARCHGW:
			//handleMqttsSearchGW((MqttsSearchGW) receivedMsg);
			break;

		case MqttsMessage.GWINFO:
			//handleMqttsGWInfo((MqttsGWInfo) receivedMsg);
			break;				

		case MqttsMessage.CONNECT:
			//we will never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "CONNECT received and ignored!!!");
			break;

		case MqttsMessage.CONNACK:
			handleMqttsConnack((MqttsConnack) receivedMsg);
			break;

		case MqttsMessage.WILLTOPICREQ:
			handleMqttsWillTopicReq((MqttsWillTopicReq) receivedMsg);
			break;

		case MqttsMessage.WILLTOPIC:
			ClientLogger.log(ClientLogger.WARN, "WILLTOPIC received and ignored!!!");
			break;

		case MqttsMessage.WILLMSGREQ:
			handleMqttsWillMsgReq((MqttsWillMsgReq) receivedMsg);
			break;

		case MqttsMessage.WILLMSG:
			ClientLogger.log(ClientLogger.WARN, "WILLMSG received and ignored!!!");
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
			ClientLogger.log(ClientLogger.WARN, "SUBSCRIBE received and ignored!!!");
			break;

		case MqttsMessage.SUBACK:
			handleMqttsSuback((MqttsSuback) receivedMsg);
			break;

		case MqttsMessage.UNSUBSCRIBE:
			ClientLogger.log(ClientLogger.WARN, "UNSUBSCRIBE and ignored received !!!");
			break;

		case MqttsMessage.UNSUBACK:
			handleMqttsUnsuback((MqttsUnsuback) receivedMsg);
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
			ClientLogger.log(ClientLogger.WARN, "WILLTOPICUPD received and ignored!!!");
			break;

		case MqttsMessage.WILLTOPICRESP:
			handleMqttsWillTopicResp((MqttsWillTopicResp) receivedMsg);
			break;

		case MqttsMessage.WILLMSGUPD:
			ClientLogger.log(ClientLogger.WARN, "WILLMSGUPD received and ignored!!!");
			break;

		case MqttsMessage.WILLMSGRESP:
			handleMqttsWillMsgResp((MqttsWillMsgResp) receivedMsg);
			break;

		default:
			ClientLogger.log(ClientLogger.WARN, "MQTT-S message of unknown type \"" 
					+ receivedMsg.getMsgType()+"\" received and ignored!!!");
		break;
		}
	}


	private void handleMqttsWillTopicResp(MqttsWillTopicResp receivedMsg) {
		// TODO Auto-generated method stub

	}

	private void handleMqttsWillMsgResp(MqttsWillMsgResp receivedMsg) {
		// TODO Auto-generated method stub

	}
	
	private void ack_rx() {
		timer.unregister(ControlMessage.ACK);
		clState= ClientState.READY;
		if (lostGw) {
			callback.connected();
			lostGw = false;
		}
	}
	
	

	private void handleMqttsDisconnect(MqttsDisconnect receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "DISCONNECT received: " + 
				Utils.hexString(receivedMsg.toBytes()));

		timer.unregister(ControlMessage.ACK);
		timer.unregister(ControlMessage.KEEP_ALIVE);
		
		//hlt 19.03.2009 Because we cannot distinguish 
		//between a new gateway => client data still valid at broker
		//or a broker restart => client data deleted
		//it is better to inform app so that app can do a restart
		//e.g. reissue register and subscriptions
		clState = ClientState.WAITING_CONNECT;
		ClientLogger.log(ClientLogger.INFO, "Disconnected, waiting for connect");
		callback.disconnected(MqttsCallback.MQTTS_OK);
	}



	private void handleMqttsPingResp(MqttsPingResp receivedMsg) {
		ClientLogger.log(ClientLogger.INFO,"PINGRESP received: " +
				Utils.hexString(receivedMsg.toBytes()));

		switch (this.clState){
		case WAITING_ACK:
			ack_rx();
			break;
		default:
			break;
		}  		 	
	}



	private void handleMqttsPingReq(MqttsPingReq receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "PINGREQ received: " +
				Utils.hexString(receivedMsg.toBytes()));
		mqtts_pingresp();		
	}



	private void handleMqttsUnsuback(MqttsUnsuback receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "UNSUBACK received: " + 
				Utils.hexString(receivedMsg.toBytes()));

		switch (this.clState){
		case WAITING_ACK:
			if(receivedMsg.getMsgId() != ((MqttsUnsubscribe)this.message).getMsgId()){
				ClientLogger.log(ClientLogger.WARN, "MsgId (\""+
						receivedMsg.getMsgId()+"\") of the received Mqtts UNSUBACK message does not match the MsgId (\""+
						((MqttsUnsubscribe)this.message).getMsgId()+
						"\") of the stored Mqtts UNSUBSCRIBE message. The message cannot be processed.");
				return;
			}	
			
			ack_rx();
			callback.unsubackReceived();
			break;

		default:
			break;
		}  		
	}

	private void handleMqttsSuback(MqttsSuback receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "SUBACK received: " +
				Utils.hexString(receivedMsg.toBytes()));

		switch (this.clState){
		case WAITING_ACK:
			if(receivedMsg.getMsgId() != ((MqttsSubscribe)this.message).getMsgId()){
				ClientLogger.log(ClientLogger.WARN, "MsgId (\""+receivedMsg.getMsgId()+"\") of the received Mqtts SUBACK message does not match the MsgId (\""+((MqttsSubscribe)this.message).getMsgId()+"\") of the stored Mqtts SUBSCRIBE message. The message cannot be processed.");
				return;
			}		  		
			
			ack_rx();
			callback.subackReceived(receivedMsg.getGrantedQoS(), receivedMsg.getTopicId(),receivedMsg.getReturnCode());

			break;

		default:
			break;
		}  
	}

	private void handleMqttsPubRel(MqttsPubRel receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "PUBREL received: "+
				Utils.hexString(receivedMsg.toBytes()));
		// TODO procedure for QoS 2 not yet checked

	}

	private void handleMqttsPubRec(MqttsPubRec receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "PUBREC received: "+
				Utils.hexString(receivedMsg.toBytes()));
		
		// TODO procedure for QoS 2 not yet checked

		switch (this.clState){
		case WAITING_ACK:
			timer.unregister(ControlMessage.ACK);
			mqtts_pubrel(receivedMsg.getMsgId()); 
			break;	
		default:			   
			break;
		}  		
	}

	private void handleMqttsPubComp(MqttsPubComp receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "PUBCOMP received: "+
				Utils.hexString(receivedMsg.toBytes()));
		
		// TODO procedure for QoS 2 not yet checked

		switch (this.clState){		  		
		case WAITING_ACK:
			ack_rx();
			callback.pubCompReceived();
			break;
		default:			   
			break;
		}  		

	}



	private void handleMqttsPuback(MqttsPuback receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "PUBACK received: "+
				Utils.hexString(receivedMsg.toBytes()));

		switch (this.clState){
		case WAITING_ACK:
			if(receivedMsg.getMsgId() != ((MqttsPublish)this.message).getMsgId()){
				ClientLogger.log(ClientLogger.WARN, "MsgId (\""+receivedMsg.getMsgId()+"\") of the received Mqtts PUBACK message does not match the MsgId (\""+((MqttsPublish)this.message).getMsgId()+"\") of the stored Mqtts PUBLISH message. The message cannot be processed.");
				return;
			}			    
			
			ack_rx();
			callback.pubAckReceived(receivedMsg.getTopicId(), receivedMsg.getReturnCode());
			
			break;

		default:
			if (receivedMsg.getReturnCode() != MqttsMessage.RETURN_CODE_ACCEPTED){
				callback.pubAckReceived(receivedMsg.getTopicId(), receivedMsg.getReturnCode());
			}
		break;
		}
	}

	private void handleMqttsPublish(MqttsPublish receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "PUBLISH received: "+
				Utils.hexString(receivedMsg.toBytes()));
		
		int returnCode=-1;
		if (receivedMsg.getTopicIdType() == MqttsMessage.PREDIFINED_TOPIC_ID) {
			if (callback instanceof MqttsCallbackPreDefinedTopicId) {
				MqttsCallbackPreDefinedTopicId ecb = (MqttsCallbackPreDefinedTopicId)callback;
				returnCode = ecb.publishArrivedPreDefinedTopicId(receivedMsg.isRetain(), receivedMsg.getQos(),
						receivedMsg.getTopicId(),receivedMsg.getData());
			} else {
				ClientLogger.log(ClientLogger.ERROR, "Unexpected publish with predefined topicId received!");
				returnCode = MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID;
			}
			
		} else {
			returnCode = callback.publishArrived(receivedMsg.isRetain(), receivedMsg.getQos(),
					receivedMsg.getTopicId(),receivedMsg.getData());
		}

		if (receivedMsg.getQos() == 1 || returnCode == MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID) {
			mqtts_puback (receivedMsg.getTopicId(),receivedMsg.getMsgId(),returnCode);			
		}
		// TODO procedure for QoS 2 not yet checked
	}



	private void handleMqttsRegack(MqttsRegack receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "REGACK received: "+
				Utils.hexString(receivedMsg.toBytes()));

		switch (this.clState){
		case WAITING_ACK:
			
			if (!(this.message instanceof MqttsRegister)) {
				ClientLogger.log(ClientLogger.ERROR, "Unexpected message received: " + this.message.getMsgType());
				break;
			}
			
			if(receivedMsg.getMsgId() != ((MqttsRegister)this.message).getMsgId()){
				ClientLogger.log(ClientLogger.WARN, "MsgId (\""+receivedMsg.getMsgId()+"\") of the received Mqtts REGACK message does not match the MsgId (\""+((MqttsRegister)this.message).getMsgId()+"\") of the stored Mqtts REGISTER message. The message cannot be processed.");
				return;
			}		    
			
			ack_rx();
			callback.regAckReceived(receivedMsg.getTopicId(), receivedMsg.getReturnCode());
			break;
		default:
			break;
		}			
	}


	private void handleMqttsRegister(MqttsRegister receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "REGISTER received: " +
				Utils.hexString(receivedMsg.toBytes()));

		mqtts_regack(receivedMsg.getTopicId(), receivedMsg.getMsgId(), MqttsMessage.RETURN_CODE_ACCEPTED);
		callback.registerReceived(receivedMsg.getTopicId(), receivedMsg.getTopicName());
	}


	private void handleMqttsWillMsgReq(MqttsWillMsgReq receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "WILLMSGREQ received "+
				Utils.hexString(receivedMsg.toBytes()));
		timer.unregister(ControlMessage.ACK);
		mqtts_willmsg();
	}

	private void handleMqttsWillTopicReq(MqttsWillTopicReq receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "WILLTOPCREQ received: "+
				Utils.hexString(receivedMsg.toBytes()));
		timer.unregister(ControlMessage.ACK);
		mqtts_willtopic();
	}



	private void handleMqttsConnack(MqttsConnack receivedMsg) {
		ClientLogger.log(ClientLogger.INFO, "CONNACK received: " + 
				Utils.hexString(receivedMsg.toBytes()));

		switch (this.clState){
		case CONNECTING_TO_GW:
			if (receivedMsg.getReturnCode() == MqttsMessage.RETURN_CODE_ACCEPTED) {
				timer.unregister(ControlMessage.ACK);
				clState= ClientState.READY; 
				callback.connected();
				lostGw = false;
			} else {
				clState = ClientState.WAITING_CONNECT;
				timer.unregister(ControlMessage.ACK);
				timer.unregister(ControlMessage.KEEP_ALIVE);
				callback.disconnected(receivedMsg.getReturnCode());
			}
			break;	
		default:
			ClientLogger.log(ClientLogger.WARN,"CONNACK received in state "+ this.clState);
			break;
		}		
	}


	private int getNewMsgId(){
		return msgId ++;
	}



	/**
    send a PUBACK message
	 */

	private void mqtts_puback(int topicId, int msgId, int returnCode) {	
		//construct a Mqtts PUBACK message
		MqttsPuback puback = new MqttsPuback();

		puback.setTopicId(topicId);
		puback.setMsgId(msgId);
		puback.setReturnCode(returnCode);

		// re-start keep alive timer and send the message
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());

		//send the Mqtts PUBACK message to the gateway	
		ClientLogger.log(ClientLogger.INFO, "Sending PUBACK message with \"TopicId\" = \"" +topicId+"\" to the gateway:"+ Utils.hexString(puback.toBytes()));
		udpInterface.sendMsg(puback);
	}

	/**
    send a REGACK message

	 */
	private void mqtts_regack(int topicId, int msgId, int returnCode) {

		//construct a Mqtts REGACK message
		MqttsRegack regack = new MqttsRegack();
		regack.setTopicId(topicId);
		regack.setMsgId(msgId);
		regack.setReturnCode(returnCode);

		// re-start keep alive timer and send the message*/
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());

		//send the Mqtts REGACK message to the gateway	
		ClientLogger.log(ClientLogger.INFO, "Sending REGACK message with \"TopicId\" = \"" +topicId+"\" to the gateway :"+ Utils.hexString(regack.toBytes()));
		udpInterface.sendMsg(regack);
	}


	/**
    send a PUBREL message
	 */
	private void mqtts_pubrel(int msgId) {
		//construct a Mqtts PUBREL message
		MqttsPubRel msg = new MqttsPubRel();
		msg.setMsgId(msgId);

		//send the Mqtts PUBREL message
		ClientLogger.log(ClientLogger.INFO, "Sending PUBREL message to the gateway: "+ Utils.hexString(msg.toBytes()));
		
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
		clState= ClientState.WAITING_ACK;

		/* we are waiting for PUBCOMP */
		/* TODO What happens if we do not rx a PUBCOMP ? Retransmit PUBREL? */
		/* backup the message for the ack*/
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
		
		udpInterface.sendMsg(msg);
	}  



	/**
    send an PINGREQ message
	 */
	private void mqtts_pingreq() {
		//construct a Mqtts PINGREQ message
		MqttsPingReq msg = new MqttsPingReq();

		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());

		//send the Mqtts PINGREQ message to the client
		ClientLogger.log(ClientLogger.INFO, "Sending Mqtts PINGREQ message to the gateway: "+ Utils.hexString(msg.toBytes()));
		clState= ClientState.WAITING_ACK;
		udpInterface.sendMsg(msg);
	}

	/**
    send an PINGRESP message
	 */
	private void mqtts_pingresp() {
		//construct a Mqtts PINGREQ message
		MqttsPingResp msg = new MqttsPingResp();

		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());

		//send the Mqtts PINGRESP message to the client
		ClientLogger.log(ClientLogger.INFO, "Sending PINGRESP to the gateway: "+ Utils.hexString(msg.toBytes()));;
		udpInterface.sendMsg(msg);

	}

	/**
    send an WILLMSG message
	 */
	private void mqtts_willmsg() {

		//construct a Mqtts WILLMSG message
		MqttsWillMsg msg = new MqttsWillMsg();
		msg.setWillMsg(this.willmsg);


		/* backup the message for the ack*/
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);

		/* star timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
		ClientLogger.log(ClientLogger.INFO, "Sending WILLMSG to the gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);

	}

	/**
    send an WILLTOPIC message
	 */
	private void mqtts_willtopic() {

		MqttsWillTopic msg = new MqttsWillTopic();
		msg.setQos(this.willQoS);
		msg.setRetain(this.willretain);
		msg.setWillTopic(this.willtopic);

		/* backup the message for the ack*/
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);

		/* start timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
		ClientLogger.log(ClientLogger.INFO, "Sending WILLTOPIC to the gateway: "+ Utils.hexString(msg.toBytes()));
		udpInterface.sendMsg(msg);
	}

	/**
    send CONNECT message
	 */

	private void mqtts_connecting(String clientid , boolean will, boolean cleanstart, int keepalive) {
		MqttsConnect msg = new MqttsConnect();
		msg.setCleanSession(cleanstart);
		msg.setClientId(clientid);
		msg.setDuration(keepalive);
		msg.setWill(will);

		/* set the value of the keep_alive timer */
		clientParms.setKeepAlivePeriod(keepalive);
		
		/* backup the message for the ack */
		mqtts_backup(MQTTS_BACKUP_MESSAGE, msg);

		/* start ack and keep_alive timers */
		timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
		timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());

		udpInterface.sendMsg(msg);
		ClientLogger.log(ClientLogger.INFO, "CONNECT sent to gateway: "+ Utils.hexString(msg.toBytes()));
		
		callback.connectSent();

		return;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	 public void run() {
		while (running) {
			readMsg();
		}
	}


	private void readMsg() {
		//read the next available Message from queue

		Message msg = null;
		try {
			msg = (Message)queue.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (msg == null) {
			return;
		}
		//get the type of the message that "internal" message carries
		int type = msg.getType();
		switch(type){		
		case Message.MQTTS_MSG:
			ClientLogger.log(ClientLogger.INFO, "Processing an mqtts message ...");
			handleMqttsMessage(msg.getMqttsMessage());
			break;

		case Message.CONTROL_MSG:
			ClientLogger.log(ClientLogger.INFO, "Processing a control message ...");
			handleControlMessage(msg.getControlMessage());
			break;

		default:
			ClientLogger.log(ClientLogger.WARN,"Internal message of unknown type \"" + msg.getType()+"\" received.");
		break;
		}

	}


	private void handleControlMessage(ControlMessage controlMessage) {
		switch (controlMessage.getMsgType()) {
		case ControlMessage.ACK:
			ClientLogger.log(ClientLogger.INFO, "ACK timeout");
			ackMissedCounter++;
			
			if (ackMissedCounter > clientParms.getMaxRetries()) {
				
				//We log a warnning and inform app
				ClientLogger.log(ClientLogger.WARN, "Too many ACKs missed, lost gw ...");
				callback.disconnected(MqttsCallback.MQTTS_LOST_GATEWAY);
				
				if (autoReconnect) {
					mqtts_backup(MQTTS_BACKUP_SEND_MESSAGE, null);
					ackMissedCounter= 0;
					ClientLogger.log(ClientLogger.WARN, "will try re-connecting ...");
				} else {
					timer.unregister(ControlMessage.ACK);
					timer.unregister(ControlMessage.KEEP_ALIVE);
					clState = ClientState.WAITING_CONNECT;
					ClientLogger.log(ClientLogger.WARN, "Waiting for new connect from application ...");
				}
				
			} else mqtts_backup(MQTTS_BACKUP_SEND_MESSAGE, null);
			break;

		case ControlMessage.KEEP_ALIVE:
			ClientLogger.log(ClientLogger.INFO, "Keep Alive timeout, send PINGREQ");   	  
			mqtts_pingreq();
			break;

		default:
			break;
		}		
	}

	private void mqtts_backup(int action, MqttsMessage msg) {
		switch (action) {
		case MQTTS_BACKUP_MESSAGE: /* back up message */		
			this.message = msg;
			ClientLogger.log(ClientLogger.INFO, "Message backup for retransmission");
			break;

		case MQTTS_BACKUP_SEND_MESSAGE: /* resend message stored in backup */		
			timer.register(ControlMessage.KEEP_ALIVE, clientParms.getKeepAlivePeriod());
			timer.register(ControlMessage.ACK, clientParms.getWaitingTime());
			ClientLogger.log(ClientLogger.INFO, "Backup message resent:"+ Utils.hexString(this.message.toBytes()));
			udpInterface.sendMsg(this.message);
			
			break;

		default:
			break;
		}
		return;
	}




	/********************************************************************************/
	private enum ClientState {
		
		NOT_ACTIVE,
		WAITING_CONNECT,
		CONNECTING_TO_GW,
		READY,

		WAITING_ACK,
		
		SEARCHING_GW,
		DISCONNECTING;

	}
	
}