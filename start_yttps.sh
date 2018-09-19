#!/bin/bash
#1. park_base_url is park's server address, localhost can be set if yttps deplyed
#on the same machine, or you have to set a real address like "192.168.1.6".
#
#2.visitor_confirm_url is the public network website address for customer to confirm
#visitor.
cd /home/yituadmin/yttps
java -jar iot-0.0.1.jar \
	 --park_base_url=http://127.0.0.1:9812 \
     --visitor_confirm_url=http://127.0.0.1/yttps \
	 --park_username=admin \
	 --park_password=admin \
	 > /dev/null &
