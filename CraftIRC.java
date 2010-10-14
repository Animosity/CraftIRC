import java.util.logging.Logger;

/**
 * @author Animosity
 * With various contributions and improvements by ricin
 * 
 */

public class CraftIRC extends Plugin {
	public static final String NAME = "CraftIRC";
	public static final String VERSION = "1.5.1";
	private static Minebot bot;

    static final CraftIRCListener listener = new CraftIRCListener();
	protected static final Logger log = Logger.getLogger("Minecraft");
	
    public void enable() 
    {
    	log.info(NAME + " Enabled.");
        // instantiate bot w/ settings
        bot = Minebot.getInstance();
        bot.init();
        

        etc.getInstance().addCommand("/irc [msg]", "Sends message to " + bot.irc_channel);
        if (bot.optn_notify_admins_cmd != null) 
        { 
            etc.getInstance().addCommand(bot.optn_notify_admins_cmd + " [msg]", "Sends your message to the admins on IRC"); 
        }
    }
    
    public void disable() 
    {
        log.info(NAME + " Disabled.");
        bot = Minebot.getInstance();
        
    	etc.getInstance().removeCommand("/irc [msg]");
    	etc.getInstance().removeCommand(bot.optn_notify_admins_cmd + " [msg]");
        bot.quitServer(NAME + " Unloaded");
    }

    public void initialize() 
    {
    	etc.getLoader().addListener(PluginLoader.Hook.SERVERCOMMAND, listener, this, PluginListener.Priority.HIGH);
    	etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.MEDIUM);
       
    }
}
