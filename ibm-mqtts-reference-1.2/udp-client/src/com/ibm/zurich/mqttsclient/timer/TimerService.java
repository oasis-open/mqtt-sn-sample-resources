/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsclient.timer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import com.ibm.zurich.mqttsclient.messages.Message;
import com.ibm.zurich.mqttsclient.messages.control.ControlMessage;
//import com.ibm.zurich.mqttsclient.timer.TimerService.TimeoutTimerTask;
import com.ibm.zurich.mqttsclient.utils.ClientLogger;
import com.ibm.zurich.mqttsclient.utils.MsgQueue;



@SuppressWarnings({"static-access"})
public class TimerService {

	private static Timer timer=null;
	private MsgQueue queue;
	private Vector<TimeoutTimerTask> timeoutTasks;

	/**
	 * Constructor.
	 */
	public TimerService(MsgQueue queue) {
		if(timer==null) {
			timer=new Timer();
		}
		this.queue = queue;	
		timeoutTasks = new Vector<TimeoutTimerTask>();
	}


	public void register(int type, int timeout) {
		ClientLogger.log(ClientLogger.INFO, "Timer "+ type +" started, duration= "+ timeout);
		long delay = timeout * 1000;
		long period = timeout * 1000;

		for(int i = 0 ; i<timeoutTasks.size(); i++) {
			TimeoutTimerTask timeoutTimerTask = (TimeoutTimerTask) timeoutTasks.get(i);
			if (timeoutTimerTask.getType() == type){
				return;
			}
		}

		TimeoutTimerTask timeoutTimerTask = new TimeoutTimerTask(type);

		//put this timeoutTimerTask in a list
		timeoutTasks.add(timeoutTimerTask);

		//schedule for future executions
		timer.scheduleAtFixedRate(timeoutTimerTask, delay, period);		
	}



	public void unregister(int type) {
		ClientLogger.log(ClientLogger.INFO, "Timer "+type+" stopped");
		for(int i = 0 ; i<timeoutTasks.size(); i++) {
			TimeoutTimerTask timeout = (TimeoutTimerTask) timeoutTasks.get(i);
			if (timeout.getType() == type){
				timeoutTasks.remove(i);
				timeout.cancel();
				break;
			}
		}
	}

	public void unregisterAll(){
		//ClientLogger.log(ClientLogger.INFO, "All timers stopped");
		for(int i = timeoutTasks.size()-1; i >= 0; i--) {
			TimeoutTimerTask timeout = (TimeoutTimerTask) timeoutTasks.get(i);
			timeoutTasks.remove(i);
			timeout.cancel();
		}		
	}

	public void terminate() {
		this.unregisterAll();
		this.timer.cancel();
	}


	public class TimeoutTimerTask extends TimerTask {
		int type;

		/**
		 * Constructor.
		 * 
		 */
		public TimeoutTimerTask(int type) {
			this.type = type;
		}

		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		public void run(){
			//create new control message
			ControlMessage controlMsg = new ControlMessage();
			controlMsg.setMsgType(type);

			//create an "internal" message
			Message msg = new Message();
			msg.setType(Message.CONTROL_MSG);
			msg.setControlMessage(controlMsg);

			//put this message to the Dispatcher's queue
			queue.addFirst(msg);
		}

		public int getType() {
			return type;
		}
	}
}