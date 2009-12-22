package sun.awt.haiku;

import java.awt.Component;

abstract class BObjectPeer {
	static {
		initIDs();
	}
	
	protected long	pData; // pointer to the native peer object.
	protected Object	target; // Associated AWT object.
	
	private boolean disposed = false;
	
	public BObjectPeer getPeerForTarget(Object t) {
		BObjectPeer peer = (BObjectPeer) BToolkit.targetToPeer(t);
		return peer;
	}
	
    /*
     * Subclasses should override disposeImpl() instead of dispose(). Client
     * code should always invoke dispose(), never disposeImpl().
     */
	abstract protected void disposeImpl();
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
	
	protected final boolean isDisposed() {
		return disposed;
	}
	
	protected void finalize() throws Throwable {
		// calling dispose() here is essentially a NOP since the current
		// implementation prohibits gc before an explicit call to dispose()
		dispose();
		super.finalize();
	}
	
	/**
	 * Initialize JNI field and method IDs
	 */
	private static native void initIDs();
}
