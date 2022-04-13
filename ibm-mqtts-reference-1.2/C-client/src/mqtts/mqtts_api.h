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
 * Description : MQTT-S client API header file 
 * 
 * 
 * 
 * 
 * 
 */



#ifndef _MQTTS_APPLICATION_API_H
#define _MQTTS_APPLICATION_API_H

/**
 * ************** MQTT-S specific parameters    **/
#define MQTTS_VERSION "1.03"
/**
 * Maximum length of network address, e.g. 4 for ZigBee, 16 for IPv6, etc.
 * Note: it just defines a max value, i.e. 16 can also be used for ZigBee */
#define MQTTS_MAX_NETWORK_ADDRESS_LENGTH  4
/**
 * Maximum size for a mqtts message */
#define MQTTS_MAX_MSG_SIZE 60
/**
 * Default value of timers, delay periods, etc.
 * All time values are in seconds  */
/**
 * max number of ACKs missed before gw is declared to be lost */
#define MAX_ACK_MISSED 4  
/**
 * ack waiting time */
#define ACK_TIME 10 
/**
 * waiting time before sending a SEARCHGW */
#define SEARCHGW_MIN_DELAY 2 
/**
 * waiting time before sending a GWINFO */
#define GWINFO_MIN_DELAY 2  
/** 
 * Broadcast radius for SEARCHGW messages */
#define MQTTS_SEARCHGW_BROADCAST_RADIUS  1



/**
 * ********************* Return codes **************/
/**
 * protocol return codes (received from gw) */
#define MQTTS_RET_ACCEPTED          0x00
#define MQTTS_RET_CONGESTION        0x01
#define MQTTS_RET_INVALID_TOPIC_ID  0x02

/* local return codes */
#define MQTTS_OK                    0xF0
#define MQTTS_ERR_STACK_NOT_READY   0xF1
#define MQTTS_ERR_DATA_TOO_LONG     0xF2
#define MQTTS_LOST_GATEWAY          0xF3


/**
 ******************** MQTTS client's states ***************/
/**
 *  Client not active, needs be started with mqtts_startStack() */
#define MQTTS_STATE_NOT_ACTIVE          0x00
/* Client is active, but waits for mqtts_connect() to setup a connection */
#define MQTTS_STATE_WAITING_CONNECT     0x01
/* Client is searching for a gateway (or forwarder) */
#define MQTTS_STATE_SEARCHING_GW        0x02
/* Client has sent a CONNECT to a gw and is waiting for its response */
#define MQTTS_STATE_CONNECTING_TO_GW    0x03
/* Client is ready for sending request to gw */
#define MQTTS_STATE_READY               0x04
/* Client has sent a request to the gw and is waiting for an acknowledgment 
 * Note that client can only have one outstanding request at a time and
 * therefore does not accept any further app's request in this state */
#define MQTTS_STATE_WAITING_ACK         0x05
/* Client has sent a DISCONNECT to gw and is waiting for its response 
 * Client will return to state WAITING_CONNECT afterwards */
#define MQTTS_STATE_DISCONNECTING       0x06


/**
 ********************* Definition of structures **************/
/* CONNECT parameters structure */
typedef struct {
	/* flags */ 
	unsigned char flagCleanSession;
	unsigned char flagWill;
	unsigned char flagWillQOS;
	unsigned char flagWillRetain;

	/* fixed length parameters */
	unsigned char flpProtocolID;
	unsigned char flpDuration[2]; /* Keep Alive timer value */

	/* variable length parameters */
	unsigned char *vlpClientID;
	unsigned char  vlpClientID_length;
	unsigned char *vlpWillMsg;
	unsigned char  vlpWillMsg_length;
	unsigned char *vlpWillTopic;
	unsigned char  vlpWillTopic_length;

} mqtts_CONNECT_Parms;

/* WILLTOPICUPD parameters structure */
typedef struct {
	/* flags */ 
	unsigned char flagWillQOS;
	unsigned char flagWillRetain;

	/* variable length parameters */
	unsigned char *vlpWillTopic;
	unsigned char  vlpWillTopic_length;

} mqtts_WILLTOPICUPD_Parms;

