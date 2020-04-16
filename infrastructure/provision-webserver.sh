#!/bin/bash

# Classpath
echo 'export CLASSPATH="$CLASSPATH:$HOME/webserver-instrumented:$HOME/webserver:$HOME/instrumentation:$HOME/instrumentation/bit-samples"' >> $HOME/.bashrc
export CLASSPATH="$CLASSPATH:$HOME/webserver-instrumented:$HOME/webserver:$HOME/instrumentation:$HOME/instrumentation/bit-samples"

# BIT instrumentation
mkdir -p $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver
shopt -s globstar       # '**' will expand to more than one directory
javac $HOME/instrumentation/**/*.java
javac $HOME/webserver/**/solver/*.java
javac $HOME/webserver/**/data/*.java
java BasicBlocks  $HOME/webserver/pt/ulisboa/tecnico/cnv/solver $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver
javac $HOME/webserver/**/server/WebServer.java

# Java server
mkdir $HOME/logs
touch $HOME/logs/server.log

CLASSPATH_STR=$(echo $CLASSPATH)  # get the value of the variable because environment variables are not available when rc.local is executed
echo "su - ec2-user -c 'java -cp $CLASSPATH_STR pt.ulisboa.tecnico.cnv.server.WebServer >> $HOME/logs/server.log 2>&1'" | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
