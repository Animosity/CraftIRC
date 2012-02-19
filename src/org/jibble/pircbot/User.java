/* 
Copyright Paul James Mutton, 2001-2009, http://www.jibble.org/

This file is part of PircBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

*/

package org.jibble.pircbot;

import java.io.Serializable;

/**
 * This class is used to represent a user on an IRC server.
 * Instances of this class are returned by the getUser and getUsers 
 * methods in the PircBot class.
 *  <p>
 * Note that this class no longer implements the Comparable interface
 * for Java 1.1 compatibility reasons.
 *
 * @author PircBot-PPF project
 * @version 1.0.0
 */
public class User implements Serializable {
    
    private static final long serialVersionUID = 4990694420758734789L;
    
    /**
     * Constructs a User object with a known prefix and nick.
     *
     * @param prefix The status of the user, for example, "@".
     * @param nick The nick of the user.
     */
    User(String prefix, String nick) {
        _prefix = prefix;
        _nick = nick;
        _lowerNick = nick.toLowerCase();
    }
    
    
    /**
     * Returns the prefix of the user. If the User object has been obtained
     * from a list of users in a channel, then this will reflect the user's
     * status in that channel.
     *
     * @return The prefix of the user. If there is no prefix, then an empty
     *         String is returned.
     */
    public String getPrefix() {
        return _prefix;
    }
    
    
    /**
     * Returns whether or not the user represented by this object is an
     * operator. If the User object has been obtained from a list of users
     * in a channel, then this will reflect the user's operator status in
     * that channel.
     * 
     * @return true if the user is an operator in the channel.
     */
    public boolean isOp() {
        return _prefix.indexOf('@') >= 0;
    }
    
    /**
     * Returns whether or not the user represented by this object has
     * voice. If the User object has been obtained from a list of users
     * in a channel, then this will reflect the user's voice status in
     * that channel.
     * 
     * @return true if the user has voice in the channel.
     */
    public boolean hasVoice() {
        return _prefix.indexOf('+') >= 0;
    }        
    
    
    /**
     * Returns the nick of the user.
     * 
     * @return The user's nick.
     */
    public String getNick() {
        return _nick;
    }
    
    
    /**
     * Returns the nick of the user complete with their prefix if they
     * have one, e.g. "@Dave".
     * 
     * @return The user's prefix and nick.
     */
    public String toString() {
        return this.getPrefix() + this.getNick();
    }
    
    
    /**
     * Returns true if the nick represented by this User object is the same
     * as the argument. A case insensitive comparison is made.
     * 
     * @return true if the nicks are identical (case insensitive).
     */
    public boolean equals(String nick) {
        return nick.toLowerCase().equals(_lowerNick);
    }
    
    
    /**
     * Returns true if the nick represented by this User object is the same
     * as the nick of the User object given as an argument.
     * A case insensitive comparison is made.
     * 
     * @return true if o is a User object with a matching lowercase nick.
     */
    public boolean equals(Object o) {
        if (o instanceof User) {
            User other = (User) o;
            return other._lowerNick.equals(_lowerNick);
        }
        return false;
    }
    
    
    /**
     * Returns the hash code of this User object.
     * 
     * @return the hash code of the User object.
     */
    public int hashCode() {
        return _lowerNick.hashCode();
    }
    
    
    /**
     * Returns the result of calling the compareTo method on lowercased
     * nicks. This is useful for sorting lists of User objects.
     * 
     * @return the result of calling compareTo on lowercased nicks.
     */
    public int compareTo(Object o) {
        if (o instanceof User) {
            User other = (User) o;
            return other._lowerNick.compareTo(_lowerNick);
        }
        return -1;
    }
    
    /**
     * Returns whether or not the user represented by this object has the given 
     * prefix. If the User object has been obtained from a list of users
     * in a channel, then this will reflect the user's status in that 
     * channel.  This is useful for checking non-standard prefixes that may
     * exist on different networks (IRCd's).
     * 
     * @param prefix the prefix to check for
     * 
     * @return true if the user has the given prefix.
     */
    public boolean hasPrefix(String prefix) {
        return _prefix.indexOf(prefix) >= 0;
    }
    
    protected void addPrefix(String prefix) {
        if(!(hasPrefix(prefix))) {
            _prefix = prefix + _prefix;
        }        
    }
    
    protected void removePrefix(String prefix) {
        if(hasPrefix(prefix)) {
            int location = _prefix.indexOf(prefix);
            _prefix = _prefix.substring(0, location) + _prefix.substring(location+1);
        }
    }

    protected void setInfo(Object info) {
        _info = info;
    }
    
    /**
     * Gets the custom Object that was set using setUserInfo(String, Object)
     * 
     * @return the info set, null if none was set
     */
    public Object getInfo() {
        return _info;
    }
    
    private String _prefix;
    private String _nick;
    private String _lowerNick;
    private Object _info;
}
