package com.animosity.craftirc;

import java.io.File;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import net.minecraft.server.MinecraftServer;
import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
// import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import com.nijikokun.bukkit.Permissions.*;
import com.nijiko.permissions.PermissionHandler;
import net.minecraft.server.ICommandListener;

/**
 * @author Animosity
 * @author ricin
 * @author Protected
 */

/**
 * @author Animosity
 *
 */
public class CraftIRC extends JavaPlugin {
    public static final String NAME = "CraftIRC";
    public static String VERSION;
    protected static final Logger log = Logger.getLogger("Minecraft");
    protected static List<String> defaultConsoleCommands = Arrays.asList("kick","ban","pardon","ban-ip","pardon-ip","op","deop","tp","give","tell","stop","save-all","save-off","save-on","say");
    //Misc class attributes
    PluginDescriptionFile desc = null;
    Server server = null;
    private final CraftIRCListener listener = new CraftIRCListener(this);
    private PermissionHandler perms  = null;
    private ArrayList<Minebot> instances;
    private boolean debug;
    private Timer holdTimer;
    protected HashMap<HoldType, Boolean> hold;

    //Bots and channels config storage
    private ArrayList<ConfigurationNode> bots;
    private ArrayList<ConfigurationNode> colormap;
    private HashMap<Integer, ArrayList<ConfigurationNode>> channodes;
    private HashMap<Integer, ArrayList<String>> channames;
    protected HashMap<DualKey, String> chanTagMap;

    public void onEnable() {
        try {
            PluginDescriptionFile desc = this.getDescription();
            server = this.getServer();

            VERSION = desc.getVersion();
            //Load node lists. Bukkit does it now, hurray!
            bots = new ArrayList<ConfigurationNode>(getConfiguration().getNodeList("bots", null));
            colormap = new ArrayList<ConfigurationNode>(getConfiguration().getNodeList("colormap", null));
            channodes = new HashMap<Integer, ArrayList<ConfigurationNode>>();
            channames = new HashMap<Integer, ArrayList<String>>();
            chanTagMap = new HashMap<DualKey, String>();
            for (int botID = 0; botID < bots.size(); botID++) {
                channodes.put(botID, new ArrayList<ConfigurationNode>(bots.get(botID).getNodeList("channels", null)));
                ArrayList<String> cn = new ArrayList<String>();
                for (Iterator<ConfigurationNode> it = channodes.get(botID).iterator(); it.hasNext(); ) {
                    String channelName = it.next().getString("name");
                    chanTagMap.put(new DualKey(botID, channelName), this.cChanTag(botID, channelName));
                    cn.add(channelName);
                    
                }
                channames.put(botID, cn);
            }
            

            CraftIRC.log.info(String.format(CraftIRC.NAME + " Channel tag map: " + chanTagMap.toString()));
   
            
            //Permissions
            Plugin check = this.getServer().getPluginManager().getPlugin("Permissions");
               if(check != null) {
                perms = ((Permissions)check).getHandler();
                log.info("Permissions detected by CraftIRC.");
               } else {
                   log.info("Permissions not detected.");
            }

            //Event listeners
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, listener, Priority.Monitor, this);
            getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, listener, Priority.Monitor, this);

            //Create bots
            instances = new ArrayList<Minebot>();
            for (int i = 0; i < bots.size(); i++)
                instances.add(new Minebot(this, i).init(cDebug()));

            log.info(NAME + " Enabled.");
            
