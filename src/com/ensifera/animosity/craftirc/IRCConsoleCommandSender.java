/**
 * 
 */
package com.ensifera.animosity.craftirc;

import java.util.Set;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
/**
 * @author Animosity
 *
 */
public class IRCConsoleCommandSender implements ConsoleCommandSender {
    private Boolean op = false;
    private RelayedMessage ircConCmd = null;
    private ConsoleCommandSender sender;
    
 
    /**
     * 
     * @param server  - Server
     * @param ircConCmdMsg - RelayedMessage
     * @param isOp - Boolean
     */
    public IRCConsoleCommandSender(Server server, RelayedMessage ircConCmd, Boolean isOp, ConsoleCommandSender sender) {
        this.sender=sender;
        this.ircConCmd = ircConCmd;
        this.op = isOp;
    }
    
    public boolean isOp() { return this.op; }
    
    public boolean isPlayer() { return false; }
    
    public void sendMessage(String message) {
        try {
            ircConCmd.getPlugin().sendMessageToTag(">> " + message, ircConCmd.srcChannelTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return sender.getName();
    }

    public Server getServer() {
        return sender.getServer();
    }

    public PermissionAttachment addAttachment(Plugin arg0) {
        return sender.addAttachment(arg0);
    }

    public PermissionAttachment addAttachment(Plugin arg0, int arg1) {
        return sender.addAttachment(arg0, arg1);
    }

    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2) {
        return sender.addAttachment(arg0, arg1, arg2);
    }

    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2, int arg3) {
        return sender.addAttachment(arg0, arg1, arg2, arg3);
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return sender.getEffectivePermissions();
    }

    public boolean hasPermission(String arg0) {
        return sender.hasPermission(arg0);
    }

    public boolean hasPermission(Permission arg0) {
        return sender.hasPermission(arg0);
    }

    public boolean isPermissionSet(String arg0) {
        return sender.isPermissionSet(arg0);
    }

    public boolean isPermissionSet(Permission arg0) {
        return sender.isPermissionSet(arg0);
    }

    public void recalculatePermissions() {
        sender.recalculatePermissions();
    }

    public void removeAttachment(PermissionAttachment arg0) {
        sender.removeAttachment(arg0);
    }

    public void setOp(boolean arg0) {
        sender.setOp(arg0);
    }
}
