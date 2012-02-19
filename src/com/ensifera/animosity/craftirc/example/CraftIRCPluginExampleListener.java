/**
 * 
 */
package com.ensifera.animosity.craftirc.example;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.ensifera.animosity.craftirc.IRCEvent;

/**
 * @author Animosity
 * 
 */
public class CraftIRCPluginExampleListener implements Listener {
    private CraftIRC plugin = null;

    public CraftIRCPluginExampleListener(CraftIRC plugin) {
        this.plugin = (CraftIRC) plugin;
    }

    @EventHandler
    public void onCustomEvent(IRCEvent event) {
        IRCEvent ircEvent = (IRCEvent) event;
        if (!ircEvent.isHandled()) {
            switch (ircEvent.eventMode) {
            case COMMAND:
                if (ircEvent.msgData.message.startsWith("example")) {
                    this.plugin.sendMessageToTag("This is an example custom CraftIRC command. The pen is %red%rrrrrrr%blue%oyal blue!",
                            ircEvent.msgData.srcChannelTag);
                    ircEvent.setHandled(true);
                }
            case AUTHED_COMMAND:
                if (ircEvent.msgData.message.startsWith("authexample")) {
                    this.plugin.sendMessageToTag("This is an example custom %u%authenticated%u% CraftIRC command.",
                            ircEvent.msgData.srcChannelTag);
                    ircEvent.setHandled(true);
                }
            }
        }
    }

}
