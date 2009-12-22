package sun.awt.haiku;

import java.awt.*;
import java.awt.peer.*;

class BRobotPeer implements RobotPeer {
	BRobotPeer(GraphicsDevice screen) {
		System.out.println("new BRobotPeer("+screen+")");
	}

    public void mouseMove(int x, int y) {
		System.out.println("BRobotPeer.mouseMove("+x+","+y+")");
	}
    public void mousePress(int buttons) {
		System.out.println("BRobotPeer.mousePress("+buttons+")");
	}
    public void mouseRelease(int buttons) {
		System.out.println("BRobotPeer.mouseRelease("+buttons+")");
	}

    public void mouseWheel(int wheelAmt) {
		System.out.println("BRobotPeer.mouseWheel("+wheelAmt+")");
	}

    public void keyPress(int keycode) {
		System.out.println("BRobotPeer.keyPress("+keycode+")");
	}
    public void keyRelease(int keycode) {
		System.out.println("BRobotPeer.keyRelease("+keycode+")");
	}

    public int getRGBPixel(int x, int y) {
		System.out.println("BRobotPeer.getRGBPixel("+x+","+y+")");
		return 0;
	}
    public int [] getRGBPixels(Rectangle bounds) {
		System.out.println("BRobotPeer.getRGBPixels("+bounds+")");
		return null;
	}
}
