package com.ensifera.animosity.craftirc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

import net.milkbowl.vault.chat.Chat;
import org.bukkit.plugin.RegisteredServiceProvider;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.util.config.Configuration;
import com.sk89q.util.config.ConfigurationNode;


/**
 * @author Animosity
 * @author ricin
 * @author Protected
 * @author mbaxter
 * 
 */

//TODO: Better handling of null method returns (try to crash the bot and then stop that from happening again)
public class CraftIRC extends JavaPlugin {
    
    public static final String NAME = "CraftIRC";
    public static String VERSION;
    private String DEFAULTCONFIG_INJAR_PATH = "/defaults/config.yml";
    static final Logger log = Logger.getLogger("Minecraft");
    
    Configuration configuration;
    
    //Misc class attributes
    PluginDescriptionFile desc = null;
    public Server server = null;
    private final CraftIRCListener listener = new CraftIRCListener(this);
    private final ConsoleListener sayListener = new ConsoleListener(this);
    private ArrayList<Minebot> instances;
    private boolean debug;
    private Timer holdTimer = new Timer();
    private Timer retryTimer = new Timer();
    Map<HoldType, Boolean> hold;
    Map<String, RetryTask> retry;

    //Bots and channels config storage
    private List<ConfigurationNode> bots;
    private List<ConfigurationNode> colormap;
    private Map<Integer, ArrayList<ConfigurationNode>> channodes;
    private Map<Path, ConfigurationNode> paths;
    
    //Endpoints
    private Map<String,EndPoint> endpoints;
    private Map<EndPoint,String> tags;
    private Map<String,CommandEndPoint> irccmds;
    private Map<String,List<String>> taggroups;
    private Chat vault;
    
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
        	configuration = new Configuration(new File(getDataFolder().getPath() + "/config.yml"));
        	configuration.load();
        	
            endpoints = new HashMap<String,EndPoint>();
            tags = new HashMap<EndPoint,String>();
            irccmds = new HashMap<String,CommandEndPoint>();
            taggroups = new HashMap<String,List<String>>();
            
            PluginDescriptionFile desc = this.getDescription();
            VERSION = desc.getVersion();
            server = this.getServer();
            
            String dataFolderPath = this.getDataFolder().getPath() + File.separator;
            (new File(dataFolderPath)).mkdir();

            //Checking if the configuration file exists and imports the default one from the .jar if it doesn't
            File configFile = new File(dataFolderPath + "config.yml");
            if (!configFile.exists()) {
                importDefaultConfig(DEFAULTCONFIG_INJAR_PATH, configFile);
                autoDisable();
                return;
            }
            
            bots = new ArrayList<ConfigurationNode>(configuration.getNodeList("bots", null));
            channodes = new HashMap<Integer, ArrayList<ConfigurationNode>>();
            for (int botID = 0; botID < bots.size(); botID++)
                channodes.put(botID, new ArrayList<ConfigurationNode>(bots.get(botID).getNodeList("channels", null)));
            
            colormap = new ArrayList<ConfigurationNode>(configuration.getNodeList("colormap", null));
            
            paths = new HashMap<Path,ConfigurationNode>();
            for (ConfigurationNode path : configuration.getNodeList("paths", new LinkedList<ConfigurationNode>())) {
                Path identifier = new Path(path.getString("source"), path.getString("target"));
                if (!identifier.getSourceTag().equals(identifier.getTargetTag()) && !paths.containsKey(identifier))
                    paths.put(identifier, path);
            }
            
            //Retry timers
            retry = new HashMap<String, RetryTask>();
            retryTimer = new Timer();

            //Event listeners
            getServer().getPluginManager().registerEvents(listener, this);
            getServer().getPluginManager().registerEvents(sayListener, this);
            
            //Native endpoints!
            if (cMinecraftTag() != null && !cMinecraftTag().equals("")) {
            	registerEndPoint(cMinecraftTag(), new MinecraftPoint(this, getServer())); //The minecraft server, no bells and whistles
            	for (String cmd : cCmdWordSay(null))
            		registerCommand(cMinecraftTag(), cmd);
            	for (String cmd : cCmdWordPlayers(null))
            		registerCommand(cMinecraftTag(), cmd);
            	if (!cMinecraftTagGroup().equals(""))
            		groupTag(cMinecraftTag(), cMinecraftTagGroup());
            }
            if (cCancelledTag() != null && !cCancelledTag().equals("")) {
            	registerEndPoint(cCancelledTag(), new MinecraftPoint(this, getServer())); //Handles cancelled chat
            	if (!cMinecraftTagGroup().equals(""))
            		groupTag(cCancelledTag(), cMinecraftTagGroup());
            }
            if (cConsoleTag() != null && !cConsoleTag().equals("")) {
            	registerEndPoint(cConsoleTag(), new ConsolePoint(this, getServer()));     //The minecraft console
            	for (String cmd : cCmdWordCmd(null))
            		registerCommand(cConsoleTag(), cmd);
            	if (!cMinecraftTagGroup().equals(""))
            		groupTag(cConsoleTag(), cMinecraftTagGroup());
            }

