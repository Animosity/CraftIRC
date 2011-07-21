package com.ensifera.animosity.craftirc;

import java.util.LinkedList;
import java.util.List;

public class IRCChannelPoint implements EndPoint {

    Minebot bot;
    String channel;
    IRCChannelPoint(Minebot bot, String channel) {
        this.bot = bot;
        this.channel = channel;
    }
    
    public Type getType() {
        return EndPoint.Type.IRC;
    }

    public void messageIn(RelayedMessage msg) {
        String message = msg.getMessage();
        
    }
    
    public boolean userMessageIn(String username, RelayedMessage msg) {

        return false;
    }
    
    public boolean adminMessageIn(RelayedMessage msg) {
        String message = msg.getMessage();
        
        return false;
    }
    
    public List<String> listUsers() {
        LinkedList<String> users = new LinkedList<String>();

        return users;
    }

}
