/**
 * 
 */
package com.animosity.craftirc;

import org.bukkit.event.Event;


public class IRCEvent extends Event {
    Minebot bot;
	Mode eventMode;
	String server, sender, channel, recipient, message;
	
	
	/**
	 * @author Animosity
	 * @param bot Minebot The instance of the bot the event is sourced from.
	 * @param eventMode IRCEvent.Mode The event type 
	 * @param server String The server address the bot is connected to (this.ircServer)
	 * @param channel String The channel the event is sourced from
	 * @param sender String The sender of the message
	 * @param message String the message
	 
	 */
	protected IRCEvent(Minebot bot, Mode eventMode, String server, String channel, String sender, String message) {
		super("IRCEvent"); // sets Bukkit event type as CUSTOM_EVENT and name to "IRCEvent"
		this.bot = bot;
		this.eventMode = eventMode;
		this.server = server;
	    this.channel = channel;
		this.sender = sender;
		this.message = message;
	}
	
	/**
	 * Event parameter patterns for each mode:
	 * 		JOIN - message is empty
	 * 		PART - message is empty
	 * 		KICK - message is empty
	 * 		BAN - message is empty
	 * 		MSG - no empty parameters
	 * 		PRIVMSG - no empty parameters
	 * 		ACTION - no empty parameters
	 * 		COMMAND - message is command string, without the command prefix
	 *      AUTHED_COMMAND - message is command string, without the command prefix - originated from an authenticated user
	 * 		HANDLED - In case other plugins don't check if IRCEvent has been handled - this mode won't commonly be checked in their listener.
	 */
	public enum Mode {
		JOIN, PART, KICK, BAN, MSG, PRIVMSG, ACTION, COMMAND, AUTHED_COMMAND, HANDLED
	}

	public String getServer() {
		return this.server;
	}
	
	public String getSender() {
		return this.sender;
	}
	
	public String getChannel() {
		return this.channel;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public void setHandled(boolean handled) {
		this.eventMode = Mode.HANDLED;
		// Also insert handler plugin's name as sender?
	}
	public boolean isHandled() {
	    if (this.eventMode == Mode.HANDLED) return true;
	    else return false;
	}

	
}
