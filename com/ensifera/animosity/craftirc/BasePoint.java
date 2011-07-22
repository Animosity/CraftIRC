package com.ensifera.animosity.craftirc;

import java.util.List;

//Basic null endpoint that can be extended by a plugin writer.
public abstract class BasePoint implements EndPoint {

    public void messageIn(RelayedMessage msg) {
    }

    public boolean userMessageIn(String username, RelayedMessage msg) {
        return false;
    }

    public boolean adminMessageIn(RelayedMessage msg) {
        return false;
    }

    public List<String> listUsers() {
        return null;
    }

    public List<String> listDisplayUsers() {
        return null;
    }
    
}
