package com.ensifera.animosity.craftirc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.jibble.pircbot.*;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.bukkit.util.config.ConfigurationNode;

/**
 * @author Animosity
 * @author Protected
 */
public class Minebot extends PircBot implements Runnable {

    private CraftIRC plugin = null;
    private boolean debug;
    private int botId;
    private String nickname;
    
    private Thread thread;

    // Connection attributes
    private boolean ssl;
    private String ircServer;
    private int ircPort;
    private String ircPass;

    // Nickname authentication
    private String authMethod;
    private String authUser;
    private String authPass;

    // Channels
    private Map<String,IRCChannelPoint> channels;

    // Other things that may be more efficient to store here
    private List<String> ignores;
    private String cmdPrefix;

    Minebot(CraftIRC plugin, int botId, boolean debug) {
        super();
        this.plugin = plugin;
        this.botId = botId;
        this.debug = debug;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void run() {
        this.setVerbose(debug);
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

        channels = new HashMap<String,IRCChannelPoint>();
        for (ConfigurationNode channelNode : plugin.cChannels(botId)) {
            String name = channelNode.getString("name");
            if (channels.containsKey(name)) continue;
            IRCChannelPoint chan = new IRCChannelPoint(this, name);
            if (!plugin.registerEndPoint(channelNode.getString("tag"), chan)) continue;
            //TODO: Unregister endpoint when necessary; Add endpoint deregistration
            channels.put(name, chan);
        }
        
        ignores = this.plugin.cBotIgnoredUsers(botId);
        cmdPrefix = this.plugin.cCommandPrefix(botId);

        try {
            this.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void start() {
        Iterator<String> it;

        try {
            this.setAutoNickChange(true);
            
            String localAddr = this.plugin.cBindLocalAddr();
            if (!localAddr.isEmpty()) {
                
                if (this.bindLocalAddr(localAddr, this.ircPort)) {
                    CraftIRC.dolog("BINDING socket to " + localAddr + ":" + this.ircPort);
                }
            }
            
            if (this.ssl) {
                CraftIRC.dolog("Connecting to " + this.ircServer + ":" + this.ircPort + " [SSL]");
                this.connect(this.ircServer, this.ircPort, this.ircPass, new TrustingSSLSocketFactory());
            } else {
                CraftIRC.dolog("Connecting to " + this.ircServer + ":" + this.ircPort);
                this.connect(this.ircServer, this.ircPort, this.ircPass);
            }

            if (this.isConnected())
                CraftIRC.dolog("Connected");
            else
                CraftIRC.dolog("Connection failed!");

            this.authenticateBot();

            ArrayList<String> onConnect = this.plugin.cBotOnConnect(botId);
            it = onConnect.iterator();
            while (it.hasNext())
                sendRawLineViaQueue(it.next());

            for (String chan : channels.keySet())
                this.joinChannel(chan, this.plugin.cChanPassword(botId, chan));

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

    public CraftIRC getPlugin() { 
        return this.plugin; 
    }
    
    public int getId() {
        return botId;
    }
    
    void authenticateBot() {
        if (this.authMethod.equalsIgnoreCase("nickserv") && !authPass.isEmpty()) {
            CraftIRC.dolog("Using Nickserv authentication.");
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
            CraftIRC.dolog("Using GameSurge authentication.");
            this.changeNick(this.nickname);
            this.sendMessage("AuthServ@Services.GameSurge.net", "AUTH " + this.authUser + " " + this.authPass);

        } else if (this.authMethod.equalsIgnoreCase("quakenet")) {
            CraftIRC.dolog("Using QuakeNet authentication.");
            this.changeNick(this.nickname);
            this.sendMessage("Q@CServe.quakenet.org", "AUTH " + this.authUser + " " + this.authPass);
        }

    }
    
    public void onJoin(String channel, String sender, String login, String hostname) {
        if (this.channels.containsKey(channel)) {
            if (sender.equals(this.nickname)) {
                CraftIRC.dolog("Joined channel: " + channel);
                ArrayList<String> onJoin = this.plugin.cChanOnJoin(botId, channel);
                Iterator<String> it = onJoin.iterator();
                while (it.hasNext())
                    sendRawLineViaQueue(it.next());
            } else {
                RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "join");
                msg.setField("sender", sender);
                msg.setField("srcChannel", channel);
                msg.post();
            }
        }
    }
    
    public void onPart(String channel, String sender, String login, String hostname, String reason) {
        if (this.channels.containsKey(channel)) {
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "part");
            msg.setField("sender", sender);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.post();
        }
    }
    
    public void onChannelQuit(String channel, String sender, String login, String hostname, String reason) {
        if (this.channels.containsKey(channel)) {
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "quit");
            msg.setField("sender", sender);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.post();
        }
    }
    
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
            String recipientNick, String reason) {
        if (this.channels.containsKey(channel)) {
            if (recipientNick.equalsIgnoreCase(this.getNick()))
                this.joinChannel(channel, this.plugin.cChanPassword(botId, channel));
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "kick");
            msg.setField("sender", recipientNick);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("moderator", kickerNick);
            msg.post();            
        }
    }

