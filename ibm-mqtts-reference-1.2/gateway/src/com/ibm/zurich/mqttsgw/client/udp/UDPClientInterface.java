/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.client.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import java.net.SocketException;
import java.util.Vector;

import com.ibm.zurich.mqttsgw.client.ClientInterface;
//import com.ibm.zurich.mqttsgw.client.udp.UDPClientInterface.Forwarder;
import com.ibm.zurich.mqttsgw.core.Dispatcher;
import com.ibm.zurich.mqttsgw.exceptions.MqttsException;
import com.ibm.zurich.mqttsgw.messages.Message;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsAdvertise;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsConnect;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsDisconnect;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsGWInfo;
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
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsSubscribe;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsUnsubscribe;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillMsg;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillMsgUpd;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillTopic;
import com.ibm.zurich.mqttsgw.messages.mqtts.MqttsWillTopicUpd;
import com.ibm.zurich.mqttsgw.utils.ClientAddress;
import com.ibm.zurich.mqttsgw.utils.GWParameters;
import com.ibm.zurich.mqttsgw.utils.GatewayLogger;
import com.ibm.zurich.mqttsgw.utils.Utils;

/**
 * This class implements a UDP interface to Mqtts clients.Implements the 
 * interface {@link com.ibm.zurich.mqttsgw.client.ClientInterface}.
 * For the reading functionality a reading thread is created. 
 * There is only one instance of this class. 
 * 
 */
public class UDPClientInterface implements ClientInterface, Runnable {

	private DatagramSocket udpSocket;
	private volatile boolean running;
	private Thread readThread;
	private Vector<Forwarder> forwarders;
	private Dispatcher dispatcher;
	private byte[] recData = new byte[512];


