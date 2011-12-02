package com.ensifera.animosity.craftirc;

public interface CommandEndPoint extends SecuredEndPoint {

    //This is called when a command is send to this endpoint.
    public void commandIn(RelayedCommand cmd);
    
}
