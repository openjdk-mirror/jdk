package sun.awt.haiku;

import java.util.*;
import java.awt.datatransfer.*;
import java.io.*;
import sun.awt.datatransfer.DataTransferer;

//
// Thoughts on BClipboard
// It'd be nice to use StartWatching/StopWatching
// to keep our java contents in sync with the native
// contents, instead of polling.
//

public class BClipboard extends Clipboard {

	//
	// "BObjectPeer" stuff
	//

	static {
		initIDs();
	}
	static private native void initIDs();

	private boolean disposed = false;

	public final void dispose() {
		boolean call_disposeImpl = false;
		if (!disposed) {
			synchronized(this) {
				if (!disposed) {
					disposed = call_disposeImpl = true;
				}
			}
		}
		if (call_disposeImpl) {
			disposeImpl();
		}
	}

	protected void finalize() throws Throwable {
		// calling dispose() here is essentially a NOP since the current
		// implementation prohibits gc before an explicit call to dispose()
		dispose();
		super.finalize();
	}

	//
	// BClipboard
	//

	BClipboard(String name) {
		super(name);
		createNativeClipboard(name);
	}

	public synchronized void setContents(Transferable contents, ClipboardOwner owner) {
		final ClipboardOwner oldOwner = this.owner;
		final Transferable oldContents = this.contents;
		
		copyContentsToNativeClipboard(contents);
		
		this.owner = owner;
		this.contents = contents;
		
		if (oldOwner != null && oldOwner != owner) {
			oldOwner.lostOwnership(this, oldContents);
		}
	}

	private byte[] jvmLocalObject = null;

	private void copyContentsToNativeClipboard(Transferable contents) {
		BDataTransferer transferer = BDataTransferer.getInstanceImpl();
		System.out.println("contents : " + contents);
		DataFlavor[] flavors = contents.getTransferDataFlavors();
		if (lock()) { // uploads data from clipboard to local object.
			clear();  // clear local object
			jvmLocalObject = null;
			for(int i = 0 ; i < flavors.length ; i++) {
				DataFlavor flavor = flavors[i];
				String mimetype = flavor.getPrimaryType() + "/" + flavor.getSubType();
				System.out.println("- flavor: " + flavor + " mimetype: " + mimetype);
				try {
					byte[] bytes = transferer.translateTransferable(contents, flavor);
					System.out.println("- add data bytes: " + bytes + "\n");
					if (DataFlavor.javaJVMLocalObjectMimeType.equals(mimetype)) {
						// This flavor can not be used outside this JVM, so
						// don't bother to put it into the native clipboard.
						// Instead, we simply cache it in a local variable
						// where it can be picked up later if necessary.
						jvmLocalObject = bytes;
					} else {
						addData(mimetype, bytes);
					}
				} catch (java.io.IOException io) {
					System.err.println("copyContentsToNativeClipboard " +
					                   "IOException: " + mimetype + "\n");
				}
			}
			System.out.println("- commit\n");
			commit(); // sends the data to the clipboard
			System.out.println("unlock");
			unlock(); // allow other threads in this app to use clipboard
		} else {
			throw new IllegalStateException();
		}
		System.out.println("done\n");
	}

    public synchronized Transferable getContents(Object requestor) {
		updateContents();
        return contents;
    }

	private class ClipboardTransferable implements Transferable {
		private final HashMap flavorsToData = new HashMap();
		private final Set flavors = new HashSet();
		private DataFlavor[] sortedFlavors = null;

		public void addFlavor(String mimetype, byte[] bytes) {
			try {
				System.err.println("addFlavor(" + mimetype + "," + bytes + ")");
				DataFlavor flavor = new DataFlavor(mimetype);
				flavors.add(flavor);
				flavorsToData.put(flavor, bytes);
			} catch (ClassNotFoundException e) {
				// just ignore the flavor
			}
		}

		public void close() {
			System.err.println("close() = " + flavors);
			sortedFlavors = DataTransferer.setToSortedDataFlavorArray(flavors);
		}

