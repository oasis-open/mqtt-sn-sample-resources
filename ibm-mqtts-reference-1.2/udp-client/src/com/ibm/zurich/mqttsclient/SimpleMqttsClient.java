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

import java.util.Hashtable;

import com.ibm.zurich.mqttsclient.messages.mqtts.MqttsMessage;
import com.ibm.zurich.mqttsclient.utils.ClientLogger;

public class SimpleMqttsClient implements MqttsCallbackPreDefinedTopicId {
	private MqttsClient client = null;
	private Hashtable<Integer, String> tableIdTopic = new Hashtable<Integer, String>();
	private Hashtable<String, Integer> tableTopicId = new Hashtable<String, Integer>();
	private volatile boolean waitConnect = false;
	private volatile boolean waitRegAck = false;
	private volatile boolean waitPubAck = false;
	private String regTopic = null;
	private volatile boolean waitSubAck = false;
	private volatile boolean waitUnSubAck = false;
	private String subTopic = null;
	private SimpleMqttsCallback callback = null;

	private boolean connected = false;

	public SimpleMqttsClient(String gw) {
		this(gw, 20000,false);
	}

	public SimpleMqttsClient(String gw, int port) {
		this(gw, port, false);
	}

	public SimpleMqttsClient(String gw, int port, boolean auto) {
		client = new MqttsClient(gw, port, auto);
		client.registerHandler(this);
	}

	public SimpleMqttsClient(String gatewayAddress, int gatewayPort,
			int maxMqttsMsgLength, int minMqttsMsgLength, 
			int maxRetries, int ackWaitingTime,
			boolean autoReconnect) {
		client = new MqttsClient(gatewayAddress, gatewayPort, maxMqttsMsgLength,
				minMqttsMsgLength, maxRetries, ackWaitingTime, autoReconnect);
		client.registerHandler(this);
	}

	public void setCallback(SimpleMqttsCallback callback) {
		this.callback = callback;
	}

	public void connect() {
		connect("" + System.currentTimeMillis());
	}

	public void connect(String clientId) {
		boolean cleanstart = true;
		int keepAlive = 120;
		connect(clientId, cleanstart, keepAlive);
	}

	public void connect(String clientId, boolean cleanStart, int keepAlive) {
		synchronized(this) {
			waitConnect = true;
			client.connect(clientId, cleanStart, (short)keepAlive);
			while(waitConnect) {
				try { wait(); } catch(InterruptedException ie) {}
			}
		}
	}

	public void connect(String clientId, boolean cleanStart, int keepAlive, String willTopic, int willQos, String willMsg, boolean willRetained) {
		synchronized(this) {
			waitConnect = true;
			client.connect(clientId, cleanStart, (short)keepAlive, willTopic,willQos,willMsg,willRetained);
			while(waitConnect) {
				try { wait(); } catch(InterruptedException ie) {}
			}
		}
	}

	//callback when CONNECT is sent to broker
	public void connectSent() {	
		ClientLogger.info("SimpleClient>> connecting ...");
	}

	//callback when CONNACK is received
	public void connected() {
		ClientLogger.info("SimpleClient>> CONNECT ACK received ...");
		synchronized(this) {
			waitConnect = false;
			connected = true;
			notifyAll();
		}
	}

	//ask client to send a DISCONNECT
	public void disconnect() {
		client.disconnect();
		connected = false;
	}

	//disconnected from broker
	public void disconnected(int returnCode) {
		connected = false;
		tableIdTopic.clear();
		tableTopicId.clear();
		synchronized(this) {
			if(waitConnect)  waitConnect  = false;
			if(waitRegAck)   waitRegAck   = false;
			if(waitSubAck)   waitSubAck   = false;
			if(waitUnSubAck) waitUnSubAck = false;
			notifyAll();
		}
		callback.disconnected(returnCode);
	}

	// TODO: not safe for use with multiple threads!
	public void subscribe(String topic) {
		if(connected) subscribe(topic, 0);
	}

	public void subscribe(String topic, int qos) {
		if(!connected) return;
		synchronized(this) {
			waitSubAck = true;
			subTopic = topic;
			client.subscribe(topic, qos, 0);
			while(waitSubAck) {
				try { wait(); } catch(InterruptedException ie) {}
			}
		}
	}

	public void subackReceived(int grantedQos, int topicId, int returnCode) {
		synchronized(this) {
			if(returnCode == MqttsMessage.RETURN_CODE_ACCEPTED) {
				tableIdTopic.put(new Integer(topicId), this.subTopic);
				tableTopicId.put(this.subTopic, new Integer(topicId));
			} else {
				ClientLogger.warn("SimpleClient>> subscribe rejected ...");
			}
			waitSubAck = false;
			notifyAll();
		}
	}

