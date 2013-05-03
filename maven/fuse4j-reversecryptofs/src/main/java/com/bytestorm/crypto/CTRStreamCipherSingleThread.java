package com.bytestorm.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class CTRStreamCipherSingleThread implements StreamCipher {

	private final Class<? extends BlockCipher> cipherClass;
	private BlockCipher cipher;
	private ParametersWithIV ctrParams;
	private byte[] cipherBlocks;
	private int cipherBlocksPos;
	private int cipherBlocksLength;
	private byte[] counter;

	public CTRStreamCipherSingleThread(Class<? extends BlockCipher> c) {
		cipherClass = c;
		cipher = getCipherInstance();
		cipherBlocksPos = Integer.MAX_VALUE;
		cipherBlocksLength = cipher.getBlockSize();
		cipherBlocks = new byte[cipherBlocksLength];
	}

	private BlockCipher getCipherInstance() {
		try {
			return cipherClass.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getAlgorithmName() {
		return cipher.getAlgorithmName() + "/CTR";
	}

	public int getBlockSize() {
		return cipher.getBlockSize();
	}

	@Override
	public void reset() {
		System.arraycopy(ctrParams.getIV(), 0, counter, 0, counter.length);
		cipherBlocksPos = Integer.MAX_VALUE;
	}

	@Override
	public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
		if (params instanceof ParametersWithIV) {
			ctrParams = (ParametersWithIV) params;
			cipher.init(true, ctrParams.getParameters());
			counter = new byte[cipher.getBlockSize()];
			reset();
			return;
		}
		throw new IllegalArgumentException("Cipher parameters must be of type ParametersWithIV");
	}

	@Override
	public byte returnByte(byte in) {
		if (cipherBlocksPos >= cipherBlocksLength) {
			generateNextCipherBlock(cipherBlocks);
			cipherBlocksPos = 0;
		}

		return (byte)(cipherBlocks[cipherBlocksPos++] ^ in);
	}

	@Override
	public void processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
		//System.arraycopy(in, inOff, out, outOff, len);
		//for (int i=0; i<len; i++) {
		//	out[outOff+i] = in[inOff+i];
		//}

		// get a cipher block from a queue if necessary
		while (len > 0) {
			if (cipherBlocksPos >= cipherBlocksLength) {
				generateNextCipherBlock(cipherBlocks);
				cipherBlocksPos = 0;
			}

			// XOR the cipher block with the plaintext, producing the cipher text
			out[outOff] = (byte)(cipherBlocks[cipherBlocksPos] ^ in[inOff]);
			outOff++;
			inOff++;
			cipherBlocksPos++;
			len--;
		}

		//		if (cipherBlocksPos >= cipherBlocksLength) {
		//			generateNextCipherBlock(cipherBlocks);
		//			cipherBlocksPos = 0;
		//
		//			while (len >= 16) {
		//				out[outOff] = (byte)(in[inOff] ^ cipherBlocks[0]);
		//				out[outOff+1] = (byte)(in[inOff+1] ^ cipherBlocks[1]);
		//				out[outOff+2] = (byte)(in[inOff+2] ^ cipherBlocks[2]);
		//				out[outOff+3] = (byte)(in[inOff+3] ^ cipherBlocks[3]);
		//				out[outOff+4] = (byte)(in[inOff+4] ^ cipherBlocks[4]);
		//				out[outOff+5] = (byte)(in[inOff+5] ^ cipherBlocks[5]);
		//				out[outOff+6] = (byte)(in[inOff+6] ^ cipherBlocks[6]);
		//				out[outOff+7] = (byte)(in[inOff+7] ^ cipherBlocks[7]);
		//				out[outOff+8] = (byte)(in[inOff+8] ^ cipherBlocks[8]);
		//				out[outOff+9] = (byte)(in[inOff+9] ^ cipherBlocks[9]);
		//				out[outOff+10] = (byte)(in[inOff+10] ^ cipherBlocks[10]);
		//				out[outOff+11] = (byte)(in[inOff+11] ^ cipherBlocks[11]);
		//				out[outOff+12] = (byte)(in[inOff+12] ^ cipherBlocks[12]);
		//				out[outOff+13] = (byte)(in[inOff+13] ^ cipherBlocks[13]);
		//				out[outOff+14] = (byte)(in[inOff+14] ^ cipherBlocks[14]);
		//				out[outOff+15] = (byte)(in[inOff+15] ^ cipherBlocks[15]);
		//				generateNextCipherBlock(cipherBlocks);
		//				len -= 16;
		//				inOff += 16;
		//				outOff += 16;
		//			}
		//		}
		//
		//		while (len > 0) {
		//			if (cipherBlocksPos >= cipherBlocksLength) {
		//				generateNextCipherBlock(cipherBlocks);
		//				cipherBlocksPos = 0;
		//			}
		//
		//			// XOR the cipher block with the plaintext, producing the cipher text
		//			out[outOff] = (byte)(cipherBlocks[cipherBlocksPos] ^ in[inOff]);
		//			outOff++;
		//			inOff++;
		//			cipherBlocksPos++;
		//			len--;
		//		}
	}

	private void generateNextCipherBlock(byte[] out) {
		cipher.processBlock(counter, 0, out, 0);
		addToCounter(1);
	}

	public void addToCounter(long val) {
		long carry = val;
		long sum;
		for (int i = counter.length-1; i >= 0 && carry > 0; i--) {
			sum = (counter[i]&0xFF) + carry;
			counter[i] = (byte) (sum&0xFF);
			carry = (sum >>> 8);
		}
	}

	public void addToCounter(int val) {
		int carry = val;
		int sum;
		for (int i = counter.length-1; i >= 0 && carry > 0; i--) {
			sum = (counter[i]&0xFF) + carry;
			counter[i] = (byte) (sum&0xFF);
			carry = (sum >>> 8);
		}
	}

}
