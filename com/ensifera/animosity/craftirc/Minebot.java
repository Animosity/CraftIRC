package com.ensifera.animosity.craftirc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jibble.pircbot.*;
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
            plugin.registerCommand(channelNode.getString("tag"), "botsay");
            plugin.registerCommand(channelNode.getString("tag"), "raw");
            //TODO: Unregister endpoint when necessary;
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
                if (msg == null) return;
                msg.setField("sender", sender);
                msg.setField("srcChannel", channel);
                msg.post();
            }
        }
    }
    
    public void onPart(String channel, String sender, String login, String hostname, String reason) {
        if (this.channels.containsKey(channel)) {
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "part");
            if (msg == null) return;
            msg.setField("sender", sender);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.post();
        }
    }
    
    public void onChannelQuit(String channel, String sender, String login, String hostname, String reason) {
        if (this.channels.containsKey(channel)) {
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "quit");
            if (msg == null) return;
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
            if (msg == null) return;
            msg.setField("sender", recipientNick);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("moderator", kickerNick);
            msg.setField("ircModPrefix", getHighestUserPrefix(getUser(kickerNick, channel)));
            msg.post();            
        }
    }

    public void onChannelNickChange(String channel, String oldNick, String login, String hostname, String newNick) {
        if (this.channels.containsKey(channel)) {
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "nick");
            if (msg == null) return;
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
            String command = splitMessage[0];
            String args = Util.combineSplit(1, splitMessage, " ");
            RelayedCommand cmd = null;
            if (command.startsWith(cmdPrefix))
                cmd = plugin.newCmd(channels.get(channel), command.substring(cmdPrefix.length()));
            if (cmd != null) {
                cmd.setField("sender", sender);
                cmd.setField("srcChannel", channel);
                cmd.setField("args", args);
                cmd.setField("ircPrefix", getHighestUserPrefix(getUser(sender, channel)));
                cmd.setFlag("admin", plugin.cBotAdminPrefixes(botId).contains(cmd.getField("ircPrefix")));
                cmd.act();
            } else {
                RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "chat");
                if (msg == null) return;
                msg.setField("sender", sender);
                msg.setField("srcChannel", channel);
                msg.setField("message", message);
                msg.setField("ircPrefix", getHighestUserPrefix(getUser(sender, channel)));
                msg.post();
            }
        } catch (Exception e) {
            e.printStackTrace();
            CraftIRC.dowarn("error while relaying IRC message: " + message);
        }
    }

    public void onAction(String sender, String login, String hostname, String target, String action) {
        RelayedMessage msg = this.plugin.newMsg(channels.get(target), null, "action");
        if (msg == null) return;
        msg.setField("sender", sender);
        msg.setField("srcChannel", target);
        msg.setField("message", action);
        msg.setField("ircPrefix", getHighestUserPrefix(getUser(sender, target)));
        msg.post();
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

}// EO Minebot