	/**
	 * This method initializes the interface.It creates an new UDP socket and
	 * a new thread for reading from the socket.
	 * @throws MqttsException 
	 */
	public void initialize() throws MqttsException {
		try {
			//create the udp socket 
			udpSocket = new DatagramSocket(GWParameters.getUdpPort());

			//get the Dispatcher
			dispatcher = Dispatcher.getInstance();

			forwarders = new Vector<Forwarder>();

			//create thread for reading
			this.readThread = new Thread (this, "UDPClientInterface");
			this.running = true;
			this.readThread.start();
		} catch (Exception e) {
			throw new MqttsException ("UDPClientInterface - Error initializing :" +e);
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.client.ClientInterface#broadcastMsg(com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage)
	 */
	public void broadcastMsg(MqttsMessage msg) {
		//		GatewayLogger.log(GatewayLogger.INFO, "UDPClientInterface - Broadcasting Mqtts \"" +Utils.hexString(msg.toBytes())+"\" message to the network.");
		for(int i = forwarders.size() - 1; i >= 0; i--) {
			Forwarder fr = (Forwarder)forwarders.get(i);
			//check also if this forwarder is inactive
			if (System.currentTimeMillis() > fr.timeout)
				forwarders.remove(i);
			else{
				try {
					byte[] wireMsg = msg.toBytes();
					byte[] data = new byte[wireMsg.length + 2];
					data[0] = (byte)0x00;//0x00 means broadcast to all network
					data[1] = (byte)0x00;
					System.arraycopy(wireMsg, 0, data, 2, wireMsg.length);
					DatagramPacket packet = new DatagramPacket(data, data.length, fr.addr, fr.port);
					udpSocket.send(packet);
				} catch (IOException e) {
					GatewayLogger.log(GatewayLogger.ERROR, "UDPClientInterface - Error while writing on the UDP socket.");
				}
			}
		}
	}		

	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.client.ClientInterface#broadcastMsg(int, com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage)
	 */
	public void broadcastMsg(int radius, MqttsMessage msg) {
		//		GatewayLogger.log(GatewayLogger.INFO, "UDPClientInterface - Broadcasting Mqtts \"" +Utils.hexString(msg.toBytes())+"\" message to the network with broadcast radius "+radius+".");
		for(int i = forwarders.size() - 1; i >= 0; i--) {
			Forwarder fr = (Forwarder)forwarders.get(i);
			//check also if this forwarder is inactive
			if (System.currentTimeMillis() > fr.timeout)
				forwarders.remove(i);
			else{
				try {
					byte[] wireMsg = msg.toBytes();
					byte[] data = new byte[wireMsg.length + 2];
					data[0] = (byte)radius;//broadcast to the specified radius
					data[1] = (byte)0x00;
					System.arraycopy(wireMsg, 0, data, 2, wireMsg.length);
					DatagramPacket packet = new DatagramPacket(data, data.length, fr.addr, fr.port);
					udpSocket.send(packet);
				} catch (IOException e) {
					GatewayLogger.log(GatewayLogger.ERROR, "UDPClientInterface - Error while writing on the UDP socket.");
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.client.ClientInterface#readMsg()
	 */
	public void readMsg() {
		DatagramPacket packet = new DatagramPacket(recData,0, recData.length);
		try {
			packet.setLength(recData.length);
			udpSocket.receive(packet);

			//add the forwarder from which we received the message to the list
			//if it is already on the list just update its timeout
			Forwarder forw = new Forwarder();
			forw.addr = packet.getAddress();
			forw.port = packet.getPort();
			forw.timeout = System.currentTimeMillis() + GWParameters.getForwarderTimeout()*1000;
			//GatewayLogger.log(GatewayLogger.INFO, "UDPClientInterface -  New forwarder:addr = " + forw.addr+ " port = "+forw.port);

			boolean found = false;
			for(int i = 0 ; i < forwarders.size(); i++) {
				Forwarder fr = (Forwarder)forwarders.get(i);
				if(forw.equals(fr)){
					found = true;
					fr.timeout = System.currentTimeMillis() + GWParameters.getForwarderTimeout()*1000;
					break;
				}				
			}			
			if (!found) forwarders.add(forw);
		
//			if(packet.getLength() > 3) { // not a keep alive packet
				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
				
				//old encaps v1.1
//				byte[] clAddr = new byte[data[1]];  //data[1] contains length of clAddr (wireless node id)
//				System.arraycopy(data, 2, clAddr, 0, clAddr.length);
//				ClientAddress address = new ClientAddress(clAddr, packet.getAddress(), packet.getPort());
//				byte[] mqttsData = new byte[data.length - clAddr.length - 2];
//				System.arraycopy(data, clAddr.length + 2, mqttsData, 0, mqttsData.length);
				//end old encaps v1.1
				
				byte[] mqttsData = null;
				ClientAddress address = null;
				
				if (data[0] == (byte)0x00) {  //old encaps v 1.1
					byte[] clAddr = new byte[data[1]];  //data[1] contains length of clAddr (wireless node id)
					System.arraycopy(data, 2, clAddr, 0, clAddr.length);
					byte[] encaps = new byte[data[1]+2];
					System.arraycopy(data, 0, encaps, 0, encaps.length);
					address = new ClientAddress(clAddr, packet.getAddress(), packet.getPort(), true, encaps);
					mqttsData = new byte[data.length - clAddr.length - 2];
					System.arraycopy(data, clAddr.length + 2, mqttsData, 0, mqttsData.length);
				} else if (data[1] == (byte)MqttsMessage.ENCAPSMSG) { //new encaps v1.2
					//we have an encapsulated msg
					byte[] clAddr = new byte[((int)data[0]&0xFF) - 3];  //data[0]: length of encaps
					System.arraycopy(data, 3, clAddr, 0, clAddr.length);
					byte[] encaps = new byte[data[0]];
					System.arraycopy(data, 0, encaps, 0, encaps.length);
					address = new ClientAddress(clAddr, packet.getAddress(), packet.getPort(), true, encaps);
					mqttsData = new byte[(int)data[data[0]]];
					System.arraycopy(data, data[0], mqttsData, 0, mqttsData.length);
				} else {
					//we have a non-encapsulated mqtts msg
					//we will create an address out of the forwarder address
					byte[] a1 = packet.getAddress().getAddress();
					byte[] a2 = new byte[2];
					a2[0] = (byte)((packet.getPort() >> 8) & 0xFF);
					a2[1] = (byte) ( packet.getPort() & 0xFF);
					byte[] clAddr = new byte[a1.length+a2.length];
					System.arraycopy(a1, 0, clAddr, 0, a1.length);
					System.arraycopy(a2, 0, clAddr, a1.length, a2.length);
					address = new ClientAddress(clAddr, packet.getAddress(), packet.getPort(), false, null);
					mqttsData = new byte[(int)data[0]];
					System.arraycopy(data, 0, mqttsData, 0, mqttsData.length);
				}
				
				
//start-just for the testing purposes we simulate here a network delay
//This will NOT be included in the final version
//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

				//end
				decodeMsg(mqttsData,address);
//			}
		}catch (IOException ex){
			ex.printStackTrace();
			GatewayLogger.log(GatewayLogger.ERROR, "UDPClientInterface - An I/O error occurred while reading from the socket.");
		}
	}

	/**
	 * This method decodes the received Mqtts message and then constructs a 
	 * general "internal" message {@link com.ibm.zurich.mqttsgw.messages.Message}
	 * which puts it to Dispatcher's queue {@link  com.ibm.zurich.mqttsgw.core.Dispatcher.
	 * 
	 * @param data The received Mqtts packet.
	 * @param address The address of the SA client.
	 */
	public void decodeMsg(byte[] data, ClientAddress address) {
		MqttsMessage mqttsMsg = null;

		//do some checks for the received packet
		if(data == null) {
			GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - The received data packet is null. The packet cannot be processed.");
			return;
		}

		if(data.length < GWParameters.getMinMqttsLength()) {
			GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts message. The received data packet is too short (length = "+data.length +"). The packet cannot be processed.");
			return;
		}

		if(data.length > GWParameters.getMaxMqttsLength()){
			GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts message. The received data packet is too long (length = "+data.length +"). The packet cannot be processed.");
			return;

		}

		if(data[0] < GWParameters.getMinMqttsLength()) {
			GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts message. Field \"Length\" in the received data packet is less than "+GWParameters.getMinMqttsLength()+" . The packet cannot be processed.");
			return;
		}

		if(data[0] != data.length) {
			GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts message. Field \"Length\" in the received data packet does not match the actual length of the packet. The packet cannot be processed.");
			return;
		}


		int msgType = (data[1] & 0xFF);
		switch (msgType) {
		case MqttsMessage.ADVERTISE:
			if(data.length != 5) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts ADVERTISE message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsAdvertise(data);
			//TODO Handle this case for load balancing issues
			break;

		case MqttsMessage.SEARCHGW:
			if(data.length != 3) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts SEARCHGW message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsSearchGW(data);
			break;

		case MqttsMessage.GWINFO:
			mqttsMsg = new MqttsGWInfo(data);
			//TODO Handle this case for load balancing issues
			break;

		case MqttsMessage.CONNECT:
			if(data.length < 7) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts CONNECT message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsConnect(data);
			break;

		case MqttsMessage.CONNACK:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLTOPICREQ:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLTOPIC:
			if(data.length < 2) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts WILLTOPIC message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsWillTopic(data);
			break;

		case MqttsMessage.WILLMSGREQ:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLMSG:
			if(data.length < 3) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts WILLMSG message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsWillMsg(data);
			break;

		case MqttsMessage.REGISTER:
			if(data.length < 7) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts REGISTER message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsRegister(data);
			break;

		case MqttsMessage.REGACK:
			if(data.length != 7) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts REGACK message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsRegack(data);
			break;

		case MqttsMessage.PUBLISH:
			if(data.length < 8) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts PUBLISH message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPublish(data);
			break;

		case MqttsMessage.PUBACK:
			if(data.length != 7) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts PUBACK message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPuback(data);
			break;

		case MqttsMessage.PUBCOMP:
			if(data.length != 4) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts PUBCOMP message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPubComp(data);
			break;

		case MqttsMessage.PUBREC:
			if(data.length != 4) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts PUBREC message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPubRec(data);
			break;

		case MqttsMessage.PUBREL:
			if(data.length != 4) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts PUBREL message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}
			mqttsMsg = new MqttsPubRel(data);
			break;

		case MqttsMessage.SUBSCRIBE:
			if(data.length < 6) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts SUBSCRIBE message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}

			try {
				mqttsMsg = new MqttsSubscribe(data);
			} catch (MqttsException e) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts SUBSCRIBE message. "+e.getMessage());
				return;
			}
			break;

		case MqttsMessage.SUBACK:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.UNSUBSCRIBE :
			if(data.length < 6) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts UNSUBSCRIBE message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}

			try {
				mqttsMsg = new MqttsUnsubscribe(data);
			} catch (MqttsException e) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts UNSUBSCRIBE message. "+e.getMessage());
				return;
			}
			break;

		case MqttsMessage.UNSUBACK:
			//we will never receive such a message from the client
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
			if(data.length < 2) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts WILLTOPICUPD message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}

