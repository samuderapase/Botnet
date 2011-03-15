dir = bot_output

all : 
	make -f Makefile clean
	make -f Makefile bots n=3
	sleep 15
	make -f Makefile master

#Creates n bots in the backgound and dumps their outputs into files located in bot_output/botX.txt
bots :
	mkdir -p $(dir);
	chmod +x makebots.sh
	./makebots.sh $(n) &

#Creates the master bot in the foreground
master :
	java -classpath pircbot.jar:mail.jar:bin BotnetServer

#Creates a LeaseMaster bot
leaseMaster :
	java -classpath pircbot.jar:commons-codec-1.4.jar:bin BotnetLeaseServer

#Kills all background bot processes and deletes the bot_output directory
clean :
	killall java;
	rm -r $(dir);