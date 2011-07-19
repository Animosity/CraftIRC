package com.ensifera.animosity.craftirc;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

/**
 * @author Animosity
 * @author ricin
 * @author Protected
 * 
 */

//TODO: Make sure all names are display names

public class CraftIRC extends JavaPlugin {
    public static final String NAME = "CraftIRC";
    public static String VERSION;
    static final Logger log = Logger.getLogger("Minecraft");
    
    //Misc class attributes
    PluginDescriptionFile desc = null;
    public Server server = null;
    private final CraftIRCListener listener = new CraftIRCListener(this);
    private ArrayList<Minebot> instances;
    private boolean debug;
    private Timer holdTimer = new Timer();
    HashMap<HoldType, Boolean> hold;

    //Bots and channels config storage
    private List<ConfigurationNode> bots;
    private List<ConfigurationNode> colormap;
    private Map<Integer, ArrayList<ConfigurationNode>> channodes;
    private Map<Path, ConfigurationNode> paths;
    
    //Endpoints
    private Map<String,EndPoint> endpoints = new HashMap<String,EndPoint>();
    private Map<EndPoint,String> tags = new HashMap<EndPoint,String>();
    
    //TODO: All logging should use these
    static void dolog(String message) {
        log.info("[" + NAME + "] " + message);
    }
    static void dowarn(String message) {
        log.log(Level.WARNING, "[" + NAME + "] " + message);
    }

