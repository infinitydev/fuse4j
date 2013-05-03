/**
 *   FUSE-J: Java bindings for FUSE (Filesystem in Userspace by Miklos Szeredi (mszeredi@inf.bme.hu))
 *
 *   Copyright (C) 2003 Peter Levart (peter@select-tech.si)
 *
 *   This program can be distributed under the terms of the GNU LGPL.
 *   See the file COPYING.LIB
 */

package com.bytestorm.fuse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bytestorm.crypto.Base58;
import com.bytestorm.crypto.CTRStreamCipherSingleThread;
import com.bytestorm.crypto.RandomNumberGenerator;
import com.bytestorm.utils.Settings;
import com.bytestorm.utils.Utils;

import fuse.Errno;
import fuse.Filesystem3;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtypeConstants;
import fuse.FuseGetattrSetter;
import fuse.FuseMount;
import fuse.FuseOpenSetter;
import fuse.FuseStatfsSetter;
import fuse.LifecycleSupport;

public class ReverseCryptoFilesystem implements Filesystem3, LifecycleSupport {
	private static final Log log = LogFactory.getLog(ReverseCryptoFilesystem.class);

	private static final String SETTING_KEY = "key";
	private static final String SETTING_IV = "iv";
	private static final int CRYPTOKEY_SIZE = 32;
	private static final int IV_SIZE = 16;
	private static final int BLOCK_SIZE = 512;
	private static final int NAME_LENGTH = 1024;
	private static final int DEFAULT_MODE = 0x1FF;

	// a root directory
	private File root;
	private byte[] key;
	private byte[] iv;

	// lookup node

	private File lookup(String path) {
		log.warn("Looking up: "+path);
		if (path.equals("/")) {
			return root;
		}

		File f = new java.io.File(root, path);

		if (log.isDebugEnabled()) {
			log.debug("  lookup(\"" + path + "\") returning: " + f);
		}

		return f;
	}

	public ReverseCryptoFilesystem() {
		root = new java.io.File(System.getProperty("user.home")+"/tmp");

		log.info("created");
	}

	@Override
	public int chmod(String path, int mode) throws FuseException {
		return Errno.ENOTSUPP;
	}

	@Override
	public int chown(String path, int uid, int gid) throws FuseException {
		return 0;
	}

	@Override
	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		log.info("getattr("+path+")");
		File file = lookup(path);

		if (file.isDirectory()) {
			getattrSetter.set(file.hashCode(), FuseFtypeConstants.TYPE_DIR | DEFAULT_MODE, 1, 0, 0, 0, file.length() * NAME_LENGTH, (file.length() * NAME_LENGTH + BLOCK_SIZE - 1) / BLOCK_SIZE, 0, (int)(file.lastModified()/1000), (int)(file.lastModified()));
			return 0;
		} else if (file.isFile()) {
			getattrSetter.set(file.hashCode(), FuseFtypeConstants.TYPE_FILE | DEFAULT_MODE, 1, 0, 0, 0, file.length(), (file.length() + BLOCK_SIZE - 1) / BLOCK_SIZE, 0, (int)(file.lastModified()/1000), (int)(file.lastModified()));
			return 0;
		}

