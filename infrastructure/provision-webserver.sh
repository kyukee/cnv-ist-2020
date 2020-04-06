#!/bin/bash

# BIT instrumentation
echo "source $HOME/instrumentation/java-config-rnl-vm.sh" >> $HOME/.bashrc
echo "source $HOME/instrumentation/config-bit.sh" >> $HOME/.bashrc
source $HOME/instrumentation/java-config-rnl-vm.sh
source $HOME/instrumentation/config-bit.sh
javac $HOME/instrumentation/**/*.java
javac $HOME/webserver/*.java
mkdir $HOME/webserver/instrumented-output
java StatisticsTool -load_store  $HOME/webserver $HOME/webserver/instrumented-output

# Java server
echo "java -cp $HOME/instrumentation/samples-bit:$HOME/webserver/instrumented-output WebServer" | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
