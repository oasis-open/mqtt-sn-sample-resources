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