#!/bin/bash
#1. park_base_url is park's server address, localhost can be set if yttps deplyed
#on the same machine, or you have to set a real address like "192.168.1.6".
#
#2.visitor_confirm_url is the public network website address for customer to confirm
#visitor.
ytpark_user='ubuntu'
cd /home/$ytpark_user/yttps/
nohup /home/$ytpark_user/park_deploy/infrastructure/shared/jdk/bin/java -jar iot-0.0.1.jar >/dev/null &
