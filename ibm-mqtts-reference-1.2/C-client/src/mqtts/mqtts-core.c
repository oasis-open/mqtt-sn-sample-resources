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
 * Description : MQTT-SN client core implementation 
 *  
 * 
 */


#include <stdlib.h>
#include <string.h>

#include "mqtts_api.h"
#include "gp_api.h"

#include "mqtts-timer.h"



/**
 * MQTT-S Message types */
#define ADVERTISE     0x00
#define	SEARCHGW      0x01
#define GWINFO        0x02

#define CONNECT       0x04
#define CONNACK       0x05
#define WILLTOPICREQ  0x06
#define WILLTOPIC     0x07
#define WILLMSGREQ    0x08
#define WILLMSG       0x09
#define REGISTER      0x0A
#define REGACK        0x0B
#define PUBLISH       0x0C
#define PUBACK        0x0D 
#define PUBCOMP       0x0E
#define PUBREC        0x0F
#define PUBREL        0x10

#define SUBSCRIBE     0x12
#define SUBACK        0x13
#define UNSUBSCRIBE   0x14
#define UNSUBACK      0x15
#define PINGREQ       0x16
#define PINGRESP      0x17
#define DISCONNECT    0x18

#define WILLTOPICUPD  0x1A
#define WILLTOPICRESP 0x1B
#define WILLMSGUPD    0x1C
#define WILLMSGRESP   0x1D


/**
 * Flags binary masks */
#define EMPTY_MASK           0x00  /* binary  :  0000 0000 */
#define DUP_FLAG_MASK        0x80  /* binary  :  1000 0000 */
#define QOS_MIN1_FLAG_MASK   0x60  /* binary  :  0110 0000 */
#define QOS2_FLAG_MASK       0x40  /* binary  :  0100 0000 */
#define QOS1_FLAG_MASK       0x20  /* binary  :  0010 0000 */
#define QOS0_FLAG_MASK       0x00  /* binary  :  0000 0000 */
#define RETAIN_FLAG_MASK     0x10  /* binary  :  0001 0000 */
#define WILL_FLAG_MASK       0x08  /* binary  :  0000 1000 */
#define CLEANSS_FLAG_MASK    0x04  /* binary  :  0000 0100 */
#define TOPICSHORT_FLAG_MASK 0x02  /* binary  :  0000 0010 */
#define TOPICIDPRE_FLAG_MASK 0x01  /* binary  :  0000 0001 */
#define TOPICID_FLAG_MASK    0x00  /* binary  :  0000 0000 */

#define GET_DUP_FLAG_MASK    0x80  /* binary  :  1000 0000 */
#define GET_QOS_FLAG_MASK    0x60  /* binary  :  0110 0000 */


/**
 * mqtts client's state */
static unsigned char mqtts_state = MQTTS_STATE_NOT_ACTIVE;
/**
 * MsgIg: incremented after having sent a message */
static unsigned char msgId0=0;
static unsigned char msgId1=0;

static unsigned char myGwAddr[MQTTS_MAX_NETWORK_ADDRESS_LENGTH]={NULL};
static unsigned char myGwAddrLength=0;
static unsigned char myGwId=0;

static unsigned char isConnected=0;
static unsigned char lostGw=0;

static unsigned char broadcastRadius;
/* backupMsg: contains msg which will be retransmitted in case of ACK time-out */
static unsigned char backupMsg[MQTTS_MAX_MSG_SIZE];

static mqtts_CONNECT_Parms* conn_parms;

#define HEADER_LENGTH 2
/*
 * macros
 */
#define msg_new(size) unsigned char msg[(size)]
#define msg_set_length(len) *((msg)+0)=(len) /*msg[0]=len*/
#define msg_get_length(msg) *((msg)+0)
#define msg_set_type(type) *((msg)+1)=(type) /*msg[1]=type*/


/*************************
 * functions prototypes
 *************************/
static void mqtts_connecting(void);
static void mqtts_willtopic(void);
static void mqtts_willmsg(void);
static void mqtts_regack(unsigned char topicId_1, unsigned char topicId_2,
		unsigned char msgId1, unsigned char msgId2, unsigned char retCode );
static void mqtts_puback(unsigned char topicID_1, unsigned char topicID_2,
		unsigned char msgID_1, unsigned char msgID_2, unsigned char retCode );
static void mqtts_pingresp(void);
static void mqtts_pubrel(unsigned char msgID_1,  unsigned char msgID_2 );
static void gwinfo_received(unsigned char *msg, unsigned char *sender, 
		unsigned char sender_len);

/****************************************************
 *  stack internal functions               
 ****************************************************/                                             

/**
 * called when we lose a gw, e.g. after multiple ack time-outs */
void lost_gw(void) {
	myGwAddrLength=0;
	mqtts_timer_stop_keep_alive();
	
	/* if we have sent a DISC to the gw then we could go state WAITING_CONNECT 
	 * otherwise we have start searching for gw */
	if (mqtts_state == MQTTS_STATE_DISCONNECTING) {
		mqtts_state = MQTTS_STATE_WAITING_CONNECT;
	} else {
		lostGw= 1;
		mqtts_state = MQTTS_STATE_SEARCHING_GW; /* start searching for a new gw */
		mqtts_timer_start_wait_searchgw();      /* delay sending out SEARCHGW */
	}
	mqttscb_disconnected(MQTTS_LOST_GATEWAY);
}

