export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export JAVA_ROOT=/usr/lib/jvm/java-7-openjdk-amd64/
export JDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export JRE_HOME=/usr/lib/jvm/java-7-openjdk-amd64/jre
export PATH=/usr/lib/jvm/java-7-openjdk-amd64/bin/:$PATH
export SDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
export _JAVA_OPTIONS="-XX:-UseSplitVerifier "$_JAVA_OPTIONS


## Setting the Classpath

AWS_SDK_PATH="$HOME/aws-java-sdk/aws-*"

# convert the glob expression to a string. We need this because brace expansion is done before variable expansion.
AWS_SDK_STR=$(echo $AWS_SDK_PATH)

AWS_1="$AWS_SDK_STR/lib/aws-java-sdk-1.??.???.jar"
AWS_1_STR=$(echo $AWS_1)

AWS_2="$AWS_SDK_STR/third-party/lib/*"

export CLASSPATH="$CLASSPATH:$AWS_1_STR:$AWS_2"
