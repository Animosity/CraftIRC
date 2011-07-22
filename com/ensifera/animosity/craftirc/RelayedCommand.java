package com.ensifera.animosity.craftirc;

import java.util.HashMap;
import java.util.Map;

public class RelayedCommand extends RelayedMessage {
    
    private Map<String,Boolean> flags;  //Command flags
    
    RelayedCommand(CraftIRC plugin, EndPoint source, CommandEndPoint target) {
        super(plugin, source, target, "command");
        flags = new HashMap<String,Boolean>();
    }
    
    public void setFlag(String key, boolean value) {
        flags.put(key, value);
    }
    public boolean getFlag(String key) {
        return flags.get(key);
    }
    
    public void act() {
        post(DeliveryMethod.COMMAND, null);
    }
    
}