/**
 * ack received in state WAITING_ACK */
void ack_rx(void) {
	mqtts_timer_stop_ack();
	mqtts_state=MQTTS_STATE_READY;
	if (lostGw == 1) {
		mqttscb_connected();
		lostGw= 0;
	}
}

/**
 * backup msg for later retransmission */
void backup_msg(unsigned char *msg) {
	unsigned char i;
	for (i=0;i<(msg_get_length(msg));i++) {
		backupMsg[i]= msg[i];
	}
}
/**
 * send backup message */
void send_backupMsg(void) {
	mqtts_timer_start_keep_alive();
	mqtts_timer_start_ack();
	gp_network_msg_send(backupMsg,myGwAddr,myGwAddrLength);
}

/* Set the MsgId field of the message. The MsgId is simply incremented for any
 * message sent
 * 
 * Input:
 * 		msgId 	pointer to first byte of the MsgId field in the msg
 * Returns:
 * 		none
 **/
static void set_msg_id(unsigned char* msgId) {
	/* msgId0 is msb, and msgId1 is lsb */
	if (msgId1==255) {
		msgId1=0;
		if (msgId0==255) msgId0=0;
		else msgId0++;
	} else msgId1++;
	msgId[0]= msgId0;
	msgId[1]= msgId1;
}


/*************************************************************************
 * Handle messages received
 * to avoid race conditions and simplify the client implementation
 * the client will respond to all gw's requests without making any check!  
 *************************************************************************/

static void handleSEARCHGW(void) {
	switch (mqtts_state) {
	case MQTTS_STATE_SEARCHING_GW:
		/* we are searching for a gw and receive a SEARCHGW before 
		 * we send SEARCHGW  => we re-schedule the sending of our SEARCHGW  */
		mqtts_timer_start_wait_searchgw();
		break;
	/*only reply with GWINFO if we are connected to a gw*/
	case MQTTS_STATE_READY:
		/* delay before sending GWINFO to give priority to gw */
		mqtts_timer_start_wait_gwinfo();
		break;
	default:
		break;
	}
}

static void handleCONNACK(unsigned char* msg) {
	switch (mqtts_state) {
	/* only accept CNNACK if we are in CONNECTING_TO_GW*/
	case MQTTS_STATE_CONNECTING_TO_GW:
		if (gp_byte_get(msg,3) == MQTTS_RET_ACCEPTED) { /* check return code */
			mqtts_timer_stop_ack();
			mqtts_state=MQTTS_STATE_READY; 
			isConnected= 1;
			lostGw= 0;
			mqttscb_connected();
			/* set flagCleanSession and flagWill to 0 so that in case we 
			 * lose the gw we can re-connect without requesting the app 
			 * to re-do the subscriptions and the will stuffs */
			//conn_parms->flagCleanSession=0;
			/* TODO the new Will feature is not yet supported by the broker */
			//conn_parms->flagWill=0;
		} else {
			/* conn is rejected => either we look for another gw, or we give 
			 * the rejection reason to the app and let it decide what to do
			 * current decision: let app decide */
			mqtts_state = MQTTS_STATE_WAITING_CONNECT;
			mqtts_timer_stop_ack();
			mqtts_timer_stop_keep_alive();
			mqttscb_disconnected(gp_byte_get(msg,3));
		}
		break;
	default:
		break;
	}
}


static void handleWILLTOPICREQ(void) {
	mqtts_timer_stop_ack();
	mqtts_willtopic();
}

static void handleWILLMSGREQ(void) {
	mqtts_timer_stop_ack();
	mqtts_willmsg();
}

static void handleREGISTER(unsigned char* msg) {
	unsigned char i;
	unsigned char topic[MQTTS_MAX_MSG_SIZE];
	unsigned char topic_len;         
	unsigned char msg_len;   

	msg_len = gp_byte_get(msg,1);
	topic_len = 0;

	/* ignore message if not sent by our gw */
	/* gw = mqtts_conn_get_gw();
    if (memcmp(sender,gw->address, gw->address_len)!=0) return; */

	/* copy topic */
	for (i=0;i<(msg_len-6);i++) {
		topic[i] = gp_byte_get(msg,7+i);
		topic_len++; 
	}

	/* send regack to gw */
	mqtts_regack(
			gp_byte_get(msg,3), gp_byte_get(msg,4), /* topic id*/
			gp_byte_get(msg,5), gp_byte_get(msg,6), /* msg id*/
			MQTTS_RET_ACCEPTED); /*return code*/ 
	/* inform app */
	mqttscb_register_received( 
			gp_byte_get(msg,3), gp_byte_get(msg,4),
			topic, topic_len); 
}

static void handleREGACK(unsigned char* msg) {

	/*gw = mqtts_conn_get_gw();*/
	/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/

	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/* check MsgId */
		if ((gp_byte_get(msg,5)!=msgId0) || (gp_byte_get(msg,6)!=msgId1)) return;

		ack_rx();
		mqttscb_regack_received(
				gp_byte_get(msg,3), gp_byte_get(msg,4),   /* Topic ID */
				gp_byte_get(msg,7));  /* Return code    */
		
		break;
	default:
		break;
	}
}

