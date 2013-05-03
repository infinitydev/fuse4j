package com.bytestorm.utils;

import java.io.File;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Some convenience methods for the input and output of strings.
 */
public class StringUtils {
	static final String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz,.-+";
	static Random rnd = new SecureRandom();

	public static long filesizeStringToLong(String size) throws NumberFormatException {
		// remove all spaces
		size = size.replaceAll("\\s", "");
		String[] suffixes = {"KiB", "MiB", "GiB", "TiB", "PB", "KB", "MB", "GB", "TB", "PB"};
		long[] multiplier = {1024L, 1048576L, 1073741824L, 1099511627776L, 1125899906842624L, 1024L, 1048576L, 1073741824L, 1099511627776L, 1125899906842624L};
		for (int i=0; i<suffixes.length; i++) {
			if (size.toLowerCase().endsWith(suffixes[i].toLowerCase())) {
				size = size.substring(0, size.length()-suffixes[i].length());
				double num = parseStringToDouble(size);
				return (long) (num*multiplier[i]);
			}
		}
		return Long.parseLong(size);
	}

	public static double parseStringToDouble(String aValue) throws NumberFormatException {
		int lastDot = aValue.lastIndexOf('.');
		int lastComma = aValue.lastIndexOf(',');
		NumberFormat format;
		if (lastComma >= 0 && (lastComma > lastDot || lastDot < 0)) {
			format = NumberFormat.getNumberInstance(Locale.GERMAN);
		} else
			format = NumberFormat.getNumberInstance(Locale.ENGLISH);

		ParsePosition parsePosition = new ParsePosition(0);
		Number number = format.parse(aValue, parsePosition);

		if(number == null) {
			throw new NumberFormatException("Invalid decimal number.");
		} else if(parsePosition.getIndex() < aValue.length()) {
			throw new NumberFormatException("Number could not be parsed completely.");
		} else {
			return number.doubleValue();
		}
	}

	/**
	 * Converts a number to human readable file size
	 * 
	 * @param size number to convert
	 * @return Size as human readable string
	 */
	public static String toFilesize(long size) {
		if (size < 1024)
			return size+" B";
		else {
			size *= 10;
			size /= 1024;
		}

		String[] prefixes = {" KiB", " MiB", " GiB", " TiB", " PiB"};

		for (int i = 0; i < prefixes.length; i++) {
			if (size < 10240)
				//if (size % 10 == 0)
				//	return (size / 10)+prefixes[i];
				//else
				return ((double)size/10)+prefixes[i];
			else
				size /= 1024;

		}

		if (size % 10 == 0)
			return (size / 10)+" EiB";
		else
			return ((double)size/10)+" EiB";
	}

	public static String makePath(String basePath, String directory, List<String> pathSegments) {
		List<String> list = new ArrayList<String>();
		list.add(basePath);
		list.add(directory);
		if (pathSegments != null)
			list.addAll(pathSegments);
		return makePath(list);
	}

	/**
	 * Builds a system dependent path string from a List of path segments
	 * @param pathSegments The path segments
	 * @return The system dependent path string build from part segments
	 */
	public static String makePath(List<String> pathSegments) {
		return makePath(pathSegments, File.separatorChar);
	}
	/**
	 * Builds a system dependent path string from a List of path segments
	 * @param pathSegments The path segments
	 * @param separator The separator character that will be put between the path segments
	 * @return The system dependent path string build from part segments
	 */
	public static String makePath(List<String> pathSegments, char separator) {
		Iterator<String> it = pathSegments.iterator();
		StringBuilder b = new StringBuilder();
		boolean prevWasEmpty = false;
		if (it.hasNext()) {
			String s = it.next().trim();
			if (s.isEmpty())
				prevWasEmpty = true;
			else
				b.append(s);
		}
		while (it.hasNext()) {
			if (!prevWasEmpty)
				b.append(separator);
			String s = it.next().trim();
			if (s.isEmpty()) {
				prevWasEmpty = true;
			} else {
				b.append(s);
				prevWasEmpty = false;
			}
		}
		return b.toString();
	}

	/**
	 * Generates a random string of specified length and prefix
	 * @param prefix Prefix of the string
	 * @param length Length of the resulting string
	 * @return the random string
	 */
	public static String randomString(String prefix, int length) {
		StringBuilder sb = new StringBuilder(length);
		if (prefix != null && !prefix.isEmpty())
			sb.append(prefix);
		for(int i = sb.length(); i < length; i++)
			sb.append(charset.charAt(rnd.nextInt(charset.length())));
		return sb.toString();
	}

	/**
	 * Convert a byte to a hexadecimal string represantation
	 * @param b The byte
	 * @return Byte as hex string
	 */
	public static String byteToHexString(byte b) {
		return padLeft(Integer.toHexString(b & 0xFF).toLowerCase(), 2, '0');
	}

	/**
	 * Convert a byte array to a hexadecimal string represantation
	 * @param b The byte array
	 * @return Byte array as hex string
	 */
	public static String bytesToHexString(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++)
			sb.append(byteToHexString(b[i]));
		return sb.toString();
	}

	/**
	 * Converts a hex encoded string to a byte array
	 * @param hex Hex encoded input data
	 * @return Byte array with the input data as binary
	 * @throws NumberFormatException On invalid input characters
	 */
	public static byte[] hexStringToBytes(String hex) throws NumberFormatException {
		byte[] bts = new byte[hex.length() / 2];
		for (int i = 0; i < bts.length; i++) {
			bts[i] = (byte) Integer.parseInt(hex.substring(i*2, i*2+2), 16);
		}
		return bts;
	}

	/**
	 * Checks if an IP in string representation is valid
	 * @param ip The IP strin to check
	 * @return true if IP is in a valid format, false otherwise
	 */
	public static boolean isIPValid(String ip) {
		StringTokenizer st = new StringTokenizer(ip, ".") ;
		int cnt = 0 ;
		while(st.hasMoreTokens() && cnt <= 5) {
			String token = st.nextToken();
			if (token.length() > 3 || token.length() <= 0)
				return false;
			try {
				int i = new Integer(token) ;
				if( i < 0 || i > 255 )
					return false;
			} catch( NumberFormatException ex ) {
				return false;
			}
			cnt++ ;
		}
		if (cnt != 4)
			return false;
		return true;
	}

	/**
	 * Converts an integer to a string representation of an IP address
	 * @param ip Integer representation of IP
	 * @return String representation of IP
	 */
	public static String intToIp(int ip) {
		return ((ip >> 24 ) & 0xFF) + "." +
				((ip >> 16 ) & 0xFF) + "." +
				((ip >>  8 ) & 0xFF) + "." +
				( ip        & 0xFF);
	}

	/**
	 * Converts a string representation of an IP to an integer in network byte order
	 * @param addr The ip address to convert as string
	 * @return Integer representation of the IP
	 */
	public static int ipToInt(final String addr) throws NumberFormatException {
		final String[] addressBytes = addr.split("\\.");

		int ip = 0;
		for (int i = 0; i < 4; i++) {
			ip <<= 8;
			ip |= Integer.parseInt(addressBytes[i]);
		}
		return ip;
	}

	public static String padLeft(String s, int n, char c) {
		if (s == null)
			return null;

		int add = n - s.length();
		if(add <= 0)
			return s;

		StringBuffer str = new StringBuffer(s.length()+add);
		char[] ch = new char[add];
		Arrays.fill(ch, c);
		str.append(ch);
		str.append(s);

		return str.toString();
	}

}
