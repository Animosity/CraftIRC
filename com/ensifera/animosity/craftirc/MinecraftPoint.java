package com.ensifera.animosity.craftirc;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public class MinecraftPoint implements EndPoint {

    Server server;
    MinecraftPoint(Server server) {
        this.server = server;
    }
    
    public Type getType() {
        return EndPoint.Type.MINECRAFT;
    }

    public void messageIn(RelayedMessage msg) {
        String message = msg.getMessage();
        for (Player p : server.getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }
    
    public boolean userMessageIn(String username, RelayedMessage msg) {
        Player p = server.getPlayer(username);
        if (p == null) return false;
        p.sendMessage(msg.getMessage());
        return true;
    }

}
