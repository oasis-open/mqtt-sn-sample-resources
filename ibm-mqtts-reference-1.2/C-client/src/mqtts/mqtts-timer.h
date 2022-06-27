/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 * which is available at:
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
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


