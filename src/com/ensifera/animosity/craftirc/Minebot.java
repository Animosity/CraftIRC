package com.ensifera.animosity.craftirc;

import org.jibble.pircbot.TrustingSSLSocketFactory;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.IrcException;

import com.sk89q.util.config.ConfigurationNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Set<String> whereAmI;
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
        this.setQueueSize(plugin.cBotQueueSize(botId));
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

        whereAmI = new HashSet<String>();
        channels = new HashMap<String,IRCChannelPoint>();
        for (ConfigurationNode channelNode : plugin.cChannels(botId)) {
            String name = channelNode.getString("name");
            if (channels.containsKey(name)) continue;
            IRCChannelPoint chan = new IRCChannelPoint(this, name);
            if (!plugin.registerEndPoint(channelNode.getString("tag"), chan)) continue;
        	if (!plugin.cIrcTagGroup().equals(""))
        		plugin.groupTag(channelNode.getString("tag"), plugin.cIrcTagGroup());
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

    //Thread start
    void start() {
        try {
            this.setAutoNickChange(true);
            
            String localAddr = this.plugin.cBindLocalAddr();
            if (!localAddr.isEmpty()) {
                
                if (this.bindLocalAddr(localAddr, this.ircPort)) {
                    CraftIRC.dolog("BINDING socket to " + localAddr + ":" + this.ircPort);
                }
            }
            
            connectToIrc();
            plugin.scheduleForRetry(this, null);

        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void connectToIrc() {
        try {
            if (this.ssl) {
                CraftIRC.dolog("Connecting to " + this.ircServer + ":" + this.ircPort + " [SSL]");
                this.connect(this.ircServer, this.ircPort, this.ircPass, new TrustingSSLSocketFactory());
            } else {
                CraftIRC.dolog("Connecting to " + this.ircServer + ":" + this.ircPort);
                this.connect(this.ircServer, this.ircPort, this.ircPass);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IrcException e) {
            e.printStackTrace();
        }
    }
    
    boolean isIn(String channel) {
        return whereAmI.contains(channel);
    }
    
    void joinIrcChannel(String chan) {
        this.joinChannel(chan, this.plugin.cChanPassword(botId, chan));
    }

    public CraftIRC getPlugin() { 
        return this.plugin; 
    }
    
    public int getId() {
        return botId;
    }
    
    public void onConnect() {
        CraftIRC.dolog("Connected");
        this.authenticateBot();

        ArrayList<String> onConnect = this.plugin.cBotOnConnect(botId);
        Iterator<String> it = onConnect.iterator();
        while (it.hasNext())
            sendRawLineViaQueue(it.next());

        for (String chan : channels.keySet())
            joinIrcChannel(chan);
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
    
    void amNowInChannel(String channel) {
        CraftIRC.dolog("Joined channel: " + channel);
        whereAmI.add(channel);
        String tag = plugin.cChanTag(botId, channel);
        if (tag != null && !plugin.endPointRegistered(tag)) plugin.registerEndPoint(tag, channels.get(channel));
        for (String line : this.plugin.cChanOnJoin(botId, channel))
            sendRawLineViaQueue(line);
    }
    
    public void onJoin(String channel, String sender, String login, String hostname) {
        if (this.channels.containsKey(channel)) {
            if (sender.equals(this.nickname)) amNowInChannel(channel);
            else {
            	if (plugin.cUseMapAsWhitelist(botId) && !plugin.cNicknameIsInIrcMap(botId, sender)) return;
                RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "join");
                if (msg == null) return;
                msg.setField("sender", plugin.cIrcDisplayName(botId, sender));
                msg.setField("realSender", sender);
                msg.setField("srcChannel", channel);
                msg.setField("username", login);
                msg.setField("hostname", hostname);
                msg.doNotColor("username");
                msg.doNotColor("hostname");
                msg.post();
            }
        }
    }

    void noLongerInChannel(String channel, boolean rejoin) {
        whereAmI.remove(channel);
        plugin.unregisterEndPoint(plugin.cChanTag(botId, channel)); 
        if (rejoin) plugin.scheduleForRetry(this, channel);
    }
    
    public void onPart(String channel, String sender, String login, String hostname, String reason) {
        if (sender.equals(this.nickname)) noLongerInChannel(channel, true);
        if (this.channels.containsKey(channel)) {
        	if (plugin.cUseMapAsWhitelist(botId) && !plugin.cNicknameIsInIrcMap(botId, sender)) return;
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "part");
            if (msg == null) return;
            msg.setField("sender", plugin.cIrcDisplayName(botId, sender));
            msg.setField("realSender", sender);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("username", login);
            msg.setField("hostname", hostname);
            msg.doNotColor("message");
            msg.doNotColor("username");
            msg.doNotColor("hostname");
            msg.post();
        }
    }
    
    public void onChannelQuit(String channel, String sender, String login, String hostname, String reason) {
        if (sender.equals(this.nickname)) noLongerInChannel(channel, false);
        if (this.channels.containsKey(channel)) {
        	if (plugin.cUseMapAsWhitelist(botId) && !plugin.cNicknameIsInIrcMap(botId, sender)) return;
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "quit");
            if (msg == null) return;
            msg.setField("sender", plugin.cIrcDisplayName(botId, sender));
            msg.setField("realSender", sender);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("username", login);
            msg.setField("hostname", hostname);
            msg.doNotColor("message");
            msg.doNotColor("username");
            msg.doNotColor("hostname");
            msg.post();
        }
    }
    
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equals(this.nickname)) noLongerInChannel(channel, true);
        if (this.channels.containsKey(channel)) {
            if (recipientNick.equalsIgnoreCase(this.getNick()))
                this.joinChannel(channel, this.plugin.cChanPassword(botId, channel));
            if (plugin.cUseMapAsWhitelist(botId) && (!plugin.cNicknameIsInIrcMap(botId, kickerNick) || !plugin.cNicknameIsInIrcMap(botId, recipientNick))) return;
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "kick");
            if (msg == null) return;
            msg.setField("sender", plugin.cIrcDisplayName(botId, recipientNick));
            msg.setField("realSender", recipientNick);
            msg.setField("srcChannel", channel);
            msg.setField("message", reason);
            msg.setField("moderator", plugin.cIrcDisplayName(botId, kickerNick));
            msg.setField("realModerator", kickerNick);
            msg.setField("ircModPrefix", getHighestUserPrefix(getUser(kickerNick, channel)));
            msg.setField("modUsername", kickerNick);
            msg.setField("modHostname", kickerLogin);
            msg.doNotColor("message");
            msg.doNotColor("modUsername");
            msg.doNotColor("modHostname");
            msg.post();            
        }
    }

    public void onChannelNickChange(String channel, String oldNick, String login, String hostname, String newNick) {
        if (oldNick.equals(this.nickname)) this.nickname = newNick;
        if (this.channels.containsKey(channel)) {
        	if (plugin.cUseMapAsWhitelist(botId) && (!plugin.cNicknameIsInIrcMap(botId, oldNick) || !plugin.cNicknameIsInIrcMap(botId, newNick))) return;
            RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "nick");
            if (msg == null) return;
            msg.setField("sender", plugin.cIrcDisplayName(botId, oldNick));
            msg.setField("realSender", oldNick);
            msg.setField("srcChannel", channel);
            msg.setField("message", plugin.cIrcDisplayName(botId, newNick));
            msg.setField("realMessage", newNick);
            msg.setField("username", login);
            msg.setField("hostname", hostname);
            msg.doNotColor("username");
            msg.doNotColor("hostname");
            msg.post();
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (ignores.contains(sender)) return;
        try {
        	if (plugin.cUseMapAsWhitelist(botId) && !plugin.cNicknameIsInIrcMap(botId, sender)) return;
            String[] splitMessage = message.split(" ");
            String command = splitMessage[0];
            String args = Util.combineSplit(1, splitMessage, " ");
            RelayedCommand cmd = null;
            String localTag = plugin.cChanTag(botId, channel);
            boolean loopbackAdmin = plugin.cPathAttribute(localTag, localTag, "attributes.admin");
            boolean userAdmin = plugin.cBotAdminPrefixes(botId).contains(getHighestUserPrefix(getUser(sender, channel)));
            if (cmdPrefix.equals("")) {
            	List<String> allCommands = new ArrayList<String>();
            	allCommands.addAll(plugin.cCmdWordCmd(botId));
            	allCommands.addAll(plugin.cCmdWordSay(botId));
            	allCommands.addAll(plugin.cCmdWordPlayers(botId));
            	for (String cmdString :allCommands)
            		if (command.equals(cmdString)) {
            			cmd = plugin.newCmd(channels.get(channel), command);
            			break;
            		}
            } else if (command.startsWith(cmdPrefix))
                cmd = plugin.newCmd(channels.get(channel), command.substring(cmdPrefix.length()));
            if (cmd != null) {
                //Normal command
            	cmd.setField("sender", plugin.cIrcDisplayName(botId, sender));
                cmd.setField("realSender", sender);
                cmd.setField("srcChannel", channel);
                cmd.setField("args", args);
                cmd.setField("ircPrefix", getHighestUserPrefix(getUser(sender, channel)));
                cmd.setField("username", login);
                cmd.setField("hostname", hostname);
                cmd.doNotColor("username");
                cmd.doNotColor("hostname");
                cmd.setFlag("admin", userAdmin);
                cmd.act();
            } else if (command.toLowerCase().equals(cmdPrefix + "botsay") && loopbackAdmin && userAdmin) {
                if (args == null) return;
                sendMessage(args.substring(0, args.indexOf(" ")), args.substring(args.indexOf(" ") + 1));
            } else if (command.toLowerCase().equals(cmdPrefix + "raw") && loopbackAdmin && userAdmin) {
                if (args == null) return;
                sendRawLine(args);
            } else {
                //Not a command
                RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "chat");
                if (msg == null) return;
                msg.setField("sender", plugin.cIrcDisplayName(botId, sender));
                msg.setField("realSender", sender);
                msg.setField("srcChannel", channel);
                msg.setField("message", message);
                msg.setField("ircPrefix", getHighestUserPrefix(getUser(sender, channel)));
                msg.setField("username", login);
                msg.setField("hostname", hostname);
                msg.doNotColor("message");
                msg.doNotColor("username");
                msg.doNotColor("hostname");
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
        if (plugin.cUseMapAsWhitelist(botId) && !plugin.cNicknameIsInIrcMap(botId, sender)) return;
        msg.setField("sender", plugin.cIrcDisplayName(botId, sender));
        msg.setField("realSender", sender);
        msg.setField("srcChannel", target);
        msg.setField("message", action);
        msg.setField("ircPrefix", getHighestUserPrefix(getUser(sender, target)));
        msg.setField("username", login);
        msg.setField("hostname", hostname);
        msg.doNotColor("message");
        msg.doNotColor("username");
        msg.doNotColor("hostname");
        msg.post();
    }
    
    public void onTopic(String channel, String topic, String sender, long date, boolean changed) {
        RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "topic");
        if (msg == null) return;
        if (plugin.cUseMapAsWhitelist(botId) && !plugin.cNicknameIsInIrcMap(botId, sender)) return;
        msg.setField("sender", plugin.cIrcDisplayName(botId, sender));
        msg.setField("realSender", sender);
        msg.setField("srcChannel", channel);
        msg.setField("message", topic);
        msg.doNotColor("message");
        msg.doNotColor("username");
        msg.doNotColor("hostname");
        msg.post();
    }
    
    protected void onMode(String channel, String moderator, String sourceLogin, String sourceHostname, String mode) {
        RelayedMessage msg = this.plugin.newMsg(channels.get(channel), null, "mode");
        if (msg == null) return;
        if (plugin.cUseMapAsWhitelist(botId) && !plugin.cNicknameIsInIrcMap(botId, moderator)) return;
        msg.setField("moderator", plugin.cIrcDisplayName(botId, moderator));
        msg.setField("realModerator", moderator);
        msg.setField("srcChannel", channel);
        msg.setField("message", mode);
        msg.setField("username", sourceLogin);
        msg.setField("hostname", sourceHostname);
        msg.doNotColor("username");
        msg.doNotColor("hostname");
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
                
                connectToIrc();
                plugin.scheduleForRetry(this, null);
                
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}// EO Minebot
