package sun.awt;

import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.SunGraphics2D;
import java.awt.GraphicsDevice;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import sun.awt.haiku.BToolkit;
import sun.awt.DisplayChangedListener;
import sun.awt.SunDisplayChanger;

import sun.awt.haiku.BPrinterJob;
import sun.awt.haiku.BFontProperties;
import sun.awt.haiku.HaikuGraphics2D;

import java.io.File;
import java.util.*;
import sun.awt.font.*;

/**
 * This is an implementation of a GraphicsEnvironment object for the
 * default local GraphicsEnvironment used by the Java Runtime Environment
 * for Haiku environments.
 *
 * @see GraphicsDevice
 * @see GraphicsConfiguration
 * @version 1.0 10/25/2003
 */
public class HaikuGraphicsEnvironment extends SunGraphicsEnvironment {
	/**
	 * Gets the number of graphics adapters in the system.
	 */
	protected int getNumScreens() {
		/*	Haiku R5.0.3 has support for one graphics device at a time.
	 		In future OSBOS implementations, we'll hopefully need a native
			method such as this to find the information
		*/
		return 1;
	}
	
	/**
	 * Gets a GraphicsDevice associated with the given screennum.
	 */
	protected GraphicsDevice makeScreenDevice(int screennum) {
		return new HaikuGraphicsDevice(screennum);
	}
	
	public PrinterJob getPrinterJob() {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkPrintJobAccess();
		}
		
		return new BPrinterJob();
	}
	
	public Graphics2D createGraphics(BufferedImage img) {
		return new HaikuGraphics2D((SunGraphics2D)super.createGraphics(img));
	}

	//
	// Font Registration functions
	//

	/* Install the JRE fonts so that the native platform
	 * can access them.
	 */
    protected void registerFontsWithPlatform(String pathName) {
		_registerFontsWithPlatform(pathName);
	}
	private native void _registerFontsWithPlatform(String pathName);
 
	protected FontProperties createFontProperties() {
		return new BFontProperties();
	}

}
