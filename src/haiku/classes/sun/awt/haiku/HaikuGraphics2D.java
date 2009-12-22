package sun.awt.haiku;

import sun.awt.HaikuSurfaceData;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.Area;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.AffineTransformOp;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.awt.Image;
import java.awt.Composite;
import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.DirectColorModel;
import java.awt.GraphicsConfiguration;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.GeneralPath;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.text.AttributedCharacterIterator;
import java.awt.Font;
import java.awt.image.ImageObserver;
import sun.awt.image.ImageRepresentation;
import sun.awt.image.BufImgSurfaceData;
import sun.awt.image.AcceleratedOffScreenImage;
import java.awt.image.ColorConvertOp;
import java.awt.Transparency;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import sun.awt.font.FontDesignMetrics;
import sun.awt.font.StandardGlyphVector;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.PixelDrawPipe;
import sun.java2d.pipe.PixelFillPipe;
import sun.java2d.pipe.ShapeDrawPipe;
import sun.java2d.pipe.ValidatePipe;
import sun.java2d.pipe.ShapeSpanIterator;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.RegionIterator;
import sun.java2d.pipe.TextPipe;
import sun.java2d.pipe.DrawImagePipe;
import sun.java2d.pipe.DuctusRenderer;
import sun.java2d.loops.FontInfo;
import sun.java2d.loops.RenderLoops;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.Blit;
import sun.java2d.loops.BlitBg;
import sun.java2d.loops.MaskFill;
import sun.awt.font.NativeFontWrapper;
import java.awt.font.FontRenderContext;
import sun.awt.font.ShapingException;
import sun.java2d.loops.XORComposite;
import sun.awt.ConstrainableGraphics;
import sun.awt.SunHints;
import java.util.Map;
import java.util.Iterator;
import sun.awt.image.OffScreenImage;
import sun.misc.PerformanceLogger;

