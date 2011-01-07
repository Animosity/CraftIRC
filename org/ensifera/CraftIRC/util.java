/**
 * 
 */
package org.ensifera.CraftIRC;

/**
 * @author Animosity
 *
 */
public class util {
	public class Colors {

	    public static final String Black = "§0";
	    public static final String Navy = "§1";
	    public static final String Green = "§2";
	    public static final String Blue = "§3";
	    public static final String Red = "§4";
	    public static final String Purple = "§5";
	    public static final String Gold = "§6";
	    public static final String LightGray = "§7";
	    public static final String Gray = "§8";
	    public static final String DarkPurple = "§9";
	    public static final String LightGreen = "§a";
	    public static final String LightBlue = "§b";
	    public static final String Rose = "§c";
	    public static final String LightPurple = "§d";
	    public static final String Yellow = "§e";
	    public static final String White = "§f";
	}
	

	// Combine string array with delimiter
	public static String combineSplit(int initialPos, String[] parts, String delimiter) throws ArrayIndexOutOfBoundsException {
		String result = "";
		for (int i = initialPos; i < parts.length; i++) {
			result = result + parts[i];
			if (i != parts.length - 1) {
				result = result + delimiter;
			}
		}
		return result;
	}
	
	public static String MessageBuilder(String[] a, String separator) {
		StringBuffer result = new StringBuffer();

		for (int i = 1; i < a.length; i++) {
			result.append(separator);
			result.append(a[i]);
		}

		return result.toString();
	}
}
