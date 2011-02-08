package com.animosity.craftirc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum EndPoint {
	UNKNOWN, GAME, IRC
}

class RelayedMessage {
	
	public CraftIRC plugin;
	public String formatting;		//Formatting string ID; Mandatory before toString
	public String sender;			//Sender of the message/Main subject
	public String message;			//Message or reason
	public String moderator;		//Person who kicked or banned, if applicable
	public EndPoint source;			//Origin endpoint of the message
	public EndPoint target;			//Target endpoint of the message
	public String srcChannel;		//Source channel; Mandatory before toString if the origin is IRC
	public int srcBot;				//Source bot ID; Mandatory before toString if the origin is IRC
	public String trgChannel;		//Target channel; Mandatory before toString if the target is IRC
	public int trgBot;				//Target bot ID; Mandatory before toString if the origin is IRC
	
	protected RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target) {
		this.plugin = plugin;
		formatting = null;
		this.source = source;
		this.target = target;
		sender = "";
		message = "";
		moderator = "";
		srcChannel = "";
		trgChannel = "";
		srcBot = -1;
		trgBot = -1;
	}
	
	public String toString() {
		String result = "";
		String msgout = message;
		if (formatting == null) return "NO FORMATTING SPECIFIED.";
		if (source == EndPoint.GAME && target == EndPoint.IRC)
			result = this.plugin.cFormatting(formatting, trgBot, trgChannel);
		if (source == EndPoint.IRC && target == EndPoint.IRC)
			result = this.plugin.cFormatting(formatting, trgBot, trgChannel);
		if (source == EndPoint.IRC && target == EndPoint.GAME) {
			//Colors in chat
			if (this.plugin.cChanChatColors(srcBot, srcChannel)) {
				msgout = msgout.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 22)
						+ "|" + Character.toString((char) 31) + ")", "");
				msgout = msgout.replaceAll(Character.toString((char) 15), this.plugin.cColorGameFromName("foreground"));
				Pattern color_codes = Pattern.compile(Character.toString((char) 3) + "([01]?[0-9])(,[0-9]{0,2})?");
				Matcher find_colors = color_codes.matcher(msgout);
				while (find_colors.find()) {
					msgout = find_colors.replaceFirst(this.plugin.cColorGameFromIrc(Integer.parseInt(find_colors.group(1))));
					find_colors = color_codes.matcher(msgout);
				}
			} else {
				msgout = msgout.replaceAll(
						"(" + Character.toString((char) 2) + "|" + Character.toString((char) 15) + "|"
								+ Character.toString((char) 22) + Character.toString((char) 31) + "|"
								+ Character.toString((char) 3) + "[0-9]{0,2}(,[0-9]{0,2})?)", "");
			}
			msgout = msgout + " ";
			result = this.plugin.cFormatting(formatting, srcBot, srcChannel);
		}
		result = result.replaceAll("%sender%", sender);
		result = result.replaceAll("%message%", msgout);
		result = result.replaceAll("%moderator%", moderator);
		result = result.replaceAll("%srcChannel%", srcChannel);
		result = result.replaceAll("%trgChannel%", trgChannel);
		return result;
	}
	
}
