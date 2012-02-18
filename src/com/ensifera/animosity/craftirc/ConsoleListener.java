package com.ensifera.animosity.craftirc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public class ConsoleListener implements Listener {
	CraftIRC plugin;
	
	public ConsoleListener(CraftIRC craftIRC) {
		plugin = craftIRC;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onServerCommand(ServerCommandEvent event) {
		if(event.getCommand().toLowerCase().startsWith("say")) {
			String message = event.getCommand().substring(4);
            RelayedMessage msg = plugin.newMsg(plugin.getEndPoint(plugin.cConsoleTag()), null, "console");
            msg.setField("message", message);
            msg.doNotColor("message");
            msg.post();
		}
	}
}
