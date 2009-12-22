/*
 * Copyright 1995-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package java.lang;

import java.io.*;

/* java.lang.Process subclass in the Haiku environment.
 *
 * @author Andrew Bachmann
 * @author Bryan Varner
 */

class HaikuProcess extends Process {
	private boolean isAlive = false;
	private int exit_code = 0;
	private int thid;
	private int exitcode;

	/* stdin of the process */
	private OutputStream stdin_stream;

	/* streams directly to the process */
	private BufferedInputStream stdout_stream;
	private DeferredCloseInputStream stdout_inner_stream;
	private DeferredCloseInputStream stderr_stream;
	
	/* FileDescriptors to the streams */
	private FileDescriptor stdin_fd;
	
	private FileDescriptor stdout_fd;
	private FileDescriptor stderr_fd;
	
	HaikuProcess(String[] cmd, String[] env) throws java.io.IOException {
		this(cmd, env, null);
	}
	
	HaikuProcess(String[] cmd, String[] env, String path) throws java.io.IOException {
		stdin_fd = new FileDescriptor();
		stdout_fd = new FileDescriptor();
		stderr_fd = new FileDescriptor();
		
		isAlive = true;
		thid = spawnAndRun(cmd, env, path, stdin_fd, stdout_fd, stderr_fd);
		
		java.security.AccessController.doPrivileged(
				new java.security.PrivilegedAction() 
				{
					public Object run() {
						stdin_stream = new BufferedOutputStream(
								new FileOutputStream(stdin_fd));
						stdout_inner_stream =
								new DeferredCloseInputStream(stdout_fd);
						stdout_stream =
								new BufferedInputStream(stdout_inner_stream);
						stderr_stream =
								new DeferredCloseInputStream(stderr_fd);
						return null;
					}
				}
		);
	/*
	 * For each subprocess spawned a corresponding reaper thread
	 * is started.  That thread is the only thread which waits
	 * for the subprocess to terminate and it doesn't hold any
	 * locks while doing so.  This design allows waitFor() and 
	 * exitStatus() to be safely executed in parallel (and they
	 * need no native code).
	 */
	 
	 java.security.AccessController.doPrivileged(
	 		new java.security.PrivilegedAction()
	 		{
	 			public Object run() {
	 				Thread t = new Thread("The Reaper Lies In Wait") {
	 					public void run() {
	 						int res = waitForThreadDeath(thid);
	 						synchronized (HaikuProcess.this) {
	 							isAlive = false;
	 							exitcode = res;
	 							HaikuProcess.this.notifyAll();
	 						}
	 					}
	 				};
	 				t.setDaemon(true);
	 				t.start();
	 				return null;
	 			}
	 		}
	 );
	}
	
	public OutputStream getOutputStream() {
		return stdin_stream;
	}

	public InputStream getInputStream() {
		return stdout_stream;
	}

	public InputStream getErrorStream() {
		return stderr_stream;
	}

	public synchronized int waitFor() throws InterruptedException {
		while (isAlive) {
			wait();
		}
		return exit_code;
	}

	public synchronized int exitValue() {
		if (isAlive) {
			throw new IllegalThreadStateException("process hasn't exited");
		}
		return exit_code;
	}

	public synchronized void destroy() {
		destroyThread(thid);
		try {
            stdin_stream.close();
            stdout_inner_stream.closeDeferred(stdout_stream);
            stderr_stream.closeDeferred(stderr_stream);
        } catch (IOException e) {
            // ignore
        }
    }

    // A FileInputStream that supports the deferment of the actual close
    // operation until the last pending I/O operation on the stream has
    // finished.  This is required on Solaris because we must close the stdin
    // and stdout streams in the destroy method in order to reclaim the
    // underlying file descriptors.  Doing so, however, causes any thread
    // currently blocked in a read on one of those streams to receive an
    // IOException("Bad file number"), which is incompatible with historical
    // behavior.  By deferring the close we allow any pending reads to see -1
    // (EOF) as they did before.
    //
    private static class DeferredCloseInputStream
	extends FileInputStream
    {

	private DeferredCloseInputStream(FileDescriptor fd) {
	    super(fd);
	}

	private Object lock = new Object();	// For the following fields
	private boolean closePending = false;
	private int useCount = 0;
	private InputStream streamToClose;

	private void raise() {
	    synchronized (lock) {
		useCount++;
	    }
	}

	private void lower() throws IOException {
	    synchronized (lock) {
		useCount--;
		if (useCount == 0 && closePending) {
		    streamToClose.close();
		}
	    }
	}

	// stc is the actual stream to be closed; it might be this object, or
	// it might be an upstream object for which this object is downstream.
	//
	private void closeDeferred(InputStream stc) throws IOException {
	    synchronized (lock) {
		if (useCount == 0) {
		    stc.close();
		} else {
		    closePending = true;
		    streamToClose = stc;
		}
	    }
	}

	public void close() throws IOException {
	    synchronized (lock) {
		useCount = 0;
		closePending = false;
	    }
	    super.close();
	}

	public int read() throws IOException {
	    raise();
	    try {
		return super.read();
	    } finally {
		lower();
	    }
	}

	public int read(byte[] b) throws IOException {
	    raise();
	    try {
		return super.read(b);
	    } finally {
		lower();
	    }
	}

	public int read(byte[] b, int off, int len) throws IOException {
	    raise();
	    try {
		return super.read(b, off, len);
	    } finally {
		lower();
	    }
	}

	public long skip(long n) throws IOException {
	    raise();
	    try {
		return super.skip(n);
	    } finally {
		lower();
	    }
	}

	public int available() throws IOException {
	    raise();
	    try {
		return super.available();
	    } finally {
		lower();
	    }
	}

	}

	// Native Methods

	private native int spawnAndRun(String[] command, String[] env, String path,
				FileDescriptor stdin_fd, FileDescriptor stdout_fd, FileDescriptor stderr_fd)
		throws java.io.IOException;
	
	private static native void destroyThread(int thid);
	
	private static native int waitForThreadDeath(int thid);
}
