#!/bin/bash

# BIT instrumentation
echo "source $HOME/instrumentation/java-config-rnl-vm.sh" >> $HOME/.bashrc
source $HOME/instrumentation/java-config-rnl-vm.sh
shopt -s globstar # '**' will expand to more than one directory
javac $HOME/instrumentation/**/*.java
javac $HOME/webserver/**/solver/*.java
javac $HOME/webserver/**/server/WebServer.java
mkdir -p $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver
java StatisticsTool -load_store  $HOME/webserver/pt/ulisboa/tecnico/cnv/solver $HOME/webserver-instrumented/pt/ulisboa/tecnico/cnv/solver

# Java server
mkdir $HOME/logs
touch $HOME/logs/server.log
echo "java -cp $HOME/webserver-instrumented:$HOME/webserver:$HOME/instrumentation:$HOME/instrumentation/bit-samples pt.ulisboa.tecnico.cnv.server.WebServer >> $HOME/logs/server.log 2>&1" | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
