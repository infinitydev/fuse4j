package com.bytestorm.utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Utils {

	private static final int EOF = -1;

	public static byte[] charArrayToByteArray(char[] c) {
		byte[] b = new byte[c.length*2];

		for(int i=0; i<c.length; i++) {
			b[2*i] = (byte) ((c[i]&0xFF00)>>8);
			b[2*i+1] = (byte) (c[i]&0x00FF);
		}

		return b;
	}

	public static void eraseCharArray(char[] c) {
		Arrays.fill(c, (char)0);
	}

	public static void eraseByteArray(byte[] b) {
		Arrays.fill(b, (byte)0);
	}

	/**
	 * Reads bytes from a RandomAccessFile.
	 * This implementation guarantees that it will read as many bytes
	 * as possible before giving up; this may not always be the case for
	 * subclasses of {@link RandomAccessFile}.
	 * 
	 * @param input where to read input from
	 * @param buffer destination
	 * @param offset initial offset into buffer
	 * @param length length to read, must be >= 0
	 * @return actual length read; may be less than requested if EOF was reached
	 * @throws IOException if a read error occurs
	 * @since 2.2
	 */
	public static int read(final RandomAccessFile input, final byte[] buffer, final int offset, final int length) throws IOException {
		if (length < 0) {
			throw new IllegalArgumentException("Length must not be negative: " + length);
		}
		int remaining = length;
		while (remaining > 0) {
			final int location = length - remaining;
			final int count = input.read(buffer, offset + location, remaining);
			if (EOF == count) { // EOF
				break;
			}
			remaining -= count;
		}
		return length - remaining;
	}
}
