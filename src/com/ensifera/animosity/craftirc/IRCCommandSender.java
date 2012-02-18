package com.ensifera.animosity.craftirc;

import java.util.Set;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class IRCCommandSender implements ConsoleCommandSender {
 
    private RelayedCommand cmd;
    private EndPoint console;
    private ConsoleCommandSender sender;
    
    IRCCommandSender(Server server, RelayedCommand cmd, EndPoint console, ConsoleCommandSender sender) {
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

    @Override
    public boolean isOp() {
        return sender.isOp();
    }
}
