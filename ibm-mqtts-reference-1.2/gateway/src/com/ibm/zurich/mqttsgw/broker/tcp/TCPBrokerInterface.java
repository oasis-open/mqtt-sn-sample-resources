/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.broker.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.ibm.zurich.mqttsgw.broker.BrokerInterface;
import com.ibm.zurich.mqttsgw.core.Dispatcher;
import com.ibm.zurich.mqttsgw.exceptions.MqttsException;
import com.ibm.zurich.mqttsgw.messages.Message;
import com.ibm.zurich.mqttsgw.messages.control.ControlMessage;
import com.ibm.zurich.mqttsgw.messages.mqtt.MqttConnack;
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
import com.ibm.zurich.mqttsgw.utils.Address;
import com.ibm.zurich.mqttsgw.utils.GWParameters;
import com.ibm.zurich.mqttsgw.utils.GatewayLogger;
import com.ibm.zurich.mqttsgw.utils.Utils;

/**
 * This class represents the interface to the broker and is instantiated by the 
 * MessageHandler.Is is used for opening a TCP/IP connection with the broker 
 * and sending/receiving Mqtt Messages.
 * For the reading functionality a reading thread is created. 
 * For every client there is one instance of this class. 
 * 
 * @see com.ibm.zurich.core.ClientMsgHandler
 * 
 * Parts of this code were imported from com.ibm.mqttdirect.modules.common.StreamDeframer.java
 * 
 *
 */
public class TCPBrokerInterface implements BrokerInterface, Runnable {

	private	DataInputStream	 streamIn	= null;		
	private	DataOutputStream streamOut	= null;
	private Socket socket;

	private Address address;
	private String brokerURL;
	private int port;
	private String clientId;

	private volatile boolean running;	
	private Thread readThread;

	private Dispatcher dispatcher;

	//the maximum length of a Mqtt fixed header
	public static final int MAX_HDR_LENGTH = 5;

	//the maximum length of the remaining part of a Mqtt message
	public static final int MAX_MSG_LENGTH = 268435455;


	/**
	 * Constructor of the broker interface.
	 */
	public TCPBrokerInterface(Address address) {
		this.address = address;
		this.brokerURL = GWParameters.getBrokerURL();
		this.port = GWParameters.getBrokerTcpPort();
		this.running = false;
		this.readThread = null;
		this.dispatcher = Dispatcher.getInstance();
	}

	/**
	 * This method opens the TCP/IP connection with the broker and creates 
	 * a new thread for reading from the socket.
	 * 
	 * @throws MqttsException 
	 */
	public void initialize() throws MqttsException{
		try {
			socket = new Socket(brokerURL, port);
			streamIn = new DataInputStream(socket.getInputStream());
			streamOut = new DataOutputStream(socket.getOutputStream());			

		} catch (UnknownHostException e) {
			disconnect();
			throw new MqttsException(e.getMessage());
		} catch (IOException e) {
			disconnect();			
			throw new MqttsException(e.getMessage());
		}

		//create thread for reading
		this.readThread = new Thread (this, "BrokerInterface");
		this.running = true;
		this.readThread.start();
	}


	/**
	 * This method sends a Mqtt message to the broker over the already established 
	 * TCP/IP connection.Before that, converts the message to byte array calling
	 * the method {@link com.ibm.zurich.mqttsgw.messages.mqtt.MqttMessage#toBytes()}.
	 * 
	 * @param message The MqttMessage to be send to the broker.
	 * @throws MqttsException 
	 */
	public void sendMsg(MqttMessage message) throws MqttsException{
		// send the message over the TCP/IP socket
		if (this.streamOut != null) {
			try {
				//System.out.println(">> sending msg: " + Utils.hexString(message.toBytes()));
				this.streamOut.write(message.toBytes());
				this.streamOut.flush();
			} catch (IOException e) {
				disconnect();
				throw new MqttsException(e.getMessage());
			}
		}else{
			disconnect();
			throw new MqttsException("Writing stream is null!");
		}
	}

