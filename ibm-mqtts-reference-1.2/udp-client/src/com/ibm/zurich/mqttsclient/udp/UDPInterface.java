/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corp.
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

package com.ibm.zurich.mqttsclient.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.ibm.zurich.mqttsclient.exceptions.MqttsException;
import com.ibm.zurich.mqttsclient.messages.Message;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsAdvertise;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsConnack;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsDisconnect;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsGWInfo;
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
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsSearchGW;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsSuback;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsUnsuback;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillMsgReq;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillMsgResp;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillTopicReq;
import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsWillTopicResp;
import com.ibm.zurich.mqttsclient.utils.ClientLogger;
import com.ibm.zurich.mqttsclient.utils.ClientParameters;
import com.ibm.zurich.mqttsclient.utils.MsgQueue;

/**
 * This class implements a UDP interface 
 */
public class UDPInterface implements Runnable {
	
	private final static int MAXUDPSIZE=65536;
	private final static int MINUDPSIZE=16; // assumed to be a "sane" value...
	public final static boolean ENCAPS=false;  //use forwarder encapsulation or not
	
	private DatagramSocket udpSocket;
	private volatile boolean running;
	private Thread readThread;
	private MsgQueue queue;
	private ClientParameters clientParms;
	private byte[] recData;


	public void initialize(MsgQueue queue, ClientParameters clientParms) throws MqttsException {
		try {
			//create the udp socket 
			udpSocket = new DatagramSocket();

			//get the queue
			this.queue = queue;
			this.clientParms = clientParms;
			// set the buffer space.
			if(this.clientParms.getMaxMqttsLength() > MAXUDPSIZE) {
				throw new IllegalArgumentException("UDP only supports packet sizes up to 64KByte!");
			}
			if(this.clientParms.getMaxMqttsLength() < MINUDPSIZE) {
				throw new IllegalArgumentException("Maximum packet size should be larger than "+MINUDPSIZE);
			}
			recData = new byte[this.clientParms.getMaxMqttsLength()];

			//create thread for reading
			this.readThread = new Thread (this, "UDPInterface");
			this.running = true;
			this.readThread.start();
		} catch (Exception e) {
			throw new MqttsException ("UDPInterface - Error initializing :" +e);
		}
	}


	public int getUdpPort() {
		return udpSocket.getLocalPort();
	}

	public void readMsg() {
		DatagramPacket packet = new DatagramPacket(recData,0, recData.length);
		try {
			packet.setLength(recData.length);
			udpSocket.receive(packet);
			
			//old encapsulation
//			byte[] data = new byte[packet.getLength()];
//			System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());					
//			byte[] clAddr = new byte[data[1]];
//			System.arraycopy(data, 2, clAddr, 0, clAddr.length);
//			byte[] mqttsData = new byte[data.length - clAddr.length - 2];
//			System.arraycopy(data, clAddr.length + 2, mqttsData, 0, mqttsData.length);
			
			//no encapsulation
//			byte[] mqttsData = new byte[packet.getLength()];
//			System.arraycopy(packet.getData(), packet.getOffset(), mqttsData, 0, packet.getLength());
			
			byte[] mqttsData = null;
			if (ENCAPS) {
				//new encapsulation spec v1.2
				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
				mqttsData = new byte[data.length - data[0]]; //data[0] contains length of encapsulation
				System.arraycopy(data, data[0], mqttsData, 0, mqttsData.length);
			} else {
				mqttsData = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), packet.getOffset(), mqttsData, 0, packet.getLength());	
			}
			
