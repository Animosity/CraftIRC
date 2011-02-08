package com.animosity.craftirc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.jibble.pircbot.*;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import com.animosity.craftirc.CraftIRC;
import com.animosity.craftirc.Util;

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
	
	// Nickname authentication
	private String authMethod;
	private String authUser;
	private String authPass;
	
	// Channel attributes
	private ArrayList<String> channels;
	
	// Other things that may be more efficient to store here
	private ArrayList<String> ignores;
	private String cmdPrefix;
	private ArrayList<String> ircCmdPrefixes;

	protected Minebot(CraftIRC plugin, int botId) {
		this.plugin = plugin;
		this.botId = botId;
	}

	public synchronized Minebot init() {
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
		
		authMethod = this.plugin.cBotAuthMethod(botId);
		authUser = this.plugin.cBotAuthUsername(botId);
		authPass = this.plugin.cBotAuthPassword(botId);
		
		channels = this.plugin.cBotChannels(botId);
		
		ignores = this.plugin.cBotIgnoredUsers(botId);
		cmdPrefix = this.plugin.cCommandPrefix(botId);
		ircCmdPrefixes = this.plugin.cIgnoredPrefixes("irc");

		try {
			this.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public void start() {

		Iterator<String> it;
		
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
			
			ArrayList<String> onConnect = this.plugin.cBotOnConnect(botId);
			it = onConnect.iterator();
			while (it.hasNext())
				sendRawLineViaQueue(it.next());

			it = channels.iterator();
			while (it.hasNext()) {
				String chan = it.next();
				this.joinChannel(chan, this.plugin.cChanPassword(botId, chan));
			}
			
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
	
	public void onJoin(String channel, String sender, String login, String hostname) {
		if (this.channels.contains(channel)) {
			if (sender.equals(this.nickname)) {
				CraftIRC.log.info(CraftIRC.NAME + " - Joined channel: " + channel);
				ArrayList<String> onJoin = this.plugin.cChanOnJoin(botId, channel);
				Iterator<String> it = onJoin.iterator();
				while (it.hasNext())
					sendRawLineViaQueue(it.next());
				
			} else {
				RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.BOTH);
			    msg.formatting = "joins";
			    msg.sender = sender;
			    msg.srcBot = botId;
			    msg.srcChannel = channel;
			    this.plugin.sendMessage(msg, null, "joins");
			    				
			}
		}
	}

	public void onPart(String channel, String sender, String login, String hostname, String reason) {
		if (this.channels.contains(channel)) {
			RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.BOTH);
		    msg.formatting = "parts";
		    msg.sender = sender;
		    msg.srcBot = botId;
		    msg.srcChannel = channel;
		    msg.message = reason;
		    this.plugin.sendMessage(msg, null, "parts");
		    
		}
	}
	
	public void onChannelQuit(String channel, String sender, String login, String hostname, String reason) {
		if (this.channels.contains(channel)) {
			RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.BOTH);
		    msg.formatting = "quits";
		    msg.sender = sender;
		    msg.srcBot = botId;
		    msg.srcChannel = channel;
		    msg.message = reason;
		    this.plugin.sendMessage(msg, null, "quits");
		}
	}

	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason) {
	 // IRCEvent - AUTHED_COMMAND
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

		if (ignores.contains(sender)) return;

		String[] splitMessage = message.split(" ");
		String command = Util.combineSplit(1, splitMessage, " ");

		try {

			// Parse admin commands here
			if (userAuthorized(channel, sender)) {
			    
			    // IRCEvent - AUTHED_COMMAND
			    Event ie = new IRCEvent(this, IRCEvent.Mode.AUTHED_COMMAND, this.ircServer, channel, sender, message.replaceFirst(this.cmdPrefix, ""));
			    this.plugin.getServer().getPluginManager().callEvent(ie);
			    
				if ((message.startsWith(cmdPrefix + "console ") || message.startsWith(cmdPrefix + "c "))
						&& splitMessage.length > 1 && this.plugin.cConsoleCommands().contains(splitMessage[1])) {
					this.sendNotice(sender, "NOT YET IMPLEMENTED IN BUKKIT");
					return;

				}

				if (message.startsWith(cmdPrefix + "botsay ") && splitMessage.length > 1) {
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

				if (message.startsWith(cmdPrefix + "raw ") && splitMessage.length > 1) {
					this.sendRawLine(command);
					this.sendNotice(sender, "Raw IRC string sent");
					return;
				}

			} // End admin commands

			// Begin public commands
			 Event ie = new IRCEvent(this, IRCEvent.Mode.COMMAND, this.ircServer, channel, sender, message.replaceFirst(this.cmdPrefix, ""));
             this.plugin.getServer().getPluginManager().callEvent(ie);
			
			// .players - list players
			if (message.equals(cmdPrefix + "players")) {
				String playerlist = this.getPlayerList();
				this.sendMessage(channel, playerlist);
				return;
			}

			// Send all IRC chatter (no command prefixes or ignored command prefixes)
			if (!ircCmdPrefixes.contains(message.substring(0,0))) {
					RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.BOTH);
				    msg.formatting = "chat";
				    msg.sender = sender;
				    msg.srcBot = botId;
				    msg.srcChannel = channel;
				    msg.message = message;
				    this.plugin.sendMessage(msg, null, "all-chat");
				    
					return;
			}

			// .say - Send single message to the game
			if (message.startsWith(cmdPrefix + "say ") || message.startsWith(cmdPrefix + "mc ")) {
				if (splitMessage.length > 1) {
					RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.GAME);
				    msg.formatting = "chat";
				    msg.sender = sender;
				    msg.srcBot = botId;
				    msg.srcChannel = channel;
				    msg.message = command;
				    this.plugin.sendMessage(msg, null, null);
					this.sendNotice(sender, "Message sent to game");
					return;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			CraftIRC.log.log(Level.SEVERE, CraftIRC.NAME + " - error while relaying IRC command: " + message);
		}

	}

	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		
		if (ignores.contains(sender)) return;
		
		String[] splitMessage = message.split(" ");
		
		try {

			if (splitMessage.length > 1 && splitMessage[0].equalsIgnoreCase("tell")) {
				if (plugin.getServer().getPlayer(splitMessage[1]) != null) {
					this.msgToGame(null, sender, Util.combineSplit(2, splitMessage, " "), messageMode.MSG_PLAYER, splitMessage[1]);
					this.sendNotice(sender, "Whispered to " + splitMessage[1]);
				}
			}

		} catch (Exception e) {}
	}

	public void onAction(String sender, String login, String hostname, String target, String action) {
	    // IRCEvent - ACTION
	    Event ie = new IRCEvent(this, IRCEvent.Mode.ACTION, this.ircServer, target, sender, action);
        this.plugin.getServer().getPluginManager().callEvent(ie);
        
		RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.BOTH);
	    msg.formatting = "action";
	    msg.sender = sender;
	    msg.srcBot = botId;
	    msg.srcChannel = target;
	    msg.message = action;
	    this.plugin.sendMessage(msg, null, "all-chat");
	    		
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

	
	/**
     * TODO: NEED TO CHANGE TO PASS THROUGH FORMATTER FIRST
	 * @param sender - The originating source/user of the IRC event
	 * @param message - The message to be relayed to the game
	 * @param mm - The message type (see messageMode)
	 * @param targetPlayer - The target player to message (for private messages), send null if mm != messageMod.MSG_PLAYER
	 */
	public void msgToGame(String source, String sender, String message, messageMode mm, String target) {

		try {

			String msg_to_broadcast;
			switch (mm) {

			// MESSAGE TO 1 PLAYER
			case MSG_PLAYER:
				if (this.plugin.isDebug()) {
					CraftIRC.log.info(String.format(CraftIRC.NAME + " msgToGame(player) : <%s> %s", sender, message));
				}
				msg_to_broadcast = (new StringBuilder()).append("[IRC privmsg]").append(" <")
						.append(sender).append(ChatColor.WHITE).append("> ")
						.append(message).toString();
				Player p = plugin.getServer().getPlayer(target);
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
	    try {
    		Player onlinePlayers[] = plugin.getServer().getOnlinePlayers();
    		int playerCount = 0;
    		int maxPlayers = this.plugin.getServer().getMaxPlayers(); // CraftBukkit-only, need generic check for server type.
    
    		//Integer maxplayers;
    		StringBuilder sb = new StringBuilder();
    
    		for (int i = 0; i < onlinePlayers.length; i++) {
    			if (onlinePlayers[i] != null) {
    				playerCount++;			
    				sb.append(" ").append(onlinePlayers[i].getName());
    			}
    		}
    
    		if (playerCount > 0) {
    			//return "Online (" + playercount + "/" + maxplayers + "): " + sb.toString();
    			return "Online ("+playerCount+"/"+maxPlayers+"): " + sb.toString();
    		} else {
    			return "Nobody is minecrafting right now.";
    		}
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "Could not retrieve player list!";
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
			if (plugin.isEnabled()) {
				CraftIRC.log.info(CraftIRC.NAME + " - disconnected from IRC server... reconnecting!");
				// Reconnect here
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
		if (this.plugin.isDebug()) {
			CraftIRC.log.info(String.format(CraftIRC.NAME + " msgToIRC <%s> : %s", target, message));
		}
		sendMessage(target, message);
	}

	@Override
	public void run() {
		this.init();
	}

	private enum messageMode {
		MSG_ALL, ACTION_ALL, MSG_PLAYER, IRC_JOIN, IRC_QUIT, IRC_PART
	}

}// EO Minebot

