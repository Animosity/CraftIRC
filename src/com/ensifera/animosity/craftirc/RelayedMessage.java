package com.ensifera.animosity.craftirc;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelayedMessage {
    
    enum DeliveryMethod {
        STANDARD,
        ADMINS,
        COMMAND
    }
    
    static String typeString = "MSG";
    
    private CraftIRC plugin;
    private EndPoint source;            //Origin endpoint of the message
    private EndPoint target;            //Target endpoint of the message
    private String eventType;           //Event type
    private LinkedList<EndPoint> cc;    //Multiple extra targets for the message
    private String template;            //Formatting string
    private Map<String,String> fields;  //All message attributes
    
    RelayedMessage(CraftIRC plugin, EndPoint source) { this(plugin, source, null, ""); }
    RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target) { this(plugin, source, target, ""); }
    RelayedMessage(CraftIRC plugin, EndPoint source, EndPoint target, String eventType) {
        this.plugin = plugin;
        this.source = source;
        this.target = target;
        if (eventType.equals("")) eventType = "generic";
        this.eventType = eventType;
        this.template = "%message%";
        if (eventType != null && eventType != "" && target != null)
            this.template = plugin.cFormatting(eventType, this);
        fields = new HashMap<String,String>();
    }
    
    public CraftIRC getPlugin() {
        return this.plugin;
    }
    EndPoint getSource() {
        return source;
    }
    EndPoint getTarget() {
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
    public Set<String> setFields() {
        return fields.keySet();
    }
    public void copyFields(RelayedMessage msg) {
        if (msg == null) return;
        for (String key : msg.setFields())
            setField(key, msg.getField(key));
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
        if (realTarget == null) {
            if (currentTarget == null) return result;
            realTarget = currentTarget;
            result = plugin.cFormatting(eventType, this, realTarget);
            if (result == null) result = template;
        }
        
        //IRC color code aliases (actually not recommended)
        if (realTarget.getType() == EndPoint.Type.IRC) {
            result = result.replaceAll("%k([0-9]{1,2})%", Character.toString((char) 3) + "$1");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", Character.toString((char) 3) + "$1,$2");
            result = result.replace("%k%", Character.toString((char) 3));
            result = result.replace("%o%", Character.toString((char) 15));
            result = result.replace("%b%", Character.toString((char) 2));
            result = result.replace("%u%", Character.toString((char) 31));
            result = result.replace("%r%", Character.toString((char) 22));
        } else if (realTarget.getType() == EndPoint.Type.MINECRAFT) {
            result = result.replaceAll("%k([0-9]{1,2})%", "");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", "");
            result = result.replace("%k%", plugin.cColorGameFromName("foreground"));
            result = result.replace("%o%", plugin.cColorGameFromName("foreground"));
            result = result.replace("%b%", "");
            result = result.replace("%u%", "");
            result = result.replace("%r%", ""); 
        } else {
            result = result.replaceAll("%k([0-9]{1,2})%", "");
            result = result.replaceAll("%k([0-9]{1,2}),([0-9]{1,2})%", "");
            result = result.replace("%k%", "");
            result = result.replace("%o%", "");
            result = result.replace("%b%", "");
            result = result.replace("%u%", "");
            result = result.replace("%r%", ""); 
        }

        //Fields and named colors (all the important stuff is here actually)
        Pattern other_vars = Pattern.compile("%([A-Za-z0-9]+)%");
        Matcher find_vars = other_vars.matcher(result);
        while (find_vars.find()) {
            if (fields.get(find_vars.group(1)) != null)
                result = find_vars.replaceFirst(Matcher.quoteReplacement(fields.get(find_vars.group(1))));
            else if (realTarget.getType() == EndPoint.Type.IRC)
                result = find_vars.replaceFirst(Character.toString((char) 3) + String.format("%02d", this.plugin.cColorIrcFromName(find_vars.group(1))));
            else if (realTarget.getType() == EndPoint.Type.MINECRAFT)
                result = find_vars.replaceFirst(this.plugin.cColorGameFromName(find_vars.group(1)));
            else
                result = find_vars.replaceFirst("");
            find_vars = other_vars.matcher(result);
        }
        
        //Convert colors
        boolean colors = plugin.cPathAttribute(fields.get("source"), fields.get("target"), "attributes.colors");
        if (source.getType() == EndPoint.Type.MINECRAFT) {
            if (realTarget.getType() == EndPoint.Type.IRC && colors) {
                Pattern color_codes = Pattern.compile("\u00A7([A-Fa-f0-9])?");
                Matcher find_colors = color_codes.matcher(result);
                while (find_colors.find()) {
                    result = find_colors.replaceFirst("\u0003" + Integer.toString(this.plugin.cColorIrcFromGame("\u00A7" + find_colors.group(1))));
                    find_colors = color_codes.matcher(result);
                }
            } else if (realTarget.getType() != EndPoint.Type.MINECRAFT || !colors) {
                //Strip colors
                result = result.replaceAll("(\u00A7([A-Fa-f0-9])?)", "");
            }
        }
        if (source.getType() == EndPoint.Type.IRC) {
            if (realTarget.getType() == EndPoint.Type.MINECRAFT && colors) {
                result = result.replaceAll("(" + Character.toString((char) 2) + "|" + Character.toString((char) 22)
                        + "|" + Character.toString((char) 31) + ")", "");
                Pattern color_codes = Pattern.compile(Character.toString((char) 3) + "([01]?[0-9])(,[0-9]{0,2})?");
                Matcher find_colors = color_codes.matcher(result);
                while (find_colors.find()) {
                    result = find_colors.replaceFirst(this.plugin.cColorGameFromIrc(Integer.parseInt(find_colors.group(1))));
                    find_colors = color_codes.matcher(result);
                }
                result = result.replaceAll(Character.toString((char) 15) + "|" + Character.toString((char) 3), this.plugin.cColorGameFromName("foreground"));
            } else if (realTarget.getType() != EndPoint.Type.IRC || !colors) {
                //Strip colors
                result = result.replaceAll(
                        "(" + Character.toString((char) 2) + "|" + Character.toString((char) 15) + "|"
                            + Character.toString((char) 22) + Character.toString((char) 31) + "|"
                            + Character.toString((char) 3) + "[0-9]{0,2}(,[0-9]{0,2})?)", "");
            }
        }
        
        return result;
    }
    
    public boolean post() {
        return post(DeliveryMethod.STANDARD, null);
    }
    public boolean post(boolean admin) {
        return post(admin ? DeliveryMethod.ADMINS : DeliveryMethod.STANDARD, null);
    }
    boolean post(DeliveryMethod dm, String username) {
        List<EndPoint> destinations;
        if (cc != null) destinations = new LinkedList<EndPoint>(cc);
        else destinations = new LinkedList<EndPoint>();
        if (target != null) destinations.add(target);
        Collections.reverse(destinations);
        return plugin.delivery(this, destinations, username, dm);
    }
    public boolean postToUser(String username) {
        return post(DeliveryMethod.STANDARD, username);
    }
    
    public String toString() {
        String rep = "{" + eventType + " " + typeString + "}";
        for(String key : fields.keySet())
            rep = rep + " (" + key + ": " + fields.get(key) + ")";
        return rep;
    }
    
}