    public void onChannelNickChange(String channel, String oldNick, String login, String hostname, String newNick) {
        if (this.channels.containsKey(channel)) {
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "nick");
            msg.setField("sender", oldNick);
            msg.setField("srcChannel", channel);
            msg.setField("message", newNick);
            msg.post();
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (ignores.contains(sender)) return;
        try {
            String[] splitMessage = message.split(" ");
            String command = Util.combineSplit(1, splitMessage, " ");
            // Parse admin commands here
            if (message.startsWith(cmdPrefix) && userAuthorized(channel, sender)) {
                if (this.plugin.isDebug())
                    CraftIRC.dolog(String.format("Authorized User %s used command %s", sender, message));
                if ((message.startsWith(cmdPrefix + "cmd ") || message.startsWith(cmdPrefix + "c "))
                        && splitMessage.length > 1) {
                    //TODO - Not sure how it was implemented
                    /*
                    //message.replaceFirst(cmdPrefix, "");
                    
                    if (//SUCCESS) {
                        if (this.plugin.isDebug())
                            CraftIRC.dolog(String.format("Authorized User %s executed command %s", sender, message));
                        return;
                    }
                    */
                } else if (message.startsWith(cmdPrefix + "botsay ") && splitMessage.length > 1) {
                    //TODO - Not sure if still needed
                    return;
                } else if (message.startsWith(cmdPrefix + "raw ") && splitMessage.length > 1) {
                    this.sendRawLine(command);
                    this.sendNotice(sender, "Raw IRC string sent");
                    return;
                } 
                    
                // ALTERNATIVE METHOD? WHAT IS THIS?
                RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "command");
                msg.setField("sender", sender);
                msg.setField("srcChannel", channel);
                msg.setField("message", message.substring(cmdPrefix.length()));
                Event ie = new IRCEvent(Mode.AUTHED_COMMAND, msg);
                this.plugin.getServer().getPluginManager().callEvent(ie);
                if (((IRCEvent)ie).isHandled()) return;
                
            } // End admin commands

            // Begin public commands

            // .say - Send single message to the game
            if (message.startsWith(cmdPrefix + "say ") || message.startsWith(cmdPrefix + "mc ")) {
                if (splitMessage.length > 1) {
                    RelayedMessage msg = this.plugin.newMsg(channels.get(channel), plugin.getEndPoint(plugin.cMinecraftTag()), "chat");
                    msg.setField("sender", sender);
                    msg.setField("srcChannel", channel);
                    msg.setField("message", message);
                    msg.post();
                    this.sendNotice(sender, "Message sent to game");
                    return;
                }
            } else if (message.startsWith(cmdPrefix + "players")) {
                if (this.plugin.isDebug()) CraftIRC.dolog("Minebot .players command");
                String playerListing = this.getPlayerList();
                this.sendMessage(channel, playerListing);
                return;
            } else {
                // Send all IRC chatter (no command prefixes or ignored command prefixes)
                RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "chat");
                msg.setField("sender", sender);
                msg.setField("srcChannel", channel);
                msg.setField("message", message);
                msg.post();
                return;
            }
            
            //TODO: Still not sure how I'm going to handle this but probably in a different manner
            /*
            // IRCEvent - COMMAND
            if (this.plugin.isDebug()) CraftIRC.dolog("Minebot IRCEVENT.COMMAND");
            RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.BOTH);
            msg.formatting = "";
            msg.sender = sender;
            msg.srcBot = botId;
            msg.srcChannel = channel;
            msg.message = message.substring(cmdPrefix.length());
            msg.updateTag();
            // PLUGIN INTEROP
            msg.setTarget(EndPoint.PLUGIN);
            Event ie = new IRCEvent(Mode.COMMAND, msg);
            this.plugin.getServer().getPluginManager().callEvent(ie);
            */

        } catch (Exception e) {
            e.printStackTrace();
            CraftIRC.dowarn("error while relaying IRC command: " + message);
        }

    }

    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        if (this.plugin.isDebug())
            CraftIRC.log.info(String.format(CraftIRC.NAME + " Minebot IRCEVENT.PRIVMSG"));
        if (ignores.contains(sender)) return;

        String[] splitMessage = message.split(" ");

        try {
                // IRCEvent - PRIVMSG
                RelayedMessage msg = this.plugin.newMsg(EndPoint.IRC, EndPoint.BOTH);
                msg.formatting = "quits";
                msg.sender = sender;
                msg.srcBot = botId;
                msg.srcChannel = "";
                msg.message = message;
                msg.updateTag();
                // PLUGIN INTEROP
                msg.setTarget(EndPoint.PLUGIN);
                Event ie = new IRCEvent(Mode.PRIVMSG, msg);
                this.plugin.getServer().getPluginManager().callEvent(ie);
        }
    }

    public void onAction(String sender, String login, String hostname, String target, String action) {
        RelayedMessage msg = this.plugin.newMsg(channels.get(target), null, "action");
        msg.setField("sender", sender);
        msg.setField("srcChannel", target);
        msg.setField("message", action);
        msg.post();
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
                return "Online (" + playerCount + "/" + maxPlayers + "): " + sb.toString();
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
                while (!this.isConnected()) this.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private enum messageMode {
        MSG_ALL, ACTION_ALL, MSG_PLAYER, IRC_JOIN, IRC_QUIT, IRC_PART
    }

}// EO Minebot
