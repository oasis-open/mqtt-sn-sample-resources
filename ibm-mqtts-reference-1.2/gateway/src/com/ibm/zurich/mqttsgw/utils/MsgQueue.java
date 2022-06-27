/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/

package com.ibm.zurich.mqttsgw.utils;

import java.util.LinkedList;

public class MsgQueue {	
	
	private LinkedList<Object> queue = new LinkedList<Object>();
	
	 /**
	 * @param o
	 */
	public void addLast(Object o) {
		synchronized(queue){
			  queue.add(o);
			  queue.notify();
		  }
	  }
	  
	 /**
	 * @param o
	 */
	public void addFirst(Object o) {
		synchronized(queue){
			  queue.addFirst(o);
		      queue.notify();
		    }
	  }
	
	public Object get() throws InterruptedException {
		synchronized(queue){
			while (queue.isEmpty())
				queue.wait();
			return queue.removeFirst();
	    }
	}
	  
	  /**
	 * @return
	 */
	public int size(){
		return queue.size();
	}
}