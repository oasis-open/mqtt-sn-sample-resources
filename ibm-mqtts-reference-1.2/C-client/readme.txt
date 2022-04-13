This package provides a reference implementation of the client side of the MQTT-SN (MQTT for Sensor Networks) 
protocol.

The reference implementation consists of the following files: - application.c 	This is a sample application which demonstrates how the MQTT-SN client API could be used.- mqtts/mqtts_api.h	This file defines the MQTT-SN client API, which includes verbs such as connect, 
                        disconnect, register, publish, etc. for communicating with the gateway/broker. 
                        The use of these verbs is explained below. It also defines the various parameters 
                        values required for the operation of the MQTT-SN protocol.- mqtts/mqtts-core.c 	This file provides the platform-neutral (generic) reference implementation of 
                        the client. It contains the client handling of the MQTT-SN protocol. - mqtts/mqtts-timer.c	This is the implementation of timer functions which are required by the MQTT-SN protocol.- mqtts/mqtts-timer.h	The header file for the timer functions.- mqtts/gp_api.h	This header file defines the interface between the platform-neutral client implementation 
                        and a generic platform. To port the reference implementation to a specific hardware platform, 
                        the functions defined in this header file have to be implemented.
To port the client reference implementation to a specific hardware platform, the following functions need to be 
implemented by the platform (see also file gp_api.h):1.	unsigned char gp_timer_new(void (*timeout_funccb)(void));This function is called by the client to request the creation of a new timer. The pointer to the function 
to be called back when the timer times out is given as parameter. If the timer could be created, 
the platform assigns to it a one-byte identity and returns that identity to the MQTT-SN client. The client 
will use this identity later on to start, stop, and end the timer. If the timer could not be created, 
the value 0xFF should returned.2.	void gp_timer_start(unsigned char id, unsigned char msb, unsigned char lsb);This function is called by the client to request the start of a timer. The byte id is the identity of the 
timer that should be started (this id was returned to the client when the timer was created). The two-byte long 
time-out value (in seconds) is indicated by msb and lsb, with msb containing the most significant byte and lsb 
the least significant byte of the value.3.	void gp_timer_stop(unsigned char id);This function is called by the client to request the stop a timer. The identity of the timer to be stopped 
is indicated by the parameter id.4.	void gp_timer_end(unsigned char id);This function is called by the client to indicate that it does not need a timer anymore. The identity of the 
timer to be freed is indicated by the parameter id. 5.	void gp_network_msg_send(unsigned char *msg, unsigned char *dest, unsigned char length);This function is called by the client when it wants to send a MQTT-SN message. The pointer *msg points to 
the first byte of the array which contains the MQTT-SN message to be sent. The length of the message is indicated 
by this first byte. The array containing the destination address is pointed by *dest and its length is indicated 
by the parameter length.6.	void gp_network_msg_broadcast(unsigned char *msg, unsigned char radius);This function is called by the client when it wants to broadcast a MQTT-SN message. The pointer *msg points 
to the first byte of the array containing the MQTT-SN message to be broadcasted. The length of the MQTT-SN 
message is indicated by this first byte. The parameter radius indicates the broadcast radius.7.	void gpcb_network_msg_received(unsigned char *msg, unsigned char *sender, unsigned char length);This callback function is called by the platform when it receives a MQTT-SN message. The first byte of the 
message received is indicated by the pointer *msg; this first byte also contains the length of the message. 
The address of the sender is indicated by the pointer *sender, and its length by the parameter length. 
The platform can release the buffer containing the message when this function returns.8.	unsigned char gp_byte_get(unsigned char *msg, unsigned char index);This function is called by the client to get a byte of a MQTT-SN message just received by the platform 
and indicated to it by the callback function gpcb_network_msg_received(…). The pointer *msg is the one 
in the callback function, and the parameter index is the index of the byte to get. Note that the first 
byte of the MQTT-SN message has the index 1! Note also that the client will call this function only 
while it processes the callback function gpcb_network_msg_received(…).