    public void onEnable() {
        try {
            
            PluginDescriptionFile desc = this.getDescription();
            VERSION = desc.getVersion();
            server = this.getServer();
                       
            //Load node lists. Bukkit does it now, hurray!
            
            if (null == getConfiguration()) {
                CraftIRC.log.info(String.format(CraftIRC.NAME + " config.yml could not be found in plugins/CraftIRC/ -- disabling!"));
                getServer().getPluginManager().disablePlugin(((Plugin) (this)));
                return;
            }
            
            bots = new ArrayList<ConfigurationNode>(getConfiguration().getNodeList("bots", null));
            channodes = new HashMap<Integer, ArrayList<ConfigurationNode>>();
            for (int botID = 0; botID < bots.size(); botID++)
                channodes.put(botID, new ArrayList<ConfigurationNode>(bots.get(botID).getNodeList("channels", null)));
            
            colormap = new ArrayList<ConfigurationNode>(getConfiguration().getNodeList("colormap", null));
            
            paths = new HashMap<Path,ConfigurationNode>();
            for (ConfigurationNode path : getConfiguration().getNodeList("paths", new LinkedList<ConfigurationNode>())) {
                Path identifier = new Path(path.getString("source"), path.getString("target"));
                if (!identifier.getSourceTag().equals(identifier.getTargetTag()) && !paths.containsKey(identifier))
                    paths.put(identifier, path);
            }

            //Event listeners
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, listener, Priority.Monitor, this);
            //TODO: Player kick event
            
            registerEndPoint(cMinecraftTag(), new MinecraftPoint(getServer()));

            //Create bots
            //TODO: Threads
            instances = new ArrayList<Minebot>();
            for (int i = 0; i < bots.size(); i++)
                instances.add(new Minebot(this, i).init(cDebug()));

            log.info(NAME + " Enabled.");

            //Hold timers
            hold = new HashMap<HoldType, Boolean>();
            holdTimer = new Timer();
            if (cHold("chat") > 0) {
                hold.put(HoldType.CHAT, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.CHAT), cHold("chat"));
            } else
                hold.put(HoldType.CHAT, false);
            if (cHold("joins") > 0) {
                hold.put(HoldType.JOINS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.JOINS), cHold("joins"));
            } else
                hold.put(HoldType.JOINS, false);
            if (cHold("quits") > 0) {
                hold.put(HoldType.QUITS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.QUITS), cHold("quits"));
            } else
                hold.put(HoldType.QUITS, false);
            if (cHold("kicks") > 0) {
                hold.put(HoldType.KICKS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.KICKS), cHold("kicks"));
            } else
                hold.put(HoldType.KICKS, false);
            if (cHold("bans") > 0) {
                hold.put(HoldType.BANS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.BANS), cHold("bans"));
            } else
                hold.put(HoldType.BANS, false);
                        
            setDebug(cDebug());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDisable() {
        try {
            holdTimer.cancel();
            //Disconnect bots
            for (int i = 0; i < bots.size(); i++) {
                instances.get(i).disconnect();
                instances.get(i).dispose(); // will this cleanup properly?
            }
            log.info(NAME + " Disabled.");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        String commandName = command.getName().toLowerCase();
         
        try {
            if (sender instanceof IRCConsoleCommandSender) sender = (IRCConsoleCommandSender)sender;
            
            if (commandName.equals("ircmsg")) {
                return this.cmdMsgToTag(sender, args);
            } else if (commandName.equals("ircmsguser")) {
                return this.cmdMsgToUser(sender, args);                
            } else if (commandName.equals("ircusers")) {
                return this.cmdGetUserList(sender, args);
            } else if (commandName.equals("admins!")) {
                return this.cmdNotifyIrcAdmins(sender, args);
            } else if (commandName.equals("ircraw")) {
                return this.cmdRawIrcCommand(sender, args);
            } else if (commandName.equals("say")) {
                // Capture the 'say' command from Minecraft Console
                if (sender instanceof ConsoleCommandSender) {
                    //TODO
                }
            } else
                return false;
        } catch (Exception e) { 
            e.printStackTrace(); 
            return false;
        }
        return debug;
        
    }

    private boolean cmdMsgToTag(CommandSender sender, String[] args) {
        try {
            if (this.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " CraftIRCListener cmdMsgToAll()"));
            if (args.length < 2) return false;
            String msgToSend = Util.combineSplit(1, args, " ");
            RelayedMessage msg = this.newMsg(getEndPoint(cMinecraftTag()), getEndPoint(args[0]), "chat");
            if (sender instanceof Player)
                msg.setField("sender", ((Player) sender).getDisplayName());
            else
                msg.setField("sender", "SERVER");
            msg.setField("message", msgToSend);
            msg.deliver();

            //TODO: Find a better way to do this (use formatting string, etc.)
            /*
            String echoedMessage = new StringBuilder().append("<").append(msg.sender)
                    .append(ChatColor.WHITE.toString()).append(" to IRC> ").append(msgToSend).toString();
            // echo -> IRC msg locally in game
            for (Player p : this.getServer().getOnlinePlayers()) {
                if (p != null) {
                    p.sendMessage(echoedMessage);
                }
            }
            */
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdMsgToUser(CommandSender sender, String[] args) {
        try {
            if (args.length < 3)
                return false;
            String msgToSend = Util.combineSplit(2, args, " ");
            RelayedMessage msg = this.newMsg(getEndPoint(cMinecraftTag()), getEndPoint(args[0]), "chat");
            if (sender instanceof Player)
                msg.setField("sender", ((Player) sender).getDisplayName());
            else
                msg.setField("sender", "SERVER");;
            msg.setField("message", msgToSend);
            msg.deliverTo(args[1]);

            //TODO: Find a better way to do this (use formatting string, etc.)
            /*
            String echoedMessage = new StringBuilder().append("<").append(msg.sender)
                    .append(ChatColor.WHITE.toString()).append(" to IRC> ").append(msgToSend).toString();

            for (Player p : this.getServer().getOnlinePlayers()) {
                if (p != null) {
                    p.sendMessage(echoedMessage);
                }
            }
            */
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdGetUserList(CommandSender sender, String[] args) {
        try {
            if (args.length == 0)
                return false;
            sender.sendMessage("Users in " + args[0] + ":");
            List<String> userlists = this.ircUserLists(args[0]);
            for (Iterator<String> it = userlists.iterator(); it.hasNext();)
                sender.sendMessage(it.next());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdNotifyIrcAdmins(CommandSender sender, String[] args) {
        try {
            if (this.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " CraftIRCListener cmdNotifyIrcAdmins()"));
            if (args.length == 0 || !(sender instanceof Player)) {
                if (this.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " CraftIRCListener cmdNotifyIrcAdmins() - args.length == 0 or Sender != player "));
                return false;
            }
            this.noticeAdmins("[Admin notice from " + ((Player) sender).getName() + "] "
                    + Util.combineSplit(0, args, " "));
            sender.sendMessage("Admin notice sent.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdRawIrcCommand(CommandSender sender, String[] args) {
        try {
            if (this.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " cmdRawIrcCommand(sender=" + sender.toString() + ", args=" + Util.combineSplit(0, args, " ")));
            if (args.length < 2) return false;
            this.sendRawToBot(Util.combineSplit(1, args, " "), Integer.parseInt(args[0]));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public RelayedMessage newMsg(EndPoint source, EndPoint target, String eventType) {
        if (source == null) return null;
        return new RelayedMessage(this, source, target, eventType);
    }
    
    public boolean registerEndPoint(String tag, EndPoint ep) {
        if (endpoints.get(tag) != null || tags.get(ep) != null) {
            dolog("Couldn't register an endpoint tagged '" + tag + "' because either the tag or the endpoint already exist."); 
            return false;
        }
        if (tag == "*") {
            dolog("Couldn't register an endpoint - the character * can't be used as a tag.");
            return false;
        }
        endpoints.put(tag, ep);
        tags.put(ep, tag);
        return true;
    }
    public EndPoint getEndPoint(String tag) {
        return endpoints.get(tag);
    }
    public String getTag(EndPoint ep) {
        return tags.get(ep);
    }

    protected void sendMessage(RelayedMessage msg, String tag, String event) {
        try {
            String realEvent = event;
            //Send to IRC
            if (msg.getTarget() == EndPoint.IRC || msg.getTarget() == EndPoint.BOTH) {
                if (msg.getSource() == EndPoint.IRC)
                    realEvent = "irc-to-irc." + event;
                if (msg.getSource() == EndPoint.GAME)
                    realEvent = "game-to-irc." + event;
                for (int i = 0; i < bots.size(); i++) {
                    ArrayList<String> chans = cBotChannels(i);
                    Iterator<String> it = chans.iterator();
                    while (it.hasNext()) {
                        String chan = it.next();
                        // Don't echo back to sending channel
                        if (msg.getSource() == EndPoint.IRC && msg.srcBot == i && msg.srcChannel.equalsIgnoreCase(chan))
                            continue;
                        // Send to all bots, channels with event enabled
                        if ((tag == null || cChanCheckTag(tag, i, chan))
                                && (event == null || cEvents(realEvent, i, chan))) {
                            msg.trgBot = i;
                            msg.trgChannel = chan;
                            if (msg.getTarget() == EndPoint.BOTH)
                                instances.get(i).sendMessage(chan, msg.asString(EndPoint.IRC));
                            else
                                instances.get(i).sendMessage(chan, msg.asString());
                        }
                    }
                }
            }

            //Send to game (doesn't allow game to game)
            if ((msg.getTarget() == EndPoint.GAME || msg.getTarget() == EndPoint.BOTH)
                    && msg.getSource() == EndPoint.IRC) {
                realEvent = "irc-to-game." + event;
                if ((tag == null || cChanCheckTag(tag, msg.srcBot, msg.srcChannel))
                        && (event == null || cEvents(realEvent, msg.srcBot, msg.srcChannel))) {
                    for (Player pl : getServer().getOnlinePlayers()) {
                        if (pl != null) {
                            if (msg.getTarget() == EndPoint.BOTH)
                                pl.sendMessage(msg.asString(EndPoint.GAME));
                            else
                                pl.sendMessage(msg.asString());
                        }
                    }
                }
            }
        } catch (RelayedMessageException rme) {
            log.log(Level.SEVERE, rme.toString());
            rme.printStackTrace();
        }
    }

    protected void sendRawToBot(String rawMessage, int bot) {
        if (this.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " sendRawToBot(bot=" + bot + ", message=" + rawMessage));
        Minebot targetBot = instances.get(bot);
        targetBot.sendRawLineViaQueue(rawMessage);
    }
    
    protected void sendMsgToTargetViaBot(String message, String target, int bot) {
        Minebot targetBot = instances.get(bot);
        targetBot.sendMessage(target, message);
    }
    
    protected List<String> ircUserLists(String tag) {
        return getEndPoint(tag).listUsers();        
    }

    protected void noticeAdmins(String message) {
        /* 
        for (int i = 0; i < bots.size(); i++) {
            ArrayList<String> chans = cBotChannels(i);
            Iterator<String> it = chans.iterator();
            while (it.hasNext()) {
                String chan = it.next();
                if (cChanAdmin(i, chan))
                    instances.get(i).sendNotice(chan, message);
            }
        }
        */
    }

    protected void setDebug(boolean d) {
        debug = d;

        for (int i = 0; i < bots.size(); i++)
            instances.get(i).setVerbose(d);

        log.info(NAME + " DEBUG [" + (d ? "ON" : "OFF") + "]");
    }

    protected boolean isDebug() {
        return debug;
    }

    private ConfigurationNode getChanNode(int bot, String channel) {
        ArrayList<ConfigurationNode> botChans = channodes.get(bot);
        for (Iterator<ConfigurationNode> it = botChans.iterator(); it.hasNext();) {
            ConfigurationNode chan = it.next();
            if (chan.getString("name").equalsIgnoreCase(channel))
                return chan;
        }
        return Configuration.getEmptyNode();
    }
    
    private ConfigurationNode getPathNode(String source, String target) {
        return paths.get(new Path(source, target));
    }

    String cMinecraftTag() {
        return getConfiguration().getString("settings.minecraft-tag", "minecraft");
    }
    
    protected boolean cDebug() {
        return getConfiguration().getBoolean("settings.debug", false);
    }

    protected ArrayList<String> cConsoleCommands() {
        return new ArrayList<String>(getConfiguration().getStringList("settings.console-commands", null));
    }

    protected int cHold(String eventType) {
        return getConfiguration().getInt("settings.hold-after-enable." + eventType, 0);
    }

    protected String cFormatting(String eventType, RelayedMessage msg) {
        String source = getTag(msg.getSource()), target = getTag(msg.getTarget());
        if (source == null || target == null) {
            dowarn("Attempted to obtain formatting for invalid path " + source + " -> " + target + " .");
            return cDefaultFormatting(eventType, msg);
        }
        ConfigurationNode pathConfig = paths.get(new Path(source, target));
        if (pathConfig != null && pathConfig.getString("formatting." + eventType, null) != null)
            return pathConfig.getString("formatting." + eventType, null);
        else
            return cDefaultFormatting(eventType, msg);
    }
    String cDefaultFormatting(String eventType, RelayedMessage msg) {
        if (msg.getSource().getType() == EndPoint.Type.MINECRAFT) return getConfiguration().getString("settings.formatting.from-game." + eventType);
        if (msg.getSource().getType() == EndPoint.Type.IRC) return getConfiguration().getString("settings.formatting.from-irc." + eventType);
        return "";
    }

    protected int cColorIrcFromGame(String game) {
        ConfigurationNode color;
        Iterator<ConfigurationNode> it = colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("game").equals(game))
                return color.getInt("irc", cColorIrcFromName("foreground"));
        }
        return cColorIrcFromName("foreground");
    }

    protected int cColorIrcFromName(String name) {
        ConfigurationNode color;
        Iterator<ConfigurationNode> it = colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("name").equalsIgnoreCase(name) && color.getProperty("irc") != null)
                return color.getInt("irc", 1);
        }
        if (name.equalsIgnoreCase("foreground"))
            return 1;
        else
            return cColorIrcFromName("foreground");
    }

    protected String cColorGameFromIrc(int irc) {
        ConfigurationNode color;
        Iterator<ConfigurationNode> it = colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getInt("irc", -1) == irc)
                return color.getString("game", cColorGameFromName("foreground"));
        }
        return cColorGameFromName("foreground");
    }

    protected String cColorGameFromName(String name) {
        ConfigurationNode color;
        Iterator<ConfigurationNode> it = colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("name").equalsIgnoreCase(name) && color.getProperty("game") != null)
                return color.getString("game", "§f");
        }
        if (name.equalsIgnoreCase("foreground"))
            return "§f";
        else
            return cColorGameFromName("foreground");
    }

    protected String cBindLocalAddr() {
        return getConfiguration().getString("settings.bind-address","");
    }

    protected String cBotNickname(int bot) {
        return bots.get(bot).getString("nickname", "CraftIRCbot");
    }

    protected String cBotServer(int bot) {
        return bots.get(bot).getString("server", "irc.esper.net");
    }

    protected int cBotPort(int bot) {
        return bots.get(bot).getInt("port", 6667);
    }

    protected String cBotLogin(int bot) {
        return bots.get(bot).getString("userident", "");
    }

    protected String cBotPassword(int bot) {
        return bots.get(bot).getString("serverpass", "");
    }

    protected boolean cBotSsl(int bot) {
        return bots.get(bot).getBoolean("ssl", false);
    }

    protected int cBotMessageDelay(int bot) {
        return bots.get(bot).getInt("message-delay", 1000);
    }

    protected String cCommandPrefix(int bot) {
        return bots.get(bot).getString("command-prefix", getConfiguration().getString("settings.command-prefix", "."));
    }

    protected ArrayList<String> cBotAdminPrefixes(int bot) {
        return new ArrayList<String>(bots.get(bot).getStringList("admin-prefixes", null));
    }

    protected ArrayList<String> cBotIgnoredUsers(int bot) {
        return new ArrayList<String>(bots.get(bot).getStringList("ignored-users", null));
    }

    protected String cBotAuthMethod(int bot) {
        return bots.get(bot).getString("auth.method", "nickserv");
    }

    protected String cBotAuthUsername(int bot) {
        return bots.get(bot).getString("auth.username", "");
    }

    protected String cBotAuthPassword(int bot) {
        return bots.get(bot).getString("auth.password", "");
    }

    protected ArrayList<String> cBotOnConnect(int bot) {
        return new ArrayList<String>(bots.get(bot).getStringList("on-connect", null));
    }

    protected String cChanName(int bot, String channel) {
        return getChanNode(bot, channel).getString("name", "#changeme");
    }

    protected String cChanTag(int bot, String channel) {
        return getChanNode(bot, channel).getString("tag", String.valueOf(bot) + "_" + channel);
    }

    protected String cChanPassword(int bot, String channel) {
        return getChanNode(bot, channel).getString("password", "");
    }

    protected ArrayList<String> cChanOnJoin(int bot, String channel) {
        return new ArrayList<String>(getChanNode(bot, channel).getStringList("on-join", null));
    }
    
    public boolean cPathExists(String source, String target) {
        ConfigurationNode pathNode = getPathNode(source, target);
        return pathNode != null && !pathNode.getBoolean("disabled", false);
    }
    
    public boolean cPathAttribute(String source, String target, String attribute) {
        return getPathNode(source, target).getBoolean(attribute, false);
    }
    
    public List<ConfigurationNode> cPathFilters(String source, String target) {
        return getPathNode(source, target).getNodeList("filters", new ArrayList<ConfigurationNode>());
    }

    protected enum HoldType {
        CHAT, JOINS, QUITS, KICKS, BANS
    }

    protected class RemoveHoldTask extends TimerTask {
        private CraftIRC plugin;
        private HoldType ht;

        protected RemoveHoldTask(CraftIRC plugin, HoldType ht) {
            super();
            this.plugin = plugin;
            this.ht = ht;
        }

        public void run() {
            this.plugin.hold.put(ht, false);
        }
    }

    protected boolean isHeld(HoldType ht) {
        return hold.get(ht);
    }

    protected boolean hasPerms() {
        return false;
    }

    protected boolean checkPerms(Player pl, String path) {
        return pl.hasPermission(path);
    }

    protected boolean checkPerms(String pl, String path) {
        Player pit = getServer().getPlayer(pl);
        if (pit != null)
            return pit.hasPermission(path);
        return false;
    }

    protected String colorizeName(String name) {
        Pattern color_codes = Pattern.compile("§[0-9a-f]");
        Matcher find_colors = color_codes.matcher(name);
        while (find_colors.find()) {
            name = find_colors.replaceFirst(Character.toString((char) 3)
                    + String.format("%02d", cColorIrcFromGame(find_colors.group())));
            find_colors = color_codes.matcher(name);
        }
        return name;
    }

    protected String getPermPrefix(String world, String pl) {
        //TODO: Get from herochat/attributeproviders?
        String result = "";
        return colorizeName(result.replaceAll("&([0-9a-f])", "§$1"));
    }

    protected String getPermSuffix(String world, String pl) {
        //TODO: Get from herochat/attributeproviders?
        String result = "";
        return colorizeName(result.replaceAll("&([0-9a-f])", "§$1"));
    }
   
    protected void enqueueConsoleCommand(String cmd) {
      try {
        getServer().dispatchCommand(new ConsoleCommandSender(getServer()), cmd);

      } catch (Exception e) {
          e.printStackTrace();
      }
    
       
    }
 

}