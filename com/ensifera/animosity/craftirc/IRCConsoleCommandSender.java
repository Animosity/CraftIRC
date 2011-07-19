/**
 * 
 */
package com.ensifera.animosity.craftirc;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
/**
 * @author Animosity
 *
 */
public class IRCConsoleCommandSender extends ConsoleCommandSender {
    private Boolean op = false;
    private RelayedMessage ircConCmd = null;
    
 
    /**
     * 
     * @param server  - Server
     * @param ircConCmdMsg - RelayedMessage
     * @param isOp - Boolean
     */
    public IRCConsoleCommandSender(Server server, RelayedMessage ircConCmd, Boolean isOp) {
        super(server);
        this.ircConCmd = ircConCmd;
        this.op = isOp;
    }
    
    public boolean isOp() { return this.op; }
    
    @Override
    public boolean isPlayer() { return false; }
    
    @Override
    public void sendMessage(String message) {
        try {
            ircConCmd.getPlugin().sendMessageToTag(">> " + message, ircConCmd.srcChannelTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
