package org.bukkit.animosity.craftirc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import org.bukkit.entity.Player;
import org.jibble.pircbot.*;
import org.bukkit.ChatColor;
import org.bukkit.animosity.craftirc.CraftIRC;
import org.bukkit.animosity.craftirc.Util;

import org.bukkit.util.config.CraftIRCConfigurationNode;

/**
 * @author Animosity
 * @author Protected
 */
public class Minebot extends PircBot implements Runnable {

	private CraftIRC plugin = null;
	private int botId;
	
	private String nickname;
	
	// Connection attributes
	private boolean ssl;
	private String ircServer;
	private int ircPort;
	private String ircPass;
	private int timeout;
	
	// Nickname authentication
	private String authMethod;
	private String authUser;
	private String authPass;
	
	// Channel attributes
	private ArrayList<String> channels;
	
	// Other things that may be more efficient to store here
	private ArrayList<String> ignores;
	private String cmdPrefix;

	protected Minebot(CraftIRC plugin, int botId) {
		this.plugin = plugin;
		this.botId = botId;
	}

	public synchronized void init() {
		this.setMessageDelay(plugin.cBotMessageDelay(botId));
		this.setName(plugin.cBotNickname(botId));
		this.setFinger(CraftIRC.NAME + " v" + CraftIRC.VERSION);
		this.setLogin(plugin.cBotLogin(botId));
		this.setVersion(CraftIRC.NAME + " v" + CraftIRC.VERSION);

		nickname = this.plugin.cBotNickname(botId);
		
		ssl = this.plugin.cBotSsl(botId);
		ircServer = this.plugin.cBotServer(botId);
		ircPort = this.plugin.cBotPort(botId);
		ircPass = this.plugin.cBotPassword(botId);
		timeout = this.plugin.cBotTimeout(botId);
		
		authMethod = this.plugin.cBotAuthMethod(botId);
		authUser = this.plugin.cBotAuthUsername(botId);
		authPass = this.plugin.cBotAuthPassword(botId);
		
		channels = this.plugin.cBotChannels(botId);
		
		ignores = this.plugin.cBotIgnoredUsers(botId);
		cmdPrefix = this.plugin.cCommandPrefix(botId);

		try {
			this.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start() {

		CraftIRC.log.info(CraftIRC.NAME + " v" + CraftIRC.VERSION + " loading.");

		try {
			this.setAutoNickChange(true);

			if (this.ssl) {
				CraftIRC.log.info(CraftIRC.NAME + " - Connecting to " + this.ircServer + ":" + this.ircPort + " [SSL]");
				this.connect(this.ircServer, this.ircPort, this.ircPass, new TrustingSSLSocketFactory());
			} else {
				CraftIRC.log.info(CraftIRC.NAME + " - Connecting to " + this.ircServer + ":" + this.ircPort);
				this.connect(this.ircServer, this.ircPort, this.ircPass);
			}

			if (this.isConnected())
				CraftIRC.log.info(CraftIRC.NAME + " - Connected");
			else
				CraftIRC.log.info(CraftIRC.NAME + " - Connection failed!");
			
			this.authenticateBot();

			Iterator<String> it = channels.iterator();
			while (it.hasNext()) {
				String chan = it.next();
				this.joinChannel(chan, this.plugin.cChanPassword(botId, chan));
			}

			Timer timer = new Timer();
			Date checkdelay = new Date();
			checkdelay.setTime(checkdelay.getTime() + this.timeout);
			timer.schedule(new CheckChannelsTask(), checkdelay);
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IrcException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	void authenticateBot() {
		if (this.authMethod.equalsIgnoreCase("nickserv") && !authPass.isEmpty()) {
			CraftIRC.log.info(CraftIRC.NAME + " - Using Nickserv authentication.");
			this.sendMessage("nickserv", "GHOST " + this.nickname + " " + this.authPass);

			// Some IRC servers have quite a delay when ghosting... ***** TO IMPROVE
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			this.changeNick(this.nickname);
			this.identify(this.authPass);
			
		} else if (this.authMethod.equalsIgnoreCase("gamesurge")) {
			CraftIRC.log.info(CraftIRC.NAME + " - Using GameSurge authentication.");
			this.changeNick(this.nickname);
			this.sendMessage("AuthServ@Services.GameSurge.net", "AUTH " + this.authUser + " " + this.authPass);

		} else if (this.authMethod.equalsIgnoreCase("quakenet")) {
			CraftIRC.log.info(CraftIRC.NAME + " - Using QuakeNet authentication.");
			this.changeNick(this.nickname);
			this.sendMessage("Q@CServe.quakenet.org", "AUTH " + this.authUser + " " + this.authPass);
		}

	}

	void checkChannels() {
		ArrayList<String> botChannels = this.getChannelList();
		Iterator<String> it = channels.iterator();
		while (it.hasNext()) {
			String chan = it.next();
			if (botChannels.contains(chan))
				CraftIRC.log.info(CraftIRC.NAME + " - Joined channel: " + chan);
		}
	}

	
	public void onJoin(String channel, String sender, String login, String hostname) {
		if (this.channels.contains(channel)) {
			if (this.plugin.cEvents("irc-to-game.joins", botId, channel)) {
				msgToGame(sender, channel, messageMode.IRC_JOIN, null);
			}
		}
	}

	public void onPart(String channel, String sender, String login, String hostname, String reason) {
		if (this.channels.contains(channel)) {
			if (this.plugin.cEvents("irc-to-game.parts", botId, channel)) {
				msgToGame(sender, channel, messageMode.IRC_PART, null);
			}
		}
	}
	
	public void onQuit(String sender, String login, String hostname, String reason) {
		if (this.plugin.cEvents("irc-to-game.quits", botId, null)) {
			msgToGame(sender, "", messageMode.IRC_QUIT, null);
		}
	}

	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason) {
		if (recipientNick.equalsIgnoreCase(this.getNick())) {
			if (this.channels.contains(channel)) {
				this.joinChannel(channel, this.plugin.cChanPassword(botId, channel));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onMessage(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void onMessage(String channel, String sender, String login, String hostname, String message) {

		if (ignores.contains(sender)) {
			return;
		}

		String[] splitMessage = message.split(" ");
		String command = Util.combineSplit(1, splitMessage, " ");

		try {

			// Parse admin commands here
			if (userAuthorized(channel, sender)) {

				if ((message.startsWith(cmdPrefix + "console") || message.startsWith(cmdPrefix + "c"))
						&& splitMessage.length > 1 && this.plugin.cConsoleCommands().contains(splitMessage[1])) {
					this.sendNotice(sender, "NOT YET IMPLEMENTED IN BUKKIT");
					return;

				}

				else if (message.startsWith(cmdPrefix + "botsay") && splitMessage.length > 1) {
					if (this.channels.contains(splitMessage[1])) {
						command = Util.combineSplit(2, splitMessage, " ");
						this.sendMessage(splitMessage[1], command);
						this.sendNotice(sender, "Sent to channel " + splitMessage[1] + ": " + command);
					} else {
						Iterator<String> it = channels.iterator();
						while (it.hasNext())
							this.sendMessage(it.next(), command);
						this.sendNotice(sender, "Sent to all channels: " + command);
					}
					return;
				}

				else if (message.startsWith(cmdPrefix + "raw") && splitMessage.length > 1) {
					this.sendRawLine(command);
					this.sendNotice(sender, "Raw IRC string sent");
					return;
				}

			} // end admin commands

			// begin public commands

			// .players - list players
			if (message.startsWith(cmdPrefix + "players")) {
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
					this.msgToGame(sender, Util.combineSplit(2, splitMessage, " "), messageMode.MSG_PLAYER,
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
		if (this.plugin.cChanAdmin(botId, channel))
			try {
				User check = this.getUser(user, channel);
				return check != null && this.plugin.cBotAdminPrefixes(botId).contains(getHighestUserPrefix(check));
			} catch (Exception e) {
				e.printStackTrace();
			}
		return false;
	}

	
	/*
	 * Form and broadcast messages to Minecraft
	 * @param sender - The originating source/user of the IRC event
	 * @param message - The message to be relayed to the game
	 * @param mm - The message type (see messageMode)
	 * @param targetPlayer - The target player to message (for private messages), send null if mm != messageMod.MSG_PLAYER
	 */
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
				
			case IRC_JOIN:
				msg_to_broadcast = (new StringBuilder()).append("[IRC] ").append(irc_relayed_user_color)
									.append(sender).append(ChatColor.WHITE).append(message).toString();

				for (Player p1 : plugin.getServer().getOnlinePlayers()) {
					if (p1 != null) {
						p1.sendMessage(msg_to_broadcast);
					}
				}
				break;
				
			case IRC_QUIT:
				msg_to_broadcast = (new StringBuilder()).append("[IRC] ").append(irc_relayed_user_color)
									.append(sender).append(ChatColor.WHITE).append(message).toString();
				for (Player p1 : plugin.getServer().getOnlinePlayers()) {
					if (p1 != null) {
						p1.sendMessage(msg_to_broadcast);
					}
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

		for (int i = 0; i < onlinePlayers.length; i++) {
			if (onlinePlayers[i] != null) {
				playercount++;			
				sb.append(" ").append(onlinePlayers[i].getName());
			}
		}

		if (playercount > 0) {
			//return "Online (" + playercount + "/" + maxplayers + "): " + sb.toString();
			return "Online: " + sb.toString();
		} else {
			return "Nobody is minecrafting right now.";
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
	
	
	/**
	 * @param target - the IRC #channel to send the message to
	 * @param message - the message to send to the target #channel; this.irc_channel and this.irc_admin_channel are the common targets.
	 * 
	 */
	public void msg(String target, String message) {
		if (CraftIRC.isDebug()) {
			log.info(String.format(CraftIRC.NAME + " msgToIRC <%s> : %s", target, message));
		}
		sendMessage(target, message);
	}

	private class CheckChannelsTask extends TimerTask {
		public void run() {
			Minebot.instance.checkChannels();
		}
	}

	@Override
	public void run() {
		this.init();
	}

	private enum messageMode {
		MSG_ALL, ACTION_ALL, MSG_PLAYER, IRC_JOIN, IRC_QUIT, IRC_PART
	}

}// EO Minebot