            //Create bots
            instances = new ArrayList<Minebot>();
            for (int i = 0; i < bots.size(); i++)
                instances.add(new Minebot(this, i, cDebug()));
            
            loadTagGroups();

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
                        
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
                public void run() {
                    if(CraftIRC.this.getServer().getPluginManager().isPluginEnabled("Vault")){
                        try{
                            CraftIRC.this.vault=((RegisteredServiceProvider<Chat>)getServer().getServicesManager().getRegistration(Chat.class)).getProvider();
                        } catch (Exception e){
                        
                        }
                    }
                }
            });
            
            setDebug(cDebug());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void importDefaultConfig(String injarPath, File destination) {
        try {
            InputStream is = this.getClass().getResourceAsStream(injarPath);
            if (is == null) {
                throw new Exception("The default configuration file could not be found in the .jar");
            }
            OutputStream os = new FileOutputStream(destination);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();
        } catch (Exception e) {
            dowarn("The default configuration file could not be imported:");
            e.printStackTrace();
            dowarn("You can MANUALLY place config.yml in " + destination.getParent());
            return;
        }
        dolog("Default configuration file created: " + destination.getPath());
        dolog("Take some time to EDIT it, then restart your server.");
    }
    
    private void autoDisable() {
        dolog("Auto-disabling...");
        getServer().getPluginManager().disablePlugin(this);
    }

    public void onDisable() {
        try {
            retryTimer.cancel();
            holdTimer.cancel();
            //Disconnect bots
            if (bots != null) {
                for (int i = 0; i < bots.size(); i++) {
                    instances.get(i).disconnect();
                    instances.get(i).dispose();
                }
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
            } else
                return false;
        } catch (Exception e) { 
            e.printStackTrace(); 
            return false;
        }
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
            msg.doNotColor("message");
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
            msg.doNotColor("message");
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
            msg.doNotColor("message");
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
        if (tag == null) dolog("Failed to register endpoint - No tag!");
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
        ungroupTag(tag);
        if (ep instanceof CommandEndPoint) {
            CommandEndPoint cep = (CommandEndPoint)ep;
            for (String cmd : irccmds.keySet()) {
                if (irccmds.get(cmd) == cep)
                    irccmds.remove(cmd);
            }
        }
        return true;
    }
    
    public boolean groupTag(String tag, String group) {
    	if (getEndPoint(tag) == null) return false;
    	List<String> tags = taggroups.get(group);
    	if (tags == null) {
    		tags = new ArrayList<String>();
    		taggroups.put(group, tags);
    	}
    	tags.add(tag);
    	return true;
    }
    public void ungroupTag(String tag) {
    	for (String group : taggroups.keySet())
    		taggroups.get(group).remove(tag);
    }
    public void clearGroup(String group) {
    	taggroups.remove(group);
    }
    public boolean checkTagsGrouped(String tagA, String tagB) {
    	for (String group : taggroups.keySet())
    		if (taggroups.get(group).contains(tagA) && taggroups.get(group).contains(tagB))
    			return true;
    	return false;
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
        //If we weren't explicitly given a recipient for the message, let's try to find one (or more)
        if (knownDestinations.size() < 1) {
            //Use all possible destinations (auto-targets)
            destinations = new LinkedList<EndPoint>();
            for (String targetTag : cPathsFrom(sourceTag)) {
            	EndPoint ep = getEndPoint(targetTag);
            	if (ep instanceof SecuredEndPoint && SecuredEndPoint.Security.REQUIRE_TARGET.equals(((SecuredEndPoint)ep).getSecurity())) continue;
                if (!cPathAttribute(sourceTag, targetTag, "attributes." + msg.getEvent())) continue;
                if (dm == RelayedMessage.DeliveryMethod.ADMINS && !cPathAttribute(sourceTag, targetTag, "attributes.admin")) continue;
                destinations.add(ep);
            }
            //Default paths to unsecured destinations (auto-paths)
            if (cAutoPaths()) {
            	for (EndPoint ep : endpoints.values()) {
            		if (msg.getSource().equals(ep) || destinations.contains(ep)) continue;
            		if (ep instanceof SecuredEndPoint && !SecuredEndPoint.Security.UNSECURED.equals(((SecuredEndPoint)ep).getSecurity())) continue;
        			String targetTag = getTag(ep);
        			if (checkTagsGrouped(sourceTag,targetTag)) continue;
        			if (!cPathAttribute(sourceTag, targetTag, "attributes." + msg.getEvent())) continue;
        			if (dm == RelayedMessage.DeliveryMethod.ADMINS && !cPathAttribute(sourceTag, targetTag, "attributes.admin")) continue;
        			if (cPathAttribute(sourceTag, targetTag, "disabled")) continue;
        			destinations.add(ep);
            	}
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
        String result="";
        if(this.vault!=null){
            try{
                result=vault.getPlayerPrefix(p);
            }catch (Exception e){
            
            }
        }
        return result;
    }
    
    String getSuffix(Player p) {
        String result="";
        if(this.vault!=null){
            try{
                result=vault.getPlayerSuffix(p);
            }catch (Exception e){
            
            }
        }
        return result;
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

    // TODO: Make sure this works
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
        getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
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
        if (result == null) return configuration.getNode("default-attributes");
        ConfigurationNode basepath;
        if (result.getKeys().contains("base") && (basepath = result.getNode("base")) != null) {
            ConfigurationNode basenode = paths.get(new Path(basepath.getString("source", ""), basepath.getString("target", "")));
            if (basenode != null) result = basenode;
        }
        return result;
    }

    String cMinecraftTag() {
        return configuration.getString("settings.minecraft-tag", "minecraft");
    }
    String cCancelledTag() {
        return configuration.getString("settings.cancelled-tag", "cancelled");
    }
    String cConsoleTag() {
        return configuration.getString("settings.console-tag", "console");
    }
    
    String cMinecraftTagGroup() {
    	return configuration.getString("settings.minecraft-group-name", "minecraft");
    }
    String cIrcTagGroup() {
    	return configuration.getString("settings.irc-group-name", "irc");
    }
    
    boolean cAutoPaths() {
    	return configuration.getBoolean("settings.auto-paths", false);
    }
    
    boolean cCancelChat() {
        return configuration.getBoolean("settings.cancel-chat", false);
    }
    
    boolean cDebug() {
        return configuration.getBoolean("settings.debug", false);
    }

    ArrayList<String> cConsoleCommands() {
        return new ArrayList<String>(configuration.getStringList("settings.console-commands", null));
    }

    public int cHold(String eventType) {
        return configuration.getInt("settings.hold-after-enable." + eventType, 0);
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
        if (msg.getSource().getType() == EndPoint.Type.MINECRAFT) return configuration.getString("settings.formatting.from-game." + eventType);
        if (msg.getSource().getType() == EndPoint.Type.IRC) return configuration.getString("settings.formatting.from-irc." + eventType);
        if (msg.getSource().getType() == EndPoint.Type.PLAIN) return configuration.getString("settings.formatting.from-plain." + eventType);
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
        return configuration.getString("settings.bind-address","");
    }
    
    int cRetryDelay() {
        return configuration.getInt("settings.retry-delay", 10) * 1000;
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
        return bots.get(bot).getString("command-prefix", configuration.getString("settings.command-prefix", "."));
    }
    
    public List<String> cCmdWordCmd(Integer bot) {
    	List<String> init = new ArrayList<String>(); init.add("cmd");
    	List<String> result = configuration.getStringList("settings.irc-commands.cmd", init);
    	if (bot != null)
    		return bots.get(bot).getStringList("irc-commands.cmd", result);
    	return result;
    }
    public List<String> cCmdWordSay(Integer bot) {
    	List<String> init = new ArrayList<String>(); init.add("say");
    	List<String> result = configuration.getStringList("settings.irc-commands.say", init);
    	if (bot != null)
    		return bots.get(bot).getStringList("irc-commands.say", result);
    	return result;
    }
    public List<String> cCmdWordPlayers(Integer bot) {
    	List<String> init = new ArrayList<String>(); init.add("players");
    	List<String> result = configuration.getStringList("settings.irc-commands.players", init);
    	if (bot != null)
    		return bots.get(bot).getStringList("irc-commands.players", result);
    	return result;
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
        else return configuration.getNode("default-attributes").getBoolean(attribute, false);
    }
    
    public List<ConfigurationNode> cPathFilters(String source, String target) {
        return getPathNode(source, target).getNodeList("filters", new ArrayList<ConfigurationNode>());
    }
    
    void loadTagGroups() {
    	List<String> groups = configuration.getKeys("settings.tag-groups");
    	if (groups == null) return;
    	for (String group : groups)
    		for (String tag : configuration.getStringList("settings.tag-groups." + group, new ArrayList<String>()))
    			groupTag(tag, group);
    }
    
    boolean cUseMapAsWhitelist(int bot) {
        return bots.get(bot).getBoolean("use-map-as-whitelist", false);
    }
    
    String cIrcDisplayName(int bot, String nickname) {
    	return bots.get(bot).getString("irc-nickname-map." + nickname, nickname);
    }
    
    boolean cNicknameIsInIrcMap(int bot, String nickname) {
    	return bots.get(bot).getString("irc-nickname-map." + nickname) != null;
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
