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

package com.ibm.zurich.mqttsclient.utils;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.zurich.mqttsclient.exceptions.MqttsException;

public class ClientLogger {
	
	public final static int ALL = 0;
	public final static int INFO  = 1;
	public final static int WARN  = 2;
	public final static int ERROR = 3;

	private static int LOG_LEVEL = WARN;

	
	public static void all(String msg) {
		DateFormat dFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");
		System.out.println(dFormat.format(new Date())+ "  INFO:  " + msg);
		if(printWriter != null){
			printWriter.println(dFormat.format(new Date())+ "  INFO:  " + msg);
			printWriter.flush();
		}			
	}

	public static void info(String msg) {
		DateFormat dFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");
		//System.out.println(dFormat.format(new Date())+ "  INFO:  " + msg);
		if(printWriter != null){
			printWriter.println(dFormat.format(new Date())+ "  INFO:  " + msg);
			printWriter.flush();
		}			
	}

	public static void warn(String msg) {
		DateFormat dFormat= new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");
		System.err.println(dFormat.format(new Date())+ "  WARN:  "+ msg);
		if(printWriter != null){
			printWriter.println(dFormat.format(new Date())+ "  WARN:  "+ msg);
			printWriter.flush();
		}
	}

	public static void error(String msg) {	
		DateFormat dFormat= new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");
		System.err.println(dFormat.format(new Date())+ "  ERROR: " + msg);
		if(printWriter != null){
			printWriter.println(dFormat.format(new Date())+ "  ERROR: " + msg);
			printWriter.flush();
		}
	}


	public static void log(int logLevel, String msg) {
		if(logLevel >= LOG_LEVEL) {			
			switch (logLevel){
			case INFO:
				if (LOG_LEVEL == ALL) all(msg);
				else info(msg);
				break;
			case WARN:
				warn(msg);
				break;
			case ERROR:
				error(msg);
				break;
			default:
			}
		}
	}


	public static void setLogLevel(int logLevel) {
		LOG_LEVEL = logLevel;
	}


	private static FileWriter fileWriter;
	private static PrintWriter printWriter;

	public static void setLogFile(String file) throws MqttsException {
		//DateFormat dFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
		try {
			fileWriter = new FileWriter(file);
			printWriter = new PrintWriter(fileWriter);
			printWriter.println();
		} catch(IOException e) {
			e.printStackTrace();
			throw new MqttsException (e.getMessage());
		}
	}
}