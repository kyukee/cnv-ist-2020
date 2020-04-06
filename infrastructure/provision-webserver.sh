#!/bin/bash

sudo yum -y update
sudo yum -y install java-1.7.0-openjdk-devel.x86_64
javac $HOME/webserver/WebServer.java
echo "java -cp $HOME/webserver WebServer" | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