static void handlePUBLISH(unsigned char* msg) {
	unsigned char i;
	unsigned char data[MQTTS_MAX_MSG_SIZE];
	unsigned char data_len= 0;         
	unsigned char msg_len= gp_byte_get(msg,1);

	/* ignore message if not sent by our gw */
	/*gw = mqtts_conn_get_gw(); */
	/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/

	/* copy data*/
	for (i=0;i<(msg_len-7);i++) {
		data[i]=gp_byte_get(msg,8+i);
		data_len++;
	}
	/* i is now return code from app */
	i= mqttscb_publish_received( 
			((gp_byte_get(msg,3) & GET_DUP_FLAG_MASK) >> 7),
			((gp_byte_get(msg,3) & GET_QOS_FLAG_MASK) >> 5),
			gp_byte_get(msg,4), gp_byte_get(msg,5), /* Topic id */
			data, data_len);
	/* send puback if QoS = 1 or retCode = MQTTS_RET_INVALID_TOPIC_ID */
	if (((gp_byte_get(msg,3) & GET_QOS_FLAG_MASK)>> 5)==1 ||
			(i == MQTTS_RET_INVALID_TOPIC_ID)) {
		mqtts_puback(gp_byte_get(msg,4), gp_byte_get(msg,5), /* Topic ID */
				gp_byte_get(msg,6), gp_byte_get(msg,7),     /* Msg Id */
				i);    /*  return code */
	}
	/* TODO QoS=2 not implemented yet */

}

static void handlePUBACK(unsigned char* msg) {
	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/*gw = mqtts_conn_get_gw();*/
		/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/
		
		// check msgId
		if ((gp_byte_get(msg,5)!=msgId0) || (gp_byte_get(msg,6)!=msgId1)) return;

		ack_rx();
		// inform app
		mqttscb_puback_received(gp_byte_get(msg,3), gp_byte_get(msg,4), /* topicId */
				gp_byte_get(msg,7)); /* Return code */
		break;
	default:
		/*inform app in any case if return code != MQTTS_RET_ACCEPTED*/
		if (gp_byte_get(msg,7)!=MQTTS_RET_ACCEPTED){
			mqttscb_puback_received(gp_byte_get(msg,3), gp_byte_get(msg,4), /* topicId */
					gp_byte_get(msg,7)); /* Return code */
		}
		break;
	}
}

static void handleSUBACK(unsigned char* msg) {

	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/*gw = mqtts_conn_get_gw();*/
		/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/
		if ((gp_byte_get(msg,6)!=msgId0) || (gp_byte_get(msg,7)!=msgId1)) return;

		ack_rx();
		mqttscb_suback_received(
				((gp_byte_get(msg,3) & GET_QOS_FLAG_MASK) >> 5),
				gp_byte_get(msg,4), gp_byte_get(msg,5),  /* Topic Id */
				gp_byte_get(msg,6));   /* Return code    */
		break;
	default:
		break;
	}
}

static void handleUNSUBACK(unsigned char* msg) {
	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/*gw = mqtts_conn_get_gw();*/
		/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/

		if ((gp_byte_get(msg,3)!=msgId0) || (gp_byte_get(msg,4)!=msgId1)) return;

		ack_rx();
		mqttscb_unsuback_received();
		break;
	default:
		break;
	}
}

/* received a PINGREQ from gw; answer it with PINGRESP */
static void handlePINGREQ(void) {
	mqtts_pingresp();
}

static void handlePINGRESP(void) {
	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/*gw = mqtts_conn_get_gw();*/
		/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/
		ack_rx();
		break;
	default:
		break;
	}
}

static void handleDISCONNECT(void) {
	mqtts_timer_stop_ack();
	mqtts_timer_stop_keep_alive();
	isConnected= 0;
	if (mqtts_state == MQTTS_STATE_DISCONNECTING) {
		mqtts_state = MQTTS_STATE_WAITING_CONNECT;
		mqttscb_disconnected(MQTTS_OK);
	} else {
		/* ops , gw does not know me any more, try to re-CONNECT */
		mqtts_state = MQTTS_STATE_CONNECTING_TO_GW;
		mqtts_connecting();
	}
}

static void handlePUBREC(unsigned char* msg) {
	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/*gw = mqtts_conn_get_gw();
    	if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/
		mqtts_timer_stop_ack();
		mqtts_pubrel(gp_byte_get(msg,3), gp_byte_get(msg,4)); /* Msg id */
		/* WARNING: stack is not ready here because pubrel waits for pubcomp */
		break;
	default:
		break;
	}
}


static void handlePUBCOMP(void) {
	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		ack_rx();
		mqttscb_pubcomp_received();
		break;
	default:
		break;
	}
}

static void handleWILLTOPICRESP(void) {
	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/*gw = mqtts_conn_get_gw();*/
		/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/
		ack_rx();
		mqttscb_willtopicresp_received();
		break;
	default:
		break;
	}
}

static void handleWILLMSGRESP(void) {
	switch (mqtts_state) {
	case MQTTS_STATE_WAITING_ACK:
		/*gw = mqtts_conn_get_gw();*/
		/*if (memcmp(sender,gw->address, gw->address_len)!=0) return;*/
		ack_rx();
		mqttscb_willmsgresp_received();
		break;
	default:
		break;
	}
}

