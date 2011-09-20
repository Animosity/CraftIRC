package com.ensifera.animosity.craftirc;

import java.util.List;

import org.bukkit.Server;


public class ConsolePoint implements CommandEndPoint {

    Server server;
    CraftIRC plugin;
    ConsolePoint(CraftIRC plugin, Server server) {
        this.server = server;
        this.plugin = plugin;
    }
    
    public Type getType() {
        return Type.PLAIN;
    }

    public void messageIn(RelayedMessage msg) {
        CraftIRC.dolog(msg.getMessage());
    }

    public boolean userMessageIn(String username, RelayedMessage msg) {
        CraftIRC.dolog("(To " + username + ")" + msg.getMessage());
        return true;
    }
    
    public boolean adminMessageIn(RelayedMessage msg) {
        CraftIRC.dolog("(To the admins)" + msg.getMessage());
        return true;
    }

    public List<String> listUsers() {
        return null;
    }
    
    public List<String> listDisplayUsers() {
        return null;
    }

    public void commandIn(RelayedCommand cmd) {
        String command = cmd.getField("command").toLowerCase();
        if (plugin.cPathAttribute(cmd.getField("source"), cmd.getField("target"), "attributes.admin") && cmd.getFlag("admin")) {
            //Admin commands
            if (command.equals("cmd") || command.equals("c")) {
                String args = cmd.getField("args");
                String ccmd = args.substring(0, args.indexOf(" "));
                if (ccmd.equals("")) return;
                if (plugin.cConsoleCommands().contains(ccmd) || plugin.cConsoleCommands().contains("*") ||  plugin.cConsoleCommands().contains("all")) {
                    IRCCommandSender sender = new IRCCommandSender(server, cmd, this);
                    server.dispatchCommand(sender, args);
                }
            }
        }
    }

}