public class HaikuGraphics2D 
	extends Graphics2D 
	implements ConstrainableGraphics, Cloneable
{
	protected SunGraphics2D target;
	protected Rectangle dirtyRegion;
	
	public HaikuGraphics2D(HaikuSurfaceData sd, Color fg, Color bg, Font f) {
		target = new SunGraphics2D(sd, fg, bg, f);
		dirtyRegion = null;
	}
	
	public HaikuGraphics2D(SunGraphics2D sg) {
		target = sg;
		dirtyRegion = null;
	}
	
	public Graphics create() {
		return new HaikuGraphics2D((SunGraphics2D)target.create());
	}
	
	public Graphics create(int x, int y, int width, int height) {
		return new HaikuGraphics2D((SunGraphics2D)super.create(x, y, width, height));
	}
	
	protected Object clone() {
		return new HaikuGraphics2D((SunGraphics2D)target.create());
	}
	
	public void setDevClip(int x, int y, int w, int h) {
		target.setDevClip(x, y, w, h);
	}
	
	public void setDevClip(Rectangle r) {
		target.setDevClip(r);
	}
	
	public void constrain(int x, int y, int w, int h) {
		target.constrain(x, y, w, h);
	}
	
	public void validatePipe() {
		target.validatePipe();
	}
	
	public Region getCompClip() {
		return target.getCompClip();
	}
	
	public WritableRaster convertRaster(Raster inRaster, 
										ColorModel inCM, 
										ColorModel outCM)
	{
		return target.convertRaster(inRaster, inCM, outCM);
	}
	
	public Font getFont() {
		return target.getFont();
	}
	
	public FontInfo checkFontInfo(FontInfo oldinfo, Font font) {
		return target.checkFontInfo(oldinfo, font);
	}
	
	public static boolean isRotated(double[] mtx) {
		return SunGraphics2D.isRotated(mtx);
	}
	
	public void setFont(Font f) {
		target.setFont(f);
	}
	
	public synchronized static FontMetrics makeFontMetrics(Font font, 
			FontRenderContext frc)
	{
		return SunGraphics2D.makeFontMetrics(font, frc);
	}
	
	public FontMetrics getFontMetrics(Font font) {
		return target.getFontMetrics(font);
	}
	
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		return target.hit(rect, s, onStroke);
	}
	
	public ColorModel getDeviceColorModel() {
		return target.getDeviceColorModel();
	}
	
	public GraphicsConfiguration getDeviceConfiguration() {
		return target.getDeviceConfiguration();
	}
	
	public SurfaceData getSurfaceData() {
		return target.getSurfaceData();
	}
	
	public void setComposite(Composite comp) {
		target.setComposite(comp);
	}
	
	public void setPaint(Paint paint) {
		target.setPaint(paint);
	}
	
	public void setStroke(Stroke s) {
		target.setStroke(s);
	}
	
	public void setRenderingHint(Key hintKey, Object hintValue) {
		target.setRenderingHint(hintKey, hintValue);
	}
	
	public Object getRenderingHint(Key hintKey) {
		return target.getRenderingHint(hintKey);
	}
	
	public void setRenderingHints(Map hints) {
		target.setRenderingHints(hints);
	}
	
	public void addRenderingHints(Map hints) {
		target.addRenderingHints(hints);
	}
	
	public RenderingHints getRenderingHints() {
		return target.getRenderingHints();
	}
	
	public void translate(double tx, double ty) {
		target.translate(tx, ty);
	}
	
	public void rotate(double theta) {
		target.rotate(theta);
	}
	
	public void rotate(double theta, double x, double y) {
		target.rotate(theta, x, y);
	}
	
	public void scale(double sx, double sy) {
		target.scale(sx, sy);
	}
	
	public void shear(double shx, double shy) {
		target.shear(shx, shy);
	}
	
	public void transform(AffineTransform xform) {
		target.transform(xform);
	}
	
	public void translate(int x, int y) {
		target.translate(x, y);
	}
	
	public void setTransform(AffineTransform Tx) {
		target.setTransform(Tx);
	}
	
	public AffineTransform getTransform() {
		return target.getTransform();
	}
	
	public AffineTransform cloneTransform() {
		return target.cloneTransform();
	}
	
	public Paint getPaint() {
		return target.getPaint();
	}
	
	public Composite getComposite() {
		return target.getComposite();
	}
	
	public Color getColor() {
		return target.getColor();
	}
	
	public void setColor(Color color) {
		target.setColor(color);
	}
	
	public void setBackground(Color color) {
		target.setBackground(color);
	}
	
	public Color getBackground() {
		return target.getBackground();
	}
	
	public Stroke getStroke() {
		return target.getStroke();
	}
	
	public Rectangle getClipBounds() {
		return target.getClipBounds();
	}
	
	public Rectangle getClipBounds(Rectangle r) {
		return target.getClipBounds(r);
	}
	
	public boolean hitClip(int x, int y, int width, int height) {
		return target.hitClip(x, y, width, height);
	}
	
	public Shape untransformShape(Shape s) {
		return target.untransformShape(s);
	}
	
	public void clipRect(int x, int y, int w, int h) {
		target.clipRect(x, y, w, h);
	}
	
	public void setClip(int x, int y, int w, int h) {
		target.setClip(x, y, w, h);
	}
	
	public Shape getClip() {
		return target.getClip();
	}
	
	public void setClip(Shape sh) {
		target.setClip(sh);
	}
	
	public void clip(Shape s) {
		target.clip(s);
	}
	
	public void setPaintMode() {
		target.setPaintMode();
	}
	
	public void setXORMode(Color c) {
		target.setXORMode(c);
	}
	
	public void copyArea(int x, int y, int w, int h, int dx, int dy) {
		target.copyArea(x, y, w, h, dx, dy);
		dirty(x + dx, y + dy, w, h);
	}
	
	public void drawLine(int x1, int y1, int x2, int y2) {
		target.drawLine(x1, y1, x2, y2);
		dirty(x1, y1, x2 - x1, y2 - y1);
	}
	
	public void drawRoundRect(int x, int y, int w, int h, int arcW, int arcH) {
		target.drawRoundRect(x, y, w, h, arcW, arcH);
		dirty(x, y, w, h);
	}
	
	public void fillRoundRect(int x, int y, int w, int h, int arcW, int arcH) {
		target.fillRoundRect(x, y, w, h, arcW, arcH);
		dirty(x, y, w, h);
	}
	
	public void drawOval(int x, int y, int w, int h) {
		target.drawOval(x, y, w, h);
		dirty(x, y, w, h);
	}
	
	public void fillOval(int x, int y, int w, int h) {
		target.fillOval(x, y, w, h);
		dirty(x, y, w, h);
	}
	
	public void drawArc(int x, int y, int w, int h,
			int startAng1, int arcAng1)
	{
		target.drawArc(x, y, w, h, startAng1, arcAng1);
		dirty(x, y, w, h);
	}
	
	public void fillArc(int x, int y, int w, int h,
			int startAng1, int arcAng1)
	{
		target.fillArc(x, y, w, h, startAng1, arcAng1);
		dirty(x, y, w, h);
	}
	
	public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
		target.drawPolyline(xPoints, yPoints, nPoints);
		for (int i = 0; i < nPoints; i++) {
			dirty(xPoints[i], yPoints[i]);
		}
	}
	
	public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
		target.drawPolygon(xPoints, yPoints, nPoints);
		for (int i = 0; i < nPoints; i++) {
			dirty(xPoints[i], yPoints[i]);
		}
	}
	
	public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
		target.fillPolygon(xPoints, yPoints, nPoints);
		for (int i = 0; i < nPoints; i++) {
			dirty(xPoints[i], yPoints[i]);
		}
	}
	
	public void drawRect(int x, int y, int w, int h) {
		target.drawRect(x, y, w, h);
		dirty(x, y, w, h);
	}
	
	public void fillRect(int x, int y, int w, int h) {
		target.fillRect(x, y, w, h);
		dirty(x, y, w, h);
	}
	
	public void clearRect(int x, int y, int w, int h) {
		target.clearRect(x, y, w, h);
		dirty(x, y, w, h);
	}
	
	public void draw(Shape s) {
		target.draw(s);
		dirty(s.getBounds());
	}
	
	public void fill(Shape s) {
		target.fill(s);
		dirty(s.getBounds());
	}
	
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		target.drawRenderedImage(img, xform);
		dirty(img.getMinX(), img.getMinY(), img.getWidth(), img.getHeight());
	}
	
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		target.drawRenderableImage(img, xform);
		dirty(img.getMinX(), img.getMinY(), img.getWidth(), img.getHeight());
	}
	
	public void drawString(String str, int x, int y) {
		target.drawString(str, x, y);
		dirty();
	}
	
	public void drawString(String str, float x, float y) {
		target.drawString(str, x, y);
		dirty();
	}
	
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		target.drawString(iterator, x, y);
		dirty();
	}
	
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		target.drawString(iterator, x, y);
		dirty();
	}
	
	public void drawGlyphVector(GlyphVector gv, float x, float y) {
		target.drawGlyphVector(gv, x, y);
		dirty();
	}
	
	public void drawChars(char data[], int offset, int length, int x, int y) {
		target.drawChars(data, offset, length, x, y);
		dirty();
	}
	
	public void drawBytes(byte data[], int offset, int length, int x, int y) {
		target.drawBytes(data, offset, length, x, y);
		dirty();
	}
	
	public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
		boolean b = target.drawImage(img, x, y, width, height, observer);
		if (b) {
			dirty(x, y, width, height);
		}
		return b;
	}
	
	public boolean drawImage(Image img, int x, int y, int width, int height, Color bg, ImageObserver observer) {
		boolean b = target.drawImage(img, x, y, width, height, bg, observer);
		if (b) {
			dirty(x, y, width, height);
		}
		return b;
	}
	
	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		boolean b = target.drawImage(img, x, y, observer);
		if (b && observer != null) {
			dirty(x, y, img.getWidth(observer), img.getHeight(observer));
		} else {
			dirty();
		}
		return b;
	}
	
	public boolean drawImage(Image img, int x, int y, Color bg, ImageObserver observer) {
		boolean b = target.drawImage(img, x, y, bg, observer);
		if (b && observer != null) {
			dirty(x, y, img.getWidth(observer), img.getHeight(observer));
		} else {
			dirty();
		}
		return b;
	}
	
	public boolean drawImage(Image img,
			int dx1, int dy1, int dx2, int dy2,
			int sx1, int sy1, int sx2, int sy2,
			ImageObserver observer)
	{
		boolean b = target.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
		if (b) {
			dirty(dx1, dy1, dx2 - dx1, dy2 - dy1);
		}
		return b;
	}
	
	public boolean drawImage(Image img,
			int dx1, int dy1, int dx2, int dy2,
			int sx1, int sy1, int sx2, int sy2,
			Color bgcolor, ImageObserver observer)
	{
		boolean b = target.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
		if (b) {
			dirty(dx1, dy1, dx2 - dx1, dy2 - dy1);
		}
		return b;
	}
	
	public boolean drawImage(Image img,
			AffineTransform xform,
			ImageObserver observer)
	{
		boolean b = target.drawImage(img, xform, observer);
		if (b) {
			dirty();
		}
		return b;
	}
	
	public void drawImage(BufferedImage bImg, BufferedImageOp op, int x, int y) {
		target.drawImage(bImg, op, x, y);
		dirty(x, y, bImg.getWidth(), bImg.getHeight());
	}
	
	public FontRenderContext getFontRenderContext() {
		return target.getFontRenderContext();
	}
	
	protected void dirty(int x, int y, int w, int h) {
		dirty(new Rectangle(x, y, w, h));
	}

	protected void dirty(float x, float y, float w, float h) {
		dirty(new Rectangle((int)x, (int)y, (int)w, (int)h));
	}
	
	protected void dirty(int x, int y) {
		dirty(new Rectangle(x, y, 0, 0));
	}
	
	protected void dirty(Rectangle r) {
		if (dirtyRegion == null) {
			dirtyRegion = r;
		} else {
			dirtyRegion.add(r);
		}
		if (dirtyRegion.x < 0){
			dirtyRegion.x = 0;
		}
		if (dirtyRegion.y < 0) {
			dirtyRegion.y = 0;
		}
	}
	
	protected void dirty() {
		dirty(target.getSurfaceData().getBounds());
	}
	
	public void dispose() {
		if (dirtyRegion != null) {
			if (target.getSurfaceData() instanceof HaikuSurfaceData) {
				((HaikuSurfaceData)target.getSurfaceData()).repaint(
						dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
			}
		}
		target.dispose();
	}
	
	public void finalize() {
	}
}
