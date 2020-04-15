#!/bin/bash

# install jdk 1.7
sudo yum -y update
sudo yum -y install java-1.7.0-openjdk-devel.x86_64

# install aws java sdk
mkdir $HOME/aws-java-sdk
cd $HOME/aws-java-sdk
wget https://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip
unzip aws-java-sdk.zip
rm aws-java-sdk.zip

# .bashrc
echo "shopt -s globstar" >> $HOME/.bashrc                 # '**' will expand to more than one directory
echo "source java-config-rnl-vm.sh" >> $HOME/.bashrc      # setup jdk and aws-java-sdk classpaths and options