            //Hold timers
            hold = new HashMap<HoldType, Boolean>();
            holdTimer = new Timer();
            Date now = new Date();
            if (cHold("chat") > 0) {
                hold.put(HoldType.CHAT, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.CHAT), now.getTime() + cHold("chat"));
            } else hold.put(HoldType.CHAT, false);
            if (cHold("joins") > 0) {
                hold.put(HoldType.JOINS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.JOINS), now.getTime() + cHold("joins"));
            } else hold.put(HoldType.JOINS, false);
            if (cHold("quits") > 0) {
                hold.put(HoldType.QUITS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.QUITS), now.getTime() + cHold("quits"));
            } else hold.put(HoldType.QUITS, false);
            if (cHold("kicks") > 0) {
                hold.put(HoldType.KICKS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.KICKS), now.getTime() + cHold("kicks"));
            } else hold.put(HoldType.KICKS, false);
            if (cHold("bans") > 0) {
                hold.put(HoldType.BANS, true);
                holdTimer.schedule(new RemoveHoldTask(this, HoldType.BANS), now.getTime() + cHold("bans"));
            } else hold.put(HoldType.BANS, false);

            setDebug(cDebug());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDisable() {

        holdTimer.cancel();
        
        //Disconnect bots
        for (int i = 0; i < bots.size(); i++)
            instances.get(i).disconnect();

        log.info(NAME + " Disabled.");
    }
    
    protected RelayedMessage newMsg(EndPoint source, EndPoint target) {
        return new RelayedMessage(this, source, target);
    }
    
    protected void sendMessage(RelayedMessage msg, String tag, String event) {
        try {
            String realEvent = event;
            //Send to IRC
            if (msg.getTarget() == EndPoint.IRC || msg.getTarget() == EndPoint.BOTH) {
                if (msg.getSource() == EndPoint.IRC) realEvent = "irc-to-irc." + event;
                if (msg.getSource() == EndPoint.GAME) realEvent = "game-to-irc." + event;
                for (int i = 0; i < bots.size(); i++) {
                    ArrayList<String> chans = cBotChannels(i);
                    Iterator<String> it = chans.iterator();
                    while (it.hasNext()) {
                        String chan = it.next();
                        // Don't echo back to sending channel
                        if (msg.getSource() == EndPoint.IRC && msg.srcBot == i && msg.srcChannel.equals(chan)) continue;
                        // Send to all bots, channels with event enabled
                        if ((tag == null || cChanCheckTag(tag, i, chan)) && (event == null || cEvents(realEvent, i, chan))) {
                            msg.trgBot = i;
                            msg.trgChannel = chan;
                            if (msg.getTarget() == EndPoint.BOTH)
                                instances.get(i).sendMessage(chan, msg.asString(EndPoint.IRC));
                            else instances.get(i).sendMessage(chan, msg.asString());
                        }
                    }
                }
            }
            
            //Send to game (doesn't allow game to game)
            if ((msg.getTarget() == EndPoint.GAME || msg.getTarget() == EndPoint.BOTH) && msg.getSource() == EndPoint.IRC) {
                realEvent = "irc-to-game." + event;
                if ((tag == null || cChanCheckTag(tag, msg.srcBot, msg.srcChannel)) && (event == null || cEvents(realEvent, msg.srcBot, msg.srcChannel))) {
                    for (Player pl: getServer().getOnlinePlayers()) {
                        if (pl != null) {
                            if (msg.getTarget() == EndPoint.BOTH)
                                pl.sendMessage(msg.asString(EndPoint.GAME));
                            else pl.sendMessage(msg.asString());
                        }
                    }
                }
            }
        } catch (RelayedMessageException rme) {
            log.log(Level.SEVERE, rme.toString());
            rme.printStackTrace();
        }
    }
    
    protected void sendRawToBot(int bot, String message) {
        Minebot target = instances.get(bot);
        target.sendRawLineViaQueue(message);
    }
    
    /** CraftIRC API call - SendMessageToTag()
     * Sends a message to an IRC tag
     * @param message (String) - The message string to send to IRC, this will pass through CraftIRC formatter
     * 
     * @param tag (String) - The IRC target tag to receive the message
     */
    public void sendMessageToTag(String message, String tag) {
        RelayedMessage rm = newMsg(EndPoint.PLUGIN, EndPoint.IRC);
        rm.message = message;
        this.sendMessage(rm, tag, null);
    }
    
    
    protected ArrayList<String> ircUserLists(String tag) {
        ArrayList<String> result = new ArrayList<String>();
        if (tag == null)
            return result;
        for (int i = 0; i < bots.size(); i++) {
            ArrayList<String> chans = cBotChannels(i);
            Iterator<String> it = chans.iterator();
            while (it.hasNext()) {
                String chan = it.next();
                if (cChanCheckTag(tag, i, chan))
                    result.add(Util.getIrcUserList(instances.get(i), chan));
            }
        }
        return result;
    }

    protected void noticeAdmins(String message) {
        for (int i = 0; i < bots.size(); i++) {
            ArrayList<String> chans = cBotChannels(i);
            Iterator<String> it = chans.iterator();
            while (it.hasNext()) {
                String chan = it.next();
                if (cChanAdmin(i, chan))
                    instances.get(i).sendNotice(chan, message);
            }
        }
    }

    public void setDebug(boolean d) {
        debug = d;

        for (int i = 0; i < bots.size(); i++)
            instances.get(i).setVerbose(d);

        log.info(NAME + " DEBUG [" + (d ? "ON" : "OFF") + "]");
    }

    public boolean isDebug() {
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

    protected boolean cDebug() {
        return getConfiguration().getBoolean("settings.debug", false);
    }

    protected String cAdminsCmd() {
        return getConfiguration().getString("settings.admins-cmd", "/admins!");
    }

    protected ArrayList<String> cConsoleCommands() {
        return new ArrayList<String>(getConfiguration().getStringList("settings.console-commands", null));
    }

    protected ArrayList<String> cIgnoredPrefixes(String source) {
        return new ArrayList<String>(getConfiguration().getStringList("settings.ignored-prefixes." + source, null));
    }

    protected int cHold(String eventType) {
        return getConfiguration().getInt("settings.hold-after-enable." + eventType, 0);
    }

    protected String cFormatting(String eventType, int bot, String channel) {
        eventType = (eventType.equals("game-to-irc.all-chat") ? "formatting.chat" : eventType);
        ConfigurationNode source = getChanNode(bot, channel);
        String result;
        if (source == null || source.getString("formatting." + eventType) == null)
            source = bots.get(bot);
        if (source == null || source.getString("formatting." + eventType) == null)
            result = getConfiguration().getString("settings.formatting." + eventType, null);
        else
            result = source.getString("formatting." + eventType, null);
        return result;
    }

    protected boolean cEvents(String eventType, int bot, String channel) {
        ConfigurationNode source = null;
        boolean def = eventType.equalsIgnoreCase("game-to-irc.all-chat")
                || eventType.equalsIgnoreCase("irc-to-game.all-chat");
        if (channel != null)
            source = getChanNode(bot, channel);
        if ((source == null || source.getProperty("events." + eventType) == null) && bot > -1)
            source = bots.get(bot);
        if (source == null || source.getProperty("events." + eventType) == null)
            return getConfiguration().getBoolean("settings.events." + eventType, def);
        else
            return source.getBoolean("events." + eventType, false);
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

    protected ArrayList<String> cBotChannels(int bot) {
        return channames.get(bot);
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

    protected int cBotTimeout(int bot) {
        return bots.get(bot).getInt("timeout", 5000);
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

    protected boolean cChanAdmin(int bot, String channel) {
        return getChanNode(bot, channel).getBoolean("admin", false);
    }

    protected ArrayList<String> cChanOnJoin(int bot, String channel) {
        return new ArrayList<String>(getChanNode(bot, channel).getStringList("on-join", null));
    }

    protected boolean cChanChatColors(int bot, String channel) {
        return getChanNode(bot, channel).getBoolean("chat-colors", true);
    }

    protected boolean cChanNameColors(int bot, String channel) {
        return getChanNode(bot, channel).getBoolean("name-colors", true);
    }

    // Check to see if channel tag exists on a bot
    protected boolean cChanCheckTag(String tag, int bot, String channel) {
        if (tag == null || tag.equals(""))
            return false;
        if (getConfiguration().getString("settings.tag", "all").equalsIgnoreCase(tag))
            return true;
        if (bots.get(bot).getString("tag", "").equalsIgnoreCase(tag))
            return true;
        if (getChanNode(bot, channel).getString("tag", "").equalsIgnoreCase(tag))
            return true;
        return false;
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
        return perms != null;
    }
    
    protected boolean checkPerms(Player pl, String path) {
        if (perms == null) return true;
        if (pl != null) return perms.has(pl, path);
        return false;
    }
    
    protected boolean checkPerms(String pl, String path) {
        if (perms == null) return true;
        Player pit = getServer().getPlayer(pl);
        if (pit != null) return perms.has(pit, path);
        return false;
    }
    
    protected String colorizeName(String name) {
        Pattern color_codes = Pattern.compile("§[0-9a-f]");
        Matcher find_colors = color_codes.matcher(name);
        while (find_colors.find()) {
            name = find_colors.replaceFirst(Character.toString((char) 3) + String.format("%02d", cColorIrcFromGame(find_colors.group())));
            find_colors = color_codes.matcher(name);
        }
        return name;
    }
        
    protected String getPermPrefix(String pl) {
        if (perms == null) return "";
        String group = perms.getGroup(pl);
        if (group == null) return "";
        String result = perms.getGroupPrefix(group);
        if (result == null) return "";
        return colorizeName(result.replaceAll("&([0-9a-f])", "§$1"));
    }
    
    protected String getPermSuffix(String pl) {
        if (perms == null) return "";
        String group = perms.getGroup(pl);
        if (group == null) return "";
        String result = perms.getGroupSuffix(group);
        if (result == null) return "";
        return colorizeName(result.replaceAll("&([0-9a-f])", "§$1"));
    }

    public static boolean queueConsoleCommand(Server server, String cmd) {
    
        try {
            Field f = CraftServer.class.getDeclaredField("console");
            f.setAccessible(true);
            MinecraftServer ms = (MinecraftServer) f.get(server);
            
            if ((!ms.g) && (MinecraftServer.a(ms))) {
                ms.a(cmd, ms);
                return true;
            }
    
        } catch (Exception e) {
        }
    
        return false;
    }
}
