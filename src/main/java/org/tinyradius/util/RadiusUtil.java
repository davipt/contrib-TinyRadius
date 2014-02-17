/**
 * $Id: RadiusUtil.java,v 1.2 2006/11/06 19:32:06 wuttke Exp $
 * Created on 09.04.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.2 $
 */
package org.tinyradius.util;

import java.io.UnsupportedEncodingException;

/**
 * This class contains miscellaneous static utility functions.
 */
public class RadiusUtil {

	/**
	 * Returns the passed string as a byte array containing the
	 * string in UTF-8 representation.
	 * @param str Java string
	 * @return UTF-8 byte array
	 */
	public static byte[] getUtf8Bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			return str.getBytes();
		}
	}
	
	/**
	 * Creates a string from the passed byte array containing the
	 * string in UTF-8 representation.
	 * @param utf8 UTF-8 byte array
	 * @return Java string
	 */
	public static String getStringFromUtf8(byte[] utf8) {
		try {
			return new String(utf8, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			return new String(utf8);
		}
	}
	
	/**
	 * Returns the byte array as a hex string in the format
	 * "0x1234".
	 * @param data byte array
	 * @return hex string
	 */
	public static String getHexString(byte[] data) {
		StringBuffer hex = new StringBuffer("0x");
		if (data != null)
			for (int i = 0; i < data.length; i++) {
				String digit = Integer.toString(data[i] & 0x0ff, 16);
				if (digit.length() < 2)
					hex.append('0');
				hex.append(digit);
			}
		return hex.toString();
	}

	 /**
     * Returns a string representation of the byte data with unprintable chars as {xx}.
     *
     * @param data the data.
     * @param start the first byte.
     * @param len the number of bytes.
     * @return the string representation of the byte[].
     */
    public static String escape(final byte[] data, final int start, final int len) {
        if (data == null)
            return null;
        if (len == 0)
            return "";
        final StringBuilder sb = new StringBuilder(len);
        final int end = start + len;
        for (int i = start; i < end; i++) {
            final byte b = data[i];
            //CHECKSTYLE:OFF MagicNumberCheck
            if (b < 32 || b == 127)
                //CHECKSTYLE:ON
                sb.append('{').append(String.format("%02X", Byte.valueOf(b))).append('}');
            else
                sb.append((char) b);
        }
        return sb.toString();
    }
}
