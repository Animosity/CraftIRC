/**
 * 
 */
package com.ensifera.craftirc.example;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import com.animosity.craftirc.*;

/**
 * @author Animosity
 *
 */
public class CraftIRCPluginExample extends JavaPlugin {
    protected static final Logger log = Logger.getLogger("Minecraft");
    protected CraftIRC craftircHandle;
    protected CraftIRCPluginExampleListener ircListener;
    ArrayList<String> ircTags = new ArrayList<String>();
    
    public CraftIRCPluginExample(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
}
    
    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    @Override
    public void onEnable() {
        Plugin checkplugin = this.getServer().getPluginManager().getPlugin("CraftIRC");
        if (checkplugin == null) {
            log.warning("CraftIRCPluginExample cannot be loaded because CraftIRC is not enabled on the server!");
            getServer().getPluginManager().disablePlugin(((org.bukkit.plugin.Plugin) (this)));
        } else {
            try {
               log.info("CraftIRCPluginExample loading...");
               // Get handle to CraftIRC, add&register your custom listener
               craftircHandle = (CraftIRC)checkplugin;
               ircListener = new CraftIRCPluginExampleListener(craftircHandle);
               this.getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, ircListener, Priority.Monitor, this);
               
               // Server owners who use CraftIRC can assign 'tags' to their bots or IRC channels, they should specify
               // which of these tags to associate with your plugin, in your plugin's configuration.
               ircTags.add("community");
               ircTags.add("admin");
               
               
            } catch (ClassCastException ex) {
                ex.printStackTrace();
                log.warning("CraftIRCHookExample can't cast plugin handle as CraftIRC plugin! Inform Animosity.");
                getServer().getPluginManager().disablePlugin(((org.bukkit.plugin.Plugin) (this)));

            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    @Override
    public void onDisable() {
        
    }


}