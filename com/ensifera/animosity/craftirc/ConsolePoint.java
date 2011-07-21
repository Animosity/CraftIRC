package com.ensifera.animosity.craftirc;

import java.util.List;

public class ConsolePoint implements EndPoint {

    public Type getType() {
        return Type.PLAIN;
    }

    public void messageIn(RelayedMessage msg) {
        CraftIRC.dolog(msg.getMessage());
    }

    public boolean userMessageIn(String username, RelayedMessage msg) {
        CraftIRC.dolog("(To " + username + ")" + msg.getMessage());
        return true;
    }

    public List<String> listUsers() {
        return null;
    }

}
