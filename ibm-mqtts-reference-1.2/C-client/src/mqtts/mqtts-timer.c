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
 * Description : MQTT-SN timer file 
 * 
 */


#include <stdlib.h>
#include <string.h>

#include "mqtts-timer.h"
#include "gp_api.h"
#include "mqtts_api.h"


/* Timer ids */
static unsigned char timer_ack;
static unsigned char timer_keep_alive;
static unsigned char timer_wait_searchgw;
static unsigned char timer_wait_gwinfo;

static unsigned char gAckMissedCnt=0;

/* Keep-alive timer value */
static unsigned char gKeepAliveTime[2]={0,0};

/* SEARCHGW procedure */
/* simplified procedure 
 * SEARCHGW delay: double after each time-out acc. until it reaches 255 sec
 */
static unsigned char gSearchGwDelay = SEARCHGW_MIN_DELAY;

/*callback functions when time-out */
void timeoutcb_ack(void);
void timeoutcb_keep_alive(void);
void timeoutcb_wait_searchgw(void);
void timeoutcb_wait_gwinfo(void);

/* functions prototypes, see mqtts-core.c */
void lost_gw(void);
void mqtts_pingreq(void);
void mqtts_searchgw(void);
void mqtts_gwinfo(void);
void send_backupMsg(void);

/**
 * init timer component */
void mqtts_timer_init(void) {
	/* ask gp to create timers */
	timer_ack = gp_timer_new(timeoutcb_ack);
	timer_keep_alive = gp_timer_new(timeoutcb_keep_alive);
	timer_wait_searchgw = gp_timer_new(timeoutcb_wait_searchgw);
	timer_wait_gwinfo = gp_timer_new(timeoutcb_wait_gwinfo);
}

/**
 * set the keep alive timer value, value in sec */
void mqtts_timer_set_keep_alive_time(unsigned char *time) {
	gKeepAliveTime[0] = time[0]; /*Most significant byte*/
	gKeepAliveTime[1] = time[1]; /*Least significant byte */
}

/**
 * stop and release all timers */
void mqtts_timer_end(void) {
	gp_timer_end(timer_ack);
	gp_timer_end(timer_keep_alive);
	gp_timer_end(timer_wait_searchgw);
	gp_timer_end(timer_wait_gwinfo);
	gAckMissedCnt=0;
	gSearchGwDelay=SEARCHGW_MIN_DELAY;
}

/**
 * start ACK timer */
void mqtts_timer_start_ack(void) {
	gp_timer_start(timer_ack, 0, ACK_TIME); 
}


/**
 * start keep alive timer */
void mqtts_timer_start_keep_alive(void) {
	gp_timer_start(timer_keep_alive, gKeepAliveTime[0], gKeepAliveTime[1]);
}


/**
 * start timer for SEARCHGW */
void mqtts_timer_start_wait_searchgw() {
	gp_timer_start(timer_wait_searchgw,0,gSearchGwDelay);

	/* douple time delay for SEARCHGW after every time-out
	 * until it reaches 255 sec */
	if ((gSearchGwDelay==0x80) || (gSearchGwDelay==0xFF)) {
		gSearchGwDelay = 0xFF;
	} else {
		gSearchGwDelay = gSearchGwDelay << 1;
	}
}

/**
 * start timer for GWINFO */
void mqtts_timer_start_wait_gwinfo() {
	gp_timer_start(timer_wait_gwinfo,0,GWINFO_MIN_DELAY);
}

/**
 * stop ack timer and reset gAckMissedCnt */
void mqtts_timer_stop_ack(void) {
	gp_timer_stop(timer_ack);
	gAckMissedCnt=0;
}
/**
 * stop keep alive timer */
void mqtts_timer_stop_keep_alive(void) {
	gp_timer_stop(timer_keep_alive);
}
/**
 * stop wait GWINFO timer */
void mqtts_timer_stop_wait_gwinfo(void) {
	gp_timer_stop(timer_wait_gwinfo);
}
/**
 * stop wait SEARCHGW timer */
void mqtts_timer_stop_wait_searchgw(void) {
	gp_timer_stop(timer_wait_searchgw);
	gSearchGwDelay = 0x01;
}

/**
 * ACK time-out */
void timeoutcb_ack(void) {
	/* increment counter and stop timer */
	gAckMissedCnt++;
	/*no need for stop, timer already stopped when times out*/
	/*gp_timer_stop(timer_ack);*/

	if (gAckMissedCnt > MAX_ACK_MISSED) { 
		/* too many ACKs missed */  
		lost_gw();
		gAckMissedCnt=0;
	} else { 
		/* send backup message */
		send_backupMsg();
	}
} 

/**
 * Keep alive time out: send a PINGREQ to gw */
void timeoutcb_keep_alive(void) {
	mqtts_pingreq();
}

/**
 * Wait GWINFO time out => send GWINFO */
void timeoutcb_wait_gwinfo(void) {
	mqtts_gwinfo();     
}
/**
 * Wait SEARCHGW time out => send SERACHGW */
void timeoutcb_wait_searchgw(void) {
	mqtts_searchgw();
}

