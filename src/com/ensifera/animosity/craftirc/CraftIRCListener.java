package com.ensifera.animosity.craftirc;

import java.lang.Exception;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerListener;

public class CraftIRCListener extends PlayerListener {

    private CraftIRC plugin = null;

    public CraftIRCListener(CraftIRC plugin) {
        this.plugin = plugin;
    }
    
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        try {
            String[] split = event.getMessage().split(" ");
            // ACTION/EMOTE can't be claimed, so use onPlayerCommandPreprocess
            if (split[0].equalsIgnoreCase("/me")) {
                RelayedMessage msg = plugin.newMsg(plugin.getEndPoint(plugin.cMinecraftTag()), null, "action");
                if (msg == null) return;
                msg.setField("sender", event.getPlayer().getDisplayName());
                msg.setField("message", Util.combineSplit(1, split, " "));
                msg.setField("world", event.getPlayer().getWorld().getName());
                msg.setField("realSender", event.getPlayer().getName());
                msg.setField("prefix", plugin.getPrefix(event.getPlayer()));
                msg.setField("suffix", plugin.getSuffix(event.getPlayer()));
                msg.doNotColor("message");
                msg.post();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
    
    public void onPlayerChat(PlayerChatEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.CHAT)) return;
        try {
            if (plugin.cCancelChat()) event.setCancelled(true);
            RelayedMessage msg;
            if (event.isCancelled())
                msg = plugin.newMsg(plugin.getEndPoint(plugin.cCancelledTag()), null, "chat");
            else 
                msg = plugin.newMsg(plugin.getEndPoint(plugin.cMinecraftTag()), null, "chat");
            if (msg == null) return;
            msg.setField("sender", event.getPlayer().getDisplayName());
            msg.setField("message", event.getMessage());
            msg.setField("world", event.getPlayer().getWorld().getName());
            msg.setField("realSender", event.getPlayer().getName());
            msg.setField("prefix", plugin.getPrefix(event.getPlayer()));
            msg.setField("suffix", plugin.getSuffix(event.getPlayer()));
            msg.doNotColor("message");
            msg.post();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.JOINS)) return;
        try {
            RelayedMessage msg = plugin.newMsg(plugin.getEndPoint(plugin.cMinecraftTag()), null, "join");
            if (msg == null) return;
            msg.setField("sender", event.getPlayer().getDisplayName());
            msg.setField("world", event.getPlayer().getWorld().getName());
            msg.setField("realSender", event.getPlayer().getName());
            msg.setField("prefix", plugin.getPrefix(event.getPlayer()));
            msg.setField("suffix", plugin.getSuffix(event.getPlayer()));
            msg.post();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.QUITS)) return;
        try {
            RelayedMessage msg = plugin.newMsg(plugin.getEndPoint(plugin.cMinecraftTag()), null, "quit");
            if (msg == null) return;
            msg.setField("sender", event.getPlayer().getDisplayName());
            msg.setField("world", event.getPlayer().getWorld().getName());
            msg.setField("realSender", event.getPlayer().getName());
            msg.setField("prefix", plugin.getPrefix(event.getPlayer()));
            msg.setField("suffix", plugin.getSuffix(event.getPlayer()));
            msg.post();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPlayerKick(PlayerKickEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.KICKS)) return;
        RelayedMessage msg = plugin.newMsg(plugin.getEndPoint(plugin.cMinecraftTag()), null, "kick");
        if (msg == null) return;
        msg.setField("sender", event.getPlayer().getDisplayName());
        msg.setField("message", (event.getReason().length() == 0) ? "no reason given" : event.getReason());
        msg.setField("realSender", event.getPlayer().getName());
        msg.setField("prefix", plugin.getPrefix(event.getPlayer()));
        msg.setField("suffix", plugin.getSuffix(event.getPlayer()));
        msg.doNotColor("message");
        msg.post();
    }

}
