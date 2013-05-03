package com.bytestorm.fuse;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.bytestorm.crypto.CTRStreamCipherSingleThread;

public class FileHandle {

	private RandomAccessFile raf;
	private ThreadLocal<CTRStreamCipherSingleThread> crypto;
	private long currentReadOffset = 0;
	private byte[] buffer = new byte[1024];

	public FileHandle(RandomAccessFile raf, final byte[] iv, final byte[] key) {
		this.raf = raf;
		this.crypto = new ThreadLocal<CTRStreamCipherSingleThread>() {
			@Override
			protected CTRStreamCipherSingleThread initialValue() {
				CTRStreamCipherSingleThread cipher = new CTRStreamCipherSingleThread(AESFastEngine.class);
				cipher.init(true, new ParametersWithIV(new KeyParameter(key), iv));
				return cipher;
			}
		};
	}

	public RandomAccessFile getRandomAccessFile() {
		return raf;
	}

	public void close() throws IOException {
		raf.close();
		buffer = null;
	}

	public CTRStreamCipherSingleThread getCrypto() {
		return crypto.get();
	}

	public long getCurrentReadOffset() {
		return currentReadOffset;
	}

	public void setCurrentReadOffset(long currentOffset) {
		this.currentReadOffset = currentOffset;
	}

	public byte[] getBuffer(int size) {
		if (buffer.length < size)
			buffer = new byte[size];
		return buffer;
	}
}
