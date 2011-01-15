package org.ensifera.CraftIRC;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;

import org.bukkit.Player;
import org.jibble.pircbot.*;
import org.bukkit.ChatColor;
import org.ensifera.CraftIRC.CraftIRC;
import org.ensifera.CraftIRC.util;

/**
 * @author Animosity
 * 
 */

public class Minebot extends PircBot implements Runnable {
	public static Minebot instance = null;
	private CraftIRC plugin = null;
	protected static final Logger log = Logger.getLogger("Minecraft");
	Properties ircSettings = new Properties();
	String ircSettingsFilename = "CraftIRC.settings";
	private static final Map<String, String> colorMap = new HashMap<String, String>();
	private static final Map<Integer, String> ircColorMap = new HashMap<Integer, String>();

	boolean bot_debug = false;
	String cmd_prefix;
	String irc_relayed_user_color;

	public String irc_handle;
	String irc_server, irc_server_port, irc_server_pass, irc_server_login, irc_message_delay;
	String irc_auth_method, irc_auth_username, irc_auth_pass;
	String irc_channel, irc_channel_pass, irc_admin_channel, irc_admin_channel_pass;
	String irc_colors; //Color code treatment - Default: Do nothing; "strip": Remove color codes sent from IRC; "equiv": Colorful messages
	Boolean irc_server_ssl = false;

	ArrayList<String> optn_main_req_prefixes = new ArrayList<String>(); // require IRC user (main) to have +/%/@/&/~ -- NOT IMPLEMENTED
	ArrayList<String> optn_admin_req_prefixes = new ArrayList<String>(); // require IRC user (admin) to have +/%/@/&/~
	ArrayList<String> optn_main_send_events = new ArrayList<String>(); // which MC events to send to main IRC channel
	ArrayList<String> optn_admin_send_events = new ArrayList<String>(); // which MC events to send to admin IRC channel
	ArrayList<String> optn_send_all_MC_chat = new ArrayList<String>(); // where to send MC chat e.g. main, admin
	HashMap<String, String> send_all_MC_chat_targets = new HashMap<String, String>();
	ArrayList<String> optn_send_all_IRC_chat = new ArrayList<String>(); // send IRC chat to MC? - now channel sources are selectable
	ArrayList<String> optn_ignored_IRC_command_prefixes = new ArrayList<String>(); // list of command prefixes to ignore in IRC, such as those for other bots.
	ArrayList<String> optn_req_MC_message_prefixes = new ArrayList<String>(); // list of message prefixes to ignore sending to IRC from MC. e.g. ChatChannels prefixes messages with a given channel's tag.
	ArrayList<String> optn_ignored_IRC_users = new ArrayList<String>(); // IRC users who are completely ignored
	String optn_notify_admins_cmd;
	ArrayList<String> optn_console_commands = new ArrayList<String>(); // whitelisted console commands to execute from IRC admin channel
	int bot_timeout = 5000; // how long to wait after joining channels to wait for the bot to check itself

	User[] irc_users_main, irc_users_admin;

	protected Minebot(CraftIRC pluginInstance) {
		plugin = pluginInstance;
	}

	public static synchronized Minebot getInstance(CraftIRC pluginInstance) {
		if (instance == null) {
			instance = new Minebot(pluginInstance);
		}
		return instance;
	}

