#!/bin/bash

# Classpath
echo 'export CLASSPATH="$CLASSPATH:$HOME/webserver-instrumented:$HOME/webserver:$HOME/instrumentation:$HOME/instrumentation/bit-samples"' >> $HOME/.bashrc
export CLASSPATH="$CLASSPATH:$HOME/webserver-instrumented:$HOME/webserver:$HOME/database-client:$HOME/instrumentation:$HOME/instrumentation/bit-samples"

# '**' will expand to more than one directory
shopt -s globstar

# Compile java classes
javac $HOME/instrumentation/**/*.java
javac $HOME/webserver/**/solver/*.java
javac $HOME/webserver/**/data/*.java
javac $HOME/database-client/**/*.java

# BIT instrumentation
mkdir -p $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver
java BasicBlocks  $HOME/webserver/pt/ulisboa/tecnico/cnv/solver $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver
javac $HOME/webserver/**/server/WebServer.java

# Java server
mkdir $HOME/logs
touch $HOME/logs/server.log

CLASSPATH_STR=$(echo $CLASSPATH)  # use the current value of the classpath because environment variables are not available when rc.local is executed
echo "su - ec2-user -c 'java -cp $CLASSPATH_STR pt.ulisboa.tecnico.cnv.server.WebServer >> $HOME/logs/server.log 2>&1'" | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