	public void unsubscribe(String topic) {
		if(!connected) return;
		synchronized(this) {
			waitUnSubAck = true;
			client.unSubscribe(topic, MqttsMessage.TOPIC_NAME);
			while(waitUnSubAck) {
				try { wait(); } catch(InterruptedException ie) {}
			}
		}
	}

	public void unsubackReceived() {	
		synchronized(this) {
			// no need for removing anything from hashtables
			waitUnSubAck = false;
			notifyAll();
		}
	}	

	public void publish(String topic, byte[] data) {
		if(connected) publish(topic, data, 0, false);
	}

	public void publish(String topic, byte[] data, int qos, boolean retained) {
		if(!connected) return;
		
		//check whether we already have a topicID; if not register first before sending publish
		int topicID = -1;
		while(topicID < 0 && connected) {
			Integer t = (Integer)tableTopicId.get(topic);
			if(t != null) {
				topicID = t.intValue();  //we have a topicID
			} else {
				waitRegAck = true;
				regTopic = topic;
				while(!client.register(topic));  //try until client sends reg
				synchronized(this) {
					while(waitRegAck) {
						try { wait(10000); } catch(InterruptedException ie) {ie.printStackTrace();}
					}
				}
			}
		}
		if(connected) {
			waitPubAck = (qos > 0);  //will only wait for puback if qos > 0
			while(!client.publish(topicID, data, qos, retained)&&connected) {
				//try {wait(5);} catch (InterruptedException e) {}
				try {Thread.sleep(2000);} catch (InterruptedException e) {}
			}
			synchronized(this) {
				while(waitPubAck) {
					try { wait(10000); } catch(InterruptedException ie) {ie.printStackTrace();}					
				}
			}
		}
	}

	//publish using a pre-defined topicId
	public boolean publish(int topicId, byte[] data, boolean retained) {
		if (!connected) return false;
		return client.publish(1, topicId, data, 0, retained);
	}

	public void regAckReceived(int topicId, int rc) {
		synchronized(this) {
			if(rc == MqttsMessage.RETURN_CODE_ACCEPTED) {
				tableTopicId.put(regTopic, new Integer(topicId));
				tableIdTopic.put(new Integer(topicId), regTopic);
			} else {
				ClientLogger.error("SimpleClient>> Cannot register topic: "+ regTopic);
			}
			waitRegAck = false;
			notifyAll();
		}
	}

	public void pubAckReceived(int topicId, int rc) {
		synchronized(this) {
			if(rc != 0) {
				//pub was rejected, remove id and topic from tables
				String topic = (String)tableIdTopic.get(new Integer(topicId));
				tableIdTopic.remove(new Integer(topicId));
				tableTopicId.remove(topic);
				ClientLogger.error("SimpleClient>> cannot publish, topic: "+ topic);
			}
			waitPubAck = false;
			notifyAll();
		}
	}

	public void pubCompReceived() {
	}

	public void registerReceived(int topicId, String topicName) {
		tableTopicId.put(topicName, new Integer(topicId));
		tableIdTopic.put(new Integer(topicId), topicName);
	}

	public int publishArrived(boolean ret, int qos, int topicId, byte[] thisPayload) {
		String topic = (String)tableIdTopic.get(new Integer(topicId));
		if(topic == null) {
			ClientLogger.warn("SimpleClient>> received pub with unknown topic id: "+topicId);
			return MqttsMessage.RETURN_CODE_INVALID_TOPIC_ID;
		}
		if(callback != null) {
			callback.publishArrived(ret, qos, topic, thisPayload);
		}
		return 0;
	}

	public int publishArrivedPreDefinedTopicId(boolean retain, int qos, int topicId,
			byte[] msg) {

		if (callback != null && callback instanceof SimpleMqttsCallbackPreDefinedTopicId) {

			SimpleMqttsCallbackPreDefinedTopicId cb = (SimpleMqttsCallbackPreDefinedTopicId)callback;
			cb.publishArrivedPreDefinedTopicId(retain,qos,topicId,msg);

		} else {
			ClientLogger.error("SimpleClient>> Error receiving pub with pre-defined topicId");
		}

		return 0;
	}

	public void terminate() {
		client.terminate();
	}

	public String getConnection() {
		String conn;
		conn = client.getClientParameters().getGatewayAddress().toString();
		conn = conn + ":" + client.getClientParameters().getGatewayPort();
		return conn;
	}

	public void setLogfile(String filename) {
		client.setLogfile(filename);
	}

	public void setLogLevel(int level) {
		client.setLogLevel(level);
	}

	public void setWaitingTime(int t) {
		client.setWaitingTime(t);
	}

	public int getLocalUDPPort() {
		return client.getLocalUDPPort();
	}

	public boolean isConnected() {
		return connected;
	}

}
