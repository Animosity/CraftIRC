/**
 * 
 */
package com.ensifera.craftirc.example;



import org.bukkit.event.Event.Type;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.CustomEventListener;

import com.animosity.craftirc.CraftIRC;
import com.animosity.craftirc.IRCEvent;

/**
 * @author Animosity
 *
 */
public class CraftIRCPluginExampleListener extends CustomEventListener implements Listener {
    private CraftIRC plugin = null;
    
    public CraftIRCPluginExampleListener(CraftIRC plugin) {
        this.plugin = (CraftIRC) plugin;
    }
    
    public void onCustomEvent(Event event) {
        if (!(event instanceof IRCEvent)) return;
        else {
            IRCEvent ircEvent = (IRCEvent) event;
            System.out.println("Event listener received IRCEvent event");
            System.out.println(ircEvent.msgData.srcChannelTag);
            switch (ircEvent.eventMode) {
                
                case COMMAND:
                    System.out.println("Event listener received IRCEvent.COMMAND");
                    if (ircEvent.msgData.message.startsWith("example ")) {
                        
                      this.plugin.sendMessageToTag("This is an example custom CraftIRC command.", ircEvent.msgData.srcChannelTag);
                      ircEvent.setHandled(true);
                    }
                
            }
            
            

        }
    }

}