			ClientLogger.log(ClientLogger.INFO, "UDPInterface - Packet received, decoding ...");
			decodeMsg(mqttsData);
		}catch (IOException ex){
			if(this.running) {
				ex.printStackTrace();
				ClientLogger.log(ClientLogger.ERROR, "UDPInterface - An I/O error occurred while reading from the socket.");
			}
		}
	}


	public void decodeMsg(byte[] data) {
		MqttsMessage mqttsMsg = null;

		//do some checks for the received packet
		if(data == null) {
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - The received data packet is null. The packet cannot be processed.");
			return;
		}

		if(data.length < clientParms.getMinMqttsLength()) {
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts message. The received data packet is too short (length = "+data.length +"). The packet cannot be processed.");
			return;
		}

		if(data.length > clientParms.getMaxMqttsLength()){
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts message. The received data packet is too long (length = "+data.length +"). The packet cannot be processed.");
			return;

		}

		if((data[0] & 0xFF) < clientParms.getMinMqttsLength()) {
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts message. Field \"Length\" (" + (data[0] & 0xFF) + ") in the received data packet is less than "+clientParms.getMinMqttsLength()+" . The packet cannot be processed.");
			return;
		}

		if((data[0] & 0xFF) != data.length) {
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts message. Field \"Length\" in the received data packet does not match the actual length of the packet. The packet cannot be processed. " + data[0] + ", " + data.length);
			return;
		}


		int msgType = (data[1] & 0xFF);
		switch (msgType) {
		case MqttsMessage.ADVERTISE:
			if(data.length != 5) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts ADVERTISE message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsAdvertise(data);
			break;

		case MqttsMessage.SEARCHGW:
			if(data.length != 3) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts SEARCHGW message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsSearchGW(data);
			break;

		case MqttsMessage.GWINFO:
			mqttsMsg = new MqttsGWInfo(data);
			break;

		case MqttsMessage.CONNECT:
			//we should never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Strange ... a CONNECT was received, something must be wrong here ...");
			break;

		case MqttsMessage.CONNACK:
			mqttsMsg = new MqttsConnack(data);
			ClientLogger.log(ClientLogger.INFO,	"UDPInterface - CONNACK received");
			break;

		case MqttsMessage.WILLTOPICREQ:
			mqttsMsg = new MqttsWillTopicReq(data);
			break;

		case MqttsMessage.WILLTOPIC:
			//we should never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Strange ... a WILLTOPIC was received, something must be wrong here ...");
			break;

		case MqttsMessage.WILLMSGREQ:
			mqttsMsg = new MqttsWillMsgReq(data);
			break;

		case MqttsMessage.WILLMSG:
			//we should never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Strange ... a WILLMSG was received, something must be wrong here ...");
			break;

		case MqttsMessage.REGISTER:
			if(data.length < 7) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts REGISTER message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsRegister(data);
			break;

		case MqttsMessage.REGACK:
			if(data.length != 7) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts REGACK message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsRegack(data);
			break;

		case MqttsMessage.PUBLISH:
			if(data.length < 8) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts PUBLISH message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPublish(data);
			break;

		case MqttsMessage.PUBACK:
			if(data.length != 7) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts PUBACK message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPuback(data);
			break;

		case MqttsMessage.PUBCOMP:
			if(data.length != 4) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts PUBCOMP message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPubComp(data);
			break;

		case MqttsMessage.PUBREC:
			if(data.length != 4) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts PUBREC message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPubRec(data);
			break;

		case MqttsMessage.PUBREL:
			if(data.length != 4) {
				ClientLogger.log(ClientLogger.WARN, "UDPInterface - Not a valid Mqtts PUBREL message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPubRel(data);
			break;

		case MqttsMessage.SUBSCRIBE:
			//we should never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Strange ... a SUBSCRIBE was received, something must be wrong here ...");
			break;

		case MqttsMessage.SUBACK:
			mqttsMsg = new MqttsSuback(data);
			break;

		case MqttsMessage.UNSUBSCRIBE :
			//we should never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Strange ... a UNSUBSCRIBE was received, something must be wrong here ...");
			break;

		case MqttsMessage.UNSUBACK:
			mqttsMsg = new MqttsUnsuback(data);
			break;

		case MqttsMessage.PINGREQ:
			mqttsMsg = new MqttsPingReq(data);
			break;

		case MqttsMessage.PINGRESP:
			mqttsMsg = new MqttsPingResp(data);
			break;

		case MqttsMessage.DISCONNECT :
			mqttsMsg = new MqttsDisconnect(data);
			break;

		case MqttsMessage.WILLTOPICUPD:
			//we should never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Strange ... a WILLTOPICUPD was received, something must be wrong here ...");
			break;

		case MqttsMessage.WILLTOPICRESP:
			mqttsMsg = new MqttsWillTopicResp(data);
			break;

		case MqttsMessage.WILLMSGUPD:
			//we should never receive such a message from the gateway
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Strange ... a WILLMSGUPD was received, something must be wrong here ...");
			break;

		case MqttsMessage.WILLMSGRESP:
			mqttsMsg = new MqttsWillMsgResp(data);
			break;

		default:
			ClientLogger.log(ClientLogger.WARN, "UDPInterface - Mqtts message of unknown type \"" + msgType+"\" received.");
			return;
		}

		//put the message to the queue
		Message msg = new Message();
		msg.setType(Message.MQTTS_MSG);
		msg.setMqttsMessage(mqttsMsg);
		this.queue.addLast(msg);
		ClientLogger.log(ClientLogger.INFO, "UDPInterface - Mqtts message \"" + msgType+"\" put in queue.");
	}

	public void sendMsg(MqttsMessage msg) {
		try {

//old encapsulation spec v1.1			
//			//cll: temp modification for testing reasons
//			//byte[] ipAddr = InetAddress.getLocalHost().getAddress();
//			byte[] ipAddr = new byte[] {127,0,0,1};
//			byte[] port = new byte[2];
//			
//			int udpPort = this.udpSocket.getLocalPort(); 
//			port[0] = (byte)((udpPort >> 8) & 0xFF);
//			port[1] = (byte) ( udpPort & 0xFF);
//			
//			byte[] clientAddr = new byte [ipAddr.length + port.length];
//			System.arraycopy(ipAddr, 0, clientAddr, 0, ipAddr.length);
//			System.arraycopy(port, 0, clientAddr, ipAddr.length, port.length);
//			
//			byte[] wireMsg = msg.toBytes();
//			byte[] data = new byte[wireMsg.length + clientAddr.length + 2];
//			data[0] = (byte)0x00;
//			data[1] = (byte)clientAddr.length;
//			System.arraycopy(clientAddr, 0, data, 2, clientAddr.length);
//			System.arraycopy(wireMsg,  0, data, clientAddr.length + 2, wireMsg.length);
//			DatagramPacket packet = new DatagramPacket(data, data.length, clientParms.getGatewayAddress(), clientParms.getGatewayPort());
//end old encapsulation
			
			if (ENCAPS) { //new encapsulation acc. spec 1.2
				byte[] ipAddr = InetAddress.getLocalHost().getAddress();
				//byte[] ipAddr = new byte[] {127,0,0,1};
				byte[] port = new byte[2];
				int udpPort = this.udpSocket.getLocalPort(); 
				port[0] = (byte)((udpPort >> 8) & 0xFF);
				port[1] = (byte) ( udpPort & 0xFF);
				byte[] wirelessNodeId = new byte [ipAddr.length + port.length];
				System.arraycopy(ipAddr, 0, wirelessNodeId, 0, ipAddr.length);
				System.arraycopy(port, 0, wirelessNodeId, ipAddr.length, port.length);

				byte[] wireMsg = msg.toBytes();
				byte[] data = new byte[wireMsg.length + wirelessNodeId.length + 3];
				data[0] = (byte)(wirelessNodeId.length+3);
				data[1] = (byte)0xFE;
				data[3] = 0x00;
				System.arraycopy(wirelessNodeId, 0, data, 3, wirelessNodeId.length);
				System.arraycopy(wireMsg,  0, data, wirelessNodeId.length + 3, wireMsg.length);
				DatagramPacket packet = new DatagramPacket(data, data.length, clientParms.getGatewayAddress(), clientParms.getGatewayPort());
				udpSocket.send(packet);
			} else {  //no encapsulation
				byte[] wireMsg = msg.toBytes();			
				DatagramPacket packet = new DatagramPacket(wireMsg, wireMsg.length, clientParms.getGatewayAddress(), clientParms.getGatewayPort());
				udpSocket.send(packet);
			}
		} catch (IOException e) {
			e.printStackTrace();
			ClientLogger.log(ClientLogger.ERROR, "UDPInterface - Error while writing on the UDP socket.");
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

	public void terminate() {
		this.running=false;
		// close socket.
		this.udpSocket.close();
		try {
			this.readThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}