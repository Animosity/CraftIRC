package com.ensifera.animosity.craftirc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum EndPoint {
    UNKNOWN,    //Can be used as placeholder for an unknown endpoint
    GAME,        //The game server
    IRC,        //An IRC channel; Further information provided by srcBot/Channel (if source) or trgBot/Channel (if target)
    BOTH,       //TARGET only: Message to be delivered to both the game server and IRC channels (don't use as source, target only)
    PLUGIN      //For plugin interop
}

public class RelayedMessage {
    
    private CraftIRC plugin;
    private EndPoint source;        //Origin endpoint of the message
    private EndPoint target;        //Target endpoint of the message
    public String formatting;       //Formatting string ID; Mandatory before toString
    public String sender;           //Sender of the message/Main subject
    public String message;          //Message, reason, target nickname
    public String moderator;        //Person who kicked or banned, if applicable
    public String srcChannel;       //Source channel; Mandatory before toString if the origin is IRC
    public int srcBot;              //Source bot ID; Mandatory before toString if the origin is IRC
    public String trgChannel;       //Target channel; Mandatory before toString if the target is IRC
    public int trgBot;              //Target bot ID; Mandatory before toString if the origin is IRC
    public String srcChannelTag;    //Source tag; Contains the tag of the source channel
    public String world;            //Source tag; The name of the world the Player sender is chatting from
    
