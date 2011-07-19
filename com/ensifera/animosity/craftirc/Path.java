package com.ensifera.animosity.craftirc;

public class Path {
    private String sourceTag;
    private String targetTag;

    Path(String sourceTag, String targetTag) {
        this.sourceTag = sourceTag;
        this.targetTag = targetTag;
    }

    public int hashCode() {
        int hashFirst = sourceTag != null ? sourceTag.hashCode() : 0;
        int hashSecond = targetTag != null ? targetTag.hashCode() : 0;

        return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(Object other) {
        if (other != null && other instanceof Path) {
            Path otherPath = (Path)other;
            return (sourceTag.equals(otherPath.getSourceTag()) || sourceTag.equals("*"))
                && (targetTag.equals(otherPath.getTargetTag()) || targetTag.equals("*"));
        }
        return false;
    }

    public String toString() { 
           return sourceTag + " -> " + targetTag; 
    }

    public String getSourceTag() {
        return sourceTag;
    }
    
    public String getTargetTag() {
        return targetTag;
    }
}

