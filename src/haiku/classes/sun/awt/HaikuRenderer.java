package sun.awt;

import java.awt.Composite;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.PixelDrawPipe;
import sun.java2d.pipe.PixelFillPipe;
import sun.java2d.pipe.ShapeDrawPipe;
import sun.java2d.pipe.SpanIterator;
import sun.java2d.pipe.ShapeSpanIterator;
import sun.java2d.pipe.LoopPipe;
import sun.java2d.loops.GraphicsPrimitive;

/**
 * Handles translating Java primitive paint calls into Haiku
 * paint primitives.
 */
public class HaikuRenderer implements 
	PixelDrawPipe, 
	PixelFillPipe, 
	ShapeDrawPipe
{
	public static native void initIDs();
	
	static {
		initIDs();
	}
	
	// Lines
	native void _drawLine(SurfaceData sData,
			Region clip, Composite comp, int color,
			int x1, int y1, int x2, int y2);
	
	public void drawLine(SunGraphics2D sg2d, 
			int x1, int y1, int x2, int y2)
	{
		int transx = sg2d.transX;
		int transy = sg2d.transY;
		_drawLine(sg2d.surfaceData, sg2d.getCompClip(),
				sg2d.composite, sg2d.rgb,
				x1 + transx, y1 + transy, x2 + transx, y2 + transy);
	}
	
	// Rects
	native void _drawRect(SurfaceData sData,
			Region clip, Composite comp, int color,
			int x, int y, int w, int h);
	
	public void drawRect(SunGraphics2D sg2d,
			int x, int y, int width, int height)
	{
		_drawRect(sg2d.surfaceData, sg2d.getCompClip(),
				sg2d.composite, sg2d.rgb,
				x + sg2d.transX, y + sg2d.transY, width, height);
	}
	
	
	// Round Rects
	native void _drawRoundRect(SurfaceData sData,
			Region clip, Composite comp, int color,
			int x, int y, int w, int h,
			int arcW, int arcH);
	
	public void drawRoundRect(SunGraphics2D sg2d, 
			int x, int y, int width, int height,
			int arcWidth, int arcHeight)
	{
		_drawRoundRect(sg2d.surfaceData, sg2d.getCompClip(),
				sg2d.composite, sg2d.rgb,
				x + sg2d.transX, y + sg2d.transY, width, height,
				arcWidth, arcHeight);
	}
	
	// Oval
	native void _drawOval(SurfaceData sData, Region clip, Composite comp,
			int color, int x, int y, int w, int h);
	
	public void drawOval(SunGraphics2D sg2d,
			int x, int y, int width, int height)
	{
		_drawOval(sg2d.surfaceData, sg2d.getCompClip(),
				sg2d.composite, sg2d.rgb,
				x + sg2d.transX, y + sg2d.transY, width, height);
	}
	
	// Arc
	native void _drawArc(SurfaceData sData, Region clip, Composite comp,
			int color, int x, int y, int w, int h,
			int angleStart, int angleExtent);
	
	public void drawArc(SunGraphics2D sg2d,
			int x, int y, int width, int height,
			int startAngle, int arcAngle)
	{
		_drawArc(sg2d.surfaceData, sg2d.getCompClip(),
				sg2d.composite, sg2d.rgb,
				x + sg2d.transX, y + sg2d.transY, width, height,
				startAngle, arcAngle);
	}
	
	// Polygon
	native void _drawPoly(SurfaceData sData, Region clip, Composite comp,
			int color, int transx, int transy, int[] xpoints, int[] ypoints,
			int npoints, boolean isclosed);
	
	/** 
	 * Draws an un-closed Polygon
	 */
	public void drawPolyline(SunGraphics2D sg2d, 
			int xpoints[], int ypoints[], int npoints)
	{
		_drawPoly(sg2d.surfaceData, sg2d.getCompClip(), sg2d.composite, sg2d.rgb,
				sg2d.transX, sg2d.transY, xpoints, ypoints, npoints, false);
	}
	
	/**
	 * Draws a closed Polygon
	 */
	public void drawPolygon(SunGraphics2D sg2d, 
			int xpoints[], int ypoints[], int npoints)
	{
		_drawPoly(sg2d.surfaceData, sg2d.getCompClip(), sg2d.composite, sg2d.rgb,
				sg2d.transX, sg2d.transY, xpoints, ypoints, npoints, true);
	}
	
	
	// Filled Rectangle
	native void _fillRect(SurfaceData sData, 
			Region clip, Composite comp, int color,
			int x, int y, int w, int h);
	
	public void fillRect(SunGraphics2D sg2d,
			int x, int y, int width, int height)
	{
		_fillRect(sg2d.surfaceData, sg2d.getCompClip(), sg2d.composite, sg2d.rgb,
				x + sg2d.transX, y + sg2d.transY, width, height);
	}
	
	// Filled Round-Rect
	native void _fillRoundRect(SurfaceData sData,
			Region clip, Composite comp, int color,
			int x, int y, int w, int h, int arcW, int arcH);
	
	public void fillRoundRect(SunGraphics2D sg2d, 
			int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
		_fillRoundRect(sg2d.surfaceData, sg2d.getCompClip(), sg2d.composite, sg2d.rgb,
				x + sg2d.transX, y + sg2d.transY, width, height, arcWidth, arcHeight);
	}
	
	// Filled Oval
	native void _fillOval(SurfaceData sData,
			Region clip, Composite comp, int color,
			int x, int y, int w, int h);

	public void fillOval(SunGraphics2D sg2d,
			int x, int y, int width, int height)
	{
		_fillOval(sg2d.surfaceData,
				sg2d.getCompClip(), sg2d.composite, sg2d.rgb,
				x+sg2d.transX, y+sg2d.transY, width, height);
	}

	// Filled Arc
	native void _fillArc(SurfaceData sData,
			Region clip, Composite comp, int color,
			int x, int y, int w, int h,
			int angleStart, int angleExtent);

	public void fillArc(SunGraphics2D sg2d,
		int x, int y, int width, int height,
		int startAngle, int arcAngle)
	{
		_fillArc(sg2d.surfaceData,
				sg2d.getCompClip(), sg2d.composite, sg2d.rgb,
				x+sg2d.transX, y+sg2d.transY, width, height,
				startAngle, arcAngle);
	}

	// Filled Polygon
	native void _fillPoly(SurfaceData sData,
			Region clip, Composite comp, int color,
			int transx, int transy,
			int[] xpoints, int[] ypoints,
			int npoints);

	public void fillPolygon(SunGraphics2D sg2d,
			int xpoints[], int ypoints[],
			int npoints)
	{
		_fillPoly(sg2d.surfaceData,
				sg2d.getCompClip(), sg2d.composite, sg2d.rgb,
				sg2d.transX, sg2d.transY, xpoints, ypoints, npoints);
	}
	
	// Shapes
	native void _shape(SurfaceData sData,
			Region clip, Composite comp, int color,
			int transX, int transY,
			GeneralPath gp, boolean isfill);

	public void draw(SunGraphics2D sg2d, Shape s) {
		// We'll figure this out for Haiku later on...
		System.err.println("HaikuRenderer.draw(SunGraphics2D, Shape);");
	}
	
	public void fill(SunGraphics2D sg2d, Shape s) {
		// We'll figure this out for Haiku later on...
		System.err.println("HaikuRenderer.fill(SunGraphics2D, Shape);");
	}
	
	// Copy Area
	native void _copyArea(SurfaceData sData, int srcx, int srcy, 
			int dx, int dy, int w, int h);
	
	public void devCopyArea(SurfaceData sData, int srcx, int srcy, 
			int dx, int dy, int w, int h)
	{
		_copyArea(sData, srcx, srcy, dx, dy, w, h);
	}
	
	public HaikuRenderer traceWrap() {
		return new Tracer();
	}
	
	public static class Tracer extends HaikuRenderer {
		void _drawLine(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x1, int y1, int x2, int y2)
		{
			GraphicsPrimitive.tracePrimitive("HaikuDrawLine");
			super._drawLine(sData, clip, comp, color, x1, y1, x2, y2);
		}
		
		void _drawRect(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h)
		{
			GraphicsPrimitive.tracePrimitive("HaikuDrawRect");
			super._drawRect(sData, clip, comp, color, x, y, w, h);
		}

		void _drawRoundRect(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h,
				int arcW, int arcH)
		{
			GraphicsPrimitive.tracePrimitive("HaikuDrawRoundRect");
			super._drawRoundRect(sData, clip, comp, color,
					x, y, w, h, arcW, arcH);
		}

		void _drawOval(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h)
		{
			GraphicsPrimitive.tracePrimitive("HaikuDrawOval");
			super._drawOval(sData, clip, comp, color, x, y, w, h);
		}
		
		void _drawArc(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h,
				int angleStart, int angleExtent)
		{
			GraphicsPrimitive.tracePrimitive("HaikuDrawArc");
			super._drawArc(sData, clip, comp, color, x, y, w, h,
					angleStart, angleExtent);
		}
		
		void _drawPoly(SurfaceData sData,
				Region clip, Composite comp, int color,
				int transx, int transy,
				int[] xpoints, int[] ypoints,
				int npoints, boolean isclosed)
		{
			GraphicsPrimitive.tracePrimitive("HaikuDrawPoly");
			super._drawPoly(sData, clip, comp, color, transx, transy,
					xpoints, ypoints, npoints, isclosed);
		}

		void _fillRect(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h)
		{
			GraphicsPrimitive.tracePrimitive("HaikuFillRect");
			super._fillRect(sData, clip, comp, color, x, y, w, h);
		}
		
		void _fillRoundRect(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h,
				int arcW, int arcH)
		{
			GraphicsPrimitive.tracePrimitive("HaikuFillRoundRect");
			super._fillRoundRect(sData, clip, comp, color,
					x, y, w, h, arcW, arcH);
		}

		void _fillOval(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h)
		{
			GraphicsPrimitive.tracePrimitive("HaikuFillOval");
			super._fillOval(sData, clip, comp, color, x, y, w, h);
		}

		void _fillArc(SurfaceData sData,
				Region clip, Composite comp, int color,
				int x, int y, int w, int h,
				int angleStart, int angleExtent)
		{
			GraphicsPrimitive.tracePrimitive("HaikuFillArc");
			super._fillArc(sData, clip, comp, color, x, y, w, h,
					angleStart, angleExtent);
		}

		void _fillPoly(SurfaceData sData,
				Region clip, Composite comp, int color,
				int transx, int transy,
				int[] xpoints, int[] ypoints,
				int npoints)
		{
			GraphicsPrimitive.tracePrimitive("HaikuFillPoly");
			super._fillPoly(sData, clip, comp, color, transx, transy,
					xpoints, ypoints, npoints);
		}

		void _shape(SurfaceData sData,
				Region clip, Composite comp, int color,
				int transX, int transY,
				GeneralPath gp, boolean isfill)
		{
			GraphicsPrimitive.tracePrimitive(isfill ? "HaikuFillShape" : "HaikuDrawShape");
			super._shape(sData, clip, comp, color,
					transX, transY, gp, isfill);
		}

		void _copyArea(SurfaceData sData,
				int srcx, int srcy,
				int dx, int dy,
				int w, int h)
		{
			GraphicsPrimitive.tracePrimitive("HaikuCopyArea");
			super._copyArea(sData, srcx, srcy, dx, dy, w, h);
		}
	}
}
