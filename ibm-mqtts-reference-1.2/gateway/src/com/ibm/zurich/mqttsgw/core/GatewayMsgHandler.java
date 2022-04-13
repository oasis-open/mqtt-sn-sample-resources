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

package com.ibm.zurich.mqttsgw.core;

import java.util.StringTokenizer;
import java.util.Vector;

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
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttUnsuback;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsAdvertise;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsGWInfo;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsPublish;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsSearchGW;
import com.ibm.zurich.mqttsgw.timer.TimerService;
import com.ibm.zurich.mqttsgw.utils.GWParameters;
import com.ibm.zurich.mqttsgw.utils.GatewayAddress;
import com.ibm.zurich.mqttsgw.utils.GatewayLogger;
import com.ibm.zurich.mqttsgw.utils.Utils;

public class GatewayMsgHandler extends MsgHandler{

	private GatewayAddress gatewayAddress = null;
	private TCPBrokerInterface brokerInterface = null;
	private TimerService timer = null;
	private Dispatcher dispatcher;
	private long advPeriodCounter = 0;
	private long checkingCounter = 0;
	private TopicMappingTable topicIdMappingTable;
	private String clientId;

	private boolean connected;
	private Vector<ClientInterface> clientInterfacesVector;


	/**
	 * 
	 */
	public GatewayMsgHandler(GatewayAddress addr) {
		this.gatewayAddress = addr;
	}

	public void initialize(){			
		brokerInterface = new TCPBrokerInterface(this.gatewayAddress);
		timer = TimerService.getInstance();
		dispatcher = Dispatcher.getInstance();
		topicIdMappingTable = new TopicMappingTable();
		topicIdMappingTable.initialize();		
		clientId = "Gateway_" + GWParameters.getGwId();

		GatewayLogger.info("GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - Establishing TCP/IP connection with "+
				GWParameters.getBrokerURL());

		//open a new TCP/IP connection with the broker 
		try {
			brokerInterface.initialize();
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.error("GatewayMsgHandler ["+
					Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
					"]/["+clientId+"] - Failed to establish TCP/IP connection with " +
					GWParameters.getBrokerURL()+ ". Gateway cannot start.");
			System.exit(1);

		}		
		GatewayLogger.info("GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - TCP/IP connection established.");
	}


	/******************************************************************************************/
	/**                             HANDLING OF MQTTS MESSAGES 		                        **/
	/****************************************************************************************/

