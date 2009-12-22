package sun.awt.haiku;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageProducer;

import sun.awt.image.ImageRepresentation;

public class BImage extends sun.awt.image.Image {
	/**
	 * Construct an image from an ImageProducer object.
	 */
	public BImage(ImageProducer producer) {
		super(producer);
	}
	
	public Graphics getGraphics() {
		throw new IllegalAccessError("getGraphics() only valid for images " + 
										"created with createImage(w, h)");
	}
	
	protected ImageRepresentation makeImageRep() {
		ColorModel cm;
		if (GraphicsEnvironment.isHeadless()) {
			cm = ColorModel.getRGBdefault();
		} else {
			cm = GraphicsEnvironment.getLocalGraphicsEnvironment().
					getDefaultScreenDevice().getDefaultConfiguration().
					getColorModel();
		}
		return new BImageRepresentation(this, cm, true);
	}
}
