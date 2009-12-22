package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;
import java.io.FilenameFilter;

class BFileDialogPeer extends BDialogPeer implements FileDialogPeer {
	static {
		initIDs(java.io.FilenameFilter.class, java.io.File.class, java.awt.Dialog.class);
	}
	static private native void initIDs(Class filenameFilter, Class file, Class dialog);
	//
	// initialization and native creation
	//

	BFileDialogPeer(FileDialog target) {
		super(target);
	}

	void create(BComponentPeer parent) {
		fInsets = new Insets(0, 0, 0, 0);
		_create(parent);
	}
	private native void _create(BComponentPeer parent);

	void initialize() {
		super.initialize();
		FileDialog target = (FileDialog)this.target;
		setFile(target.getFile());
		setDirectory(target.getDirectory());
		setFilenameFilter(target.getFilenameFilter());		
	}

	//
	// FileDialogPeer
	//

	public void setFile(String file) {
		_setFile(file);
	}
	private native void _setFile(String file);

	public void setDirectory(String dir) {
		_setDirectory(dir);
	}
	private native void _setDirectory(String file);

	public void setFilenameFilter(FilenameFilter filter) {
		_setFilenameFilter(filter);
	}
	private native void _setFilenameFilter(FilenameFilter filter);

	//
	// ComponentPeer
	//

    public void reshape(int x, int y, int width, int height) {
		// block reshape on FileDialog
    }
}
