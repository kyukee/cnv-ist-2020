export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export JAVA_ROOT=/usr/lib/jvm/java-7-openjdk-amd64/
export JDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export JRE_HOME=/usr/lib/jvm/java-7-openjdk-amd64/jre
export PATH=/usr/lib/jvm/java-7-openjdk-amd64/bin/:$PATH
export SDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export _JAVA_OPTIONS="-XX:-UseSplitVerifier "$_JAVA_OPTIONS

AWS_SDK_PATH="$HOME/aws-java-sdk/aws-*"
export CLASSPATH="$CLASSPATH:$AWS_SDK_PATH/lib/aws-java-sdk-1.??.???.jar:$AWS_SDK_PATH/third-party/lib/*"