static void gwFound(void) {
	mqtts_timer_stop_wait_searchgw();
	if (isConnected == 0) {
		mqtts_state = MQTTS_STATE_CONNECTING_TO_GW;
		mqtts_connecting();
	} else {
		/* we have lost the gw and find now one again 
		 * we resend the last message without reconnecting */
		mqtts_state = MQTTS_STATE_WAITING_ACK;
		send_backupMsg();
	}
}

static void handleADVERTISE(unsigned char* msg, unsigned char* sender, 
		unsigned char sender_len) {

	mqtts_timer_stop_wait_gwinfo(); /* cancel send gwinfo */
	switch (mqtts_state) {
	case MQTTS_STATE_SEARCHING_GW:
#if MQTTS_DEBUG
		gp_debug((unsigned char*)"adv ",4);
#endif
		memcpy(myGwAddr,sender,sender_len);
		myGwAddrLength= sender_len;
		myGwId=gp_byte_get(msg,3);
		gwFound();
		break;
	default:
		/* ignore ADVERTISE if we are not searching for GW */
		break;
	}
}

static void handleGWINFO(unsigned char* msg, unsigned char* sender, 
		unsigned char sender_len) {

	/* we receive a GWINFO before sending our GWINFO */
	/* => we don't send our GWINFO */
	mqtts_timer_stop_wait_gwinfo();

	switch (mqtts_state) {
	case MQTTS_STATE_SEARCHING_GW:
#if MQTTS_DEBUG
		gp_debug((unsigned char*)"gwi ",4);
#endif
		gwinfo_received(msg,sender,sender_len); 
		gwFound();
		break;
	default:
		/* ignore GWINFO if we are not searching for GW */
		break;
	}
}

/********************************************************************/

/***
 * send a SEARCHGW message */
void mqtts_searchgw(void) { 
	unsigned char msg[3]= {3,SEARCHGW,MQTTS_SEARCHGW_BROADCAST_RADIUS};

	/* broadcast message and restart timer */ 
	mqtts_timer_start_wait_searchgw();
	gp_network_msg_broadcast(msg,MQTTS_SEARCHGW_BROADCAST_RADIUS);
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"s_gw ",5);
#endif
}

/***
 * send a GWINFO message */
void mqtts_gwinfo(void) {
	unsigned char msg[MQTTS_MAX_NETWORK_ADDRESS_LENGTH + HEADER_LENGTH + 1];
	unsigned char i;

	msg_set_type(GWINFO);
	msg[HEADER_LENGTH]= myGwId;
	msg_set_length (HEADER_LENGTH + 1 + myGwAddrLength);
	for (i=0;i<myGwAddrLength;i++) {
		msg[HEADER_LENGTH+1+i]=myGwAddr[i];
	}

	/* broadcast message �*/
	gp_network_msg_broadcast(msg,broadcastRadius);
	/* Note: GWINFO is broadcasted with the same radius as received in SEARCHGW
	 * while SEARCHGW itself is broacasted with radius=MQTTS_SEARCHGW_BROADCAST_RADIUS
	 */
}



/***
 * send a PUBACK message */
static void mqtts_puback(unsigned char topicID_1, unsigned char topicID_2,
		unsigned char msgID_1, unsigned char msgID_2, unsigned char retCode ) {

	unsigned char msg[HEADER_LENGTH+5];

	msg_set_type(PUBACK);
	/* (Header size) + 2 (Topic ID) + 2 (Message ID) + 1 (Return code) */
	msg_set_length(HEADER_LENGTH+5);

	/* Fill fixed length parameters */
	msg[HEADER_LENGTH]=topicID_1;
	msg[HEADER_LENGTH+1]=topicID_2;
	msg[HEADER_LENGTH+2]=msgID_1;
	msg[HEADER_LENGTH+3]=msgID_2;
	msg[HEADER_LENGTH+4]=retCode;

	/* start keep alive timer and send the message*/
	mqtts_timer_start_keep_alive();
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
}

/***
 * send a REGACK message  */
static void mqtts_regack(unsigned char topicId_1, unsigned char topicId_2,
		unsigned char msgId1, unsigned char msgId2, unsigned char retCode ) {

	unsigned char msg[HEADER_LENGTH+5];

	msg_set_type(REGACK);
	/* (Header size) + 2 (topic id) + 2 (Message ID) + 1 (Return code) */
	msg_set_length(HEADER_LENGTH+5);

	/* Fill fixed length parameters */
	msg[HEADER_LENGTH]=topicId_1;
	msg[HEADER_LENGTH+1]=topicId_2;
	msg[HEADER_LENGTH+2]=msgId1;
	msg[HEADER_LENGTH+3]=msgId2;      
	msg[HEADER_LENGTH+4]=retCode;

	/* start keep alive timer and send the message*/
	mqtts_timer_start_keep_alive();
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
}


/***
 * send a PUBREL message  */
