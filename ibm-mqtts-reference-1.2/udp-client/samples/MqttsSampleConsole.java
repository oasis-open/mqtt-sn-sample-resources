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

/*
 * Sample application for demonstrating how to use the java MQTT-S client
 * 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ibm.zurich.mqttsclient.*;
import com.ibm.zurich.mqttsclient.exceptions.MqttsException;
import com.ibm.zurich.mqttsclient.utils.*;


public class MqttsSampleConsole implements MqttsCallback {
	
	private MqttsClient mqClient; 	// client

	protected String server; 			// name of server hosting the broker
	protected int port; 				// broker's port
	protected String mqttsClientId; 		// client id
	private boolean mqttsCleanStart=false;
	private short mqttsKeepAliveDuration = 600; // seconds

	private int maxMqttsMsgLength;  	//bytes
	private int minMqttsMsgLength;	//bytes
	private int maxRetries;
	private int ackTime;				//seconds

	protected boolean connected; 		// true if connected to a broker
	protected Hashtable<Integer, String> topicTable;
	private String tName;

	private boolean pubFlag;   //indicates a pub has to be sent when REGACK is received
	private String pubTopic;
	private byte[] pubMsg;
	private int pubQos;
	private boolean pubRetained;

	private boolean autoReconnect=false;

	/* 
	 * Constructor
	 * initialize fields and connect to broker
	 */

	public MqttsSampleConsole(String server, int port, String clientId, boolean cleanStart,
			int maxMqttsMsgLength, int minMqttsMsgLength, 
			int maxRetries, int ackWaitingTime, boolean autoReconnect) {

		this.topicTable = new Hashtable<Integer, String>();
		this.pubFlag = false; this.pubTopic = null;
		this.server = server;
		this.port = port;
		this.mqttsClientId = clientId;
		this.mqttsCleanStart= cleanStart;

		this.maxMqttsMsgLength= maxMqttsMsgLength;
		this.minMqttsMsgLength= minMqttsMsgLength;
		this.maxRetries= maxRetries;
		this.ackTime= ackWaitingTime;

		this.autoReconnect=autoReconnect;

		this.connected = false;

		mqClient = new MqttsClient (this.server ,this.port,
				this.maxMqttsMsgLength, this.minMqttsMsgLength, 
				this.maxRetries, this.ackTime, this.autoReconnect);
		mqClient.registerHandler(this);

		System.out.print("** mqtts java client version "+
				MqttsClient.version + " started, ");
		if (autoReconnect) System.out.println("autoreconnect= true");
		else System.out.println("autoreconnect= false");
		System.out.println("");

		connect();		

	}

	public static void main(String[] args) {
		String srv = "localhost"; 	// default gateway
		int port = 20000; 			// default port
		String clientId = "mqtts_console_" + System.currentTimeMillis(); 		// default client id
		boolean cleanStart=false;

		int maxMqttsMsgLength=60;
		int minMqttsMsgLength=2;
		int maxRetries=2;
		int ackTime=3;
		boolean autoReconnect=true;
		// parse command line arguments -s server -p port -id clientId
		// and overwrite default values if present
		int i = 0;
		String arg;
		while (i < args.length && args[i].startsWith("-")) {
			arg = args[i++];
			if (arg.equals("-s")) {
				srv = args[i++];
			}
			if (arg.equals("-p")) {
				port = Integer.parseInt(args[i++]);
			}
			if (arg.equals("-id")) {
				clientId = args[i++];
			}
			if (arg.equals("-cs")) {
				int cs=Integer.parseInt(args[i++]);
				if(cs==0) cleanStart=false; else cleanStart=true;
			}
			if (arg.equals("-log")) {
				try {
					ClientLogger.setLogFile(args[i++]);
				} catch (MqttsException e) {
					e.printStackTrace();
				} 
			}
			if (arg.equals("-level")) {
				ClientLogger.setLogLevel(Integer.parseInt(args[i++]));	
			}
			if (arg.equals("-auto")) {
				if (args[i++].equals("0")) autoReconnect=false;
				else autoReconnect=true;
			}
		}

		System.out.println("");
		System.out.println("** Starting MQTT-S console ... ");
		// create console and launch the thread
		MqttsSampleConsole console = new MqttsSampleConsole(srv,port,clientId,cleanStart,
				maxMqttsMsgLength,minMqttsMsgLength,maxRetries,ackTime,autoReconnect);
		console.run();
	}

	public void run() {
		while (true) {
			//System.out.println("");

			// read command line from system.in
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line="";
			try {
				line = in.readLine();
			}
			catch (IOException e) {  // that should never happen
				System.out.println("** IOException: " + e);
				System.exit(0);
			}

			StringTokenizer st = new StringTokenizer(line);
			String token[]= new String[10];
			int i=0;
			while (st.hasMoreTokens()) {
				token[i] = st.nextToken(); i++;
			}

			if (i == 0) {
				printHelp();
				continue;
			}

			if (token[0].equals("exit")) terminate();
			if (token[0].equals("t")) terminate();
			if (token[0].equals("help")) printHelp();
			if (token[0].equals("h")) printHelp();
			if (token[0].equals("d")) disconnect();	
			if (token[0].equals("print")) printTopicTable();
			if (token[0].equals("c")) {
				if (!connected) {
					connect();
				} else {
					System.out.println("** already connected to " + server +
							" as " + mqttsClientId);
				}
			}


			if (token[0].equals("s") ) {
				if (connected) {
					if (token[1] != null) {
						subscribe(token[1]);
					} else {
						System.out.println(">> error: missing topic");
					}
					continue;
				} else {
					System.out.println(">> disconnected, subscribe not possible!");
				}
			}

			if (token[0].equals("u")) {
				if (connected) {
					if (token[1] != null) {
						unsubscribe(token[1]);
					} else {
						System.out.println(">> error: missing topic");
					}
					continue;
				} else {
					System.out.println(">> disconnected, unsubscribe not possible!");
				}
			}

			if (token[0].equals("r")) {
				if (connected) {
					if (token[1] != null) {
						register(token[1]);
					} else {
						System.out.println(">> error: missing topic");
					}
					continue;
				} else {
					System.out.println(">> disconnected, register not possible!");
				}
			}


			if (token[0].equals("p")) {
				if (connected) {
					if (token[1]!=null && token[2]!=null) {
						boolean retained=false;
						if (token[3] == null) {
							publish(token[1],token[2],0,retained);
						} else {
							publish(token[1],token[2],1,retained);
						}
					} else {
						System.out.println(">> error, pub format is \"p topic msg \"");
					}
				} 
				else System.out.println(">> disconnected, publish not possible!");
			}
		} //end while
	} //end run method

	public void connect() {
		try {
			if (mqClient == null) {
				System.out.println("** Starting MQTTS-S java client version "+
						MqttsClient.version);
				mqClient = new MqttsClient (this.server ,this.port,
						maxMqttsMsgLength, minMqttsMsgLength, maxRetries,
						ackTime);
				mqClient.registerHandler(this);
			}
			//			cleanStart= false;
			//mqClient.connect(this.mqttsClientId,mqttsCleanStart,mqttsKeepAliveDuration);
			mqClient.connect(this.mqttsClientId,mqttsCleanStart,mqttsKeepAliveDuration,
					"down",1,this.mqttsClientId,true);
		} catch (Exception e){
			connected = false;
			System.out.println("** connection to " + server + " failed!");
			System.out.println("** exception: " + e);
			//System.out.println("Exiting ... ");
			//System.exit(0);
		}	
	}

	public void register(String topicName) {
		mqClient.register(topicName);
		this.tName = topicName;
	}

	public void disconnect() {
		System.out.println("** disconnecting...");
		if (connected) {
			try {
				mqClient.disconnect();
				//System.out.println("** connection to gateway closed");
				connected = false;
			}
			catch (Exception e) {
				System.out.println("** cannot disconnect, exception: " + e);
			}
		} else {
			System.out.println("** already disconnected ... ");
		}
	}

	public boolean publish(String topic, String msg, int qos, boolean retained) {
		byte[] message = msg.getBytes();

		return publish(topic, message, qos, retained);
	}

	public boolean publish(String topic, byte[] msg, int qos, boolean retained) {

		boolean retVal = false;

		Iterator<Integer> iter = topicTable.keySet().iterator();
		Iterator<String> iterVal = topicTable.values().iterator();
		Integer ret = new Integer(-1);
		while (iter.hasNext()) { //check whether topic is in topicTable
			Integer topicId = (Integer)iter.next();			
			String tname = (String)iterVal.next();
			if(tname.equals(topic)) {
				ret = topicId;
				break;
			}
		}
		int topicID = ret.intValue();
		if (topicID == -1) { //topic not in topicTable, have to register it
			register(topic);
			pubFlag = true;  //set the flag and wait for REG ACK
			pubTopic= topic; //store the values for later publish
			pubMsg = msg;
			pubQos = qos;
			pubRetained = retained;
			//System.out.println("** topic not in table, have to register it first");
		} else {
			try {
				retVal = mqClient.publish(topicID, msg, qos, retained);
				//System.out.println("** published: \"" + topic + ": " + 
				//		Utils.hexString(msg) + "\"");
			} catch (Exception e) {
				System.out.println("** publish exception: " + e);
			}
		}
		return retVal;
	}

	public void subscribe(String topic) {
		if (topic != null) {
			try {
				mqClient.subscribe(topic,1,0); //topic name
			}
			catch (Exception e) {
				System.out.println("** sub exception: " + e);
			}
		}
		else {
			System.out.println("** sub error: topic missing!");
		}

		this.tName = topic;

	}

	public void unsubscribe(String topic) {
		if (topic != null) {
			try {
				mqClient.unSubscribe(topic,0); //topic name
			}
			catch (Exception e) {
				System.out.println("** unsub exception: " + e);
			}
		}
		else {
			System.out.println("** unsub error: topic missing!");
		}
		this.tName = topic;
	}

	public void terminate() {
		disconnect();
		if (mqClient != null) {
			mqClient.terminate();
			mqClient = null;
			System.out.println("** client terminated!");
		}
		System.out.println("** exiting ...");
		System.exit(0);
	}

	// callback: publishArrived
	public int publishArrived(boolean dup, int qos, int topicId, byte[] msg ){

		DateFormat df = new SimpleDateFormat("dd.MM HH:mm:ss.SSS");
		String topic = (String)topicTable.get(new Integer(topicId));
		//String message = new String(msg);

		if (topic == null) {
			System.out.println("** PUB with invalid topic id " +topicId +" rejected ...");
			return 2; //return invalid topic id
		}

		System.out.print(df.format(new Date()) + " " + topic +": ");
		//System.out.println(df.format(new Date()) +" >>> " + topic + " msg= " + Utils.hexString(msg));
		System.out.println(Utils.hexString(msg));

		return 0;
	}

	private void printHelp() {
		System.out.println("");
		System.out.println("Type c for connect, d for disconnect, " +
				"p for publish, s for subscribe, u for unsubscribe, " +
				"and t for terminate.");
		System.out.println("");
	}

	//call back: CONNECT is sent to broker, waiting for CONNACK
	public void connectSent() {
		System.out.println("** CONNECT sent to " + server +" ...");
	}

	//call back: CONNACK received
	public void connected() {
		connected = true;
		System.out.println("** connected to " + server +	":" + port + " as " + mqttsClientId);
	}

	//call back: DISCONNECT received
	public void disconnected(int returnType) {
		connected= false;
		switch(returnType) {
		case MqttsCallback.MQTTS_OK:
			System.out.println("** disconnected");
			break;
		case MqttsCallback.MQTTS_LOST_GATEWAY:
			System.out.println("** disconnected, no answer from gateway/broker!");
			break;
		default:
			System.out.println("** disconnected, unknown cause= " + returnType);
		}
	}

	//call back: PUBACK received
	public void pubAckReceived(int topicId, int returnCode) {
		if (returnCode != 0) {
			System.out.println("** WARNING: puback received with rc="+returnCode);
			topicTable.clear();
		} else {
			System.out.println("** puback received.");
		}
	}

	//call back: PUBCOMP received
	public void pubCompReceived() {
		System.out.println("** pubcomp received.");

	}

	//callback: REGACK received
	public void regAckReceived(int topicId, int returnCode) {

		//		System.out.println("** registered: topic= " + tName + 
		//				" topicId= "+ topicId + " rc= " + returnCode);
		topicTable.put(new Integer(topicId), tName);
		tName=null;

		if (pubFlag) {
			publish(pubTopic, pubMsg, pubQos, pubRetained);
			pubFlag = false;
		}

	}

	//callback: REGISTER received
	public void registerReceived(int topicId, String topicName) {
		//		System.out.println("** REG received: topic= " + topicName + 	
		//				" topicId= "+ topicId);
		topicTable.put(new Integer (topicId), topicName);
	}

	//callback: SUBACK received
	public void subackReceived(int grantedQos, int topicId, int rc) {
		if (rc == 0) {
			System.out.println("** subscribed: topic= " + tName + " qos= "+
					grantedQos+ " topicId= " +topicId + " rc= " + rc);
			topicTable.put(new Integer(topicId), tName);
			tName=null;
		} else {
			System.err.println("** SUB rejected: topic= " + tName + " qos= "+
					grantedQos+ " topicId= " +topicId + " rc= " + rc);
		}
	}

	//callback: UNSUBACK received
	public void unsubackReceived() {
		System.out.println("** Unsubscribed: topic= "+this.tName);
		Iterator<Integer> iter = topicTable.keySet().iterator();
		Iterator<String> iterVal = topicTable.values().iterator();
		while (iter.hasNext()) {
			Integer topicId = (Integer)iter.next();			
			String topic = (String)iterVal.next();	
			if (this.tName.equals(topic)) {
				topicTable.remove(topicId);
				break;
			}
		}

	}

	public void printTopicTable() {
		Iterator<Integer> iter = topicTable.keySet().iterator();
		Iterator<String> iterVal = topicTable.values().iterator();
		int n = 0;
		System.out.println("");
		while (iter.hasNext()) {
			Integer topicId = (Integer)iter.next();			
			String topicName = (String)iterVal.next();
			System.out.println("n= " + ++n + ", topicId= "+ topicId + 
					", topic= " + topicName);
		}
	}

}
