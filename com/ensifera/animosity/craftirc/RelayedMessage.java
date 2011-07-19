package com.ensifera.animosity.craftirc;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelayedMessage {
    
    private CraftIRC plugin;
    private EndPoint source;        //Origin endpoint of the message
    private EndPoint target;        //Target endpoint of the message
    private String template;        //Formatting string
    private Map<String,String> fields;
    
    RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target) {
        this.plugin = plugin;
        template = "";
        this.source = source;
        this.target = target;
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
    
    public void setField(String key, String value) {
        fields.put(key, value);
    }
    public String getField(String key) {
        return fields.get(key);
    }
        
    public String getMessage() {
        String result = template;

        //Convert colors
        if (source.getType() == EndPoint.Type.MINECRAFT && target.getType() == EndPoint.Type.IRC) {
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
        }
        if (source.getType() == EndPoint.Type.IRC && target.getType() == EndPoint.Type.MINECRAFT) {
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
        }
        
        //IRC color code aliases
        result = result.replaceAll("%k([0-9]{1,2})%", Character.toString((char) 3) + "$1");
        result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", Character.toString((char) 3) + "$1,$2");
        result = result.replace("%k%", Character.toString((char) 3));
        result = result.replace("%o%", Character.toString((char) 15));
        result = result.replace("%b%", Character.toString((char) 2));
        result = result.replace("%u%", Character.toString((char) 31));
        result = result.replace("%r%", Character.toString((char) 22));
        
        //Fields and named colors
        Pattern other_vars = Pattern.compile("%([A-Za-z0-9]+)%");
        Matcher find_vars = other_vars.matcher(result);
        while (find_vars.find()) {
            if (fields.get(find_vars.group(1)) != null)
                result = find_vars.replaceFirst(fields.get(find_vars.group(1)));
            else if (target.getType() == EndPoint.Type.IRC)
                result = find_vars.replaceFirst(Character.toString((char) 3) + String.format("%02d", this.plugin.cColorIrcFromName(find_vars.group(1))));
            else if (target.getType() == EndPoint.Type.MINECRAFT)
                result = find_vars.replaceFirst(this.plugin.cColorGameFromName(find_vars.group(1)));
            find_vars = other_vars.matcher(result);
        }
        
        return result;
    }
}
