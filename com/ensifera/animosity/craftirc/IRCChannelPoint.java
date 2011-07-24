package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jibble.pircbot.User;


class NicknameComparator implements Comparator<String> {

    Minebot bot;
    NicknameComparator(Minebot bot) {
        this.bot = bot;
    }
    
    public int compare(String o1, String o2) {
        String prefixes = bot.getUserPrefixesInOrder();
        if (!prefixes.contains(o1.substring(0,1)) && !prefixes.contains(o2.substring(0,1))) return o1.compareToIgnoreCase(o2);
        else if (!prefixes.contains(o1.substring(0,1))) return 1;
        else if (!prefixes.contains(o2.substring(0,1))) return -1;
        else return prefixes.indexOf(o1.substring(0,1)) - prefixes.indexOf(o2.substring(0,1));
    }
    
}

public class IRCChannelPoint implements CommandEndPoint {

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
        if (bot.getChannelPrefixes().contains(username.substring(0, 1))) return false;
        bot.sendNotice(username, msg.getMessage(this));
        return true;
    }
    
    public boolean adminMessageIn(RelayedMessage msg) {
        String message = msg.getMessage(this);
        boolean success = false;
        for (String nick : listDisplayUsers()) {
            if (bot.getPlugin().cBotAdminPrefixes(bot.getId()).contains(nick.substring(0, 1))) {
                success = true;
                bot.sendNotice(nick.substring(1), message);
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
        Collections.sort(users, new NicknameComparator(bot));
        return users;
    }

    public void commandIn(RelayedCommand cmd) {
        String command = cmd.getField("command").toLowerCase();
        //TODO: Not working for some reason (issues in the registration?)
        //TODO: Current system doesn't allow for multiple endpoints to handle the same command.
        if (bot.getPlugin().cPathAttribute(cmd.getField("source"), cmd.getField("target"), "attributes.admin") && cmd.getFlag("admin")) {
            String args = cmd.getField("args");
            if (command.equals("botsay")) {
                if (args == null) return;
                bot.sendMessage(args.substring(0, args.indexOf(" ")), args.substring(args.indexOf(" ") + 1));
            } else if (command.equals("raw")) {
                if (args == null) return;
                bot.sendRawLine(args);
            }
        }
    }

}