	public void handleMqttsMessage(MqttsMessage receivedMsg){
		//get the type of the Mqtts message and handle the message according to that type	
		switch(receivedMsg.getMsgType()){
		case MqttsMessage.ADVERTISE:
			handleMqttsAdvertise((MqttsAdvertise) receivedMsg);
			break;

		case MqttsMessage.SEARCHGW:
			handleMqttsSearchGW((MqttsSearchGW) receivedMsg);
			break;

		case MqttsMessage.GWINFO:
			handleMqttsGWInfo((MqttsGWInfo) receivedMsg);
			break;				

		case MqttsMessage.CONNECT:
			//we will never receive such a message 
			break;

		case MqttsMessage.CONNACK:
			//we will never receive such a message 
			break;

		case MqttsMessage.WILLTOPICREQ:
			//we will never receive such a message 
			break;

		case MqttsMessage.WILLTOPIC:
			//we will never receive such a message 
			break;

		case MqttsMessage.WILLMSGREQ:
			//we will never receive such a message
			break;

		case MqttsMessage.WILLMSG:
			//we will never receive such a message 
			break;

		case MqttsMessage.REGISTER:
			//we will never receive such a message 
			break;

		case MqttsMessage.REGACK:
			//we will never receive such a message 
			break;

		case MqttsMessage.PUBLISH:
			handleMqttsPublish((MqttsPublish) receivedMsg);
			break;

		case MqttsMessage.PUBACK:
			//we will never receive such a message 
			break;

		case MqttsMessage.PUBCOMP:
			//we will never receive such a message 
			break;

		case MqttsMessage.PUBREC:
			//we will never receive such a message 
			break;

		case MqttsMessage.PUBREL:
			//we will never receive such a message 
			break;

		case MqttsMessage.SUBSCRIBE:
			//we will never receive such a message 
			break;

		case MqttsMessage.SUBACK:
			//we will never receive such a message 
			break;

		case MqttsMessage.UNSUBSCRIBE:
			//we will never receive such a message 
			break;

		case MqttsMessage.UNSUBACK:
			//we will never receive such a message
			break;

		case MqttsMessage.PINGREQ:
			//we will never receive such a message 
			break;

		case MqttsMessage.PINGRESP:
			//we will never receive such a message 
			break;			

		case MqttsMessage.DISCONNECT:
			//we will never receive such a message 
			break;

		case MqttsMessage.WILLTOPICUPD:
			//we will never receive such a message 
			break;

		case MqttsMessage.WILLTOPICRESP:
			//we will never receive such a message
			break;

		case MqttsMessage.WILLMSGUPD: 
			//we will never receive such a message
			break;

		case MqttsMessage.WILLMSGRESP:
			//we will never receive such a message
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtts message of unknown type \"" + receivedMsg.getMsgType()+"\" received.");
			break;
		}
	}


	/**
	 * @param receivedMsg
	 */
	private void handleMqttsAdvertise(MqttsAdvertise receivedMsg) {
		// TODO implement this method for load balancing issues
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtts ADVERTISE message received.");
	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttsSearchGW(MqttsSearchGW receivedMsg) {		
		//		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtts SEARCHGW message with \"Radius\" = \""+receivedMsg.getRadius()+"\" received.");

		//construct a Mqtts GWINFO message for the reply to the received Mqtts SEARCHGW message
		MqttsGWInfo msg = new MqttsGWInfo();
		msg.setGwId(GWParameters.getGwId());

		//get the broadcast radius 
		byte radius = (byte)receivedMsg.getRadius();

		//broadcast the Mqtts GWINFO message to the network
		//		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(this.gatewayAddress.getAddress())+"]/["+clientId+"] - Broadcasting Mqtts GWINFO message to the network with broadcast radius \""+radius+"\".");

		Vector<?> interfaces = GWParameters.getClientInterfaces();
		for(int i = 0; i < interfaces.size(); i++){
			ClientInterface inter = (ClientInterface) interfaces.get(i);
			inter.broadcastMsg(radius, msg);
		}		
	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttsGWInfo(MqttsGWInfo receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtts GWINFO message received.");

		// TODO implement this method for load balancing issues

	}


	/**
	 * @param receivedMsg
	 */
	private void handleMqttsPublish(MqttsPublish receivedMsg) {
		if(receivedMsg.getTopicIdType() == MqttsMessage.NORMAL_TOPIC_ID)
			GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(this.gatewayAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" and \"TopicId\" = \""+receivedMsg.getTopicId()+"\" received.");
		else if (receivedMsg.getTopicIdType() == MqttsMessage.PREDIFINED_TOPIC_ID)
			GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(this.gatewayAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" and \"TopicId\" = \""+receivedMsg.getTopicId()+"\" (predefined topic Id) received.");
		else
			GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(this.gatewayAddress.getAddress())+"]/["+clientId+"] - Mqtts PUBLISH message with \"QoS\" = \""+receivedMsg.getQos()+"\" and \"TopicId\" = \""+receivedMsg.getShortTopicName()+"\" (short topic name) received.");

		//construct a Mqtt PUBLISH message
		MqttPublish publish = new MqttPublish();

		//check the TopicIdType in the received Mqtts PUBLISH message
		switch(receivedMsg.getTopicIdType()){

		//if the TopicIdType is a normal TopicId
		case MqttsMessage.NORMAL_TOPIC_ID:
			GatewayLogger.log(GatewayLogger.WARN, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Topic Id type "+ receivedMsg.getTopicIdType()+" is invalid. Publish with \"QoS\" = \"-1\" supports only predefined topis Ids (topic Id type = \"1\") or short topic names (topic Id type = \"2\").");
			return;

			//if the TopicIdType is a shortTopicName then simply copy it to the topicName field of the Mqtt PUBLISH message
		case MqttsMessage.SHORT_TOPIC_NAME:
			publish.setTopicName(receivedMsg.getShortTopicName());	
			break;

			//if the TopicIdType is a predifinedTopiId
		case MqttsMessage.PREDIFINED_TOPIC_ID:
			if(receivedMsg.getTopicId() > GWParameters.getPredfTopicIdSize()){
				GatewayLogger.log(GatewayLogger.WARN, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + receivedMsg.getTopicId() + "\") of the received Mqtts PUBLISH message is out of the range of predefined topic Ids [1,"+GWParameters.getPredfTopicIdSize()+"]. The message cannot be processed.");
				return;
			}				

			//get the predefined topic name that corresponds to the received predefined topicId
			String topicName = topicIdMappingTable.getTopicName(receivedMsg.getTopicId());

			//this should not happen as predefined topic ids are already stored
			if(topicName == null){
				GatewayLogger.log(GatewayLogger.WARN, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Predefined topicId (\"" + receivedMsg.getTopicId() + "\") of the received Mqtts PUBLISH message does not exist. The message cannot be processed.");
				return;
			}
			publish.setTopicName(topicName);
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Unknown topicIdType (\"" + receivedMsg.getTopicIdType()+"\"). The received Mqtts PUBLISH message cannot be processed.");
			return;	
		}

		//populate the Mqtt PUBLISH message 
		publish.setDup(false);

		//set QoS = 0
		publish.setQos(0);
		publish.setRetain(false);

		//there is no msg id in QoS = 0 publish messages

		publish.setPayload(receivedMsg.getData());
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(this.gatewayAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt PUBLISH message with \"QoS\" = \""+publish.getQos()+"\" and \"TopicName\" = \""+publish.getTopicName()+ "\" to the broker.");

		//send the Mqtt PUBLISH message to the broker
		try {
			brokerInterface.sendMsg(publish);
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.log(GatewayLogger.ERROR, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed sending Mqtt PUBLISH message to the broker.");
			connectionLost();
		}
	}


	/******************************************************************************************/
	/**                      HANDLING OF MQTT MESSAGES FROM THE BROKER                      **/
	/****************************************************************************************/

	/**
	 * @param receivedMsg
	 */
	public void handleMqttMessage(MqttMessage receivedMsg){
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
			GatewayLogger.log(GatewayLogger.WARN, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt message of unknown type \"" + receivedMsg.getMsgType()+"\" received.");
			break;
		}
	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttConnack(MqttConnack receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - Mqtt CONNACK message received.");

		//if the return code of the Mqtt CONNACK message is not "Connection Accepted"
		if (receivedMsg.getReturnCode() != MqttMessage.RETURN_CODE_CONNECTION_ACCEPTED){
			GatewayLogger.log(GatewayLogger.ERROR, "GatewayMsgHandler ["+
					Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
					"]/["+clientId+"] - Return Code of Mqtt CONNACK message it is not \"Connection Accepted\".");
			GatewayLogger.log(GatewayLogger.ERROR, "GatewayMsgHandler ["+
					Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
					"]/["+clientId+"] - Mqtt connection with the broker cannot be established. Gateway cannot start.");
			System.exit(1);
		}

		//the connection was accepted by the broker
		GatewayLogger.info("GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - Mqtt connection established.");

		this.connected = true;

		//initialize all available client interfaces for communication with clients
		GatewayLogger.info("GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - Initializing all available Client interfaces...");

		clientInterfacesVector = new Vector<ClientInterface>();
		StringTokenizer st = new StringTokenizer(GWParameters.getClientIntString(),",");
		boolean init = false;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			String clInte = token.substring(1, token.length()-1);
			ClientInterface inter = null;
			try {				
				Class<?> cl = Class.forName(clInte);
				inter = (ClientInterface)cl.newInstance();
				inter.initialize();
				GatewayLogger.info("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - "+inter.getClass().getName()+ " initialized.");
				clientInterfacesVector.add(inter);
				init = true;
			}catch (IllegalAccessException e) {
				GatewayLogger.warn("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed to instantiate "+clInte+".");
				e.printStackTrace();
			}catch (InstantiationException e) {					
				GatewayLogger.warn("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed to instantiate "+clInte+".");
				e.printStackTrace();
			}catch (ClassNotFoundException e) {
				GatewayLogger.warn("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed to instantiate "+clInte+".");
				e.printStackTrace();
			}catch (MqttsException e) {
				GatewayLogger.warn("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed to initialize "+ inter.getClass().getName());
				e.printStackTrace();
			}
		}	

		if(!init){
			GatewayLogger.error("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed to initialize at least one Client interface.Gateway cannot start.");
			System.exit(1);
		}

		GWParameters.setClientInterfacesVector(clientInterfacesVector);

		//broadcast the Mqtts ADVERTISE message to the clients (whole network)
		GatewayLogger.info("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Broadcasting initial Mqtts ADVERTISE message...");

//		sendMqttsAdvertise();

		GatewayLogger.info("-------- Mqtts Gateway started --------");

		//Send a initial Mqtts PINGREQ message to the broker
		sendMqttPingReq();

		//set a keep alive timer for sending subsequent Mqtt PINGREQ messages to the broker
		timer.register(gatewayAddress, ControlMessage.SEND_KEEP_ALIVE_MSG, GWParameters.getKeepAlivePeriod());
	}


	/**
	 * @param receivedMsg
	 */
	private void handleMqttPingResp(MqttPingResp receivedMsg) {
		//		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt PINGRESP message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttPingReq(MqttPingReq receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt PINGREQ message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttUnsuback(MqttUnsuback receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt UNSUBACK message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttSuback(MqttSuback receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt SUBACK message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttPubComp(MqttPubComp receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt PUBCOMP message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttPubRel(MqttPubRel receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt PUBREL message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttPubRec(MqttPubRec receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt PUBREC message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttPuback(MqttPuback receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt PUBACK message received.");

		// TODO Auto-generated method stub

	}

	/**
	 * @param receivedMsg
	 */
	private void handleMqttPublish(MqttPublish receivedMsg) {
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtt PUBLISH message received.");

		// TODO Auto-generated method stub

	}

	/******************************************************************************************/
	/**                        HANDLING OF CONTROL MESSAGES AND TIMEOUTS	                **/
	/****************************************************************************************/

	/**
	 * @param receivedMsg
	 */
	public void handleControlMessage(ControlMessage receivedMsg){
		//get the type of the Control message and handle the message according to that type	
		switch(receivedMsg.getMsgType()){
		case ControlMessage.CONNECTION_LOST:
			connectionLost();
			break;	

		case ControlMessage.WAITING_WILLTOPIC_TIMEOUT:
			//we will never receive such a message 
			break;

		case ControlMessage.WAITING_WILLMSG_TIMEOUT:
			//we will never receive such a message 
			break;

		case ControlMessage.WAITING_REGACK_TIMEOUT:
			//we will never receive such a message 
			break;

		case ControlMessage.CHECK_INACTIVITY:
			//ignore it
			break;	

		case ControlMessage.SEND_KEEP_ALIVE_MSG:
			handleControlKeepAlive();
			break;		

		case ControlMessage.SHUT_DOWN:
			shutDown();
			break;			

		default:
			GatewayLogger.log(GatewayLogger.WARN, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Control message of unknown type \"" + receivedMsg.getMsgType()+"\" received.");
			break;
		}
	}	

	/**
	 * 
	 */
	private void connectionLost(){
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - Control CONNECTION_LOST message received.");

		GatewayLogger.error("GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - TCP/IP connection with the broker was lost.");

		if (this.connected){	

			//close the connection with the broker (if any)
			brokerInterface.disconnect();

			this.connected = false;

			//generate a control message 
			ControlMessage controlMsg = new ControlMessage();
			controlMsg.setMsgType(ControlMessage.SHUT_DOWN);

			//construct an "internal" message and put it to dispatcher's queue
			//@see com.ibm.zurich.mqttsgw.core.Message
			Message msg = new Message(null);
			msg.setType(Message.CONTROL_MSG);
			msg.setControlMessage(controlMsg);
			this.dispatcher.putMessage(msg);	
		}else{
			GatewayLogger.error("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed to establish Mqtt connection with the broker.Gateway cannot start.");
			System.exit(1);
		}
	}



	/**
	 * 
	 */
	private void handleControlKeepAlive() {
		//		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Control SEND_KEEP_ALIVE_MSG message received.");

		//send a Mqtts PINGREQ to the broker
		sendMqttPingReq();

		//update the advertising period counter
		advPeriodCounter = advPeriodCounter + GWParameters.getKeepAlivePeriod();
		if (advPeriodCounter >= GWParameters.getAdvPeriod()){
			//broadcast the Mqtts ADVERTISE message to the network
//			sendMqttsAdvertise();			
			advPeriodCounter = 0;
		}

		//update the clean up period counter
		checkingCounter = checkingCounter + GWParameters.getKeepAlivePeriod();
		if(checkingCounter >= GWParameters.getCkeckingPeriod ()){
			//send a check timeout message to all ClientMsgHandlers
			sendCheckInactivity();	
			checkingCounter = 0;
		}		
	}


	/**
	 * 
	 */
	private void shutDown() {		
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Control SHUT_DOWN message received.");

		//stop the reading thread of the BrokerInterface (if any)
		//(this does not have any effect to the input and output streams which remain active)
		brokerInterface.setRunning(false);

		//construct a Mqtt DISCONNECT message
		MqttDisconnect mqttDisconnect = new MqttDisconnect();

		//send the Mqtt DISCONNECT message to the broker
		//(don't bother if the sending of Mqtt DISCONNECT message to the broker was successful or not)
		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(this.gatewayAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt DISCONNECT message to the broker.");
		try {
			brokerInterface.sendMsg(mqttDisconnect);
		} catch (MqttsException e) {
			// do nothing
		}

		//close the connection with the broker
		brokerInterface.disconnect();
	}	


	/******************************************************************************************/
	/**                               OTHER METHODS              	                        **/
	/****************************************************************************************/

	/**
	 * @throws MqttsException 
	 * 
	 */
	public void connect() {
		//construct a new Mqtt CONNECT message
		MqttConnect mqttConnect = new MqttConnect();		
		mqttConnect.setProtocolName(GWParameters.getProtocolName());
		mqttConnect.setProtocolVersion (GWParameters.getProtocolVersion());
		mqttConnect.setWillRetain (GWParameters.isRetain());
		mqttConnect.setWillQoS (GWParameters.getWillQoS());
		mqttConnect.setWill (GWParameters.isWillFlag());	
		mqttConnect.setCleanStart (GWParameters.isCleanSession());
		mqttConnect.setKeepAlive(GWParameters.getKeepAlivePeriod());
		mqttConnect.setClientId (clientId);
		mqttConnect.setWillTopic (GWParameters.getWillTopic());
		mqttConnect.setWillMessage (GWParameters.getWillMessage());

		//		GatewayLogger.info("** will= " + mqttConnect.isWill() + 
		//				" willTopic= " + mqttConnect.getWillTopic() +
		//				", willMessage= " + mqttConnect.getWillMessage());

		GatewayLogger.info("GatewayMsgHandler ["+
				Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
				"]/["+clientId+"] - Establishing MQTT connection with the broker...");

		//		GatewayLogger.log(GatewayLogger.INFO, "GatewayMsgHandler ["+Utils.hexString(this.gatewayAddress.getAddress())+"]/["+clientId+"] - Sending Mqtt CONNECT message to the broker.");
		//send the Mqtt CONNECT message to the broker
		try {
			brokerInterface.sendMsg(mqttConnect);
		} catch (MqttsException e) {
			e.printStackTrace();
			GatewayLogger.error("GatewayMsgHandler ["+
					Utils.hexString(GWParameters.getGatewayAddress().getAddress())+
					"]/["+clientId+"] - Failed to establish Mqtt connection with the broker. Gateway cannot start.");
			System.exit(1);
		}
	}


	/**
	 * 
	 */
	private void sendMqttPingReq() {
		//construct a Mqtt PINGREQ message
		MqttPingReq pingreq = new MqttPingReq();

		//		GatewayLogger.info("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Sending Mqtt PINGREQ message to the broker.");

		//send the Mqtt PINGREQ message to the broker
		try {
			brokerInterface.sendMsg(pingreq);
		} catch (MqttsException e) {			
			e.printStackTrace();
			GatewayLogger.error("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Failed sending Mqtts PINGREQ message to the broker.");			
			connectionLost();
		}				
	}


	/**
	 * 
	 */
//	private void sendMqttsAdvertise() {
//		MqttsAdvertise adv = new MqttsAdvertise();
//		adv.setGwId(GWParameters.getGwId());
//		adv.setDuration(GWParameters.getAdvPeriod());
//		
//		//broadcast the message to all available interfaces
//		Vector interfaces = GWParameters.getClientInterfaces();
//		for(int i = 0; i < interfaces.size(); i++){
//			ClientInterface inter = (ClientInterface) interfaces.get(i);
//			inter.broadcastMsg(adv);
//		}	
//		
//		GatewayLogger.info("GatewayMsgHandler ["+Utils.hexString(GWParameters.getGatewayAddress().getAddress())+"]/["+clientId+"] - Mqtts ADVERTISE message was broadcasted to the network.");
//	}
	
	
	/**
	 * 
	 */
	private void sendCheckInactivity() {
		//generate a control message 
		ControlMessage controlMsg = new ControlMessage();
		controlMsg.setMsgType(ControlMessage.CHECK_INACTIVITY);

		//generate an "internal" message
		//@see com.ibm.zurich.mqttsgw.core.Message

		//addressed to all ClientMsgHandlers
		Message msg = new Message(null);

		msg.setType(Message.CONTROL_MSG);
		msg.setControlMessage(controlMsg);
		this.dispatcher.putMessage(msg);		
	}

	/**
	 * @param topicName
	 */
	//	private void sendMqttSubscribe(String topicName){

	//	}
}
