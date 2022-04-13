This package contains the Java source code of an MQTT-SN gateway, which could be used to connect
MQTT-SN clients to an MQTT broker. The main class is defined in
com.ibm.zurich.mqttsgw.Gateway.java

The gateway connects itself to an MQTT broker via the TCP port 1883 and listens on UDP port 20000
for incoming client connections. These ports numbers, and other variables, are defined in the
file gateway.properties.
