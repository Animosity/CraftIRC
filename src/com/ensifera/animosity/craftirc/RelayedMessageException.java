package com.ensifera.animosity.craftirc;

public class RelayedMessageException extends Exception {

	private static final long serialVersionUID = 1L;
	private RelayedMessage rm;
	
	protected RelayedMessageException(RelayedMessage rm) {
		super();
		this.rm = rm;
	}
	
	public String toString() {
		return "Failed to format RelayedMessage.";
	}
	
	public RelayedMessage getRM() {
		return rm;
	}

}
