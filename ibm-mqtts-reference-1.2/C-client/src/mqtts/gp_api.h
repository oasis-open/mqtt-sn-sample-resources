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
 * Description : Generic platform (gp) API header file 
 * 
 *
 */




#ifndef _MQTTS_GP_API_H
#define _MQTTS_GP_API_H



/*************************************************************************
 * The following functions are called by the mqtts client and have to be 
 * implemented by the generic platform (gp):
 *   1. Timer related functions: 
 *        * gp_timer_new
 *        * gp_timer_start
 *        * gp_timer_stop
 *        * gp_timer_end
 *   2. Messages related functions:
 *        * gp_network_msg_send
 *        * gp_network_msg_broadcast
 *        * gp_byte_get
 *   3. Help/Utility functions (optional, need not be implemented)
 *        * gp_debug (e.g. to print debug info)
 * 
 * There is only one callback function from gp, namely:
 * 		gpcb_network_msg_received()
 *************************************************************************/


/**
 * request gp to create a new timer
 * 
 * Parameters:
 *          timeout_funccb      callback-function when time-out happens
 * Return: 
 *          id of timer created
 */
unsigned char gp_timer_new(void (*timeout_funccb)(void));


/**
 * request gp to start indicated timer with the indicated timer value in seconds
 *   Parameters:
 *          id      id of timer to be started
 *          msb		most significant byte of timer value (in seconds)
 *          lsb		least significant byte of timer value
 *   Return:
 * 			none
 */
void gp_timer_start(unsigned char id,
		            unsigned char msb,
		            unsigned char lsb);


/**
 * request gp to stop indicated timer
 * 
 * Parameters:
 *          id  id of timer to be stopped
 * 
 * Return:
 *          none
 * 
 */
void gp_timer_stop(unsigned char id);

/**
 * request gp to end indicated timer (id cannot be used anymore)
 * 
 * Parameters:  id    id of timer to be ended
 * 
 * Return: 
 *          none
 * 
 */
void gp_timer_end(unsigned char id);

/**
 * request gp to send a message
 * 
 * Parameters: 
 *          *msg    pointer to first byte to be sent
 *                  (message's length is in first byte to send!)
 *          *dest   pointer to destination address
 *          length  length of destination address
 * 
 * Return:
 * 			none
 * 
 */
void gp_network_msg_send(unsigned char *msg,
		                 unsigned char *dest,
		                 unsigned char length);

/**
 * request gp to broadcast a message
 *
 * Parameters:
 *          *msg        pointer to first byte to be broadcasted
 *          			(message's length is in first byte to send!)
 *          radius      broadcast radius (0 means all network broadcast)
 * 
 * Return:
 * 			none
 * 
 */    
void gp_network_msg_broadcast(unsigned char *msg, 
		                      unsigned char radius);


/**
 * get a byte from a message just received
 * only called within the handling of gpcb_network_msg_received()
 * 
 * Parameters:
 *          *msg    pointer to the mqtts message 
 *                  (as received in gpcb_network_msg_received())
 *          index   index of the byte to get
 *                  index=1 => mqtts msg length field!
 * 
 * Return: 
 *          requested byte
 * 
 */
unsigned char gp_byte_get(unsigned char *msg, unsigned char index);


/** 
 * Optional function: print/display e.g. debug information
 * 
 * Parameters:
 *           *s       pointer to string or array to be displayed
 *           length   length of string/array to be displayed
 * 
 * Return:   
 * 			none
 * 
 */
#if MQTTS_DEBUG
//void gp_debug(char *s, ...);
void gp_debug(unsigned char *s, unsigned char length);
#endif


/*************************************************************************
 * callback functions (implemented by mqtts, see mqtts-core.c) 
 *************************************************************************/

/**
 * indicate the reception of a mqtts message
 * to be called by gp to give the msg to mqtts client
 * 
 * Parameters:
 *          *msg    	pointer to message received
 *          *sender 	pointer to sender's address
 *          length     	length of sender's address
 * 
 * Return:
 * 
 * NOTE 1: use gp_byte_get(msg,i) to get msg's byte #i !
 * NOTE 2: mqtts length field is in msg[1], not in msg[0] !
 */
void gpcb_network_msg_received(
		unsigned char *msg,
		unsigned char *sender,
		unsigned char length);


#endif
