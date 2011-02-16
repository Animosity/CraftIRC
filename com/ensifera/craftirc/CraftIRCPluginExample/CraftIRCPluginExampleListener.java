/**
 * 
 */
package com.ensifera.craftirc.CraftIRCPluginExample;


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
public class CraftIRCPluginExampleListener implements Listener {
    private CraftIRC plugin = null;
    
    public CraftIRCPluginExampleListener(CraftIRC plugin) {
        this.plugin = (CraftIRC) plugin;
    }
    
    public void onCustomEvent(Event event) {
        if (!(event instanceof IRCEvent)) return;
        else {
            IRCEvent ircEvent = (IRCEvent) event;
            
            switch (ircEvent.eventMode) {
                case COMMAND:
                    if (ircEvent.msgData.message.startsWith("example ")) {
                      this.plugin.plgnSendMessageToTag("This is an example custom CraftIRC command.", ircEvent.msgData.srcTag);
                      ircEvent.setHandled(true);
                    }
                
            }
            
            

        }
    }

}