/* WILLMSGUPD parameters structure */
typedef struct {
	unsigned char *vlpWillMsg;
	unsigned char  vlpWillMsg_length;
} mqtts_WILLMSGUPD_Parms;

/* REGISTER parameters structure */
typedef struct {
	/* variable length parameters */
	unsigned char *vlpTopic;
	unsigned char vlpTopic_length;
} mqtts_REGISTER_Parms;

/* PUBLISH parameters structure */
typedef struct {
	/* flags */ 
	/* DUP is set by mqtts when retransmitting the message */
	unsigned char flagQOS;
	unsigned char flagRetain;
	unsigned char flagTopicIdType;
	/* fixed length parameters */
	unsigned char flpTopicID[2];
	/* variable length parameters */
	unsigned char *vlpData;
	unsigned char vlpData_length;
} mqtts_PUBLISH_Parms;

/* SUBSCRIBE parameters structure */
typedef struct {
	/* flags */
	unsigned char flagQOS;
	unsigned char flagTopicIdType;
	/* variable length parameters: TopicName or TopicId */
	unsigned char *vlpTopic;
	unsigned char vlpTopic_length;
} mqtts_SUBSCRIBE_Parms;

/* UNSUBSCRIBE parameters structure */
typedef struct {
	/* flags */ 
	unsigned char flagTopicIdType;
	/* variable length parameters: TopicName or TopicId */
	unsigned char *vlpTopic;
	unsigned char vlpTopic_length;
} mqtts_UNSUBSCRIBE_Parms;


/*************************************************************  
 *                                          
 *               Functions provided by the mqtts client 
 * 
 * Functions that trigger a request to be sent to the gw/broker and
 * required a reply from the gw/broker are non-blocking, i.e. the client 
 * with return immediately after having sent the request to the gw/broker.
 * The gw/broker's response will then be indicated by the corresponding
 * call-back function.
 * 
 *************************************************************/

/**
    start the mqtts client: client will go to state WAITING_CONNECT
    and begin to process ADVERTISE and GWINFO, but it will wait for
    mqtts_connect() to initialize a connection to a gw

    Parameters : none

    Returns : none

 */
void mqtts_startStack(void);


/**
    stop the mqtts client:
    client will then ignore all messages and go to state "NOT_ACTIVE";
    app needs to re-issue mqtts_startStack()

    Parameters : none

    Returns : none

 */
void mqtts_stopStack(void);


/**
    request client to setup a connection to a gw
    client stack has to be already started (with mqtts_startStack() )

    Parameters : 
        pParms - pointer to a mqtts_CONNECT_Parms       
    Returns :
        MQTTS_ERR_STACK_NOT_READY
        MQTTS_ERR_DATA_TOO_LONG
        MQTTS_OK

 */
unsigned char mqtts_connect(mqtts_CONNECT_Parms *pParms);


/**
    request client to disconnect 
    client will wait again for mqtts_connect() to setup a connection

    Parameters : none

    Returns :
        MQTTS_ERR_STACK_NOT_READY
        MQTTS_OK

 */
unsigned char mqtts_disconnect(void);


/**
    request client to send a REGISTER message 
    only accepted if client state = READY 

    Parameters :
        pParms - pointer to a REGISTER parameter

    Returns :
        MQTTS_ERR_STACK_NOT_READY
        MQTTS_OK
 * 
 */
unsigned char mqtts_register(mqtts_REGISTER_Parms *pParms);



/**
    request client to send a PUBLISH message
    only accepted if client state = READY

    Parameters :
        pParms - pointer to a PUBLISH parameter

    Returns :
        MQTTS_ERR_STACK_NOT_READY
        MQTTS_OK

 */
unsigned char mqtts_publish(mqtts_PUBLISH_Parms *pParms);


/**
    request client to send a SUBSCRIBE message 
    only accepted if client state = READY

    Parameters :
        pParms - pointer to a SUBSCRIBE parameter

    Returns :
        MQTTS_ERR_STACK_NOT_READY
        MQTTS_OK

 */
