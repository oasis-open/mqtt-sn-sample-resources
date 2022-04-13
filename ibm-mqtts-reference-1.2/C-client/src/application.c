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
 * Description : MQTT-SN application template
 * 
 */


#include <stdlib.h>
#include <stdio.h>

#include "mqtts/mqtts_api.h"
#include "mqtts/gp_api.h"



static mqtts_CONNECT_Parms conn_p;
static mqtts_REGISTER_Parms reg_p;
static mqtts_PUBLISH_Parms pub_p;
static mqtts_SUBSCRIBE_Parms sub_p;
static mqtts_UNSUBSCRIBE_Parms usub_p;

/* Codes and functions that are inside an (#if MQTTS_DEBUG, #endif)-block are
 * optional. They could e.g. used to exchange debug information with
 * the hw device via a serial port
 */
/**** function prototype */
void appInit(void);

/**
 * Programming model: appRun() will be called regularly by the operating system
 * (e.g. by a round-robin scheduler) of the hw device
 **/
void appRun(void) {  
	static unsigned char initiated=0;
	// init app only once
	if (initiated==0) {
		initiated=1;
		appInit();
	} else {
		/* define here what app has to do regularly */
	}
}


/**
 * Initialization: start mqtts client and ask it to connect to a gw 
 */

void appInit(void) { 

	/* CONNECT parameters */
	conn_p.flagCleanSession=0;
	conn_p.flagWill =0;
	conn_p.flagWillQOS=0;
	conn_p.flagWillRetain=0;

	conn_p.flpProtocolID=0x01;
	conn_p.flpDuration[0] = 0;   /* Keep Alive Timer MostSignificantByte */
	conn_p.flpDuration[1] = 15;  /* Keep Alive Timer LSB = 15 sec */

	conn_p.vlpClientID = (unsigned char *)"mqttsSampleAppl";
	conn_p.vlpClientID_length = 15;
	
	conn_p.vlpWillMsg = NULL;
	conn_p.vlpWillMsg_length = 0;
	conn_p.vlpWillTopic = NULL;
	conn_p.vlpWillTopic_length = 0;

	/* start mqtts and ask it to connect to a gw */
	mqtts_startStack();
	if (mqtts_connect(&conn_p)!= MQTTS_OK) {
#if MQTTS_DEBUG
		gp_debug((unsigned char*)"connect error\r\n",15);
#endif
	}

}

#if MQTTS_DEBUG
/**
 * appHandleCmd() could be called e.g. when a character is typed in a terminal
 * and sent to the hw device via a serial port.
 * Its aim is to demonstrate the capabilities of mqtts, e.g
 *     requesting the sending of a REGISTER message
 *     requesting the sending of a PUBLISH message
 *     etc.
 */
void appHandleCmd(unsigned char cmd) {  //cmd: character typed in terminal 

	switch(cmd) {
	case '\r':
	case '\n':
		gp_debug((unsigned char*)"sample app\r\n", 12);
		break;

	case 'a':
		gp_debug((unsigned char*)"start stack\r\n",13);
		mqtts_startStack();
		break;

	case 't':
		gp_debug((unsigned char*)"stop stack\r\n",12);
		mqtts_stopStack();
		break;

	case 'c':       /* ask mqtts to connect to a gw */
		if (mqtts_connect(&conn_p)== MQTTS_OK) {
			gp_debug((unsigned char*)"waiting for ready\r\n",19);
		} else {
			gp_debug((unsigned char*)"connect error!\r\n",16);
		}
		break;

	case 'd':
		gp_debug((unsigned char*)"sending disconnect ... \r\n",25);
		if (mqtts_disconnect()!=MQTTS_OK) {
			gp_debug((unsigned char*)"disc error!\r\n",13);
		}
		break;

	case 'r':
		gp_debug((unsigned char*)"send REGISTER, topic: wsn/data\r\n",32);
		reg_p.vlpTopic = (unsigned char *)"wsn/data";
		reg_p.vlpTopic_length = 8;
		mqtts_register(&reg_p);
		break;

	case 'p':
		gp_debug((unsigned char*)"send PUBLISH \"PUB QoS 0\" with QoS 0\r\n",37);
		pub_p.flagQOS = 0;
		pub_p.flagRetain = 0;
		pub_p.flagTopicIdType = 0;
		pub_p.vlpData = (unsigned char *)"PUB QoS 0";
		pub_p.vlpData_length = 5;
		mqtts_publish(&pub_p);
		break;

	case 'q':
		gp_debug((unsigned char*)"send PUBLISH \"PUB QoS 1\" with QoS 1\r\n",37);
		pub_p.flagQOS = 1;
		pub_p.vlpData = (unsigned char *)"PUB QoS 1";
		pub_p.vlpData_length = 5;
		mqtts_publish(&pub_p);
		break;

	case 's':
		gp_debug((unsigned char*)"send SUBSCRIBE to topic: wsn/cmd\r\n",34);
		sub_p.flagQOS = 0;
		sub_p.flagTopicIdType = 0;
		sub_p.vlpTopic = (unsigned char *)"wsn/cmd";
		sub_p.vlpTopic_length = 7;
		mqtts_subscribe(&sub_p);
		break;

	case 'u':
		gp_debug((unsigned char*)"send UNSUBSCRIBE topic: wsn/cmd\r\n",33);
		usub_p.flagTopicIdType= 0;
		usub_p.vlpTopic= (unsigned char *)"wsn/cmd";
		usub_p.vlpTopic_length = 7;
		mqtts_unsubscribe(&usub_p);
		break;

	default:
		gp_debug((unsigned char*)"unknown cmd\r\n",13);
	}

}
#endif