static void mqtts_pubrel(unsigned char msgID_1,  unsigned char msgID_2 ) {

	unsigned char msg[HEADER_LENGTH+2];

	/* Fill the header */
	msg_set_type(PUBREL);
	/*(Header size) + 2 (Message ID) */
	msg_set_length(HEADER_LENGTH+2); 

	/* Fill fixed length parameters */
	msg[HEADER_LENGTH]=msgID_1;
	msg[HEADER_LENGTH+1]=msgID_2;

	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	
	/* we are waiting for PUBCOMP */
	/* TODO What happens if we do not rx a PUBCOMP ? Retransmit PUBREL? */
	/* backup the message for the ack*/
	backup_msg(msg);
	mqtts_state=MQTTS_STATE_WAITING_ACK;
	
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
}  



/***
 * send an PINGRESP message */
static void mqtts_pingresp(void) {
	unsigned char msg[2]={2,PINGRESP};
	mqtts_timer_start_keep_alive();
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
}

/**
 * send an PINGREQ message  */
void mqtts_pingreq(void) {
	unsigned char msg[2]={2,PINGREQ};
	/* backup the message for the ack*/
	backup_msg(msg);
	/* start timers */
	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	mqtts_state=MQTTS_STATE_WAITING_ACK;
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
}

/**
    send an WILLMSG message
 */
static void mqtts_willmsg(void) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	/* Fill the header */
	msg_set_type(WILLMSG);

	/* Length = Header size + WillMsg */
	msg_set_length(HEADER_LENGTH + conn_parms->vlpWillMsg_length);

	/* then the variable parameters */
	for (i=0; i<conn_parms->vlpWillMsg_length; i++){
		msg[HEADER_LENGTH+i]= conn_parms->vlpWillMsg[i];
	}

	/* backup the message for the ack*/
	backup_msg(msg);
	
	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
}

/**
    send an WILLTOPIC message
 */
static void mqtts_willtopic(void) {

	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	/* Fill the header */
	msg_set_type(WILLTOPIC);
	/* Length = Header size + Flags + WillTopic */
	msg_set_length(HEADER_LENGTH + 1 + conn_parms->vlpWillTopic_length);

	/* First the flags */
	msg[HEADER_LENGTH] = EMPTY_MASK;

	switch (conn_parms->flagWillQOS) {
	case 2:
		msg[HEADER_LENGTH] |= QOS2_FLAG_MASK;
		break;

	case 1:
		msg[HEADER_LENGTH] |= QOS1_FLAG_MASK;
		break;

	default:
		msg[HEADER_LENGTH] |= QOS0_FLAG_MASK;
		break;
	}

	switch (conn_parms->flagWillRetain) {
	case 1:
		msg[HEADER_LENGTH] |= RETAIN_FLAG_MASK;
		break;

	default:
		break;
	}

	/* then the variable parameters */
	for (i=0; i<conn_parms->vlpWillTopic_length; i++)
	{
		msg[HEADER_LENGTH+1+i]=conn_parms->vlpWillTopic[i];
	}

	/* backup the message for the ack*/
	backup_msg(msg);

	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
}

/**
    send CONNECT message
 */

static void mqtts_connecting(void) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	/* set the value of the keep_alive timer */
	mqtts_timer_set_keep_alive_time(conn_parms->flpDuration);

	/* Fill the header */
	msg_set_type(CONNECT);
	msg_set_length(HEADER_LENGTH + 4 + conn_parms->vlpClientID_length);

	/* Fill the message parameters */

	/* First the flags */
	msg[HEADER_LENGTH] = EMPTY_MASK;

	switch (conn_parms->flagWill) {
	case 1:
		msg[HEADER_LENGTH] |= WILL_FLAG_MASK;
		break;

	default:
		break;
	}

	switch (conn_parms->flagCleanSession)
	{
	case 1:
		msg[HEADER_LENGTH] |= CLEANSS_FLAG_MASK;
		break;

	default:
		break;
	}

	/* then the fixed parameters */
	msg[HEADER_LENGTH+1] = conn_parms->flpProtocolID;
	msg[HEADER_LENGTH+2] = conn_parms->flpDuration[0];
	msg[HEADER_LENGTH+3] = conn_parms->flpDuration[1];


	/* then the variable parameters */
	for (i=0; i<conn_parms->vlpClientID_length; i++) {
		msg[HEADER_LENGTH + 4 + i]= conn_parms->vlpClientID[i];
	}


	/* backup the message for the ack */
	backup_msg(msg);

	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
	/* inform app about CONNECT sent */
	mqttscb_connect_sent();
	return;
}

/*************************************************/
/**                                             **/ 
/**      application API Implementation         **/
/**                                             **/ 
/*************************************************/


void mqtts_startStack (void) {
	if (mqtts_state != MQTTS_STATE_NOT_ACTIVE) return;

	mqtts_state = MQTTS_STATE_WAITING_CONNECT;
	mqtts_timer_init();

#if MQTTS_DEBUG
	gp_debug((unsigned char *)"MQTT-S client v.",16);
	gp_debug((unsigned char *)MQTTS_VERSION,4);
	gp_debug((unsigned char *)", ",2);
#endif
}


void mqtts_stopStack(void) {
	/* reset all stack variables and stop/delete all timers */
	mqtts_state = MQTTS_STATE_NOT_ACTIVE;
	isConnected= 0;
	lostGw= 0;
	msgId0=msgId1= 0;
	mqtts_timer_end();    

}


