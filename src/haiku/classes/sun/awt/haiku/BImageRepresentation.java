package sun.awt.haiku;

import java.awt.Transparency;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import sun.awt.image.Image;
import sun.awt.image.ImageRepresentation;
import java.util.Hashtable;

class BImageRepresentation extends ImageRepresentation {
	ColorModel deviceCM;
	
	public BImageRepresentation(sun.awt.image.Image im, ColorModel cmodel,
								boolean forceCMhint)
	{
		super(im, cmodel, forceCMhint);
		
		if (forceCMhint) {
			deviceCM = cmodel;
		} else {
			deviceCM = this.cmodel;
		}
	}
	
	protected BufferedImage createImage(ColorModel cm,
										WritableRaster raster,
										boolean isRasterPremultiplied,
										Hashtable properties)
	{
		return super.createImage(cm, raster, isRasterPremultiplied, properties);
	}
}
