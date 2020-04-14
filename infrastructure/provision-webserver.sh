#!/bin/bash

# BIT instrumentation
echo "source $HOME/instrumentation/java-config-rnl-vm.sh" >> $HOME/.bashrc
source $HOME/instrumentation/java-config-rnl-vm.sh
echo "shopt -s globstar" >> $HOME/.bashrc
shopt -s globstar # '**' will expand to more than one directory
javac $HOME/instrumentation/**/*.java
javac $HOME/webserver/**/solver/*.java
mkdir -p $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver
java BasicBlocks  $HOME/webserver/pt/ulisboa/tecnico/cnv/solver $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver
javac $HOME/webserver/**/server/WebServer.java

# Java server
mkdir $HOME/logs
touch $HOME/logs/server.log
echo "su - ec2-user -c 'java -cp $HOME/webserver-instrumented:$HOME/webserver:$HOME/instrumentation:$HOME/instrumentation/bit-samples pt.ulisboa.tecnico.cnv.server.WebServer >> $HOME/logs/server.log 2>&1'" | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