unsigned char mqtts_connect(mqtts_CONNECT_Parms *pParms) { 

	if ((mqtts_state != MQTTS_STATE_WAITING_CONNECT)) {
		return MQTTS_ERR_STACK_NOT_READY;
	}

	/* (Header size) + 4 ( 1 (Flags) + 1 (ProtocolId)  + 2 (Duration))  */
	if ( (pParms->vlpClientID_length) > 
	(MQTTS_MAX_MSG_SIZE - (HEADER_LENGTH + 4)) )
	{
		return MQTTS_ERR_DATA_TOO_LONG;
	}

	conn_parms = pParms;

	if (myGwAddrLength == 0) { /* no gw available yet */
		mqtts_state = MQTTS_STATE_SEARCHING_GW;
		mqtts_timer_start_wait_searchgw(); 
	} else {
		mqtts_state = MQTTS_STATE_CONNECTING_TO_GW;
		mqtts_connecting();
	}

	return MQTTS_OK;
}


unsigned char mqtts_disconnect(void) {
	unsigned char msg[2]={2,DISCONNECT};

	switch (mqtts_state) {
	case MQTTS_STATE_NOT_ACTIVE:
	case MQTTS_STATE_WAITING_CONNECT:
		return MQTTS_ERR_STACK_NOT_READY;
	case MQTTS_STATE_CONNECTING_TO_GW:
	case MQTTS_STATE_SEARCHING_GW:
		/* no need for sending a DISC since we are not connected */
		mqtts_state = MQTTS_STATE_WAITING_CONNECT;
		isConnected= 0;
		mqtts_timer_stop_ack();
		mqtts_timer_stop_keep_alive();
		mqtts_timer_stop_wait_gwinfo();
		mqtts_timer_stop_wait_searchgw();
		mqttscb_disconnected(MQTTS_OK);		
		break;
	case MQTTS_STATE_READY:
	case MQTTS_STATE_WAITING_ACK:
		/* send DISC to gw and wait for DISC */		
		backup_msg(msg);
		mqtts_timer_start_ack();
		mqtts_timer_stop_keep_alive();
		mqtts_timer_stop_wait_gwinfo();
		mqtts_timer_stop_wait_searchgw();
		mqtts_state = MQTTS_STATE_DISCONNECTING;
		gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
		break;
	default:
		break;
	}
	return MQTTS_OK;
}


unsigned char mqtts_register(mqtts_REGISTER_Parms *pParms) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	if (mqtts_state != MQTTS_STATE_READY) return MQTTS_ERR_STACK_NOT_READY;

	/* check topic length */
	if ( (pParms->vlpTopic_length) > 
	(MQTTS_MAX_MSG_SIZE - (HEADER_LENGTH + 2)) )
	{
		return MQTTS_ERR_DATA_TOO_LONG;
	}

	msg_set_type(REGISTER);
	msg_set_length(HEADER_LENGTH + 4 + pParms->vlpTopic_length);

	/* TopicId field */
	msg[HEADER_LENGTH] = 0x00;
	msg[HEADER_LENGTH+1] = 0x00;
	/* MsgId field */
	set_msg_id(msg+HEADER_LENGTH+2);
	/* TopicName Field */
	for (i=0; i<pParms->vlpTopic_length; i++) {
		msg[HEADER_LENGTH + 4 + i]=pParms->vlpTopic[i];
	}

	/* backup the message for the ack */
	backup_msg(msg);
	/* we waiting for REGACK */
	mqtts_state=MQTTS_STATE_WAITING_ACK;
	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);

	return MQTTS_OK;

}

unsigned char mqtts_publish(mqtts_PUBLISH_Parms *pParms) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	if (mqtts_state != MQTTS_STATE_READY) return MQTTS_ERR_STACK_NOT_READY;

	/* Fill the header */
	msg_set_type(PUBLISH);

	/* Header size + 5 (1 (Flags) + 2 (Message ID) + 2 (Topic ID)) */

	if ( (pParms->vlpData_length) > (MQTTS_MAX_MSG_SIZE - (HEADER_LENGTH+5)) )
	{
		return MQTTS_ERR_DATA_TOO_LONG;
	}

	msg_set_length(HEADER_LENGTH + 5 + pParms->vlpData_length);


	/* Fill the message parameters */

	/* First the flags */
	msg[HEADER_LENGTH] = EMPTY_MASK;

	switch (pParms->flagQOS)   {
	case 2:
		msg[HEADER_LENGTH] |= QOS2_FLAG_MASK;
		set_msg_id(msg+5);
		break;

	case 1:
		msg[HEADER_LENGTH] |= QOS1_FLAG_MASK;
		/* set the message id */
		set_msg_id(msg+5);
		break;

	default:
		msg[HEADER_LENGTH] |= QOS0_FLAG_MASK;
		/* message id = 0x0000 in case of QoS level 0*/
		msg[5]=0x00;
		msg[6]=0x00;
		break;
	}

	switch (pParms->flagRetain) 
	{
	case 1:
		msg[HEADER_LENGTH] |= RETAIN_FLAG_MASK;
		break;

	default:
		break;
	}

	switch (pParms->flagTopicIdType)
	{
	case 2:
		msg[HEADER_LENGTH] |= TOPICSHORT_FLAG_MASK;
		break;

	case 1:
		msg[HEADER_LENGTH] |= TOPICIDPRE_FLAG_MASK;
		break;

	default:
		break;
	}

	/* then the fixed parameters */
	msg[HEADER_LENGTH+1] = pParms->flpTopicID[0];
	msg[HEADER_LENGTH+2] = pParms->flpTopicID[1];

	/* then the variable parameters */
	for (i=1; i<=pParms->vlpData_length; i++)
	{
		msg[(msg_get_length(msg)-i)]=pParms->vlpData[pParms->vlpData_length-i];
	}

	if (pParms->flagQOS!=0)
	{
		/* save the flag and set the DUP flag for the backup */
		i = msg[2];
		msg[2] |= DUP_FLAG_MASK; 
		/* backup the message for the ack or pubrec*/
		backup_msg(msg);
		/* reload the saved flag */
		msg[2] = i;
		/* start the ack or pubrec timer */
		mqtts_timer_start_ack();
		mqtts_timer_start_keep_alive();
		mqtts_state=MQTTS_STATE_WAITING_ACK;
	}

	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);

	return MQTTS_OK;
}


