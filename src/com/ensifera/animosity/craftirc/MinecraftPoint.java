package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.entity.Player;


public class MinecraftPoint implements CommandEndPoint {

    Server server;
    CraftIRC plugin;
    MinecraftPoint(CraftIRC plugin, Server server) {
        this.server = server;
        this.plugin = plugin;
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
    
    public boolean adminMessageIn(RelayedMessage msg) {
        String message = msg.getMessage(this);
        boolean success = false;
        for (Player p : server.getOnlinePlayers())
            if (p.isOp()) {
                p.sendMessage(message);
                success = true;
            }
        return success;
    }
    
    public List<String> listUsers() {
        LinkedList<String> users = new LinkedList<String>();
        for (Player p : server.getOnlinePlayers()) {
            users.add(p.getName());
        }
        return users;
    }
    
    public List<String> listDisplayUsers() {
        LinkedList<String> users = new LinkedList<String>();
        for (Player p : server.getOnlinePlayers()) {
            users.add(p.getDisplayName());
        }
        Collections.sort(users);
        return users;  
    }

    public void commandIn(RelayedCommand cmd) {
        String command = cmd.getField("command").toLowerCase();
        if (command.equals("say") || command.equals("mc")) {
            RelayedMessage fwd = plugin.newMsg(cmd.getSource(), this, "chat");
            fwd.copyFields(cmd);
            fwd.setField("message", cmd.getField("args"));
            fwd.doNotColor("message");
            this.messageIn(fwd);
        } else if (command.equals("players")) {
            List<String> users = listDisplayUsers();
            int playerCount = users.size();
            String result = "Nobody is minecrafting right now.";
            if (playerCount > 0) {
                List<String> userlist = listDisplayUsers();
                String userstring = (userlist.size() > 0 ? userlist.get(0) : "");
                for (int i = 1; i < userlist.size(); i++) userstring = userstring + " " + userlist.get(i);
                result = "Online (" + playerCount + "/" + server.getMaxPlayers() + "): " + userstring;
            }
            //Reply to remote endpoint! 
            RelayedMessage response = plugin.newMsgToTag(this, cmd.getField("source"), "");
            response.setField("message", result);
            response.post();
        }
    }

}
