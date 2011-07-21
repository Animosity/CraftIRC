package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jibble.pircbot.User;

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
        bot.sendMessage(channel, msg.getMessage(this));
    }
    
    public boolean userMessageIn(String username, RelayedMessage msg) {
        bot.sendNotice(username, msg.getMessage(this));
        return true;
    }
    
    public boolean adminMessageIn(RelayedMessage msg) {
        String message = msg.getMessage();
        boolean success = false;
        for (String nick : listDisplayUsers()) {
            if (bot.getPlugin().cBotAdminPrefixes(bot.getId()).contains(nick.charAt(0))) {
                success = true;
                bot.sendNotice(nick, message);
            }
        }
        return success;
    }
    
    public List<String> listUsers() {
        List<String> users = new LinkedList<String>();
        for (User user : bot.getUsers(channel))
            users.add(user.getNick());
        return users;
    }
    
    public List<String> listDisplayUsers() {
        List<String> users = new LinkedList<String>();
        for (User user : bot.getUsers(channel))
            users.add(bot.getHighestUserPrefix(user) + user.getNick());
        Collections.sort(users);
        return users;
    }

}