/*****************************************************
 * mqtts callback functions
 *****************************************************/

/**
 * CONNECT sent to gw, client is waiting for answer from gw */
void mqttscb_connect_sent(void)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"CONNECT sent\r\n",14);
#endif
}

/**
 * client is now connected to a gw, app can now start publishing, ... */
void mqttscb_connected(void)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"connected\r\n",11);
#endif
}

/**
 * client is disconnected, app has to call mqtts_connect() again */
void mqttscb_disconnected(unsigned char returnCode)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"disconnected\r\n",15);
#endif
}

/**
 * REGACK received, app can now use topic id for publishing */
void mqttscb_regack_received(
		unsigned char topicID_1,
		unsigned char topicID_2,
		unsigned char returnCode)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"REGACK received\r\n",17);
#endif
	
	pub_p.flpTopicID[0] = topicID_1;
	pub_p.flpTopicID[1] = topicID_2;

}

/**
 * PUBLISH received from gw/broker */
unsigned char mqttscb_publish_received(
		unsigned char dup,
		unsigned char qos,
		unsigned char topicID_0,    /* Topic ID[0] */
		unsigned char topicID_1,    /* Topic ID[1] */
		unsigned char *data,
		unsigned char data_len)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"PUBLISH received\r\n",18);
#endif
	return 0;
}

/**
 * PUBACK received, only for PUBLISH with QoS 1 */ 
void mqttscb_puback_received(
		unsigned char topicId_0,
		unsigned char topicId_1,
		unsigned char returnCode)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"PUBACK received\r\n",17);
#endif
}

/**
 * PUBCOMP received, only for PUBLISH with QoS 2 */
void mqttscb_pubcomp_received()
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"PUBCOMP received\r\n",18);
#endif
}

/**
 * SUBACK received */
void mqttscb_suback_received(
		unsigned char qos,
		unsigned char topicID_1,
		unsigned char topicID_2,
		unsigned char returnCode)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"SUBACK received\r\n",17);
#endif
}

/**
 * UNSUBACK received */
void mqttscb_unsuback_received()
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"UNSUBACK received\r\n",19);
#endif
}

/**
 * REGISTER received */
void mqttscb_register_received(
		unsigned char topicID_0, /* Topic ID[0] */
		unsigned char topicID_1, /* Topic ID[1] */
		unsigned char *topic,
		unsigned char topic_len)
{
#if MQTTS_DEBUG
	gp_debug((unsigned char*)"REGISTER received\r\n",19);
#endif
}

/**
 * WILLTOPICRESP received */
void mqttscb_willtopicresp_received() {

}


/**
 * WILLMSGRESP received */
void mqttscb_willmsgresp_received() {

}

/***********************************************
 * 
 * END OF FILE
 * 
 */

