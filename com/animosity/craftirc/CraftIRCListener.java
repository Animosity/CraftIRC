package com.animosity.craftirc;

import java.lang.Exception;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
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
	    Player player = event.getPlayer();
	 // ACTION/EMOTE can't be claimed, so use onPlayerCommand
        if (split[0].equalsIgnoreCase("/me")) {
            String msgtosend = "* " + player.getName() + " " + Util.combineSplit(1, split, " ");
            this.plugin.sendMessage(msgtosend, null, "game-to-irc.all-chat");
        }
	}
	
	public void onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		//String[] split = event.getMessage().split(" ");
		Player player = (Player) sender;
		String commandName = command.getName().toLowerCase();

		if (commandName.equalsIgnoreCase("/irc")) {

			/*if (args.length < 2) {
				player.sendMessage("\247cCorrect usage is: /irc [message]");
				return;
			}*/

			// player used command correctly
			String player_name = "(" + player.getName() + ") ";
			String msgtosend = Util.combineSplit(0, args, " ");

			String ircMessage = player_name + msgtosend;
			String echoedMessage = new StringBuilder().append("<")
					.append(player.getName()).append(ChatColor.WHITE.toString()).append(" to IRC> ").append(msgtosend)
					.toString();

			this.plugin.sendMessage(ircMessage, null, null);
			// echo -> IRC msg locally in game
			for (Player p : this.plugin.getServer().getOnlinePlayers()) {
				if (p != null) {
					p.sendMessage(echoedMessage);
				}
			}
			return;
		} // *** /irc <msg> 
		
		if (commandName.equalsIgnoreCase("/ircm")) {

			if (args.length < 2) {
				player.sendMessage("\247cCorrect usage is: /ircm [tag] [message]");
				return;
			}
			
			String player_name = "(" + player.getName() + ") ";
			String msgtosend = Util.combineSplit(1, args, " ");

			String ircMessage = player_name + msgtosend;
			String echoedMessage = new StringBuilder().append("<")
					.append(player.getName()).append(ChatColor.WHITE.toString()).append(" to IRC> ").append(msgtosend)
					.toString();

			this.plugin.sendMessage(ircMessage, args[0], null);
			// echo -> IRC msg locally in game
			for (Player p : this.plugin.getServer().getOnlinePlayers()) {
				if (p != null) {
					p.sendMessage(echoedMessage);
				}
			}
			return;
			
		}

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
		
		// IRC user list
		if (commandName.equalsIgnoreCase("/ircwho") && args.length == 1) {
			player.sendMessage("IRC users in " + args[0] + " channel(s):");
			ArrayList<String> userlists = this.plugin.ircUserLists(args[1]);
			for (Iterator<String> it = userlists.iterator(); it.hasNext(); )
				player.sendMessage(it.next());
		}
		
		// notify/call admins in the admin IRC channel
		if (commandName.equalsIgnoreCase("/admins!")) {
			this.plugin.noticeAdmins("[Admin notice from " + player.getName() + "] " + Util.combineSplit(0, args, " "));
			player.sendMessage("Admin notice sent.");
			return;
		}

		
	}

	public void onPlayerChat(PlayerChatEvent event) {
		if (this.plugin.isHeld(CraftIRC.HoldType.CHAT)) return;
		// String[] split = message.split(" ");
		try {
			if (event.isCancelled() && !this.plugin.cEvents("game-to-irc.cancelled-chat", -1, null)) return;
			this.plugin.sendMessage("(" + event.getPlayer().getDisplayName() + ") " + event.getMessage(), null, "game-to-irc.all-chat");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onPlayerJoin(PlayerEvent event) {
		if (this.plugin.isHeld(CraftIRC.HoldType.JOINS)) return;
		try {
			Player player = event.getPlayer();
			this.plugin.sendMessage("[" + player.getDisplayName() + " connected]", null, "game-to-irc.joins");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onPlayerQuit(PlayerEvent event) {
		if (this.plugin.isHeld(CraftIRC.HoldType.QUITS)) return;
		try {
			Player player = event.getPlayer();
			this.plugin.sendMessage("[" + player.getDisplayName() + " disconnected]", null, "game-to-irc.quits");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onBan(Player mod, Player player, String reason) {
		if (this.plugin.isHeld(CraftIRC.HoldType.BANS)) return;
		if (reason.length() == 0) reason = "no reason given";
		this.plugin.sendMessage("[" + mod + " BANNED " + player.getDisplayName() + " because: " + reason + "]", null, "game-to-irc.bans");
	}

	public void onIpBan(Player mod, Player player, String reason) {
		if (this.plugin.isHeld(CraftIRC.HoldType.BANS)) return;
		if (reason.length() == 0) reason = "no reason given";
		this.plugin.sendMessage("[" + mod + " IP BANNED " + player.getDisplayName() + " because: " + reason + "]", null, "game-to-irc.bans");
	}

	public void onKick(Player mod, Player player, String reason) {
		if (this.plugin.isHeld(CraftIRC.HoldType.KICKS)) return;
		if (reason.length() == 0) reason = "no reason given";
		this.plugin.sendMessage("[" + mod + " KICKED " + player.getDisplayName() + " because: " + reason + "]", null, "game-to-irc.kicks");
	}

	// 

}
