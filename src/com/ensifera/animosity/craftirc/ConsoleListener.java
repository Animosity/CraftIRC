package com.ensifera.animosity.craftirc;

import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListener;

public class ConsoleListener extends ServerListener {
	CraftIRC plugin;
	
	public ConsoleListener(CraftIRC craftIRC) {
		plugin = craftIRC;
	}

	public void onServerCommand(ServerCommandEvent event) {
		if(event.getCommand().toLowerCase().startsWith("say")) {
			String message = event.getCommand().substring(4);
            RelayedMessage msg = plugin.newMsg(plugin.getEndPoint(plugin.cConsoleTag()), null, "console");
            msg.setField("message", message);
            msg.post();
		}
	}
}
