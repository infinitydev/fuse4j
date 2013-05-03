package com.bytestorm.crypto;

import java.net.NetworkInterface;
import java.util.Enumeration;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.ThreadedSeedGenerator;

public class RandomNumberGenerator {
	private DigestRandomGenerator random;

	public RandomNumberGenerator() {
		this.random = new DigestRandomGenerator(new SHA256Digest());
	}

	public RandomNumberGenerator addRandomness() {
		//System.out.println("Adding randomness...");
		random.addSeedMaterial(System.currentTimeMillis());
		random.addSeedMaterial(System.nanoTime());
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				byte[] mac = ifaces.nextElement().getHardwareAddress();
				if (mac != null)
					random.addSeedMaterial(mac);
			}
		} catch (Exception e) {
		}
		ThreadedSeedGenerator seedGen = new ThreadedSeedGenerator();
		random.addSeedMaterial(seedGen.generateSeed(1024, false));

		return this;
	}

	public void nextBytes(byte[] bytes) {
		random.nextBytes(bytes);
	}

	/**
	 * Generates a pseudo-random byte array.
	 * @return pseudo-random byte array of <tt>len</tt> bytes.
	 */
	public byte[] generateRandomBytes(int len) {
		byte[] bytes = new byte[len];
		random.nextBytes(bytes);
		return bytes;
	}

}
