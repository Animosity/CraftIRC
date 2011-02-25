/**
 * 
 */
package com.ensifera.animosity.craftirc;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
/**
 * @author Animosity
 *
 */
public class IRCConsoleCommandSender extends ConsoleCommandSender {
    private Boolean op = false;
    private Minebot srcBot = null;
    private RelayedMessage ircConCmd = null;
    
 
    /**
     * 
     * @param server  - Server
     * @param ircConCmdMsg - RelayedMessage
     * @param isOp - Boolean
     */
    public IRCConsoleCommandSender(Server server, RelayedMessage ircConCmd, Boolean isOp) {
        super(server);
        this.op = isOp;
        this.srcBot = srcBot;
    }
    
    public boolean isOp() { return this.op; }
    
    @Override
    public void sendMessage(String message) {
        try {
            srcBot.getPlugin().sendMessageToTag("[CONSOLE] " + message, ircConCmd.srcChannelTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