    protected RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target) {
        this.plugin = plugin;
        formatting = null;
        setSource(source);
        setTarget(target);
        sender = "";
        message = "";
        moderator = "";
        srcChannel = "";
        trgChannel = "";
        srcBot = -1;
        trgBot = -1;
        srcChannelTag = plugin.chanTagMap.get(new DualKey(srcBot, srcChannel));
        world = "";
        //System.out.println("RelayedMessage: "+srcBot+ " " + srcChannel + " srcChannelTag = " + srcChannelTag);
        //if (srcChannelTag == null) srcChannelTag = String.valueOf(this.srcBot) + "_" + srcChannel; 
        
    }
    public void updateTag() {
        srcChannelTag = plugin.chanTagMap.get(new DualKey(srcBot, srcChannel));
    }
    
    public CraftIRC getPlugin() {
        return this.plugin;
    }
    
    public EndPoint getSource() {
        return source;
    }
    public EndPoint getTarget() {
        return target;
    }
    public void setSource(EndPoint ep) {
        if (ep == EndPoint.BOTH) source = EndPoint.UNKNOWN;
        else source = ep;
    }
    public void setTarget(EndPoint ep) {
        target = ep;
    }
    
    public String asString() throws RelayedMessageException {
        if (target != EndPoint.BOTH) return asString(target);
        else return asString(EndPoint.UNKNOWN);
    }
    public String asString(EndPoint realTarget) throws RelayedMessageException {
        String result = "";
        String msgout = message;
        int formattingBot = trgBot;
        String formattingChannel = trgChannel;
 
        if (source == EndPoint.PLUGIN || target == EndPoint.PLUGIN || target == EndPoint.UNKNOWN) 
            result = this.message;
        if (source == EndPoint.GAME && target == EndPoint.IRC)
            result = this.plugin.cFormatting("game-to-irc." + formatting, trgBot, trgChannel);
        if (source == EndPoint.IRC && (target == EndPoint.IRC || target == EndPoint.BOTH && realTarget == EndPoint.IRC))
            result = this.plugin.cFormatting("irc-to-irc." + formatting, trgBot, trgChannel);
        if (source == EndPoint.IRC && (target == EndPoint.GAME || target == EndPoint.BOTH && realTarget == EndPoint.GAME)) {
            //Colors in chat
            if (this.plugin.cChanChatColors(srcBot, srcChannel)) {
                msgout = msgout.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 22)
                        + "|" + Character.toString((char) 31) + ")", "");
                Pattern color_codes = Pattern.compile(Character.toString((char) 3) + "([01]?[0-9])(,[0-9]{0,2})?");
                Matcher find_colors = color_codes.matcher(msgout);
                while (find_colors.find()) {
                    msgout = find_colors.replaceFirst(this.plugin.cColorGameFromIrc(Integer.parseInt(find_colors.group(1))));
                    find_colors = color_codes.matcher(msgout);
                }
                msgout = msgout.replaceAll(Character.toString((char) 15) + "|" + Character.toString((char) 3), this.plugin.cColorGameFromName("foreground"));
            } else {
                msgout = msgout.replaceAll(
                        "(" + Character.toString((char) 2) + "|" + Character.toString((char) 15) + "|"
                                + Character.toString((char) 22) + Character.toString((char) 31) + "|"
                                + Character.toString((char) 3) + "[0-9]{0,2}(,[0-9]{0,2})?)", "");
            }
            msgout = msgout + " ";
            formattingBot = srcBot;
            formattingChannel = srcChannel;
            result = this.plugin.cFormatting("irc-to-game." + formatting, srcBot, srcChannel);
        }
        if (result == null) throw new RelayedMessageException(this);
        result = result.replaceAll("%k([0-9]{1,2})%", Character.toString((char) 3) + "$1");
        result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", Character.toString((char) 3) + "$1,$2");
        result = result.replace("%k%", Character.toString((char) 3));
        result = result.replace("%o%", Character.toString((char) 15));
        result = result.replace("%b%", Character.toString((char) 2));
        result = result.replace("%u%", Character.toString((char) 31));
        result = result.replace("%r%", Character.toString((char) 22));
        result = result.replace("%sender%", sender);
        result = result.replace("%message%", msgout);
        result = result.replace("%moderator%", moderator);
        result = result.replace("%srcChannel%", srcChannel);
        result = result.replace("%trgChannel%", trgChannel);
        result = result.replace("%world%", world);
        if (source == EndPoint.GAME && this.plugin.hasPerms() && this.plugin.cChanNameColors(trgBot, trgChannel)) {
            //if (this.plugin.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " RelayedMessage: hasPerms="+String.valueOf(this.plugin.hasPerms())+" cChanNameColors()="+String.valueOf(this.plugin.cChanNameColors(trgBot, trgChannel)))+", apply pre/suffixes ");
            result = result.replace("%prefix%", this.plugin.getPermPrefix(world, sender));
            result = result.replace("%suffix%", this.plugin.getPermSuffix(world, sender));
            if (!moderator.equals("")) {
                result = result.replace("%modPrefix%", this.plugin.getPermPrefix(world, moderator));
                result = result.replace("%modSuffix%", this.plugin.getPermSuffix(world, moderator));
            } else {
                result = result.replace("%modPrefix%", "");
                result = result.replace("%modSuffix%", "");
            }
        } else {
            //if (this.plugin.isDebug()) CraftIRC.log.info(String.format(CraftIRC.NAME + " RelayedMessage: hasPerms="+String.valueOf(this.plugin.hasPerms())+" cChanNameColors()="+String.valueOf(this.plugin.cChanNameColors(trgBot, trgChannel)))+", apply EMPTY pre/suffixes ");
            result = result.replace("%prefix%", "");
            result = result.replace("%suffix%", "");
            result = result.replace("%modPrefix%", "");
            result = result.replace("%modSuffix%", "");
        }
        String aux;
        Pattern other_vars = Pattern.compile("%([A-Za-z0-9]+)%");
        Matcher find_vars = other_vars.matcher(result);
        while (find_vars.find()) {
            aux = this.plugin.cFormatting("custom." + find_vars.group(1), formattingBot, formattingChannel);
            if (aux != null)
                result = find_vars.replaceFirst(aux);
            else if (target == EndPoint.IRC || target == EndPoint.BOTH && realTarget == EndPoint.IRC)
                result = find_vars.replaceFirst(Character.toString((char) 3) + String.format("%02d", this.plugin.cColorIrcFromName(find_vars.group(1))));
            else if (target == EndPoint.GAME || target == EndPoint.BOTH && realTarget == EndPoint.GAME)
                result = find_vars.replaceFirst(this.plugin.cColorGameFromName(find_vars.group(1)));
            find_vars = other_vars.matcher(result);
        }
        return result;
    }
    
}