unsigned char mqtts_subscribe(mqtts_SUBSCRIBE_Parms *pParms) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	if (mqtts_state != MQTTS_STATE_READY) return MQTTS_ERR_STACK_NOT_READY;

	/* Fill the header */
	msg_set_type(SUBSCRIBE);

	/* Header size + 1 (Flags) + 2 (MsgId)*/

	if ( (pParms->vlpTopic_length) > (MQTTS_MAX_MSG_SIZE - (HEADER_LENGTH+3)) )
	{
		return MQTTS_ERR_DATA_TOO_LONG;
	}

	msg_set_length((HEADER_LENGTH+3) + pParms->vlpTopic_length);

	/* Fill the message parameters */
	/* First the flags */
	msg[HEADER_LENGTH] = EMPTY_MASK;

	switch (pParms->flagQOS)
	{
	case 2:
		msg[HEADER_LENGTH] |= QOS2_FLAG_MASK;
		break;

	case 1:
		msg[HEADER_LENGTH] |= QOS1_FLAG_MASK;
		break;

	default:
		msg[HEADER_LENGTH] |= QOS0_FLAG_MASK;
		break;
	}

	switch (pParms->flagTopicIdType)
	{
	case 2:
		msg[HEADER_LENGTH] |= TOPICSHORT_FLAG_MASK;
		break;
	case 1:
		msg[HEADER_LENGTH] |= TOPICIDPRE_FLAG_MASK;
		break;
	default:
		break;
	}

	/* MsgId field */
	set_msg_id(msg+HEADER_LENGTH+1);

	/* then the variable parameters */
	for (i=0; i<pParms->vlpTopic_length; i++)
	{
		msg[HEADER_LENGTH + 3 + i]=pParms->vlpTopic[i];
	}

	/* save the flag and set the DUP flag for the backup */
	i = msg[HEADER_LENGTH];
	msg[HEADER_LENGTH] |= DUP_FLAG_MASK; 
	/* backup the message for the ack */
	backup_msg(msg);
	/* reload the saved flag */
	msg[HEADER_LENGTH] = i;
	/* we wait for SUBACK */
	mqtts_state=MQTTS_STATE_WAITING_ACK;
	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);


	return MQTTS_OK;
}


unsigned char mqtts_unsubscribe(mqtts_UNSUBSCRIBE_Parms *pParms) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	if (mqtts_state != MQTTS_STATE_READY) return MQTTS_ERR_STACK_NOT_READY;

	/* Fill the header */
	msg_set_type(UNSUBSCRIBE);
	/* Header size + 3 (Flags+MsgId) + (TopicLength))*/
	msg_set_length(HEADER_LENGTH + 3 + pParms->vlpTopic_length);

	/* Fill the message parameters */

	/* First the flags */
	msg[HEADER_LENGTH] = EMPTY_MASK;

	switch (pParms->flagTopicIdType)
	{
	case 2:
		msg[HEADER_LENGTH] |= TOPICSHORT_FLAG_MASK;
		break;
	case 1:
		msg[HEADER_LENGTH] |= TOPICIDPRE_FLAG_MASK;
		break;
	default:
		break;
	}

	/* MsgId field */
	set_msg_id(msg+HEADER_LENGTH+1);

	/* then the variable parameters */
	for (i=0; i<pParms->vlpTopic_length; i++)
	{
		msg[HEADER_LENGTH + 3 + i]=pParms->vlpTopic[i];
	}

	/* save the flag and set the DUP flag for the backup*/
	i = msg[HEADER_LENGTH];
	msg[HEADER_LENGTH] |= DUP_FLAG_MASK;
	/* backup the message for the ack*/
	backup_msg(msg);
	/* reload the saved flag */
	msg[HEADER_LENGTH] = i;
	/* we wait for UNSUBACK */
	mqtts_state=MQTTS_STATE_WAITING_ACK;
	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
	

	return MQTTS_OK;    

}


