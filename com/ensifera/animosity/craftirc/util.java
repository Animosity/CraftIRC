package com.ensifera.animosity.craftirc;

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
public class util {
	public class Colors {

	    public static final String Black = "§0";
	    public static final String Navy = "§1";
	    public static final String Green = "§2";
	    public static final String Blue = "§3";
	    public static final String Red = "§4";
	    public static final String Purple = "§5";
	    public static final String Gold = "§6";
	    public static final String LightGray = "§7";
	    public static final String Gray = "§8";
	    public static final String DarkPurple = "§9";
	    public static final String LightGreen = "§a";
	    public static final String LightBlue = "§b";
	    public static final String Rose = "§c";
	    public static final String LightPurple = "§d";
	    public static final String Yellow = "§e";
	    public static final String White = "§f";
	}
	

	// Combine string array with delimiter
	public static String combineSplit(int initialPos, String[] parts, String delimiter) throws ArrayIndexOutOfBoundsException {
		String result = "";
		for (int i = initialPos; i < parts.length; i++) {
			result = result + parts[i];
			if (i != parts.length - 1) {
				result = result + delimiter;
			}
		}
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
	
	/*public String colorizePlayer(Player player) {
	String playerColorPrefix = "";
	if (this.irc_colors.equalsIgnoreCase("equiv")) {
		String[] splitPlayerColorPrefix = player.getColor().split("�");
		for (int i = 1; i < splitPlayerColorPrefix.length; i++) {
			playerColorPrefix += Character.toString((char) 3) + this.getIRCColor("�" + splitPlayerColorPrefix[i].substring(0,1));

			if (splitPlayerColorPrefix[i].length() > 1) { 
				playerColorPrefix += splitPlayerColorPrefix[i].substring(1,splitPlayerColorPrefix[i].length());
			}
		}
		return playerColorPrefix + player.getName() + Character.toString((char) 15);
	}
	else {
		return player.getName();
	}
}*/
}
