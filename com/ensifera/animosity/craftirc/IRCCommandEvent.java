package com.ensifera.animosity.craftirc;

import org.bukkit.event.Event;

public class IRCCommandEvent extends Event {
    
    private static final long serialVersionUID = 1L;
    private boolean handled; 
    public Mode eventMode;
    
    protected IRCCommandEvent(Mode mode) {
        super("IRCEvent");
        this.handled = false;
        this.eventMode = mode;
    }

    public enum Mode {
        COMMAND, AUTHED_COMMAND
    }

    public void setHandled(boolean handled) {
        this.handled = true;
    }

    public boolean isHandled() {
        return handled;
    }

}
