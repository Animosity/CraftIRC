package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelayedMessage {
    
    private CraftIRC plugin;
    private EndPoint source;            //Origin endpoint of the message
    private EndPoint target;            //Target endpoint of the message
    private String eventType;           //Event type
    private LinkedList<EndPoint> cc;    //Multiple extra targets for the message
    private String template;            //Formatting string
    private Map<String,String> fields;
    
    RelayedMessage(CraftIRC plugin, EndPoint source) { this(plugin, source, null, ""); }
    RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target) { this(plugin, source, target, ""); }
    RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target, String eventType) {
        this.plugin = plugin;
        this.source = source;
        this.target = target;
        if (eventType.equals("")) eventType = "generic";
        this.eventType = eventType;
        this.template = "%message%";
        if (eventType != null && eventType != "")
            this.template = plugin.cFormatting(eventType, this);
        fields = new HashMap<String,String>();
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
    public String getEvent() {
        return eventType;
    }
    
    public void setField(String key, String value) {
        fields.put(key, value);
    }
    public String getField(String key) {
        return fields.get(key);
    }
    
    public boolean addExtraTarget(EndPoint ep) {
        if (cc.contains(ep)) return false;
        cc.add(ep);
        return true;
    }
        
    public String getMessage() {
        return getMessage(null);
    }
    public String getMessage(EndPoint currentTarget) {
        String result = template;
        EndPoint realTarget;
        
        //Resolve target
        realTarget = target;
        if (realTarget == null) realTarget = currentTarget;
        if (currentTarget == null) return result;

        //Convert colors
        if (source.getType() == EndPoint.Type.MINECRAFT) {
            if (realTarget.getType() == EndPoint.Type.IRC) {
                /*
                if(this.plugin.cGameChatColors(trgBot, trgChannel)) {
                    Pattern color_codes = Pattern.compile("\u00A7([A-Za-z0-9])?");
                    Matcher find_colors = color_codes.matcher(msgout);
                    while (find_colors.find()) {
                        msgout = find_colors.replaceFirst("\u0003" + Integer.toString(this.plugin.cColorIrcFromGame("\u00C2\u00A7" + find_colors.group(1))));
                        find_colors = color_codes.matcher(msgout);
                    }
                }
                else
                    msgout = msgout.replaceAll("(\u00A7([A-Za-z0-9])?)", "");
                
                result = this.plugin.cFormatting("game-to-irc." + formatting, trgBot, trgChannel);
                */
            } else {
                //Strip colors
            }
        }
        if (source.getType() == EndPoint.Type.IRC) {
            if (realTarget.getType() == EndPoint.Type.MINECRAFT) {
                /*
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
                */
            } else {
                //Strip colors
            }
        }
        
        //IRC color code aliases
        if (realTarget.getType() == EndPoint.Type.IRC) {
            result = result.replaceAll("%k([0-9]{1,2})%", Character.toString((char) 3) + "$1");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", Character.toString((char) 3) + "$1,$2");
            result = result.replace("%k%", Character.toString((char) 3));
            result = result.replace("%o%", Character.toString((char) 15));
            result = result.replace("%b%", Character.toString((char) 2));
            result = result.replace("%u%", Character.toString((char) 31));
            result = result.replace("%r%", Character.toString((char) 22));
        } else {
            result = result.replaceAll("%k([0-9]{1,2})%", "");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", "");
            result = result.replace("%k%", "");
            result = result.replace("%o%", "");
            result = result.replace("%b%", "");
            result = result.replace("%u%", "");
            result = result.replace("%r%", "");            
        }
        
        //Fields and named colors
        Pattern other_vars = Pattern.compile("%([A-Za-z0-9]+)%");
        Matcher find_vars = other_vars.matcher(result);
        while (find_vars.find()) {
            if (fields.get(find_vars.group(1)) != null)
                result = find_vars.replaceFirst(fields.get(find_vars.group(1)));
            else if (realTarget.getType() == EndPoint.Type.IRC)
                result = find_vars.replaceFirst(Character.toString((char) 3) + String.format("%02d", this.plugin.cColorIrcFromName(find_vars.group(1))));
            else if (realTarget.getType() == EndPoint.Type.MINECRAFT)
                result = find_vars.replaceFirst(this.plugin.cColorGameFromName(find_vars.group(1)));
            else
                result = find_vars.replaceFirst("");
            find_vars = other_vars.matcher(result);
        }
        
        return result;
    }
    
    public void post() {
        post(false);
    }
    public void post(boolean admins) {
        List<EndPoint> destinations = new LinkedList<EndPoint>(cc);
        if (target != null) destinations.add(target);
        Collections.reverse(destinations);
        plugin.delivery(this, destinations, null, admins);
    }
    
    public boolean postToUser(String username) {
        List<EndPoint> destinations = new LinkedList<EndPoint>(cc);
        if (target != null) destinations.add(target);
        Collections.reverse(destinations);
        return plugin.delivery(this, destinations, username);
    }
}
