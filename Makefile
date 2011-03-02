dir = bot_output

#Creates n bots in teh backgound and dumps their outputs into files located in bot_output/botX.txt
bots :
	mkdir -p $(dir);
	chmod +x makebots.sh
	./makebots.sh $(n) &

#Creates the master bot in the foreground
master :
	java -classpath pircbot.jar:bin BotnetServer;

#Kills all background bot processes and deletes the bot_output directory
clean :
	killall java;
	rm -r $(dir);