			mqttsMsg = new MqttsWillTopicUpd(data);
			break;

		case MqttsMessage.WILLTOPICRESP:
			//we will never receive such a message from the client
			break;

		case MqttsMessage.WILLMSGUPD:
			if(data.length < 3) {
				GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Not a valid Mqtts WILLMSGUPD message. Wrong packet length (length = "+data.length +"). The packet cannot be processed.");
				return;
			}

			mqttsMsg = new MqttsWillMsgUpd(data);
			break;

		case MqttsMessage.WILLMSGRESP:
			//we will never receive such a message from the client
			break;

		default:
			GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - Mqtts message of unknown type \"" + msgType+"\" received.");
			return;
		}

		//construct an "internal" message and put it to dispatcher's queue
		Message msg = new Message(address);
		msg.setType(Message.MQTTS_MSG);
		msg.setMqttsMessage(mqttsMsg);
		msg.setClientInterface(this);
		this.dispatcher.putMessage(msg);		
	}

	/* (non-Javadoc)
	 * @see com.ibm.zurich.mqttsgw.client.ClientInterface#sendMsg(com.ibm.zurich.mqttsgw.utils.SAaddress, com.ibm.zurich.mqttsgw.messages.mqtts.MqttsMessage)
	 */
	public void sendMsg(ClientAddress address, MqttsMessage msg) {
		//		GatewayLogger.log(GatewayLogger.INFO, "UDPClientInterface - Sending Mqtts \"" + Utils.hexString(msg.toBytes())+ "\" message to the client with address \"" +Utils.hexString(address.getAddress())+"\".");

		if(address == null) {
			GatewayLogger.log(GatewayLogger.WARN, "UDPClientInterface - The address of the receiver is null.The Mqtts message " + Utils.hexString(msg.toBytes())+ " cannot be sent.");
			return;
		}

		try {
			byte[] addr = address.getAddress();
			byte[] wireMsg = msg.toBytes();
			
			//old encaps v1.1
//			byte[] data = new byte[wireMsg.length + addr.length + 2];
//			data[0] = (byte)0x00;
//			data[1] = (byte)addr.length;
//			System.arraycopy(addr, 0, data, 2, addr.length);
//			System.arraycopy(wireMsg,  0, data, addr.length + 2, wireMsg.length);
			//end old encaps v1.1
			
			//new encaps v1.2
			byte[] data = null;
			if (address.isEncaps()) {
//				byte[] encaps = new byte[3+addr.length];
//				encaps[0] = (byte)(addr.length + 3);
//				encaps[1] = (byte) MqttsMessage.ENCAPSMSG;
//				encaps[2] = 0x00;
				byte[] encaps = address.getEncaps();
				System.arraycopy(addr, 0, encaps, 0, addr.length);
				data = new byte[encaps.length + wireMsg.length];
				System.arraycopy(encaps, 0, data, 0, encaps.length);
				System.arraycopy(wireMsg, 0, data, encaps.length, wireMsg.length);				
			} else {
				data = wireMsg;
			}
			//end new encaps v1.2
			
			DatagramPacket packet = new DatagramPacket(data, data.length, address.getIPaddress(), address.getPort());
			udpSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			GatewayLogger.log(GatewayLogger.ERROR, "UDPClientInterface - Error while writing on the UDP socket.");
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
	 * This class represents a forwarder which is defined in the specifications of the
	 * Mqtts protocol.
	 */
	public static class Forwarder {
		private InetAddress addr = null;
		private int port = 0;
		private long timeout = 0;

		public boolean equals(Object o) {
			boolean same = false;
			if(o == null) {
				same = false;
			} else if(o instanceof Forwarder) {
				Forwarder fr = (Forwarder)o;
				if(addr != null && addr.equals(fr.addr) && fr.port == port) {
					same = true;
				}
			}
			return same;
		}
	}
}