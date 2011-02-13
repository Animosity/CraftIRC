package com.animosity.craftirc;

import java.lang.Exception;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CraftIRCListener extends PlayerListener {
	
	private CraftIRC plugin = null;
	
	public CraftIRCListener(CraftIRC plugin) {
		this.plugin = plugin;
	}
	
	public void onPlayerCommand(PlayerChatEvent event) {
	    String[] split = event.getMessage().split(" ");
	    // ACTION/EMOTE can't be claimed, so use onPlayerCommand
        if (split[0].equalsIgnoreCase("/me")) {
            RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
            msg.formatting = "action";
            msg.sender = event.getPlayer().getName();
            msg.message = Util.combineSplit(1, split, " ");
            this.plugin.sendMessage(msg, null, "all-chat");
        }
	}
	

	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String commandName = command.getName().toLowerCase();
		boolean permCheck = sender instanceof Player;
		
		if (commandName.equals("irc")) {
			if (permCheck && !this.plugin.checkPerms((Player)sender, "craftirc.irc")) return true; 
		    return this.cmdMsgToAll(sender, args);
		} else if (commandName.equals("ircm")) {
			if (permCheck && !this.plugin.checkPerms((Player)sender, "craftirc.ircm")) return true;
		    return this.cmdMsgToTag(sender, args);
	    } else if (commandName.equals("ircwho")) {
	    	if (permCheck && !this.plugin.checkPerms((Player)sender, "craftirc.ircwho")) return true;
			return this.cmdGetIrcUserList(sender, args);
		} else if (commandName.equals("admins!")) {
			if (permCheck && !this.plugin.checkPerms((Player)sender, "craftirc.admins!")) return true;
		    return this.cmdNotifyIrcAdmins(sender, args);
		} else if (commandName.equals("ircraw")) {
			if (permCheck && !this.plugin.checkPerms((Player)sender, "craftirc.ircraw")) return true;
			return this.cmdRawIrcCommand(sender, args);
		} else return false;
		
		// Whispering to IRC users
        /* ***** MULTIPLE USERS MAY HAVE SAME NICKNAME IN DIFFERENT NETWORKS - Come back here later and figure out how to tell them apart
        
        if (split[0].equalsIgnoreCase("/ircw")) {

            if (split.length < 3) {
                player.sendMessage("\247cCorrect usage is: /ircw [IRC user] [message]");
                return;
            }

            String player_name = "(" + player.getName() + ") ";
            String ircMessage = player_name + Util.combineSplit(2, split, " ");
            bot.sendMessage(split[1], ircMessage);
            String echoedMessage = "Whispered to IRC";
            player.sendMessage(echoedMessage);
            event.setCancelled(true);
            return;
        } // ** /ircw <user> <msg>
        */
	}

	private boolean cmdMsgToAll(CommandSender sender, String[] args) {
	    try {
    	    if (args.length == 0) return false;
    	    String msgToSend= Util.combineSplit(0, args, " ");
    	    RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
    	    if (sender instanceof Player) msg.sender = ((Player)sender).getName();
            else msg.sender = "SERVER";  
            msg.formatting = "chat";
            msg.message = msgToSend;
            this.plugin.sendMessage(msg, null, null);
            
            String echoedMessage = new StringBuilder().append("<")
                .append(msg.sender).append(ChatColor.WHITE.toString()).append(" to IRC> ").append(msgToSend)
                .toString();
            // echo -> IRC msg locally in game
            for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                if (p != null) {
                    p.sendMessage(echoedMessage);
                }
            }
            return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	private boolean cmdMsgToTag(CommandSender sender, String[] args) {
	    try {
    	    if (args.length < 2) return false;
            String msgToSend = Util.combineSplit(1, args, " ");
            RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
            if (sender instanceof Player) msg.sender = ((Player)sender).getName();
            else msg.sender = "SERVER";  
            msg.formatting = "chat";
            msg.message = msgToSend;
            this.plugin.sendMessage(msg, args[0], null);
    
            String echoedMessage = new StringBuilder().append("<")
                    .append(msg.sender).append(ChatColor.WHITE.toString()).append(" to IRC> ").append(msgToSend)
                    .toString();
    
            for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                if (p != null) {
                    p.sendMessage(echoedMessage);
                }
            }
            return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	private boolean cmdGetIrcUserList(CommandSender sender, String[] args) {
	    try {
	        if (args.length == 0) return false;
    	    sender.sendMessage("IRC users in " + args[0] + " channel(s):");
            ArrayList<String> userlists = this.plugin.ircUserLists(args[1]);
            for (Iterator<String> it = userlists.iterator(); it.hasNext(); )
                sender.sendMessage(it.next());
            return true;
	    } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
	}
	
	private boolean cmdNotifyIrcAdmins(CommandSender sender, String[] args) {
	    try {
	        if (args.length == 0 || !(sender instanceof Player)) return false;
	        this.plugin.noticeAdmins("[Admin notice from " + ((Player)sender).getName() + "] " + Util.combineSplit(0, args, " "));
            sender.sendMessage("Admin notice sent.");
	        return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	private boolean cmdRawIrcCommand(CommandSender sender, String[] args) {
		if (args.length < 2) return false;
		this.plugin.sendRawToBot(Integer.parseInt(args[0]), Util.combineSplit(1, args, " "));
		return true;
	}

	public void onPlayerChat(PlayerChatEvent event) {
		if (this.plugin.isHeld(CraftIRC.HoldType.CHAT)) return;
		// String[] split = message.split(" ");
		try {
			if (event.isCancelled() && !this.plugin.cEvents("game-to-irc.cancelled-chat", -1, null)) return;
			
			RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
		    msg.formatting = "chat";
		    msg.sender = event.getPlayer().getName();
		    msg.message = event.getMessage();
		    this.plugin.sendMessage(msg, null, "all-chat");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onPlayerJoin(PlayerEvent event) {
		if (this.plugin.isHeld(CraftIRC.HoldType.JOINS)) return;
		try {
			RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
		    msg.formatting = "joins";
		    msg.sender = event.getPlayer().getName();
		    this.plugin.sendMessage(msg, null, "joins");
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onPlayerQuit(PlayerEvent event) {
		if (this.plugin.isHeld(CraftIRC.HoldType.QUITS)) return;
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
	    if (this.plugin.isHeld(CraftIRC.HoldType.KICKS)) return;
        RelayedMessage msg = this.plugin.newMsg(EndPoint.GAME, EndPoint.IRC);
        msg.formatting = "kicks";
        msg.sender = event.getPlayer().getName();
        msg.message = (event.getReason().length() == 0) ? "no reason given" : event.getReason();
        msg.moderator = "Admin"; //there is no moderator context in CBukkit, oh no.
        if (this.plugin.isHeld(CraftIRC.HoldType.KICKS)) return;
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
