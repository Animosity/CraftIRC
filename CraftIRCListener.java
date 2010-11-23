import java.lang.Exception;
import java.util.Arrays;
import java.util.logging.Logger;

public class CraftIRCListener extends PluginListener {
	protected static final Logger log = Logger.getLogger("Minecraft");
	private static Minebot bot;

	public CraftIRCListener() {
		bot = Minebot.getInstance();
	}

	public boolean onCommand(Player player, String split[]) {

		if (split[0].equalsIgnoreCase("/irc") && (!bot.optn_send_all_MC_chat.contains("main"))) {
			if (player.canUseCommand("/irc")) {

				if (split.length < 2) // TODO determine the proper length to use here, sometime
				{
					player.sendMessage("\247cCorrect usage is: /irc [message]");
					return true;
				}

				// player used command correctly
				String player_name = "(" + player.getName() + ") ";
				String msgtosend = MessageBuilder(split, " ");

				String ircmessage = player_name + msgtosend;
				String echoedMessage = new StringBuilder().append("<").append(bot.irc_relayed_user_color)
						.append(player.getName()).append(Colors.White).append(" to IRC> ").append(msgtosend).toString();

				bot.msg(bot.irc_channel, ircmessage);

				// echo -> IRC msg locally in game
				for (Player p : etc.getServer().getPlayerList()) {
					if (p != null) {
						p.sendMessage(echoedMessage);
					}
				}
				return true;
			}
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

		if (split[0].equalsIgnoreCase("/me") && bot.optn_send_all_MC_chat.size() > 0) {
			String msgtosend = "* " + player.getName() + " " + bot.combineSplit(1, split, " ");
			if (bot.optn_send_all_MC_chat.contains("main")) {
				bot.sendMessage(bot.irc_channel, msgtosend);
			}

			if (bot.optn_send_all_MC_chat.contains("admin")) {
				bot.sendMessage(bot.irc_admin_channel, msgtosend);
			}
		}

		return false;

	}

	public boolean onConsoleCommand(String[] split) {
		if (split[0].equalsIgnoreCase("craftirc") && (split.length >= 2)) {

			if (split[1].equalsIgnoreCase("verbose")) {
				if (split[2].equalsIgnoreCase("on")) {
					bot.setVerbose(true);
				}
				if (split[2].equalsIgnoreCase("off")) {
					bot.setVerbose(false);
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

		if (bot.optn_send_all_MC_chat.contains("main") || bot.optn_send_all_MC_chat.contains("true")) {
			String playername = "(" + player.getName() + ") ";
			String ircmessage = playername + message;
			bot.msg(bot.irc_channel, ircmessage);
		}

		if (bot.optn_send_all_MC_chat.contains("admin")) {
			String playername = "(" + player.getName() + ") ";
			String ircmessage = playername + message;
			bot.msg(bot.irc_admin_channel, ircmessage);

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
