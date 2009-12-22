package sun.awt;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;

/**
 * Haiku Specific Graphics Device class. This differs highly from the Win32 and X11 
 * implementations. Haiku supports different ColorModel's per configuration, unlike
 * windows and X11. Therefore, all ColorModel handling has been moved to the 
 * HaikuGraphicsConfiguration.
 *
 * @author Bryan Varner
 */
public class HaikuGraphicsDevice extends GraphicsDevice {
	int screen;
	
	GraphicsConfiguration[] configs;
	GraphicsConfiguration defaultConfig; // Original configuration before any dev changes?
	
	/**
	 * Creates a graphics device for the given display number
	 */
	public HaikuGraphicsDevice(int screennum) {
		this.screen = screennum;
	}
	
	/* Initialize JNI fields */
	private static native void initIDs();
	
	static {
		if (!GraphicsEnvironment.isHeadless()) {
			initIDs();
		}
	}
	
	public int getScreen() {
		return screen;
	}
	
	public int getType() {
		return TYPE_RASTER_SCREEN;
	}
	
	public String getIDstring() {
		// Future addition may be to return the name from /dev/graphics
		return "//" + screen;
	}
	
	/* Returns an array of the workspace configurations */
	public GraphicsConfiguration[] getConfigurations() {
		GraphicsConfiguration[] ret = configs;
		if (ret == null) {
			int num = getWorkspaceCount(screen);
			ret = new GraphicsConfiguration[num];
			for (int i = 0; i < num; i++) {
				ret[i] = HaikuGraphicsConfig.getConfig(this, i);
			}
			configs = ret;
		}
		
		return ret;
	}
	
	/* Returns the current workspace that is visible */
	public GraphicsConfiguration getDefaultConfiguration() {
		checkConfigs(screen);
		return configs[getCurrentWorkspace(screen)];
	}
	
	public native int getWorkspaceCount(int screen);
	
	public native int getCurrentWorkspace(int screen);
	
	/* resets the caches workspace configs if necessary */
	private void checkConfigs(int screen) {
		if (configs == null || configs.length != getWorkspaceCount(screen)) {
			configs = null;
			getConfigurations();
		}
	}
	
	public boolean isFullScreenSupported() {
		return false;
	}
}