		return Errno.ENOENT;
	}

	@Override
	public int getdir(String path, FuseDirFiller filler) throws FuseException {
		File file = lookup(path);

		if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				if (subFile.isFile())
					filler.add(subFile.getName(), file.hashCode(), FuseFtypeConstants.TYPE_FILE);
				else if (subFile.isDirectory())
					filler.add(subFile.getName(), file.hashCode(), FuseFtypeConstants.TYPE_DIR);
			}

			return 0;
		}

		return Errno.ENOTDIR;
	}

	@Override
	public int link(String from, String to) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int mkdir(String path, int mode) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int mknod(String path, int mode, int rdev) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int rename(String from, String to) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int rmdir(String path) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		statfsSetter.set(BLOCK_SIZE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, NAME_LENGTH);
		return 0;
	}

	@Override
	public int symlink(String from, String to) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int truncate(String path, long size) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int unlink(String path) throws FuseException {
		return Errno.EROFS;
	}

	@Override
	public int utime(String path, int atime, int mtime) throws FuseException {
		return 0;
	}

	@Override
	public int readlink(String path, CharBuffer link) throws FuseException {
		return Errno.ENOENT;
	}

	// if open returns a filehandle by calling FuseOpenSetter.setFh() method, it will be passed to every method that supports 'fh' argument

	@Override
	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		File file = lookup(path);

		if (file != null && file.isFile()) {
			try {
				openSetter.setFh(new FileHandle(new RandomAccessFile(file, "rw"), iv, key));
				return 0;
			} catch (FileNotFoundException e) {
			}
		}

		return Errno.ENOENT;
	}

	// fh is filehandle passed from open,
	// isWritepage indicates that write was caused by a writepage

	@Override
	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		return Errno.EROFS;
	}

	// fh is filehandle passed from open

	@Override
	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		try {
			log.warn("Read offset: "+offset+" Thread: "+Thread.currentThread().getId()+" "+Thread.currentThread().getName());
			//log.warn("Reading up to "+buf.remaining()+" bytes from offset "+offset+" in file "+path);
			if (fh instanceof FileHandle) {
				FileHandle handle = (FileHandle) fh;
				//synchronized(handle) {
				//log.warn(offset+" == "+handle.getCurrentReadOffset());
				if (offset == handle.getCurrentReadOffset()) {
					byte[] bb = handle.getBuffer(buf.remaining());
					RandomAccessFile raf = handle.getRandomAccessFile();
					try {
						int len;
						synchronized (raf) {
							raf.seek(offset);
							len = Utils.read(raf, bb, 0, buf.remaining());
						}
						if (len > 0) {
							handle.getCrypto().processBytes(bb, 0, len, bb, 0);
							buf.put(bb, 0, len);
							handle.setCurrentReadOffset(offset+len);
						}
					} catch (IOException e) {
						log.warn("Read failed at "+offset+" in "+path+" ("+e.getMessage()+")");
						return Errno.EIO;
					}
				} else {
					log.warn("Restarting at new offset "+offset +"(expected: "+handle.getCurrentReadOffset()+")");
					int internalOffset = (int) (offset & 0xF);
					long blockOffset = offset ^ internalOffset;
					int readLen = buf.remaining()+internalOffset;
					byte[] bb = handle.getBuffer(readLen);
					//log.warn("internalOffset: "+internalOffset+" blockOffset: "+blockOffset+" readLen: "+readLen);

					RandomAccessFile raf = handle.getRandomAccessFile();
					try {
						raf.seek(blockOffset);
						int len = Utils.read(raf, bb, 0, readLen);
						if (len > 0) {
							CTRStreamCipherSingleThread crypto = handle.getCrypto();
							crypto.reset();
							crypto.addToCounter(blockOffset>>4);
							crypto.processBytes(bb, 0, len, bb, 0);
							buf.put(bb, internalOffset, len-internalOffset);
							handle.setCurrentReadOffset(offset+len-internalOffset);
						}
					} catch (IOException e) {
						log.warn("Read failed at "+offset+" in "+path+" ("+e.getMessage()+")");
						return Errno.EIO;
					}
				}
				return 0;
				//}
			}
		} catch (Exception e) {
			for (StackTraceElement elm : e.getStackTrace()) {
				log.warn(elm.toString());
			}
			throw new FuseException(e).initErrno(Errno.EFAULT);
		}

		return Errno.EBADF;
	}

	// new operation (called on every filehandle close), fh is filehandle passed from open

	@Override
	public int flush(String path, Object fh) throws FuseException {
		if (fh instanceof FileHandle) {
			return 0;
		}

		return Errno.EBADF;
	}

	// new operation (Synchronize file contents), fh is filehandle passed from open,
	// isDatasync indicates that only the user data should be flushed, not the meta data

	@Override
	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		if (fh instanceof FileHandle) {
			return 0;
		}

		return Errno.EBADF;
	}

	// (called when last filehandle is closed), fh is filehandle passed from open

	@Override
	public int release(String path, Object fh, int flags) throws FuseException {
		if (fh instanceof FileHandle) {
			try {
				((FileHandle) fh).close();
			} catch (IOException e) {
				log.warn("Closing failed: "+path+" ("+e.getMessage()+")");
				return Errno.EIO;
			}
			return 0;
		}

		return Errno.EBADF;
	}

	//
	// LifeCycleSupport

	@Override
	public int init() {
		log.info("Initializing Filesystem");
		try {
			String keyBase58 = Settings.getString(SETTING_KEY);
			String ivBase58 = Settings.getString(SETTING_IV);

			if (keyBase58 == null || ivBase58 == null) {
				System.out.println("No keys found. Generating keys...");
				generateKeys();
			} else {
				key = Base58.decode(keyBase58);
				iv = Base58.decode(ivBase58);
				if (key.length != CRYPTOKEY_SIZE)
					throw new IllegalStateException("Crypto length is invalid (expected: "+CRYPTOKEY_SIZE+" is: "+key.length);
				if (iv.length != IV_SIZE)
					throw new IllegalStateException("IV length is invalid (expected: "+IV_SIZE+" is: "+iv.length);
				System.out.println("Keys loaded from file "+Settings.SETTINGS_FILE);
			}
		} catch (Exception e) {
			log.error("Failed to initialize crypto system.", e);
			return Errno.EIO;
		}
		return 0;
	}

	private void generateKeys() {
		RandomNumberGenerator rng = new RandomNumberGenerator();
		rng.addRandomness();
		key = rng.generateRandomBytes(CRYPTOKEY_SIZE);
		rng.addRandomness();
		iv = rng.generateRandomBytes(IV_SIZE);
		Settings.setString(SETTING_KEY, Base58.encode(key));
		Settings.setString(SETTING_IV, Base58.encode(iv));
	}

	@Override
	public int destroy() {
		log.info("Closing Filesystem");
		return 0;
	}

	//
	// Java entry point

	public static void main(String[] args) {
		log.info("entering");

		try {
			FuseMount.mount(args, new ReverseCryptoFilesystem(), log);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			log.info("exiting");
		}
	}
}