	    /**
	     * Returns an array of DataFlavor objects indicating the flavors the data 
	     * can be provided in.  The array should be ordered according to preference
	     * for providing the data (from most richly descriptive to least descriptive).
	     * @return an array of data flavors in which this data can be transferred
	     */
	    public DataFlavor[] getTransferDataFlavors() {
			System.err.println("getTransferDataFlavors() = " + sortedFlavors);
			return sortedFlavors;
	    }
	
	    /**
	     * Returns whether or not the specified data flavor is supported for
	     * this object.
	     * @param flavor the requested flavor for the data
	     * @return boolean indicating whether or not the data flavor is supported
	     */
	    public boolean isDataFlavorSupported(DataFlavor flavor) {
			System.err.println("isDataFlavorSupported(" + flavor + ")");
			return flavorsToData.containsKey(flavor);
	    }
	
	    /**
	     * Returns an object which represents the data to be transferred.  The class 
	     * of the object returned is defined by the representation class of the flavor.
	     *
	     * @param flavor the requested flavor for the data
	     * @see DataFlavor#getRepresentationClass
	     * @exception IOException                if the data is no longer available
	     *              in the requested flavor.
	     * @exception UnsupportedFlavorException if the requested data flavor is
	     *              not supported.
	     */
	    public Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException, IOException
	    {
			System.err.println("getTransferData(" + flavor + ")");
	        if (!isDataFlavorSupported(flavor)) {
	            throw new UnsupportedFlavorException(flavor);
	        }
	        Object ret = flavorsToData.get(flavor);
	        if (ret instanceof IOException) {
				// rethrow IOExceptions generated while fetching data
	            throw (IOException)ret;
	        } else if (ret instanceof byte[]) {
				// Now we can render the data
				BDataTransferer transferer = BDataTransferer.getInstanceImpl();
				byte[] bytes = (byte[])ret;
				ret = transferer.translateBytes(bytes, flavor, this);
			}
	        return ret;
	    }
	}

	private void updateContents() {
		if (lock()) { // uploads data from clipboard to local object.
			ClipboardTransferable transferable = new ClipboardTransferable();
			List types = new ArrayList();
			int count = 0;
			while (true) {
				String type = getType(count++);
				if (type == null) {
					break;
				}
				types.add(type);
			}
			Iterator types_iter = types.iterator();
			while (types_iter.hasNext()) {
				String type = (String)types_iter.next();
				int i = 0;
				while (true) {
					byte[] bytes = findData(type, i++);
					if (bytes == null) {
						break;
					}
					transferable.addFlavor(type, bytes);
				}
			}
			owner = null;
			transferable.close();
			contents = transferable;
			unlock(); // allow other threads in this app to use clipboard
		} else {
			throw new IllegalStateException();
		}
	}

	//
	// native methods
	//

	private long    peer; // pointer to the native peer object.

	protected void createNativeClipboard(String name) {
		peer = _createNativeClipboard(name);
	}
	// return new BClipboard(name)
	private native long _createNativeClipboard(String name);

	protected boolean lock() {
		return _lock(peer);
	}
	// return peer->Lock()
	private native boolean _lock(long peer);

	protected void clear() {
		_clear(peer);
	}
	// peer->Clear()
	private native void _clear(long peer);

	protected void addData(String mimetype, byte[] bytes) {
		_addData(peer, mimetype, bytes, bytes.length);
	}
	// peer->AddData(mimetype, B_MIME_TYPE, bytes, bytes.length)
	private native void _addData(long peer, String mimetype, byte[] bytes, int length);

	protected void commit() {
		_commit(peer);
	}
	// peer->Commit()
	private native void _commit(long peer);

	protected void unlock() {
		_unlock(peer);
	}
	// peer->Unlock()
	private native void _unlock(long peer);

	protected void disposeImpl() {
		_disposeImpl(peer);
	}
	// delete peer;
	private native void _disposeImpl(long peer);

	protected String getType(int i) {
		return _getType(peer, i);
	}
	// GetInfo(..., i, &result, ....)
	private native String _getType(long peer, int i);

	protected byte[] findData(String mimetype, int i) {
		return _findData(peer, mimetype, i);
	}
	// FindData(...
	private native byte[] _findData(long peer, String mimetype, int i);

}
