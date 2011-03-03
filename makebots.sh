if [ $# -gt 0 ]; then
    NUM=$1
else
    NUM=3
fi

DIR="bot_output"
NUM_BOTS=0

while [ $NUM_BOTS -lt $NUM ]; do
    java -classpath pircbot.jar:bin BotnetClient > "${DIR}/bot${NUM_BOTS}.txt" &
    NUM_BOTS=`expr $NUM_BOTS + 1`
    sleep 3
done

echo "Created $NUM bots in the background. Their output is dumped in /${DIR}/botX.txt"