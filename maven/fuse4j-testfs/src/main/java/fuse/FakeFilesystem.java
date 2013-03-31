/**
 *   FUSE-J: Java bindings for FUSE (Filesystem in Userspace by Miklos Szeredi (mszeredi@inf.bme.hu))
 *
 *   Copyright (C) 2003 Peter Levart (peter@select-tech.si)
 *
 *   This program can be distributed under the terms of the GNU LGPL.
 *   See the file COPYING.LIB
 */

package fuse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FakeFilesystem implements Filesystem3, LifecycleSupport {
	private static final Log log = LogFactory.getLog(FakeFilesystem.class);

	private static final int BLOCK_SIZE = 512;
	private static final int NAME_LENGTH = 1024;
	private static final int DEFAULT_MODE = 0x1FF;

	// a root directory
	private File root;

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

	public FakeFilesystem() {
		root = new java.io.File(System.getProperty("user.home")+"/tmp");

		log.info("created");
	}

	public int chmod(String path, int mode) throws FuseException {
		return Errno.ENOTSUPP;
	}

	public int chown(String path, int uid, int gid) throws FuseException {
		return 0;
	}

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

	public int link(String from, String to) throws FuseException {
		return Errno.EROFS;
	}

	public int mkdir(String path, int mode) throws FuseException {
		return Errno.EROFS;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		return Errno.EROFS;
	}

	public int rename(String from, String to) throws FuseException {
		return Errno.EROFS;
	}

	public int rmdir(String path) throws FuseException {
		return Errno.EROFS;
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		statfsSetter.set(BLOCK_SIZE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, NAME_LENGTH);
		return 0;
	}

	public int symlink(String from, String to) throws FuseException {
		return Errno.EROFS;
	}

	public int truncate(String path, long size) throws FuseException {
		return Errno.EROFS;
	}

	public int unlink(String path) throws FuseException {
		return Errno.EROFS;
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
		return 0;
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		return Errno.ENOENT;
	}

	// if open returns a filehandle by calling FuseOpenSetter.setFh() method, it will be passed to every method that supports 'fh' argument

	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		File file = lookup(path);

		if (file != null && file.isFile()) {
			try {
				openSetter.setFh(new RandomAccessFile(file, "rw"));
				return 0;
			} catch (FileNotFoundException e) {
			}
		}

		return Errno.ENOENT;
	}

	// fh is filehandle passed from open,
	// isWritepage indicates that write was caused by a writepage

	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		return Errno.EROFS;
	}

	// fh is filehandle passed from open

	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		log.warn("Reading up to "+buf.remaining()+" bytes from offset "+offset+" in file "+path);
		if (fh instanceof RandomAccessFile) {
			RandomAccessFile raf = (RandomAccessFile) fh;
			try {
				raf.seek(offset);
				raf.getChannel().read(buf);
			} catch (IOException e) {
				log.warn("Read failed at "+offset+" in "+path+" ("+e.getMessage()+")");
				return Errno.EIO;
			}
			//buf.put(file.content, (int) offset, Math.min(buf.remaining(), file.content.length - (int) offset));

			return 0;
		}

		return Errno.EBADF;
	}

	// new operation (called on every filehandle close), fh is filehandle passed from open

	public int flush(String path, Object fh) throws FuseException {
		if (fh instanceof RandomAccessFile) {
			return 0;
		}

		return Errno.EBADF;
	}

	// new operation (Synchronize file contents), fh is filehandle passed from open,
	// isDatasync indicates that only the user data should be flushed, not the meta data

	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		if (fh instanceof RandomAccessFile) {
			return 0;
		}

		return Errno.EBADF;
	}

	// (called when last filehandle is closed), fh is filehandle passed from open

	public int release(String path, Object fh, int flags) throws FuseException {
		if (fh instanceof RandomAccessFile) {
			try {
				((RandomAccessFile) fh).close();
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

	public int init() {
		log.info("Initializing Filesystem");
		return 0;
	}

	public int destroy() {
		log.info("Destroying Filesystem");
		return 0;
	}

	//
	// Java entry point

	public static void main(String[] args) {
		log.info("entering");

		try {
			FuseMount.mount(args, new FakeFilesystem(), log);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			log.info("exiting");
		}
	}
}
