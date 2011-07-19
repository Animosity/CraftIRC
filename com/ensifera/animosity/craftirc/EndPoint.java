package com.ensifera.animosity.craftirc;

public interface EndPoint {
    
    public enum Type {
        MINECRAFT,
        IRC
    }
    
    public Type getType();
    public void messageIn(RelayedMessage msg);

}