unsigned char mqtts_willtopic_update(mqtts_WILLTOPICUPD_Parms *pParms) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	if (mqtts_state != MQTTS_STATE_READY) return MQTTS_ERR_STACK_NOT_READY;

	/* Fill the header */
	msg_set_type(WILLTOPICUPD);

	if (pParms == NULL) { //send an empty WILLTOPICUPD to delete will topic and msg
		msg_set_length(HEADER_LENGTH);		
	} else {
		msg_set_length(HEADER_LENGTH + 1 + pParms->vlpWillTopic_length);
		msg[HEADER_LENGTH] = EMPTY_MASK;
		switch (pParms->flagWillQOS) {
		case 2:
			msg[HEADER_LENGTH] |= QOS2_FLAG_MASK;
			break;		    
		case 1:
			msg[HEADER_LENGTH] |= QOS1_FLAG_MASK;
			break;
		default:
			msg[HEADER_LENGTH] |= QOS0_FLAG_MASK;
			break;
		}
		switch (pParms->flagWillRetain) {
		case 1:
			msg[HEADER_LENGTH] |= RETAIN_FLAG_MASK;
			break;
		default:
			break;
		}
		for (i=0; i<pParms->vlpWillTopic_length; i++)
		{
			msg[HEADER_LENGTH+1+i]=pParms->vlpWillTopic[i];
		}
	}

	/* backup the message for the ack*/
	backup_msg(msg);
	/* we wait for WILLTOPICRESP */
	mqtts_state=MQTTS_STATE_WAITING_ACK;
	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);

	return MQTTS_OK;    
}



unsigned char mqtts_willmsg_update(mqtts_WILLMSGUPD_Parms *pParms) {
	unsigned char msg[MQTTS_MAX_MSG_SIZE];
	unsigned char i;

	if (mqtts_state != MQTTS_STATE_READY) return MQTTS_ERR_STACK_NOT_READY;

	/* Fill the header */
	msg_set_type(WILLMSGUPD);

	/* Length = Header size + WillMsg */
	msg_set_length(HEADER_LENGTH + pParms->vlpWillMsg_length);

	/* then the variable parameters */
	for (i=0; i<pParms->vlpWillMsg_length; i++){
		msg[HEADER_LENGTH+i]= pParms->vlpWillMsg[i];
	}
	/* backup the message for the ack*/
	backup_msg(msg);
	mqtts_state=MQTTS_STATE_WAITING_ACK;
	mqtts_timer_start_ack();
	mqtts_timer_start_keep_alive();
	
	gp_network_msg_send(msg,myGwAddr,myGwAddrLength);
	
	return MQTTS_OK; 
}




static void gwinfo_received(unsigned char *msg, unsigned char *sender, unsigned char sender_len) {
	unsigned char i;
	unsigned char data[MQTTS_MAX_MSG_SIZE];
	unsigned char data_len;         
	unsigned char msg_len;   

	msg_len = gp_byte_get(msg,1);
	data_len = 0;        	

	switch(msg_len) {
	case 3:
		/* GWINFO was sent by a gw */
		memcpy(myGwAddr,sender,sender_len);
		myGwAddrLength= sender_len;
		myGwId= gp_byte_get(msg,3);
		break;
	default:
		/* GWINFO was sent by a client */
		for (i=0;i<(msg_len-3);i++) {
			data[i]=gp_byte_get(msg,4+i);
			data_len++;
		}
		memcpy(myGwAddr,data,data_len);
		myGwAddrLength= data_len;
		myGwId= gp_byte_get(msg,3);
		break;
	}

}



void gpcb_network_msg_received(unsigned char *msg,unsigned char *sender,unsigned char sender_len) {

	unsigned char msg_type= gp_byte_get(msg,2);

	if (mqtts_state == MQTTS_STATE_NOT_ACTIVE) {
		/* stack not started, all msgs are ignored */
		return;
	}

	switch (msg_type) {
	case ADVERTISE:
		handleADVERTISE(msg, sender, sender_len);
		return;

	case GWINFO:
		handleGWINFO(msg, sender, sender_len);
		return;

	case SEARCHGW:
		broadcastRadius= gp_byte_get(msg,3);
		handleSEARCHGW();
		return;

	default:
		/* ignore msg if not sent by my gw */
		if (memcmp(sender,myGwAddr, myGwAddrLength)!=0) return;
	}

	switch (msg_type) {
	case CONNACK:
		handleCONNACK(msg);
		break;

	case WILLTOPICREQ:
		handleWILLTOPICREQ();
		break;

	case WILLMSGREQ:
		handleWILLMSGREQ();
		break;

	case REGISTER:
		handleREGISTER(msg);
		break;

	case REGACK:
		handleREGACK(msg);
		break;

	case PUBLISH:
		handlePUBLISH(msg);
		break;

	case PUBACK:
		handlePUBACK(msg);
		break;

	case PUBREC:
		handlePUBREC(msg);
		break;

	case PUBCOMP:
		handlePUBCOMP();
		break;

	case SUBACK:
		handleSUBACK(msg);
		break;

	case UNSUBACK:
		handleUNSUBACK(msg);
		break;

	case PINGREQ:
		handlePINGREQ();
		break;

	case PINGRESP:
		handlePINGRESP();
		break;

	case DISCONNECT:
		handleDISCONNECT();
		break;

	case WILLTOPICRESP:
		handleWILLTOPICRESP();
		break;
	case WILLMSGRESP:
		handleWILLMSGRESP();
		break;

	default:
		break;

	}  /* end switch (msg_type) */
}

unsigned char mqtts_get_state(void) {
	return (mqtts_state);
}


/* END OF FILE */