	public synchronized void init() {

		this.initColorMap();
		try {
			ircSettings.load(new FileInputStream(ircSettingsFilename));
		} catch (IOException e) {
			log.info(CraftIRC.NAME + " - Error while READING settings file " + ircSettingsFilename);
			e.printStackTrace();
		}

		try {
			cmd_prefix = ircSettings.getProperty("command-prefix", ".").trim();
			if (colorMap.containsKey(ircSettings.getProperty("irc-relayed-user-color").toLowerCase())) {
				irc_relayed_user_color = colorMap.get(ircSettings.getProperty("irc-relayed-user-color").toLowerCase());
			} else {
				irc_relayed_user_color = colorMap.get("white");
			}

			irc_handle = ircSettings.getProperty("irc-handle", "CraftIRCBot").trim();

			irc_server = ircSettings.getProperty("irc-server").trim();
			irc_server_port = ircSettings.getProperty("irc-server-port").trim();
			irc_server_pass = ircSettings.getProperty("irc-server-password").trim();
			irc_server_ssl = Boolean.parseBoolean(ircSettings.getProperty("irc-server-ssl", "false").trim());

			irc_auth_method = ircSettings.getProperty("irc-auth-method").trim();
			irc_auth_username = ircSettings.getProperty("irc-auth-username").trim();
			irc_auth_pass = ircSettings.getProperty("irc-auth-password").trim();

			irc_channel = ircSettings.getProperty("irc-channel").toLowerCase().trim();
			irc_channel_pass = ircSettings.getProperty("irc-channel-password").trim();
			irc_admin_channel = ircSettings.getProperty("irc-admin-channel").toLowerCase().trim();
			irc_admin_channel_pass = ircSettings.getProperty("irc-admin-channel-password".trim());
			optn_send_all_MC_chat = this.getChatRelayChannels(ircSettings.getProperty("send-all-chat").toLowerCase()
					.trim(), "send-all-chat");
			optn_send_all_IRC_chat = this.getChatRelayChannels(ircSettings.getProperty("send-all-IRC").toLowerCase()
					.trim(), "send-all-IRC");
			optn_main_send_events = this.getCSVArrayList(ircSettings.getProperty("send-events").trim());
			optn_admin_send_events = this.getCSVArrayList(ircSettings.getProperty("admin-send-events").trim());
			this.irc_colors = ircSettings.getProperty("irc-colors", "").trim();

			try {
				bot_debug = Boolean.parseBoolean(ircSettings.getProperty("bot-debug").trim());
			} catch (Exception e) {
				bot_debug = false;
			}
			CraftIRC.setDebug(bot_debug);

			if (ircSettings.containsKey("notify-admins-cmd")) {
				optn_notify_admins_cmd = ircSettings.getProperty("notify-admins-cmd").trim();
				if (optn_notify_admins_cmd.length() < 1) {
					log.info(CraftIRC.NAME + " - no notify-admins-cmd set, disabling admin notification command.");
					optn_notify_admins_cmd = null;
				} else {
					if (!optn_notify_admins_cmd.startsWith("/")) {
						optn_notify_admins_cmd = "/" + optn_notify_admins_cmd;
					}
				}
			}

			// get the 'checkChannels()' delay from properties
			if (ircSettings.containsKey("bot-timeout")) {
				try {
					this.bot_timeout = 1000 * Integer.parseInt(ircSettings.getProperty("bot-timeout").trim(), 5000);
				} // get input in seconds, convert to ms
				catch (Exception e) {
					this.bot_timeout = 5000;
				}

			}

			if (ircSettings.containsKey("irc-admin-prefixes")) {
				this.optn_admin_req_prefixes = this.getCSVArrayList(ircSettings.getProperty("irc-admin-prefixes")
						.trim());

			}

			if (ircSettings.containsKey("irc-console-commands")) {
				this.optn_console_commands = this.getCSVArrayList(ircSettings.getProperty("irc-console-commands")
						.trim());

			} else {
				log.log(Level.WARNING, CraftIRC.NAME + " no irc-console-commands defined for the Admin IRC channel!");
			}

			if (ircSettings.containsKey("irc-server-login") && !ircSettings.getProperty("irc-server-login").isEmpty()) {
				irc_server_login = ircSettings.getProperty("irc-server-login").trim();
			} else {
				irc_server_login = this.irc_handle;
			}

			if (ircSettings.containsKey("irc-message-delay") && !ircSettings.getProperty("irc-message-delay").isEmpty()) {
				irc_message_delay = ircSettings.getProperty("irc-message-delay").trim();
				this.setMessageDelay(Long.parseLong(irc_message_delay));
			}

			if (ircSettings.containsKey("irc-ignored-command-prefixes")) {
				this.optn_ignored_IRC_command_prefixes = this.getCSVArrayList(ircSettings.getProperty(
						"irc-ignored-command-prefixes").trim());
			}

			if (ircSettings.containsKey("game-ignored-message-prefixes")) {
				this.optn_req_MC_message_prefixes = this.getCSVArrayList(ircSettings.getProperty(
						"game-ignored-message-prefixes").trim());
			}

			if (ircSettings.containsKey("irc-ignored-users")) {
				this.optn_ignored_IRC_users = this.getCSVArrayList(ircSettings.getProperty("irc-ignored-users").trim());
			}

		}

		catch (Exception e) {
			log.info(CraftIRC.NAME + " - Error while LOADING settings from " + this.ircSettingsFilename);
			e.printStackTrace();
		}

		/*if (irc_handle.isEmpty()) {
			this.irc_handle = "CraftIRCBot";
		}
		 */

		this.setName(this.irc_handle);
		this.setFinger(CraftIRC.NAME + " v" + CraftIRC.VERSION);
		this.setLogin(this.irc_server_login);
		this.setVersion(CraftIRC.NAME + " v" + CraftIRC.VERSION);

		try {
			this.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void initColorMap() {
		colorMap.put("black", ChatColor.BLACK.toString());
		colorMap.put("navy", ChatColor.DARK_AQUA.toString());
		colorMap.put("green", ChatColor.GREEN.toString());
		colorMap.put("blue", ChatColor.BLUE.toString());
		colorMap.put("red", ChatColor.DARK_RED.toString());
		colorMap.put("purple", ChatColor.DARK_PURPLE.toString());
		colorMap.put("gold", ChatColor.GOLD.toString());
		colorMap.put("lightgray", ChatColor.GRAY.toString());
		colorMap.put("gray", ChatColor.GRAY.toString());
		colorMap.put("darkpurple", ChatColor.DARK_PURPLE.toString());
		colorMap.put("lightgreen", ChatColor.GREEN.toString());
		colorMap.put("lightblue", ChatColor.BLUE.toString());
		colorMap.put("rose", ChatColor.RED.toString());
		colorMap.put("lightpurple", ChatColor.LIGHT_PURPLE.toString());
		colorMap.put("yellow", ChatColor.YELLOW.toString());
		colorMap.put("white", ChatColor.WHITE.toString());
		ircColorMap.put(0, ChatColor.WHITE.toString());
		ircColorMap.put(1, ChatColor.BLACK.toString());
		ircColorMap.put(2, ChatColor.DARK_AQUA.toString());
		ircColorMap.put(3, ChatColor.GREEN.toString());
		ircColorMap.put(4, ChatColor.RED.toString());
		ircColorMap.put(5, ChatColor.DARK_RED.toString());
		ircColorMap.put(6, ChatColor.DARK_PURPLE.toString());
		ircColorMap.put(7, ChatColor.GOLD.toString());
		ircColorMap.put(8, ChatColor.YELLOW.toString());
		ircColorMap.put(9, ChatColor.GREEN.toString());
		ircColorMap.put(10, ChatColor.DARK_BLUE.toString());
		ircColorMap.put(11, ChatColor.BLUE.toString());
		ircColorMap.put(12, ChatColor.DARK_PURPLE.toString());
		ircColorMap.put(13, ChatColor.LIGHT_PURPLE.toString());
		ircColorMap.put(14, ChatColor.DARK_GRAY.toString());
		ircColorMap.put(15, ChatColor.GRAY.toString());
	}

	public Integer getIRCColor(String mccolor) {
		for (Map.Entry<Integer, String> entry : ircColorMap.entrySet()) {
			if (entry.getValue().equals(mccolor)) {
				return entry.getKey();
			}
		}
		return 0;
	}

	/*public String colorizePlayer(Player player) {
		String playerColorPrefix = "";
		if (this.irc_colors.equalsIgnoreCase("equiv")) {
			String[] splitPlayerColorPrefix = player.getColor().split("§");
			for (int i = 1; i < splitPlayerColorPrefix.length; i++) {
				playerColorPrefix += Character.toString((char) 3) + this.getIRCColor("§" + splitPlayerColorPrefix[i].substring(0,1));
	
				if (splitPlayerColorPrefix[i].length() > 1) { 
					playerColorPrefix += splitPlayerColorPrefix[i].substring(1,splitPlayerColorPrefix[i].length());
				}
			}
			return playerColorPrefix + player.getName() + Character.toString((char) 15);
		}
		else {
			return player.getName();
		}
	}*/

	public String colorizePlayer(Player player) {
		return player.getName();
	}

	// Sets the directionality for MC->IRC chat (channels are targets)
	// And also sets the channel sources for IRC->MC chat
	private ArrayList<String> getChatRelayChannels(String csv_relay_channels, String propertyName)
	// Modified to deal w/ send-all-IRC, in addition to the existing send-all-chat support
	{
		ArrayList<String> relayChannels = this.getCSVArrayList(csv_relay_channels);

		// backward compatibility w/ boolean argument of past
		if (relayChannels.contains("true")) {
			relayChannels.clear();
			relayChannels.add("main");
			relayChannels.add("admin");
		}

		if ((!relayChannels.contains("main") && !relayChannels.contains("admin"))) {
			log.info(CraftIRC.NAME + " - No valid Minecraft chat relay channels set, disabling feature \""
					+ propertyName + "\"");
			return new ArrayList<String>(Arrays.asList(""));
		}
		return relayChannels;
	}

	// Spit out an ArrayList from CSV string
	private ArrayList<String> getCSVArrayList(String csv_string) {
		return new ArrayList<String>(Arrays.asList(csv_string.toLowerCase().split(",")));
	}

	public void start() {

		log.info(CraftIRC.NAME + " v" + CraftIRC.VERSION + " loading.");

		try {
			this.setAutoNickChange(true);

			if (this.irc_server_ssl) {
				log.info(CraftIRC.NAME + " - Connecting to " + this.irc_server + ":" + this.irc_server_port + " [SSL]");
				this.connect(this.irc_server, Integer.parseInt(this.irc_server_port), this.irc_server_pass,
						new TrustingSSLSocketFactory());
			} else {
				log.info(CraftIRC.NAME + " - Connecting to " + this.irc_server + ":" + this.irc_server_port);
				this.connect(this.irc_server, Integer.parseInt(this.irc_server_port), this.irc_server_pass);
			}

			if (this.isConnected()) {
				log.info(CraftIRC.NAME + " - Connected");
			} else {
				log.info(CraftIRC.NAME + " - Connection failed!");
			}

			this.authenticateBot(); // will always GHOST own registered nick if auth method is nickserv

			this.joinChannel(irc_channel, irc_channel_pass);
			this.joinAdminChannel();

			Timer timer = new Timer();
			Date checkdelay = new Date();
			checkdelay.setTime(checkdelay.getTime() + this.bot_timeout);
			timer.schedule(new CheckChannelsTask(), checkdelay);

		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (NickAlreadyInUseException e) {
			this.authenticateBot();
			this.joinChannel(irc_channel, irc_channel_pass);
			this.joinAdminChannel();
			Timer timer = new Timer();
			Date checkdelay = new Date();
			checkdelay.setTime(checkdelay.getTime() + this.bot_timeout);
			timer.schedule(new CheckChannelsTask(), checkdelay);

		} catch (IOException e) {
			e.printStackTrace();

		} catch (IrcException e) {
			e.printStackTrace();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	void authenticateBot() {
		if (this.irc_auth_method.equalsIgnoreCase("nickserv") && !irc_auth_pass.isEmpty()) {
			log.info(CraftIRC.NAME + " - Using Nickserv authentication.");
			this.sendMessage("nickserv", "GHOST " + irc_handle + " " + irc_auth_pass);
			// this.setAutoNickChange(false);

			// Some IRC servers have quite a delay when ghosting...
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			this.changeNick(irc_handle);
			this.identify(irc_auth_pass);

		} else if (this.irc_auth_method.equalsIgnoreCase("gamesurge")) {
			log.info(CraftIRC.NAME + " - Using GameSurge authentication.");
			this.changeNick(irc_handle);
			this.sendMessage("AuthServ@Services.GameSurge.net", "AUTH " + irc_auth_username + " " + irc_auth_pass);

		} else if (this.irc_auth_method.equalsIgnoreCase("quakenet")) {
			log.info(CraftIRC.NAME + " - Using QuakeNet authentication.");
			this.changeNick(irc_handle);
			this.sendMessage("Q@CServe.quakenet.org", "AUTH " + irc_auth_username + " " + irc_auth_pass);
		}

	}

	// Obsoleting.

	public void joinAdminChannel() {
		if (irc_admin_channel == null || irc_admin_channel.equals("")) {
			optn_admin_send_events.clear(); // clear any event option because we
											// don't have anywhere to send them
			return;
		}

		else {
			this.joinChannel(irc_admin_channel, irc_admin_channel_pass);
		}

	}

	// Determine which of the selected channels the bot is actually present in -
	// disable features if not in the required channels.
	void checkChannels() {
		ArrayList<String> botChannels = this.getChannelList();
		if (!botChannels.contains(this.irc_channel)) {
			log.info(CraftIRC.NAME + " - " + this.getNick() + " not in main channel: " + this.irc_channel
					+ ", disabling all events for channel");
			this.optn_main_send_events.clear();
			this.optn_send_all_IRC_chat.remove("main");
			this.optn_send_all_MC_chat.remove("main");
			this.optn_main_req_prefixes.clear();

		} else {
			log.info(CraftIRC.NAME + " - Joined main channel: " + this.irc_channel);

		}

		if (!botChannels.contains(this.irc_admin_channel)) {

			log.info(CraftIRC.NAME + " - " + this.getNick() + " not in admin channel: " + this.irc_admin_channel
					+ ", disabling all events for channel");
			this.optn_admin_send_events.clear();
			this.optn_send_all_IRC_chat.remove("admin");
			this.optn_send_all_MC_chat.remove("admin");
			this.optn_notify_admins_cmd = null;
			this.optn_admin_req_prefixes.clear();

		} else {
			log.info(CraftIRC.NAME + " - Joined admin channel: " + this.irc_admin_channel);
		}

	}

	// Update users
	public void onJoin(String channel, String sender, String login, String hostname) {
		if (channel.equalsIgnoreCase(this.irc_channel)) {
			this.irc_users_main = this.getUsers(channel);
		}
		if (channel.equalsIgnoreCase(this.irc_admin_channel)) {
			this.irc_users_admin = this.getUsers(channel);
		}

		// if irc-joins, send event to game
	}

	// Update users
	public void onPart(String channel, String sender, String login, String hostname) {
		if (channel.equalsIgnoreCase(this.irc_channel)) {
			this.irc_users_main = this.getUsers(channel);
		}
		if (channel.equalsIgnoreCase(this.irc_admin_channel)) {
			this.irc_users_admin = this.getUsers(channel);
		}

		// if irc-quits, send event to game
	}

	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason) {
		if (recipientNick.equalsIgnoreCase(this.getNick())) {
			if (channel.equalsIgnoreCase(this.irc_channel)) {
				this.joinChannel(this.irc_channel, this.irc_channel_pass);
			}
			if (channel.equalsIgnoreCase(this.irc_admin_channel)) {
				this.joinChannel(this.irc_admin_channel, this.irc_admin_channel_pass);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onMessage(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void onMessage(String channel, String sender, String login, String hostname, String message) {

		if (this.optn_ignored_IRC_users.contains(sender.toLowerCase())) {
			return;
		}

		String[] splitMessage = message.split(" ");
		String command = util.combineSplit(1, splitMessage, " ");

		try {

			// Parse admin commands here
			if (channel.equalsIgnoreCase(this.irc_admin_channel) && userAuthorized(channel, sender)) {

				if ((message.startsWith(cmd_prefix + "console") || message.startsWith(cmd_prefix + "c"))
						&& splitMessage.length > 1 && this.optn_console_commands.contains(splitMessage[1])) {
					this.sendNotice(sender, "NOT YET IMPLEMENTED IN BUKKIT");
					/*log.info(CraftIRC.NAME + " - " + channel + " - " + sender + " used: "
							+ this.combineSplit(0, splitMessage, " "));
					// Have to call parseConsoleCommand first if you want to use any of the hMod console commands
					this.sendNotice(sender, "Executed: " + command); // send notice first, in case you're disabling/reloading the bot
					if (!etc.getInstance().parseConsoleCommand(command, etc.getMCServer())) {
						etc.getServer().useConsoleCommand(command);
					}*/
					return;

				}

				/*
				 //Aliased commands
				
				 else if ((message.startsWith(cmd_prefix + "kick") && this.optn_console_commands.contains("kick")) && splitMessage.length > 1) {
					log.info(CraftIRC.NAME + " - " + channel + " - " + sender + " used: " + this.combineSplit(0, splitMessage, " "));
					etc.getServer().useConsoleCommand("kick " + this.combineSplit(1, splitMessage, " "));
					this.sendNotice(sender, "Executed: " + command);
					return;
				}
				
				else if ((message.startsWith(cmd_prefix + "ban") && this.optn_console_commands.contains("ban")) && splitMessage.length > 1) {
					log.info(CraftIRC.NAME + " - " + channel + " - " + sender + " used: " + this.combineSplit(0, splitMessage, " "));
					etc.getServer().useConsoleCommand("ban " + this.combineSplit(1, splitMessage, " "));
					this.sendNotice(sender, "Executed: " + command);
					return;
				}
				
				else if (message.startsWith(cmd_prefix + "broadcast") && this.optn_console_commands.contains("say") && splitMessage.length > 1) {
					log.info(CraftIRC.NAME + " - " + channel + " - " + sender + " used: " + this.combineSplit(0, splitMessage, " "));
					etc.getServer().useConsoleCommand("say " + this.combineSplit(1, splitMessage, " "));
					this.sendNotice(sender, "Executed: " + command);
					return;
				}*/

				else if (message.startsWith(cmd_prefix + "botsay") && this.getChannelList().contains(this.irc_channel)
						&& splitMessage.length > 1) {
					this.sendMessage(this.irc_channel, command);
					this.sendNotice(sender, "Sent to main channel: " + command);
					return;
				}

				else if (message.startsWith(cmd_prefix + "raw")) {
					// message = message.substring(message.indexOf(" ")).trim();
					if (splitMessage.length > 1) {
						this.sendRawLine(util.combineSplit(1, splitMessage, " "));
						this.sendNotice(sender, "Raw IRC string sent");
						return;
					}
				}

			} // end admin commands

			// begin public commands

			// .players - list players
			if (message.startsWith(cmd_prefix + "players")) {
				String playerlist = this.getPlayerList();
				this.sendMessage(channel, playerlist); // set this to reply to the
														// channel it was requested
														// from
				return;
			}

			// Send all IRC chatter from main channel - (no command prefixes or ignored command prefixes)
			if (channel.equalsIgnoreCase(this.irc_channel) && this.optn_send_all_IRC_chat.contains("main")) {
				if (!message.startsWith(cmd_prefix)
						&& !this.optn_ignored_IRC_command_prefixes.contains(splitMessage[0].charAt(0))) {
					msgToGame(sender, message, messageMode.MSG_ALL, null);

				}

			}

			// Send all IRC chatter from admin channel - (no command prefixes or ignored command prefixes)
			else if (channel.equalsIgnoreCase(this.irc_admin_channel) && this.optn_send_all_IRC_chat.contains("admin")) {
				if (!message.startsWith(cmd_prefix)
						&& !this.optn_ignored_IRC_command_prefixes.contains(splitMessage[0].charAt(0))) {
					msgToGame(sender, message, messageMode.MSG_ALL, null);

				}

			}

			// .say - Send single message to the game
			else if (message.startsWith(cmd_prefix + "say") || message.startsWith(cmd_prefix + "mc")) {
				// message = message.substring(message.indexOf(" ")).trim();
				if (splitMessage.length > 1) {
					msgToGame(sender, command, messageMode.MSG_ALL, null);
					this.sendNotice(sender, "Message sent to game");
					return;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.log(Level.SEVERE, CraftIRC.NAME + " - error while relaying IRC command: " + message);
		}

	}

	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		try {
			String[] splitMessage = message.split(" ");
			ArrayList<Player> playerList = new ArrayList<Player>(Arrays.asList(plugin.getServer().getOnlinePlayers()));

			if (splitMessage.length > 1 && splitMessage[0].equalsIgnoreCase("tell")) {
				if (plugin.getServer().getPlayer(splitMessage[1]) != null) {
					this.msgToGame(sender, util.combineSplit(2, splitMessage, " "), messageMode.MSG_PLAYER,
							splitMessage[1]);
					this.sendNotice(sender, "Whispered to " + splitMessage[1]);
				}
			}

			// if no 'tell' then assume whisper-target is set (hashmap)
			// get whisper-target for IRC handle
			// 

		} catch (Exception e) {

		}
	}

	public void onAction(String sender, String login, String hostname, String target, String action) {

		if (this.optn_send_all_IRC_chat.contains("main") || this.optn_send_all_IRC_chat.contains("admin")) {
			msgToGame(sender, action, messageMode.ACTION_ALL, null);

		}

	}

	// IRC user authorization check against prefixes
	// Currently just for admin channel as first-order level of security
	public boolean userAuthorized(String channel, String user) {
		if (channel.equalsIgnoreCase(this.irc_admin_channel)) {
			User[] adminUsers = (User[]) super.getUsers(channel).clone(); // I just want a copy of it god damnit
			//User[] adminUsers = irc_users_admin;
			// may get NPE if user is disconnected
			try {
				for (int i = 0; i < adminUsers.length; i++) {
					User iterUser = adminUsers[i];
					if (iterUser.getNick().equalsIgnoreCase(user)
							&& this.optn_admin_req_prefixes.contains(iterUser.getPrefix())) {
						return true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return false;
	}

	// Form and broadcast messages to Minecraft
	public void msgToGame(String sender, String message, messageMode mm, String targetPlayer) {

		try {
			if (this.irc_colors.equalsIgnoreCase("strip")) {
				message = message.replaceAll(
						"(" + Character.toString((char) 2) + "|" + Character.toString((char) 15) + "|"
								+ Character.toString((char) 22) + Character.toString((char) 31) + "|"
								+ Character.toString((char) 3) + "[0-9]{0,2}(,[0-9]{0,2})?)", "");
			}
			if (this.irc_colors.equalsIgnoreCase("equiv")) {
				message = message.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 22)
						+ "|" + Character.toString((char) 31) + ")", "");
				message = message.replaceAll(Character.toString((char) 15), this.ircColorMap.get(0));
				Pattern color_codes = Pattern.compile(Character.toString((char) 3) + "([01]?[0-9])(,[0-9]{0,2})?");
				Matcher find_colors = color_codes.matcher(message);
				while (find_colors.find()) {
					message = find_colors.replaceFirst(this.ircColorMap.get(Integer.parseInt(find_colors.group(1))));
					find_colors = color_codes.matcher(message);
				}
				message = message + " ";
			}

			Player player;
			String msg_to_broadcast;
			// MESSAGE TO ALL PLAYERS
			switch (mm) {
			case MSG_ALL:
				if (CraftIRC.isDebug()) {
					log.info(String.format(CraftIRC.NAME + " msgToGame(all) : <%s> %s", sender, message));
				}
				msg_to_broadcast = (new StringBuilder()).append("[IRC]").append(" <").append(irc_relayed_user_color)
						.append(sender).append(ChatColor.WHITE).append("> ").append(message).toString();

				for (Player p : plugin.getServer().getOnlinePlayers()) {
					if (p != null) {
						p.sendMessage(msg_to_broadcast);
					}
				}
				break;

			// ACTION
			case ACTION_ALL:
				if (CraftIRC.isDebug()) {
					log.info(String.format(CraftIRC.NAME + " msgToGame(action) : <%s> %s", sender, message));
				}
				msg_to_broadcast = (new StringBuilder()).append("[IRC]").append(irc_relayed_user_color).append(" * ")
						.append(sender).append(" ").append(message).toString();

				for (Player p : plugin.getServer().getOnlinePlayers()) {
					if (p != null) {
						p.sendMessage(msg_to_broadcast);
					}
				}
				break;

			// MESSAGE TO 1 PLAYER
			case MSG_PLAYER:
				if (CraftIRC.isDebug()) {
					log.info(String.format(CraftIRC.NAME + " msgToGame(player) : <%s> %s", sender, message));
				}
				msg_to_broadcast = (new StringBuilder()).append("[IRC privmsg]").append(" <")
						.append(irc_relayed_user_color).append(sender).append(ChatColor.WHITE).append("> ")
						.append(message).toString();
				Player p = plugin.getServer().getPlayer(targetPlayer);
				if (p != null) {
					p.sendMessage(msg_to_broadcast);
				}

				break;
			} //end switch

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Return the # of players and player names on the Minecraft server
	private String getPlayerList() {
		Player onlinePlayers[] = plugin.getServer().getOnlinePlayers();
		Integer playercount = 0;

		//Integer maxplayers;
		StringBuilder sb = new StringBuilder();

		//for (Player p : plugin.getServer().getOnlinePlayers())  {
		for (int i = 0; i < onlinePlayers.length; i++) {
			if (onlinePlayers[i] != null) {
				playercount++;
				if (this.irc_colors.equalsIgnoreCase("equiv")) {
					sb.append(" ").append(this.colorizePlayer(onlinePlayers[i]));
				} else {
					sb.append(" ").append(onlinePlayers[i].getName());
				}
			}
		}

		if (playercount > 0) {
			//return "Online (" + playercount + "/" + maxplayers + "): " + sb.toString();
			return "Online: " + sb.toString();
		} else {
			return "nobody is minecrafting right now";
		}
	}

	public ArrayList<String> getChannelList() {

		try {
			return new ArrayList<String>(Arrays.asList(this.getChannels()));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// Bot restart upon disconnect, if the plugin is still enabled
	public void onDisconnect() {
		try {
			if (this.instance != null && plugin.isEnabled()) {
				log.info(CraftIRC.NAME + " - disconnected from IRC server... reconnecting!");
				plugin.recover();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void msg(String target, String message) {
		if (CraftIRC.isDebug()) {
			log.info(String.format(CraftIRC.NAME + " msgToIRC <%s> : %s", target, message));
		}
		sendMessage(target, message);
	}

	public class CheckChannelsTask extends TimerTask {
		public void run() {
			Minebot.instance.checkChannels();
		}
	}

	@Override
	public void run() {
		this.init();
	}

	private enum messageMode {
		MSG_ALL, ACTION_ALL, MSG_PLAYER
	}

}// EO Minebot

