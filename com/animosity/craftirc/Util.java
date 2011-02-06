package com.animosity.craftirc;

import java.util.ArrayList;
import java.util.Arrays;

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
			if (bot.getChannelList().contains(channel)) {
				ArrayList<User> users = new ArrayList<User>(Arrays.asList(bot.getUsers(channel)));
				for (int i = 0; i < users.size(); i++)
					sbIrcUserList.append(bot.getHighestUserPrefix(users.get(i)) + users.get(i).getNick()).append(" ");
				return sbIrcUserList.toString();
			}
		} catch (Exception e) {
			CraftIRC.log.warning(CraftIRC.NAME + ": error while retrieving IRC user list!");
			e.printStackTrace();
		}
		return "";
	}
	
}
