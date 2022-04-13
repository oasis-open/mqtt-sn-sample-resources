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

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class MsgQueue {
	
   private LinkedList<Object> queue;
	private volatile boolean isClosed;

	public MsgQueue() {
		queue = new LinkedList<Object>();
		isClosed = false;
	}

	public void close() {
		synchronized (queue) {
			isClosed = true;
			queue.notifyAll();
		}
	}

	/**
	 * @param o
	 */
	public void addLast(Object o) {
		synchronized (queue) {
			if(isClosed) {
				throw new IllegalStateException("Queue is closed.");
			}
			queue.add(o);
			queue.notify();
		}
	}

	/**
	 * @param o
	 */
	public void addFirst(Object o) {
		synchronized (queue) {
			if(isClosed) {
				throw new IllegalStateException("Queue is closed.");
			}
			queue.addFirst(o);
			queue.notify();
		}
	}

	public Object get() throws InterruptedException {
		synchronized (queue) {
			if(isClosed) {
				throw new IllegalStateException("Queue is closed.");
			}
			while (queue.isEmpty() & !this.isClosed)
				queue.wait();
			Object res=null;
			try {
				res=queue.removeFirst();
			} catch(NoSuchElementException e) {
				res=null;
			}
			return res;
		}
	}

	/**
	 * @return
	 */
	public int size() {
		return queue.size();
	}
}