package com.ensifera.animosity.craftirc;

import java.lang.Exception;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

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
            RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
            msg.formatting = "action";
            msg.sender = event.getPlayer().getName();
            msg.message = Util.combineSplit(1, split, " ");
            this.plugin.sendMessage(msg, null, "all-chat");
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
    
    public void onPlayerChat(PlayerChatEvent event) {
       
        if (this.plugin.isHeld(CraftIRC.HoldType.CHAT)) {
            return;
            
        }
        // String[] split = message.split(" ");
        try {
            if (this.plugin.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " onPlayerChat(): <%s> %s", event.getMessage(), event.getPlayer()));
            if (event.isCancelled() && !this.plugin.cEvents("game-to-irc.cancelled-chat", -1, null)) {
                if (this.plugin.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " onPlayerChat(CHAT CANCELLED!): <%s> %s", event.getMessage(), event.getPlayer()));
                return;
            }
            RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
            msg.formatting = "chat";
            msg.sender = event.getPlayer().getName();
            msg.message = event.getMessage();
            msg.world = event.getPlayer().getWorld().getName();
            this.plugin.sendMessage(msg, null, "all-chat");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.JOINS))
            return;
        try {
            RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
            msg.formatting = "joins";
            msg.sender = event.getPlayer().getName();
            this.plugin.sendMessage(msg, null, "joins");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.QUITS))
            return;
        try {

            RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
            msg.formatting = "quits";
            msg.sender = event.getPlayer().getName();
            this.plugin.sendMessage(msg, null, "quits");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPlayerKick(PlayerKickEvent event) {
        if (this.plugin.isHeld(CraftIRC.HoldType.KICKS))
            return;
        RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
        msg.formatting = "kicks";
        msg.sender = event.getPlayer().getName();
        msg.message = (event.getReason().length() == 0) ? "no reason given" : event.getReason();
        msg.moderator = "Admin"; //there is no moderator context in CBukkit, oh no.
        if (this.plugin.isHeld(CraftIRC.HoldType.KICKS))
            return;
        this.plugin.sendMessage(msg, null, "kicks");
    }

    /* THESE ARE HMOD-signature EVENTS
     * Keeping on hand for when Craftbukkit gains them
     * 
     * public void onBan(Player mod, Player player, String reason) {
        if (this.plugin.isHeld(CraftIRC.HoldType.BANS)) return;
        if (reason.length() == 0) reason = "no reason given";
        
        RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
        msg.formatting = "game-to-irc.bans";
        msg.sender = player.getName();
        msg.message = reason;
        msg.moderator = mod.getName();
        this.plugin.sendMessage(msg, null, "game-to-irc.bans");
    }

    public void onIpBan(Player mod, Player player, String reason) {
        if (this.plugin.isHeld(CraftIRC.HoldType.BANS)) return;
        if (reason.length() == 0) reason = "no reason given";
        
        RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
        msg.formatting = "game-to-irc.bans";
        msg.sender = player.getName();
        msg.message = reason;
        msg.moderator = mod.getName();
        this.plugin.sendMessage(msg, null, "game-to-irc.bans");
    }

    public void onKick(Player mod, Player player, String reason) {
        if (this.plugin.isHeld(CraftIRC.HoldType.KICKS)) return;
        if (reason.length() == 0) reason = "no reason given";

        RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
        msg.formatting = "game-to-irc.kicks";
        msg.sender = player.getName();
        msg.message = reason;
        msg.moderator = mod.getName();
        this.plugin.sendMessage(msg, null, "game-to-irc.kicks");
    }
    */

    // 

}
