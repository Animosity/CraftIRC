package com.ensifera.animosity.craftirc;

/**
 * @author Animosity
 *
 */
public class Util {

    // Combine string array with delimiter
    public static String combineSplit(int initialPos, String[] parts, String delimiter) throws ArrayIndexOutOfBoundsException {
        if (initialPos >= parts.length) return "";
        String result = parts[initialPos];
        for (int i = initialPos + 1; i < parts.length; i++)
            result = result + delimiter + parts[i];
        return result;
    }
        
}
