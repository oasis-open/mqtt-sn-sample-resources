/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
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