package com.ensifera.animosity.craftirc;

import java.util.LinkedList;
import java.util.List;

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
        String message = msg.getMessage(this);
        for (Player p : server.getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }
    
    public boolean userMessageIn(String username, RelayedMessage msg) {
        Player p = server.getPlayer(username);
        if (p == null) return false;
        p.sendMessage(msg.getMessage(this));
        return true;
    }
    
    public List<String> listUsers() {
        LinkedList<String> users = new LinkedList<String>();
        for (Player p : server.getOnlinePlayers()) {
            users.add(p.getDisplayName());
        }
        return users;
    }

}
