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

/**
 * 
 * 
 * Description : MQTT-SN timer header file 
 * 
 *
 */


/**
 * Initialize mqtts-timer component */ 
void mqtts_timer_init();
/**
 * set value of keep alive timer */
void mqtts_timer_set_keep_alive_time(unsigned char *time);
/**
 * end and delete all timers */
void mqtts_timer_end(void);
/**
 * stack ACK timer */
void mqtts_timer_start_ack(void);
/**
 * start keep alive timer */ 
void mqtts_timer_start_keep_alive(void);
/**
 * start wait timer before sending SEARCHGW */
void mqtts_timer_start_wait_searchgw(void);
/** 
 * start wait timer before sending GWINFO */
void mqtts_timer_start_wait_gwinfo(void);
/**
 * stop ACK timer */
void mqtts_timer_stop_ack(void);
/**
 * stop keep alive timer */
void mqtts_timer_stop_keep_alive(void);
/**
 * stop GWINFO wait timer */
void mqtts_timer_stop_wait_gwinfo(void);
/** 
 * stop SEARCHGW wait timer */
void mqtts_timer_stop_wait_searchgw(void);


