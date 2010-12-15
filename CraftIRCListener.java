import java.lang.Exception;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class CraftIRCListener extends PluginListener {
	protected static final Logger log = Logger.getLogger("Minecraft");
	private static ArrayList<String> logMessages = new ArrayList<String>();
	private static Minebot bot;

	// private HashMap<Player,String> IRCWhisperMemory = new HashMap<Player,String>();

	public CraftIRCListener() {
		bot = Minebot.getInstance();
	}

	public boolean onCommand(Player player, String split[]) {

		if (player.canUseCommand("/irc")) {

			if (split[0].equalsIgnoreCase("/irc") && (!bot.optn_send_all_MC_chat.contains("main"))) {

				if (split.length < 2) {
					player.sendMessage("\247cCorrect usage is: /irc [message]");
					return true;
				}

				// player used command correctly
				String player_name = "(" + player.getName() + ") ";
				String msgtosend = bot.combineSplit(1, split, " ");

				String ircMessage = player_name + msgtosend;
				String echoedMessage = new StringBuilder().append("<").append(bot.irc_relayed_user_color)
						.append(player.getName()).append(Colors.White).append(" to IRC> ").append(msgtosend).toString();

				bot.msg(bot.irc_channel, ircMessage);

				// echo -> IRC msg locally in game
				for (Player p : etc.getServer().getPlayerList()) {
					if (p != null) {
						p.sendMessage(echoedMessage);
					}
				}
				return true;
			}

			// Whispering to IRC users
			if (split[0].equalsIgnoreCase("/ircw")) {

				if (split.length < 3) {
					player.sendMessage("\247cCorrect usage is: /ircw [IRC user] [message]");
					return true;
				}

				String player_name = "(" + player.getName() + ") ";
				String ircMessage = player_name + bot.combineSplit(2, split, " ");
				bot.sendMessage(split[1], ircMessage);
				String echoedMessage = "Whispered to IRC";
				player.sendMessage(echoedMessage);
				return true;

			}

			// notify/call admins in the admin IRC channel
			if (bot.optn_notify_admins_cmd != null) {
				if (split[0].equalsIgnoreCase(bot.optn_notify_admins_cmd)) {
					bot.sendNotice(bot.irc_admin_channel,
							"[Admin notice from " + player.getName() + "] " + bot.combineSplit(1, split, " "));
					player.sendMessage("Admin notice sent.");
					return true;
				}
			}

			// ACTION/EMOTE
			if (split[0].equalsIgnoreCase("/me") && bot.optn_send_all_MC_chat.size() > 0) {
				String msgtosend = "* " + player.getName() + " " + bot.combineSplit(1, split, " ");
				if (bot.optn_send_all_MC_chat.contains("main")) {
					bot.sendMessage(bot.irc_channel, msgtosend);
				}

				if (bot.optn_send_all_MC_chat.contains("admin")) {
					bot.sendMessage(bot.irc_admin_channel, msgtosend);
				}
			}
		} // endif player.canUseCommand("/irc")

		return false;

	}

	public boolean onConsoleCommand(String[] split) {
		if (split[0].equalsIgnoreCase("craftirc") && (split.length >= 2)) {

			if (split[1].equalsIgnoreCase("debug")) {
				if (split[2].equalsIgnoreCase("on")) {
					CraftIRC.setDebug(true);
				}
				if (split[2].equalsIgnoreCase("off")) {
					CraftIRC.setDebug(false);
				}
				return true;
			}

			if (split[1].equalsIgnoreCase("say")) {
				//
				ArrayList<String> botChannels = bot.getChannelList();
				if (botChannels.contains(bot.irc_channel)) {
					bot.sendMessage(bot.irc_channel, bot.combineSplit(2, split, " "));
				}
				if (botChannels.contains(bot.irc_admin_channel)) {
					bot.sendMessage(bot.irc_admin_channel, bot.combineSplit(2, split, " "));
				}
				return true;
			}

		}
		return false;
	}

	public boolean onChat(Player player, String message) {
		// String[] split = message.split(" ");
		if (bot.optn_send_all_MC_chat.size() > 0) {
			sendToIRC(player, message);
		}
		return false;
	}
 
	public void sendToIRC(Player player, String message) {

		// TODO - functionize this.
		try {
			String playername = player.getName();
			String playerColorPrefix = player.getColor();
			Integer playerColor = bot.getIRCColor(player.getColor());
			String playerPrefix = "";
			
			if (playerColorPrefix.length() > 2) {
				playerColor = bot.getIRCColor(playerColorPrefix.substring(0, 2));
				playerPrefix = playerColorPrefix.substring(2,playerColorPrefix.length());
			}
			
			if (bot.irc_colors.equalsIgnoreCase("equiv")) {
				playername = Character.toString((char) 3)
						+ playerColor
						+ playerPrefix
						+ playername
						+ Character.toString((char) 15); 
			}
	
			if (bot.optn_send_all_MC_chat.contains("main") || bot.optn_send_all_MC_chat.contains("true")) {
				playername = "(" + playername + ") ";
				String ircmessage = playername + message;
				bot.msg(bot.irc_channel, ircmessage);
			}
	
			if (bot.optn_send_all_MC_chat.contains("admin")) {
				playername = "(" + playername + ") ";
				String ircmessage = playername + message;
				bot.msg(bot.irc_admin_channel, ircmessage);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onLogin(Player player) {
		try {
			if (bot.optn_main_send_events.contains("joins")) {
				bot.msg(bot.irc_channel, "[" + player.getName() + " connected]");
			}
			if (bot.optn_admin_send_events.contains("joins")) {
				bot.msg(bot.irc_admin_channel, "[" + player.getName() + " connected]");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onDisconnect(Player player) {

		try {
			if (bot.optn_main_send_events.contains("quits")) {
				bot.msg(bot.irc_channel, "[" + player.getName() + " disconnected]");
			}
			if (bot.optn_admin_send_events.contains("quits")) {
				bot.msg(bot.irc_admin_channel, "[" + player.getName() + " disconnected]");
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
			bot.msg(bot.irc_channel, "[" + mod.getName() + " BANNED " + player.getName() + " because: " + reason + "]");
		}
		if (bot.optn_admin_send_events.contains("bans")) {
			bot.msg(bot.irc_admin_channel, "[" + mod.getName() + " BANNED " + player.getName() + " because: " + reason
					+ "]");
		}
	}

	public void onIpBan(Player mod, Player player, String reason) {
		if (reason.length() == 0) {
			reason = "no reason given";
		}
		if (bot.optn_main_send_events.contains("bans")) {
			bot.msg(bot.irc_channel, "[" + mod.getName() + " IP BANNED " + player.getName() + " because: " + reason
					+ "]");
		}
		if (bot.optn_admin_send_events.contains("bans")) {
			bot.msg(bot.irc_admin_channel, "[" + mod.getName() + " IP BANNED " + player.getName() + " because: "
					+ reason + "]");
		}
	}

	public void onKick(Player mod, Player player, String reason) {
		if (reason.length() == 0) {
			reason = "no reason given";
		}
		if (bot.optn_main_send_events.contains("kicks")) {
			bot.msg(bot.irc_channel, "[" + mod.getName() + " KICKED " + player.getName() + " because: " + reason + "]");
		}
		if (bot.optn_admin_send_events.contains("kicks")) {
			bot.msg(bot.irc_admin_channel, "[" + mod.getName() + " KICKED " + player.getName() + " because: " + reason
					+ "]");
		}
	}

	// 
	public static String MessageBuilder(String[] a, String separator) {
		StringBuffer result = new StringBuffer();

		for (int i = 1; i < a.length; i++) {
			result.append(separator);
			result.append(a[i]);
		}

		return result.toString();
	}

}
