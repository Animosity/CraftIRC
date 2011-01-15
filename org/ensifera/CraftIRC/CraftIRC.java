package org.ensifera.CraftIRC;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.ensifera.CraftIRC.CraftIRCListener;
import org.ensifera.CraftIRC.Minebot;
/**
 * @author Animosity With various contributions and improvements by ricin
 * 
 */

public class CraftIRC extends JavaPlugin {
	public static final String NAME = "CraftIRC";
	public static final String VERSION = "1.68c BETA";

	private static boolean debug = false;
	private static Minebot bot;
    private final CraftIRCListener listener = new CraftIRCListener(this);
    //private final CraftIRC_ServerListener consoleListener = new CraftIRC_ServerListener(this);
	protected static final Logger log = Logger.getLogger("Minecraft");
	
	public CraftIRC(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);
		initialize();
	}
	
	public void onEnable() {
		log.info(NAME + " Enabled.");
		// instantiate bot w/ settings
		bot = Minebot.getInstance(this);
		bot.init();
		
		/*etc.getInstance().addCommand("/irc [msg]", "Sends message to " + bot.irc_channel);
		if (bot.optn_notify_admins_cmd != null) {
			etc.getInstance().addCommand(bot.optn_notify_admins_cmd + " [msg]",
					"Sends your message to the admins on IRC");
		}*/
	}

	public void onDisable() {
		log.info(NAME + " Disabled.");
		//etc.getInstance().removeCommand("/irc [msg]");
		//etc.getInstance().removeCommand(bot.optn_notify_admins_cmd + " [msg]");
		bot.quitServer(NAME + " Unloaded");
	}

	// Use the first instance of bot to do the auto-reconnection/recovery
	public void recover() {
		bot.init();
	}

	public void initialize() {
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, listener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, listener, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND, listener, Priority.Highest, this);
        getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, listener, Priority.Highest, this);
        //getServer().getPluginManager().registerEvent(Event.Type.CONSOLE_COMMAND, consoleListener, Priority.Normal, this);
        
		/*
			etc.getLoader().addListener(PluginLoader.Hook.SERVERCOMMAND, listener, this, PluginListener.Priority.HIGH);
			etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
			etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.MEDIUM);
			etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.MEDIUM);
			etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.MEDIUM);
			etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.MEDIUM);
			etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.MEDIUM);
			etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.MEDIUM);
		 */
	}

	public static void setDebug(boolean d) {
		log.info(NAME + " DEBUG [" + (d ? "ON" : "OFF") + "]");
		debug = d;
		if (bot != null) {
			bot.setVerbose(d);
		}
	}

	public static boolean isDebug() {
		return debug;
	}

}
