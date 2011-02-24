package com.ensifera.animosity.craftirc;

/**
 * @author Animosity
 * 
 */
public class DualKey {

    private Integer key1;
    private String key2;

    public DualKey(Integer i, String s) {
        key1 = i;
        key2 = s;
    }

    public Integer getKey1() {
        return key1;
    }

    public String getKey2() {
        return key2;
    }

    public boolean equals(Object obj) {
        if (obj instanceof DualKey) {
            DualKey k2 = (DualKey) obj;
            return key1.equals(k2.getKey1()) && key2.equals(k2.getKey2());
        }
        return false;
    }

    public int hashCode() {
        return key1.hashCode() ^ key2.hashCode();
    }
}