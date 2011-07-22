package com.ensifera.animosity.craftirc;

public interface CommandEndPoint extends EndPoint {

    //This is called when a command is send to this endpoint.
    public void commandIn(RelayedCommand cmd);
    
}
