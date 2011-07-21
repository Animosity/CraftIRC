package com.ensifera.animosity.craftirc;

import java.util.List;

public interface EndPoint {
    
    //The type is used to format the message and such.
    public enum Type {
        MINECRAFT,
        IRC,
        PLAIN
    }
    
    public Type getType();
    
    //This is called when a message is sent to all users in this endpoint.
    public void messageIn(RelayedMessage msg);
    
    //This is called when a message is sent to a specific user in this endpoint; Return false if the message could not be delivered.
    public boolean userMessageIn(String username, RelayedMessage msg);
    
    //Return a list of online users, if possible, or null otherwise.
    public List<String> listUsers();

}
