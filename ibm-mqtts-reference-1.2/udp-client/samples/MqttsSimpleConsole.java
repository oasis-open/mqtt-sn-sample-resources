/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import com.ibm.zurich.mqttsclient.SimpleMqttsCallback;
import com.ibm.zurich.mqttsclient.SimpleMqttsClient;
import com.ibm.zurich.mqttsclient.exceptions.MqttsException;
import com.ibm.zurich.mqttsclient.utils.ClientLogger;

public class MqttsSimpleConsole implements SimpleMqttsCallback {

	private SimpleMqttsClient mqttsClient;
	private String broker;
//	private int port;
	private String clientId;

	public MqttsSimpleConsole(String broker, int port, String clientId,
			boolean cleanStart, int maxMqttsMsgLength, int minMqttsMsgLength,
			int maxRetries, int ackWaitingTime, boolean autoReconnect) {

		this.broker = broker;
//		this.port = port;
		this.clientId = clientId;		

		mqttsClient = new SimpleMqttsClient(broker, port, maxMqttsMsgLength, minMqttsMsgLength, 
				maxRetries, ackWaitingTime, autoReconnect);
		mqttsClient.setCallback(this);

		connect();
	}


	private void connect() {
		boolean cleanStart = true;
		int keepAlive = 60; //in sec
		String willTopic = "will";
		int willQos = 0;
		String willMsg = "console no more alive";
		boolean willRetained = true;

		mqttsClient.connect(clientId, cleanStart, keepAlive, willTopic, willQos, willMsg, willRetained);

		if (mqttsClient.isConnected()) {
			System.out.println("Connected to "+broker+" as "+clientId);
		} else {
			System.err.println("Cannot connect to "+broker+" as "+clientId);
		}

	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String srv = "localhost"; 	// default gateway
		int port = 20000; 			// default port
		String clientId = "mqtts_console"; 		// default client id
		boolean cleanStart=true;

		int maxMqttsMsgLength=120;
		int minMqttsMsgLength=2;
		int maxRetries=2;
		int ackWaitingTime=3;  //in sec
		boolean autoReconnect=false;

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
		MqttsSimpleConsole console = new MqttsSimpleConsole(srv,port,clientId,cleanStart,
				maxMqttsMsgLength,minMqttsMsgLength,maxRetries,ackWaitingTime,autoReconnect);

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
			if (token[0].equals("c")) {
				if (!mqttsClient.isConnected()) {
					connect();
				} else {
					System.out.println("** already connected to " + broker +
							" as " + clientId);
				}
			}


			if (token[0].equals("s") ) {
				if (mqttsClient.isConnected()) {
					if (token[1] != null) {
						mqttsClient.subscribe(token[1]);
					} else {
						System.out.println(">> error: missing topic");
					}
					continue;
				} else {
					System.out.println(">> disconnected, subscribe not possible!");
				}
			}

			if (token[0].equals("u")) {
				if (mqttsClient.isConnected()) {
					if (token[1] != null) {
						mqttsClient.unsubscribe(token[1]);
					} else {
						System.out.println(">> error: missing topic");
					}
					continue;
				} else {
					System.out.println(">> disconnected, unsubscribe not possible!");
				}
			}

			if (token[0].equals("p")) {
				if (mqttsClient.isConnected()) {
					if (token[1]!=null && token[2]!=null) {
						boolean retained=false;
						int qos = 0;
						if (token[3] != null) qos = 1;						
						try {
							mqttsClient.publish(token[1],token[2].getBytes("UTF-8"),qos,retained);
						} catch (Exception e) {

						}

					} else {
						System.out.println(">> error, pub format is \"p topic msg \"");
					}
				} 
				else System.out.println(">> disconnected, publish not possible!");
			}
		} //end while
	} //end run method

	private void printHelp() {
		System.out.println("");
		System.out.println("Type c for connect, d for disconnect, " +
				"p for publish, s for subscribe, u for unsubscribe, " +
				"and t for terminate.");
		System.out.println("");
	}
	public void terminate() {
		disconnect();
		if (mqttsClient != null) {
			mqttsClient.terminate();
			mqttsClient = null;
			System.out.println("** client terminated!");
		}
		System.out.println("** exiting ...");
		System.exit(0);
	}
	public void disconnect() {
		System.out.println("** disconnecting...");
		if (mqttsClient.isConnected()) {
			try {
				mqttsClient.disconnect();
				//System.out.println("** connection to gateway closed");
			}
			catch (Exception e) {
				System.out.println("** cannot disconnect, exception: " + e);
			}
		} else {
			System.out.println("** already disconnected ... ");
		}
	}

	public void disconnected(int reason) {
		System.out.println("** disconnected, reason= " + reason);

	}


	public void publishArrived(boolean retain, int qos, String topic, byte[] data) {
		try {
			System.out.println("** pubArrived topic= "+topic+", msg= "+ new String(data,"UTF-8"));	
		} catch (Exception e) {

		}
	}

}
