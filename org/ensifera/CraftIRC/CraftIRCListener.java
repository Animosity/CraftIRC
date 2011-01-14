package org.ensifera.CraftIRC;

import org.ensifera.CraftIRC.*;
import java.lang.Exception;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.bukkit.Location;
import org.bukkit.Player;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;



public class CraftIRCListener extends PlayerListener {
	private final CraftIRC plugin;
	protected static final Logger log = Logger.getLogger("Minecraft");
	private static ArrayList<String> logMessages = new ArrayList<String>();
	private static Minebot bot;
	
	// private HashMap<Player,String> IRCWhisperMemory = new HashMap<Player,String>();

	public CraftIRCListener(CraftIRC pluginInstance) {
		plugin = pluginInstance;
		bot = Minebot.getInstance(plugin);
	}

	public void onPlayerCommand(PlayerChatEvent event) {
			String[] split = event.getMessage().split(" ");
			Player player = event.getPlayer();
			
			if (split[0].equalsIgnoreCase("/irc") && (!bot.optn_send_all_MC_chat.contains("main"))) {

				if (split.length < 2) {
					player.sendMessage("\247cCorrect usage is: /irc [message]");
					return;
				}

				// player used command correctly
				String player_name = "(" + player.getName() + ") ";
				String msgtosend = util.combineSplit(1, split, " ");

				String ircMessage = player_name + msgtosend;
				String echoedMessage = new StringBuilder().append("<").append(bot.irc_relayed_user_color)
						.append(player.getName()).append(ChatColor.WHITE.toString()).append(" to IRC> ").append(msgtosend).toString();

				bot.msg(bot.irc_channel, ircMessage);
				// echo -> IRC msg locally in game
				for (Player p : plugin.getServer().getOnlinePlayers()) {
					if (p != null) {
						p.sendMessage(echoedMessage);
					}
				}
				event.setCancelled(true);
				return;
			} // *** /irc <msg> 

			// Whispering to IRC users
			if (split[0].equalsIgnoreCase("/ircw")) {

				if (split.length < 3) {
					player.sendMessage("\247cCorrect usage is: /ircw [IRC user] [message]");
					return;
				}

				String player_name = "(" + player.getName() + ") ";
				String ircMessage = player_name + util.combineSplit(2, split, " ");
				bot.sendMessage(split[1], ircMessage);
				String echoedMessage = "Whispered to IRC";
				player.sendMessage(echoedMessage);
				event.setCancelled(true);
				return;
			} // ** /ircw <user> <msg>

			// notify/call admins in the admin IRC channel
			if (bot.optn_notify_admins_cmd != null) {
				if (split[0].equalsIgnoreCase(bot.optn_notify_admins_cmd)) {
					bot.sendNotice(bot.irc_admin_channel,
							"[Admin notice from " + player.getName() + "] " + util.combineSplit(1, split, " "));
					player.sendMessage("Admin notice sent.");
					return;
				}
			}

			// ACTION/EMOTE
			if (split[0].equalsIgnoreCase("/me") && bot.optn_send_all_MC_chat.size() > 0) {
				String msgtosend = "* " + player.getName() + " " + util.combineSplit(1, split, " ");
				if (bot.optn_send_all_MC_chat.contains("main")) {
					bot.sendMessage(bot.irc_channel, msgtosend);
				}

				if (bot.optn_send_all_MC_chat.contains("admin")) {
					bot.sendMessage(bot.irc_admin_channel, msgtosend);
				}
			}
		 // endif player.canUseCommand("/irc")

		return;

	}
	
	public void onPlayerChat(PlayerChatEvent event) {
		// String[] split = message.split(" ");
		try {
			if (bot.optn_send_all_MC_chat.size() > 0) { sendToIRC(event.getPlayer(), event.getMessage()); }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
	public void sendToIRC(Player player, String message) {
		try {
			String playername = "(" 
			+ bot.colorizePlayer(player)
			+ ") " ;
			
			if (bot.optn_send_all_MC_chat.contains("main") || bot.optn_send_all_MC_chat.contains("true")) {
				//playername = "(" + playername + ") ";
				String ircmessage = playername + message;
				bot.msg(bot.irc_channel, ircmessage);
			}
	
			if (bot.optn_send_all_MC_chat.contains("admin")) {
				//playername = "(" + playername + ") ";
				String ircmessage = playername + message;
				bot.msg(bot.irc_admin_channel, ircmessage);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onPlayerJoin(PlayerEvent event) {
		
		try {
			Player player = event.getPlayer();

			if (bot.optn_main_send_events.contains("joins")) {
				bot.msg(bot.irc_channel, "[" + bot.colorizePlayer(player) + " connected]");
			}
			if (bot.optn_admin_send_events.contains("joins")) {
				bot.msg(bot.irc_admin_channel, "[" + bot.colorizePlayer(player) + " connected]");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onPlayerQuit(PlayerEvent event) {
		try {
			Player player = event.getPlayer();
			if (bot.optn_main_send_events.contains("quits")) {
				bot.msg(bot.irc_channel, "[" + bot.colorizePlayer(player) + " disconnected]");
			}
			if (bot.optn_admin_send_events.contains("quits")) {
				bot.msg(bot.irc_admin_channel, "[" + bot.colorizePlayer(player) + " disconnected]");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onBan(Player mod, Player player, String reason) {
		if (reason.length() == 0) {
			reason = "no reason given";
		}
		if (bot.optn_main_send_events.contains("bans")) {
			bot.msg(bot.irc_channel, "[" +bot.colorizePlayer(mod) + " BANNED " + bot.colorizePlayer(player) + " because: " + reason + "]");
		}
		if (bot.optn_admin_send_events.contains("bans")) {
			bot.msg(bot.irc_admin_channel, "[" + bot.colorizePlayer(mod) + " BANNED " + bot.colorizePlayer(player) + " because: " + reason
					+ "]");
		}
	}

	public void onIpBan(Player mod, Player player, String reason) {
		if (reason.length() == 0) {
			reason = "no reason given";
		}
		if (bot.optn_main_send_events.contains("bans")) {
			bot.msg(bot.irc_channel, "[" + bot.colorizePlayer(mod) + " IP BANNED " + bot.colorizePlayer(player) + " because: " + reason
					+ "]");
		}
		if (bot.optn_admin_send_events.contains("bans")) {
			bot.msg(bot.irc_admin_channel, "[" + bot.colorizePlayer(mod) + " IP BANNED " + bot.colorizePlayer(player) + " because: "
					+ reason + "]");
		}
	}

	public void onKick(Player mod, Player player, String reason) {
		if (reason.length() == 0) {
			reason = "no reason given";
		}
		if (bot.optn_main_send_events.contains("kicks")) {
			bot.msg(bot.irc_channel, "[" + bot.colorizePlayer(mod) + " KICKED " + bot.colorizePlayer(player) + " because: " + reason + "]");
		}
		if (bot.optn_admin_send_events.contains("kicks")) {
			bot.msg(bot.irc_admin_channel, "[" + bot.colorizePlayer(mod) + " KICKED " + bot.colorizePlayer(player) + " because: " + reason
					+ "]");
		}
	}

	// 


}
