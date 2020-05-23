#!/bin/bash

# Classpath
export CLASSPATH="$CLASSPATH:$HOME/loadbalancer:$HOME/database-client"
CLASSPATH_STR=$(echo $CLASSPATH) # save the classpath value as a string, with all the variables replaced with their absolute values
echo "export CLASSPATH='$CLASSPATH_STR'" >> $HOME/.bashrc

# '**' will expand to more than one directory
shopt -s globstar

# Compile java classes
javac $HOME/loadbalancer/**/*.java
javac $HOME/database-client/**/*.java

# Java server
mkdir $HOME/logs
touch $HOME/logs/server.log

# CLASSPATH_STR is the current value of the classpath, as a string. we use it because environment variables are not available when rc.local is executed
echo "su - ec2-user -c 'java -cp $CLASSPATH_STR pt.ulisboa.tecnico.cnv.balancer.BalancerServer >> $HOME/logs/server.log 2>&1'" | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
