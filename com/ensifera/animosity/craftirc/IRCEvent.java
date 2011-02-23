package com.ensifera.animosity.craftirc;

import org.bukkit.event.Event;

/**
 * @author Animosity
 *
 */
public class IRCEvent extends Event {
	Mode eventMode;
	String server, sender, channel, recipient, message;
	boolean handled;
	
	/**
	 * @param eventMode - 
	 * @param server -
	 * @param sender -
	 * @param channel -
	 * @param recipient -
	 * @param message -
	 */
	protected IRCEvent(Mode eventMode, String server, String sender, String channel, String message) {
		super("IRCEvent"); // sets Bukkit event type as CUSTOM_EVENT and name to "IRCEvent"
		this.eventMode = eventMode;
		this.server = server;
		this.sender = sender;
		this.channel = channel;
		this.message = message;
	}
	
	/**
	 * Event parameter patterns for each mode:
	 * 		JOIN - message is empty
	 * 		PART - message is empty
	 * 		KICK - message is empty
	 * 		BAN - message is empty
	 * 		MSG - no empty parameters
	 * 		PRIVMSG - channel is empty
	 * 		ACTION - no empty parameters
	 * 		COMMAND - message is command string, without the command prefix
	 * 
	 * 		HANDLED - In case other plugins don't check if IRCEvent has been handled - this mode won't commonly be checked in their listener.
	 */
	public enum Mode {
		JOIN, PART, KICK, BAN, MSG, PRIVMSG, ACTION, COMMAND, HANDLED
	}

	String getServer() {
		return this.server;
	}
	
	String getSender() {
		return this.sender;
	}
	
	String getChannel() {
		return this.channel;
	}
	
	String getMessage() {
		return this.message;
	}
	
	void setHandled(boolean handled) {
		this.handled = handled;
		// Also insert handler plugin's name as sender?
	}

	
}