unsigned char mqtts_subscribe(mqtts_SUBSCRIBE_Parms *pParms);


/**
    request client to send an UNSUBSCRIBE message 
    only accepted if client state = READY

    Parameters :
        pParms - pointer to a UNSUBSCRIBE parameter

    Returns :
        MQTTS_ERR_STACK_NOT_READY
        MQTTS_OK

 */
unsigned char mqtts_unsubscribe(mqtts_UNSUBSCRIBE_Parms *pParms);

/**
 * request client to send a WILLTOPICUPD message to update the Will topic
 * only accepted if client state = READY
 * 
 * Parameters:
 *     pParms - pointer to a WILLTOPIC parameter
 *              NULL to send an empty WILLTOPICUPD message (delete Will)
 * 
 * Returns :
 *    MQTTS_ERR_STACK_NOT_READY
 *    MQTTS_OK
 * 
 * 
 */
unsigned char mqtts_willtopic_update(mqtts_WILLTOPICUPD_Parms *pParms);

/**
 * request client to send a WILLMSGUPD message to update the Will message
 * only accepted if client state = READY
 * 
 * Parameters:
 *     pParms - pointer to WILLMSGUPD parameter
 * 
 * Returns :
 *    MQTTS_ERR_STACK_NOT_READY
 *    MQTTS_OK
 * 
 * 
 */
unsigned char mqtts_willmsg_update(mqtts_WILLMSGUPD_Parms *pParms);


/**
 * Request for client state
 * 
 * Inputs: none
 * 
 * Returns: client state
 * 			
 * 
 */
unsigned char mqtts_get_state(void);



/***************************************************                                                   
 *   callback functions (to be implemented by the app)    
 ****************************************************                                                      
 */

/* CONNECT sent to gw, client is waiting for gw's answer */
void mqttscb_connect_sent(void);

/* client is connected to a gw, app can now start publishing, ... */
void mqttscb_connected(void);

/* client is disconnected
 * 		reason: reason of disconnection */
void mqttscb_disconnected(unsigned char reason);

/* REGACK received, app can now use the indicated topicID for publishing */
void mqttscb_regack_received(
		unsigned char topicID_0,
		unsigned char topicID_1,
		unsigned char returnCode);

/* PUBLISH received from gw/broker 
 * app should return immediately either with 
 * MQTTS_RET_ACCEPTED or MQTTS_RET_INVALID_TOPIC_ID
 * before calling any other mqtts function */
unsigned char mqttscb_publish_received(
		unsigned char dup,
		unsigned char qos,
		unsigned char topicID_0, /* Topic ID[0] */
		unsigned char topicID_1, /* Topic ID[1] */
		unsigned char *data,
		unsigned char data_len);

/* PUBACK received */ 
void mqttscb_puback_received(
		unsigned char topicID_0, /* Topic ID[0] */
		unsigned char topicID_1, /* Topic ID[1] */
		unsigned char returnCode);

/* TODO QoS Level 2 not supported yet */
/* we only need to inform app with PUBCOMP */
/* PUBREC received (QoS 2) */
/* void mqttscb_pubrec_received(void); */
/* PUBCOMP received (QoS 2) */
void mqttscb_pubcomp_received(void);

/* SUBACK received */
void mqttscb_suback_received(
		unsigned char qos,
		unsigned char topicID_0,
		unsigned char topicID_1,
		unsigned char returnCode);

/* UNSUBACK received */
void mqttscb_unsuback_received(void);

/* REGISTER received from gw */
void mqttscb_register_received(
		unsigned char topicID_0, /* Topic ID[0] */
		unsigned char topicID_1, /* Topic ID[1] */
		unsigned char *topic,
		unsigned char topic_len);    

/* WILLTOPICRESP received from gw */
void mqttscb_willtopicresp_received(void);

/* WILLMSGRESP received from gw */
void mqttscb_willmsgresp_received(void);

#endif

