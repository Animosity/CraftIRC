package org.bukkit.animosity.craftirc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.bukkit.entity.Player;
import org.jibble.pircbot.User;

/**
 * @author Animosity
 *
 */
public class Util {

	// Combine string array with delimiter
	public static String combineSplit(int initialPos, String[] parts, String delimiter) throws ArrayIndexOutOfBoundsException {
		if (initialPos >= parts.length) return "";
		String result = parts[initialPos];
		for (int i = initialPos + 1; i < parts.length; i++)
			result = result + delimiter + parts[i];
		return result;
	}
	
	public static String MessageBuilder(String[] a, String separator) {
		StringBuffer result = new StringBuffer();

		for (int i = 1; i < a.length; i++) {
			result.append(separator);
			result.append(a[i]);
		}

		return result.toString();
	}
	
	
	// TODO: Sort this list properly
	public static String getIrcUserList(Minebot bot, String channel) {
		StringBuilder sbIrcUserList = new StringBuilder();
		try {
			if (channel.equalsIgnoreCase("main") && bot.getChannelList().contains(bot.irc_channel)) {
				ArrayList<User> users = new ArrayList<User>(Arrays.asList(bot.getUsers(bot.irc_channel)));
				//bot.irc_users_main = bot.getUsers(bot.irc_channel);
				for (int i = 0; i < users.size(); i++) {
					sbIrcUserList.append(bot.getHighestUserPrefix(users.get(i)) + users.get(i).getNick()).append(" ");
				}
				return sbIrcUserList.toString();
			}
			else if (channel.equalsIgnoreCase("admin") && bot.getChannelList().contains(bot.irc_admin_channel)) {
				ArrayList<User> users = new ArrayList<User>(Arrays.asList(bot.getUsers(bot.irc_admin_channel)));
				//bot.irc_users_admin = bot.getUsers(bot.irc_admin_channel);
				for (int i = 0; i < users.size(); i++) {
					sbIrcUserList.append(bot.getHighestUserPrefix(users.get(i)) + users.get(i).getNick()).append(" ");
				}
				return sbIrcUserList.toString();
			}
			else { return ""; }
			
		} catch (Exception e) {
			bot.log.warning(CraftIRC.NAME + ": error while retrieving IRC user list!");
			e.printStackTrace();
			return "";
		}
	}

	
	public static String colorizePlayer(Player player) {
		return player.getName();
	}
	
}
