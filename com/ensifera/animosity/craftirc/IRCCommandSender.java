package com.ensifera.animosity.craftirc;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;

public class IRCCommandSender extends ConsoleCommandSender {
 
    private RelayedCommand cmd;
    private EndPoint console;
    
    IRCCommandSender(Server server, RelayedCommand cmd, EndPoint console) {
        super(server);
        this.cmd = cmd;
        this.console = console;
    }
    
    public String getField(String name) {
        return cmd.getField(name);
    }
        
    public void sendMessage(String message) {
        try {
            RelayedMessage msg = cmd.getPlugin().newMsgToTag(console, cmd.getField("source"), "generic");
            msg.post();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
