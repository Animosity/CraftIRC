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

import net.myshelter.minecraft.PlayerInfo;

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

//TODO: Ask outsider for suggestions for appropriate command feedback (usability tests)
//TODO: Better handling of null method returns (try to crash the bot and then stop that from happening again)
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
    private Timer retryTimer = new Timer();
    Map<HoldType, Boolean> hold;
    Map<String, RetryTask> retry;
    private PlayerInfo infoservice = null;

    //Bots and channels config storage
    private List<ConfigurationNode> bots;
    private List<ConfigurationNode> colormap;
    private Map<Integer, ArrayList<ConfigurationNode>> channodes;
    private Map<Path, ConfigurationNode> paths;
    
    //Endpoints
    private Map<String,EndPoint> endpoints;
    private Map<EndPoint,String> tags;
    private Map<String,CommandEndPoint> irccmds;
    
    static void dolog(String message) {
        log.info("[" + NAME + "] " + message);
    }
    static void dowarn(String message) {
        log.log(Level.WARNING, "[" + NAME + "] " + message);
    }

    /***************************
     Bukkit stuff
     ***************************/
    
    public void onEnable() {
        try {
            endpoints = new HashMap<String,EndPoint>();
            tags = new HashMap<EndPoint,String>();
            irccmds = new HashMap<String,CommandEndPoint>();
            
            PluginDescriptionFile desc = this.getDescription();
            VERSION = desc.getVersion();
            server = this.getServer();
                       
            //Load node lists. Bukkit does it now, hurray!
            if (null == getConfiguration()) {
                dowarn("config.yml could not be found in plugins/CraftIRC/ -- disabling!");
                getServer().getPluginManager().disablePlugin(((Plugin) (this)));
                return;
            }
            
            getConfiguration().load(); //For ircreload support
            
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
            
            //Prefixes and suffixes
            try {
                infoservice = getServer().getServicesManager().load(PlayerInfo.class);
            } catch (java.lang.NoClassDefFoundError e) {}
            
            //Retry timers
            retry = new HashMap<String, RetryTask>();
            retryTimer = new Timer();

            //Event listeners
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_KICK, listener, Priority.Monitor, this);
            
            //Native endpoints!
            registerEndPoint(cMinecraftTag(), new MinecraftPoint(this, getServer())); //The minecraft server, no bells and whistles
            registerEndPoint(cCancelledTag(), new MinecraftPoint(this, getServer())); //Handles cancelled chat
            registerEndPoint(cConsoleTag(), new ConsolePoint(this, getServer()));     //The minecraft console
            registerCommand(cMinecraftTag(), "say");
            registerCommand(cMinecraftTag(), "mc");
            registerCommand(cMinecraftTag(), "players");
            registerCommand(cConsoleTag(), "cmd");
            registerCommand(cConsoleTag(), "c");

            //Create bots
            instances = new ArrayList<Minebot>();
            for (int i = 0; i < bots.size(); i++)
                instances.add(new Minebot(this, i, cDebug()));

            dolog("Enabled.");

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
            retryTimer.cancel();
            holdTimer.cancel();
            //Disconnect bots
            for (int i = 0; i < bots.size(); i++) {
                instances.get(i).disconnect();
                instances.get(i).dispose();
            }
            dolog("Disabled.");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /***************************
    Minecraft command handling
    ***************************/

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        String commandName = command.getName().toLowerCase();
         
        try {
            if (sender instanceof IRCCommandSender) sender = (IRCCommandSender)sender;
            
            if (commandName.equals("ircmsg")) {
                if (!sender.hasPermission("craftirc." + commandName)) return false;
                return this.cmdMsgToTag(sender, args);
            } else if (commandName.equals("ircmsguser")) {
                if (!sender.hasPermission("craftirc." + commandName)) return false;
                return this.cmdMsgToUser(sender, args);                
            } else if (commandName.equals("ircusers")) {
                if (!sender.hasPermission("craftirc." + commandName)) return false;
                return this.cmdGetUserList(sender, args);
            } else if (commandName.equals("admins!")) {
                if (!sender.hasPermission("craftirc.admins")) return false;
                return this.cmdNotifyIrcAdmins(sender, args);
            } else if (commandName.equals("ircraw")) {
                if (!sender.hasPermission("craftirc." + commandName)) return false;
                return this.cmdRawIrcCommand(sender, args);
            } else if (commandName.equals("ircreload")) {
                if (!sender.hasPermission("craftirc." + commandName)) return false;
                getServer().getPluginManager().disablePlugin(this);
                getServer().getPluginManager().enablePlugin(this);
                return true;
            } else if (commandName.equals("say")) {
                // Capture the 'say' command from Minecraft Console
                if (sender instanceof ConsoleCommandSender) {
                    RelayedMessage msg = newMsg(getEndPoint(cConsoleTag()), null, "generic");
                    msg.setField("message", Util.combineSplit(1, args, " "));
                    msg.post();
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
            if (this.isDebug()) dolog("CraftIRCListener cmdMsgToAll()");
            if (args.length < 2) return false;
            String msgToSend = Util.combineSplit(1, args, " ");
            RelayedMessage msg = this.newMsg(getEndPoint(cMinecraftTag()), getEndPoint(args[0]), "chat");
            if (msg == null) return true;
            if (sender instanceof Player)
                msg.setField("sender", ((Player) sender).getDisplayName());
            else
                msg.setField("sender", "SERVER");
            msg.setField("message", msgToSend);
            msg.post();
            sender.sendMessage("Message sent.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdMsgToUser(CommandSender sender, String[] args) {
        try {
            if (args.length < 3) return false;
            String msgToSend = Util.combineSplit(2, args, " ");
            RelayedMessage msg = this.newMsg(getEndPoint(cMinecraftTag()), getEndPoint(args[0]), "private");
            if (msg == null) return true;
            if (sender instanceof Player)
                msg.setField("sender", ((Player) sender).getDisplayName());
            else
                msg.setField("sender", "SERVER");;
            msg.setField("message", msgToSend);
            msg.postToUser(args[1]);
            sender.sendMessage("Message sent.");
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
            if (this.isDebug()) dolog("CraftIRCListener cmdNotifyIrcAdmins()");
            if (args.length == 0 || !(sender instanceof Player)) {
                if (this.isDebug()) dolog("CraftIRCListener cmdNotifyIrcAdmins() - args.length == 0 or Sender != player ");
                return false;
            }
            RelayedMessage msg = newMsg(getEndPoint(cMinecraftTag()), null, "admin");
            if (msg == null) return true;
            msg.setField("sender", ((Player) sender).getDisplayName());
            msg.setField("message", Util.combineSplit(0, args, " "));
            msg.setField("world", ((Player) sender).getWorld().getName());
            msg.post(true);
            sender.sendMessage("Admin notice sent.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean cmdRawIrcCommand(CommandSender sender, String[] args) {
        try {
            if (this.isDebug()) dolog("cmdRawIrcCommand(sender=" + sender.toString() + ", args=" + Util.combineSplit(0, args, " "));
            if (args.length < 2) return false;
            this.sendRawToBot(Util.combineSplit(1, args, " "), Integer.parseInt(args[0]));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
        
    /***************************
    Endpoint and message interface (to be used by CraftIRC and external plugins)
    ***************************/
    
    //Null target: Sends message through all possible paths.
    public RelayedMessage newMsg(EndPoint source, EndPoint target, String eventType) {
        if (source == null) return null;
        if (target == null || cPathExists(getTag(source), getTag(target)))
            return new RelayedMessage(this, source, target, eventType);
        else {
            if (isDebug())
                dolog("Failed to prepare message: " + getTag(source) + " -> " + getTag(target) + " (missing path)");
            return null;
        }
    }
    public RelayedMessage newMsgToTag(EndPoint source, String target, String eventType) {
        if (source == null) return null;
        EndPoint targetpoint = null;
        if (target != null) {
            if (cPathExists(getTag(source), target)) {
                targetpoint = getEndPoint(target);
                if (targetpoint == null) dolog("The requested target tag '" + target + "' isn't registered.");
            } else return null;
        }
        return new RelayedMessage(this, source, targetpoint, eventType);
    }
    public RelayedCommand newCmd(EndPoint source, String command) {
        if (source == null) return null;
        CommandEndPoint target = irccmds.get(command);
        if (target == null) return null;
        if (!cPathExists(getTag(source), getTag(target))) return null;
        RelayedCommand cmd = new RelayedCommand(this, source, target);
        cmd.setField("command", command);
        return cmd;
    }
    
    public boolean registerEndPoint(String tag, EndPoint ep) {
        if (isDebug()) dolog("Registering endpoint: " + tag);
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
    public boolean endPointRegistered(String tag) {
        return endpoints.get(tag) != null;
    }
    EndPoint getEndPoint(String tag) {
        return endpoints.get(tag);
    }
    String getTag(EndPoint ep) {
        return tags.get(ep);
    }
    public boolean registerCommand(String tag, String command) {
        if (isDebug()) dolog("Registering command: " + command + " to endpoint:" + tag);
        EndPoint ep = getEndPoint(tag);
        if (ep == null) {
            dolog("Couldn't register the command '" + command + "' at the endpoint tagged '" + tag + "' because there is no such tag.");
            return false;
        }
        if (!(ep instanceof CommandEndPoint)) {
            dolog("Couldn't register the command '" + command + "' at the endpoint tagged '" + tag + "' because it's not capable of handling commands.");
            return false;
        }
        if (irccmds.containsKey(command)) {
            dolog("Couldn't register the command '" + command + "' at the endpoint tagged '" + tag + "' because that command is already registered.");
            return false;
        }
        irccmds.put(command, (CommandEndPoint)ep);
        return true;
    }
    public boolean unregisterCommand(String command) {
        if (!irccmds.containsKey(command)) return false;
        irccmds.remove(command);
        return true;
    }
    public boolean unregisterEndPoint(String tag) {
        EndPoint ep = getEndPoint(tag);
        if (ep == null) return false;
        endpoints.remove(tag);
        tags.remove(ep);
        if (ep instanceof CommandEndPoint) {
            CommandEndPoint cep = (CommandEndPoint)ep;
            for (String cmd : irccmds.keySet()) {
                if (irccmds.get(cmd) == cep)
                    irccmds.remove(cmd);
            }
        }
        return true;
    }
    
    /***************************
    Heart of the beast! Unified method with no special cases that replaces the old sendMessage
    ***************************/
    
    boolean delivery(RelayedMessage msg) {
        return delivery(msg, null, null, RelayedMessage.DeliveryMethod.STANDARD);
    }
    boolean delivery(RelayedMessage msg, List<EndPoint> destinations) {
        return delivery(msg, destinations, null, RelayedMessage.DeliveryMethod.STANDARD);
    }
    boolean delivery(RelayedMessage msg, List<EndPoint> knownDestinations, String username) {
        return delivery(msg, knownDestinations, username, RelayedMessage.DeliveryMethod.STANDARD);
    }
    //Only successful if all known targets (or if there is none at least one possible target) are successful!
    boolean delivery(RelayedMessage msg, List<EndPoint> knownDestinations, String username, RelayedMessage.DeliveryMethod dm) {
        String sourceTag = getTag(msg.getSource());
        msg.setField("source", sourceTag);
        List<EndPoint> destinations;
        if (this.isDebug())
            dolog("X->" + (knownDestinations.size() > 0 ? knownDestinations.toString() : "*") + ": " + msg.toString());
        if (knownDestinations.size() < 1) {
            //Use all possible destinations
            destinations = new LinkedList<EndPoint>();
            for (String targetTag : cPathsFrom(sourceTag)) {
                if (!cPathAttribute(sourceTag, targetTag, "attributes." + msg.getEvent())) continue;
                if (dm == RelayedMessage.DeliveryMethod.ADMINS && !cPathAttribute(sourceTag, targetTag, "attributes.admin")) continue;
                destinations.add(getEndPoint(targetTag));
            }
        } else destinations = new LinkedList<EndPoint>(knownDestinations);
        if (destinations.size() < 1) return false;
        //Deliver the message
        boolean success = true;
        for (EndPoint destination : destinations) {
            String targetTag = getTag(destination);
            msg.setField("target", targetTag);
            //Check against path filters
            if (msg instanceof RelayedCommand && matchesFilter(msg, cPathFilters(sourceTag, targetTag))) {
                if (knownDestinations != null) success = false;
                continue;
            }
            //Finally deliver!
            if (this.isDebug())
                dolog("-->X: " + msg.toString());
            if (username != null)
                success = success && destination.userMessageIn(username, msg);
            else if (dm == RelayedMessage.DeliveryMethod.ADMINS) 
                success = destination.adminMessageIn(msg);
            else if (dm == RelayedMessage.DeliveryMethod.COMMAND) {
                if (!(destination instanceof CommandEndPoint)) continue;
                ((CommandEndPoint)destination).commandIn((RelayedCommand)msg);
            } else
                destination.messageIn(msg);
        }
        return success;
    }
    
    boolean matchesFilter(RelayedMessage msg, List<ConfigurationNode> filters) {
        if (filters == null) return false;
        newFilter: for (ConfigurationNode filter : filters) {
            for (String key : filter.getKeys()) {
                Pattern condition = Pattern.compile(filter.getString(key, ""));
                if (condition == null) continue newFilter;
                String subject = msg.getField(key);
                if (subject == null) continue newFilter;
                Matcher check = condition.matcher(subject);
                if (!check.find()) continue newFilter;
            }
            return true; 
        }
        return false;
    }

    /***************************
    Auxiliary methods
    ***************************/
    
    void sendRawToBot(String rawMessage, int bot) {
        if (this.isDebug()) dolog("sendRawToBot(bot=" + bot + ", message=" + rawMessage);
        Minebot targetBot = instances.get(bot);
        targetBot.sendRawLineViaQueue(rawMessage);
    }
    
    void sendMsgToTargetViaBot(String message, String target, int bot) {
        Minebot targetBot = instances.get(bot);
        targetBot.sendMessage(target, message);
    }
    
    List<String> ircUserLists(String tag) {
        return getEndPoint(tag).listDisplayUsers();        
    }

    void setDebug(boolean d) {
        debug = d;

        for (int i = 0; i < bots.size(); i++)
            instances.get(i).setVerbose(d);

        dolog("DEBUG [" + (d ? "ON" : "OFF") + "]");
    }
    
    String getPrefix(Player p) {
        if (infoservice != null) return infoservice.getPrefix(p);
        else return "";
    }
    
    String getSuffix(Player p) {
        if (infoservice != null) return infoservice.getSuffix(p);
        else return "";
    }

    boolean isDebug() {
        return debug;
    }
    
    boolean checkPerms(Player pl, String path) {
        return pl.hasPermission(path);
    }

    boolean checkPerms(String pl, String path) {
        Player pit = getServer().getPlayer(pl);
        if (pit != null)
            return pit.hasPermission(path);
        return false;
    }

    String colorizeName(String name) {
        Pattern color_codes = Pattern.compile("§[0-9a-f]");
        Matcher find_colors = color_codes.matcher(name);
        while (find_colors.find()) {
            name = find_colors.replaceFirst(Character.toString((char) 3)
                    + String.format("%02d", cColorIrcFromGame(find_colors.group())));
            find_colors = color_codes.matcher(name);
        }
        return name;
    }
   
    protected void enqueueConsoleCommand(String cmd) {
      try {
        getServer().dispatchCommand(new ConsoleCommandSender(getServer()), cmd);
      } catch (Exception e) {
          e.printStackTrace();
      }
    }
    
    //If the channel is null it's a reconnect, otherwise a rejoin
    void scheduleForRetry(Minebot bot, String channel) {
        retryTimer.schedule(new RetryTask(this, bot, channel), cRetryDelay());
    }
    
    /***************************
    Read stuff from config
    ***************************/

    private ConfigurationNode getChanNode(int bot, String channel) {
        ArrayList<ConfigurationNode> botChans = channodes.get(bot);
        for (Iterator<ConfigurationNode> it = botChans.iterator(); it.hasNext();) {
            ConfigurationNode chan = it.next();
            if (chan.getString("name").equalsIgnoreCase(channel))
                return chan;
        }
        return Configuration.getEmptyNode();
    }
    
    List<ConfigurationNode> cChannels(int bot) {
        return channodes.get(bot);
    }
    
    private ConfigurationNode getPathNode(String source, String target) {
        ConfigurationNode result = paths.get(new Path(source, target));
        if (result == null) return getConfiguration().getNode("default-attributes");
        ConfigurationNode basepath;
        if (result.getKeys().contains("base") && (basepath = result.getNode("base")) != null) {
            ConfigurationNode basenode = paths.get(new Path(basepath.getString("source", ""), basepath.getString("target", "")));
            if (basenode != null) result = basenode;
        }
        return result;
    }

    String cMinecraftTag() {
        return getConfiguration().getString("settings.minecraft-tag", "minecraft");
    }
    String cCancelledTag() {
        return getConfiguration().getString("settings.cancelled-tag", "cancelled");
    }
    String cConsoleTag() {
        return getConfiguration().getString("settings.console-tag", "console");
    }
    
    boolean cCancelChat() {
        return getConfiguration().getBoolean("settings.cancel-chat", false);
    }
    
    boolean cDebug() {
        return getConfiguration().getBoolean("settings.debug", false);
    }

    ArrayList<String> cConsoleCommands() {
        return new ArrayList<String>(getConfiguration().getStringList("settings.console-commands", null));
    }

    public int cHold(String eventType) {
        return getConfiguration().getInt("settings.hold-after-enable." + eventType, 0);
    }

    String cFormatting(String eventType, RelayedMessage msg) {
        return cFormatting(eventType, msg, null);
    }
    String cFormatting(String eventType, RelayedMessage msg, EndPoint realTarget) {
        String source = getTag(msg.getSource()), target = getTag(realTarget != null ? realTarget : msg.getTarget());
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
        if (msg.getSource().getType() == EndPoint.Type.PLAIN) return getConfiguration().getString("settings.formatting.from-plain." + eventType);
        return "";
    }

    int cColorIrcFromGame(String game) {
        ConfigurationNode color;
        Iterator<ConfigurationNode> it = colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("game").equals(game))
                return color.getInt("irc", cColorIrcFromName("foreground"));
        }
        return cColorIrcFromName("foreground");
    }

    int cColorIrcFromName(String name) {
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

    String cColorGameFromIrc(int irc) {
        ConfigurationNode color;
        Iterator<ConfigurationNode> it = colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getInt("irc", -1) == irc)
                return color.getString("game", cColorGameFromName("foreground"));
        }
        return cColorGameFromName("foreground");
    }

    String cColorGameFromName(String name) {
        ConfigurationNode color;
        Iterator<ConfigurationNode> it = colormap.iterator();
        while (it.hasNext()) {
            color = it.next();
            if (color.getString("name").equalsIgnoreCase(name) && color.getProperty("game") != null)
                return color.getString("game", "\u00C2\u00A7f");
        }
        if (name.equalsIgnoreCase("foreground"))
            return "\u00C2\u00A7f";
        else
            return cColorGameFromName("foreground");
    }

    String cBindLocalAddr() {
        return getConfiguration().getString("settings.bind-address","");
    }
    
    int cRetryDelay() {
        return getConfiguration().getInt("settings.retry-delay", 10) * 1000;
    }

    String cBotNickname(int bot) {
        return bots.get(bot).getString("nickname", "CraftIRCbot");
    }

    String cBotServer(int bot) {
        return bots.get(bot).getString("server", "irc.esper.net");
    }

    int cBotPort(int bot) {
        return bots.get(bot).getInt("port", 6667);
    }

    String cBotLogin(int bot) {
        return bots.get(bot).getString("userident", "");
    }

    String cBotPassword(int bot) {
        return bots.get(bot).getString("serverpass", "");
    }

    boolean cBotSsl(int bot) {
        return bots.get(bot).getBoolean("ssl", false);
    }

    int cBotMessageDelay(int bot) {
        return bots.get(bot).getInt("message-delay", 1000);
    }
    
    int cBotQueueSize(int bot) {
        return bots.get(bot).getInt("queue-size", 5);
    }

    public String cCommandPrefix(int bot) {
        return bots.get(bot).getString("command-prefix", getConfiguration().getString("settings.command-prefix", "."));
    }

    public ArrayList<String> cBotAdminPrefixes(int bot) {
        return new ArrayList<String>(bots.get(bot).getStringList("admin-prefixes", null));
    }

    ArrayList<String> cBotIgnoredUsers(int bot) {
        return new ArrayList<String>(bots.get(bot).getStringList("ignored-users", null));
    }

    String cBotAuthMethod(int bot) {
        return bots.get(bot).getString("auth.method", "nickserv");
    }

    String cBotAuthUsername(int bot) {
        return bots.get(bot).getString("auth.username", "");
    }

    String cBotAuthPassword(int bot) {
        return bots.get(bot).getString("auth.password", "");
    }

    ArrayList<String> cBotOnConnect(int bot) {
        return new ArrayList<String>(bots.get(bot).getStringList("on-connect", null));
    }

    String cChanName(int bot, String channel) {
        return getChanNode(bot, channel).getString("name", "#changeme");
    }

    String cChanTag(int bot, String channel) {
        return getChanNode(bot, channel).getString("tag", String.valueOf(bot) + "_" + channel);
    }

    String cChanPassword(int bot, String channel) {
        return getChanNode(bot, channel).getString("password", "");
    }

    ArrayList<String> cChanOnJoin(int bot, String channel) {
        return new ArrayList<String>(getChanNode(bot, channel).getStringList("on-join", null));
    }
    
    List<String> cPathsFrom(String source) {
        List<String> results = new LinkedList<String>();
        for (Path path : paths.keySet()) {
            if (!path.getSourceTag().equals(source)) continue;
            if (paths.get(path).getBoolean("disable", false)) continue;
            results.add(path.getTargetTag());
        }
        return results;
    }
    
    List<String> cPathsTo(String target) {
        List<String> results = new LinkedList<String>();
        for (Path path : paths.keySet()) {
            if (!path.getTargetTag().equals(target)) continue;
            if (paths.get(path).getBoolean("disable", false)) continue;
            results.add(path.getSourceTag());
        }
        return results;
    }
    
    
    public boolean cPathExists(String source, String target) {
        ConfigurationNode pathNode = getPathNode(source, target);
        return pathNode != null && !pathNode.getBoolean("disabled", false);
    }
    
    public boolean cPathAttribute(String source, String target, String attribute) {
        ConfigurationNode node = getPathNode(source, target);
        if (node.getProperty(attribute) != null) return node.getBoolean(attribute, false);
        else return getConfiguration().getNode("default-attributes").getBoolean(attribute, false);
    }
    
    public List<ConfigurationNode> cPathFilters(String source, String target) {
        return getPathNode(source, target).getNodeList("filters", new ArrayList<ConfigurationNode>());
    }

    enum HoldType {
        CHAT, JOINS, QUITS, KICKS, BANS
    }

    class RemoveHoldTask extends TimerTask {
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

    boolean isHeld(HoldType ht) {
        return hold.get(ht);
    }

}