	/**
	 * This method is used for reading a Mqtt message from the socket.It blocks on the 
	 * reading stream until a message arrives.
	 */
	public void readMsg(){
		byte [] body = null;

		// read the header from the input stream
		MqttHeader hdr = new MqttHeader();
		hdr.header = new byte[MAX_HDR_LENGTH];

		if (this.streamIn == null){
			return;
		}

		try{
			int res = streamIn.read();
			hdr.header[0]=(byte) res;
			hdr.headerLength=1; 
			if(res==-1) {
				// if EOF detected
				throw new EOFException();
			}
			// read the Mqtt length
			int multiplier = 1;
			hdr.remainingLength=0;
			do {
				//read MsgLength bytes
				res = streamIn.read();
				if(res==-1) {
					// if EOF detected.
					throw new EOFException();
				}
				hdr.header[hdr.headerLength++] = (byte) res;
				hdr.remainingLength += (res & 127) * multiplier;
				multiplier *= 128;
			} while ((res & 128) != 0 && hdr.headerLength<MAX_HDR_LENGTH);

			//some checks
			if (hdr.headerLength > MAX_HDR_LENGTH || hdr.remainingLength > MAX_MSG_LENGTH || hdr.remainingLength < 0) {
				GatewayLogger.log(GatewayLogger.WARN, "TCPBrokerInterface ["+Utils.hexString(this.address.getAddress())+"]/["+clientId+"] - Not a valid Mqtts message.");
				return;
			}			

			body = new byte[hdr.remainingLength+hdr.headerLength]; 

			for (int i = 0; i < hdr.headerLength; i++) {
				body[i] = hdr.header[i]; 
			}

			if (hdr.remainingLength >= 0) {
				streamIn.readFully(body, hdr.headerLength, hdr.remainingLength);
			}


			//start:just for the testing purposes we simulate here a network delay
			//TODO This will NOT be included in the final version
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//end			


			if(body!=null)
				decodeMsg(body);
		}catch(IOException e){
			if(e instanceof InterruptedIOException) {
				//do nothing
			}else if(this.running == true){
				//an error occurred
				//stop the reading thread
				this.running = false;

				//generate a control message 
				ControlMessage controlMsg = new ControlMessage();
				controlMsg.setMsgType(ControlMessage.CONNECTION_LOST);

				//construct an "internal" message and put it to dispatcher's queue
				//@see com.ibm.zurich.mqttsgw.core.Message
				Message msg = new Message(this.address);
				msg.setType(Message.CONTROL_MSG);
				msg.setControlMessage(controlMsg);
				this.dispatcher.putMessage(msg);
			}
		} 
	}

	/**
	 * This method is used for decoding the received Mqtt message from the broker.
	 * @param data The Mqtt message as it was received from the socket (byte array).
	 */
	private void decodeMsg(byte[] data){
		MqttMessage mqttMsg = null;
		int msgType = (data[0] >>> 4) & 0x0F;
		switch (msgType) {
		case MqttMessage.CONNECT:
			// we will never receive such a message from the broker
			break;

		case MqttMessage.CONNACK:
			mqttMsg = new MqttConnack(data);
			break;

		case MqttMessage.PUBLISH:
			mqttMsg = new MqttPublish(data);
			break;

		case MqttMessage.PUBACK:
			mqttMsg = new MqttPuback(data);
			break;

		case MqttMessage.PUBREC:
			mqttMsg = new MqttPubRec(data);
			break;

		case MqttMessage.PUBREL:
			mqttMsg = new MqttPubRel(data);
			break;

		case MqttMessage.PUBCOMP:
			mqttMsg = new MqttPubComp(data);
			break;

		case MqttMessage.SUBSCRIBE:
			//we will never receive such a message from the broker
			break;

		case MqttMessage.SUBACK:
			mqttMsg = new MqttSuback(data);
			break;

		case MqttMessage.UNSUBSCRIBE:
			//we will never receive such a message from the broker
			break;

		case MqttMessage.UNSUBACK:
			mqttMsg = new MqttUnsuback(data);
			break;

		case MqttMessage.PINGREQ:
			mqttMsg = new MqttPingReq(data);
			break;

		case MqttMessage.PINGRESP:
			mqttMsg = new MqttPingResp(data);
			break;

		case MqttMessage.DISCONNECT:
			//we will never receive such a message from the broker
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "TCPBrokerInterface ["+Utils.hexString(this.address.getAddress())+"]/["+clientId+"] - Mqtt message of unknown type \"" + msgType+"\" received.");
			break;				
		}

		//construct an "internal" message and put it to dispatcher's queue
		//@see com.ibm.zurich.mqttsgw.core.Message
		Message msg = new Message(this.address);
		msg.setType(Message.MQTT_MSG);
		msg.setMqttMessage(mqttMsg);
		this.dispatcher.putMessage(msg);		
	}


	/**
	 * This method is used to close the TCP/IP connection with the broker.
	 */
	public void disconnect() {
		//stop the reading thread (if any)
		this.running = false;

		//close the out stream
		if (this.streamOut != null) {
			try {
				this.streamOut.flush();
				this.streamOut.close();
			} catch (IOException e) {
				// ignore it
			}
			this.streamOut = null;
		}	

		//close the in stream
		if (this.streamIn != null) {
			try {
				this.streamIn.close();
			} catch (IOException e) {
				// ignore it
			}
			streamIn = null;
		}	

		//close the socket
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
			socket = null;
		}
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (running) {
			readMsg();
		}
	}


	/**
	 * @param running
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}	

	/**
	 * @param clientId
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}


	/**
	 * This class represents a Mqtt header and is used for decoding a Mqtt message
	 * from the broker.
	 */
	public static class MqttHeader {
		public byte[]	header;
		public int remainingLength;
		public int headerLength;
	}
}    	