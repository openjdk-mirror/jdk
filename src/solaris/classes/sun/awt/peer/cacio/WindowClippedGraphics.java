/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.awt.peer.cacio;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import sun.awt.ConstrainableGraphics;

public class WindowClippedGraphics extends Graphics2D
                                   implements ConstrainableGraphics {

    private Graphics2D wrapped;

    private Shape baseClip;

    private Shape userClip;

    public WindowClippedGraphics(Graphics2D w, Shape c) {
        wrapped = w;
        baseClip = c;
        userClip = null;
        wrapped.setClip(baseClip);
    }

    @Override
    public void clipRect(int x, int y, int w, int h) {
        // Update user clip.
        if (userClip == null) {
            userClip = new Rectangle(x, y, w, h);
        } else {
            if (userClip instanceof Rectangle) {
                Rectangle c = (Rectangle) userClip;
                Rectangle2D.intersect(c, new Rectangle(x, y, w, h), c);
            } else {
                Area a;
                if (userClip instanceof Area) {
                    a = (Area) userClip;
                } else {
                    a = new Area(userClip);
                }
                a.intersect(new Area(new Rectangle(x, y, w, h)));
                userClip = a;
            }
        }
        wrapped.clipRect(x, y, w, h);
    }

    @Override
    public void clip(Shape s) {
        // Update user clip.
        if (userClip == null) {
            userClip = s;
        } else {
            if (userClip instanceof Rectangle && s instanceof Rectangle) {
                Rectangle c1 = (Rectangle) userClip;
                Rectangle c2 = (Rectangle) s;
                Rectangle2D.intersect(c1, c2, c1);
            } else {
                Area a;
                if (userClip instanceof Area) {
                    a = (Area) userClip;
                } else {
                    a = new Area(userClip);
                }
                a.intersect(new Area(s));
                userClip = a;
            }
        }
        wrapped.clip(s);
    }

    @Override
    public Shape getClip() {
        return userClip;
    }

    @Override
    public Rectangle getClipBounds() {
        Rectangle clipBounds;
        if (userClip == null) {
            clipBounds = null;
        } else {
            clipBounds = userClip.getBounds();
        }
        return clipBounds;
    }

    @Override
    public Rectangle getClipBounds(Rectangle b) {
        if (b == null) {
            return userClip.getBounds();
        } else {
            b.setBounds(userClip.getBounds());
            return b;
        }
    }

    @Override
    public Rectangle getClipRect() {
        return userClip.getBounds();
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle(x, y, width, height));
    }

    @Override
    public void setClip(Shape clip) {
        // Update user clip.
        userClip = clip;
        // Do the real clip.
        Shape realClip;
        if (clip == null) {
            realClip = baseClip;
        } else {
            // Intersect the base and user clip.
            Area a;
            if (baseClip instanceof Area) {
                // Common case.
                a = (Area) baseClip;
            } else {
                a = new Area(baseClip);
            }
            Area b = new Area(clip);
            b.intersect(a);
            realClip = b;
        }
        wrapped.setClip(realClip);
    }

    private Shape transformShape(Shape s, AffineTransform t) {
        Shape r;
        if (s == null) {
            r = null;
        } else {
            Area a;
            if (s instanceof Area) {
                a = (Area) s;
            } else {
                a = new Area(s);
            }
            r =  a.createTransformedArea(t);
        }
        return r;
    }

    @Override
    public void rotate(double theta) {
        // Update base and user clip.
        AffineTransform t = AffineTransform.getRotateInstance(theta);
        try {
            AffineTransform i = t.createInverse();
            baseClip = transformShape(baseClip, i);
            userClip = transformShape(userClip, i);
        } catch (NoninvertibleTransformException ex) {
            assert false; // Should never happen.
        }
        wrapped.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        // Update base and user clip.
        AffineTransform t = AffineTransform.getRotateInstance(theta, x, y);
        try {
            AffineTransform i = t.createInverse();
            baseClip = transformShape(baseClip, i);
            userClip = transformShape(userClip, i);
        } catch (NoninvertibleTransformException ex) {
            assert false; // Should never happen.
        }
        wrapped.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        // Update base and user clip.
        AffineTransform t = AffineTransform.getScaleInstance(sx, sy);
        try {
            AffineTransform i = t.createInverse();
            baseClip = transformShape(baseClip, i);
            userClip = transformShape(userClip, i);
        } catch (NoninvertibleTransformException ex) {
            assert false; // Should never happen.
        }
        wrapped.scale(sx, sy);
    }

    @Override
    public void setTransform(AffineTransform tx) {
        // Untransform base and user clip.
        AffineTransform t = getTransform();
        baseClip = transformShape(baseClip, t);
        userClip = transformShape(userClip, t);
        // Transform base and user clips with new transform.
        try {
            AffineTransform i = tx.createInverse();
            baseClip = transformShape(baseClip, i);
            userClip = transformShape(userClip, i);
        } catch (NoninvertibleTransformException ex) {
            assert false; // Should never happen.
        }
        wrapped.setTransform(tx);
    }

    @Override
    public void shear(double shx, double shy) {
        // Update base and user clip.
        AffineTransform t = AffineTransform.getShearInstance(shx, shy);
        try {
            AffineTransform i = t.createInverse();
            baseClip = transformShape(baseClip, i);
            userClip = transformShape(userClip, i);
        } catch (NoninvertibleTransformException ex) {
            assert false; // Should never happen.
        }
        wrapped.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform tx) {
        // Update base and user clip.
        try {
            AffineTransform i = tx.createInverse();
            baseClip = transformShape(baseClip, i);
            userClip = transformShape(userClip, i);
        } catch (NoninvertibleTransformException ex) {
            assert false; // Should never happen.
        }
        wrapped.transform(tx);
    }

    @Override
    public void translate(int x, int y) {
        AffineTransform t = AffineTransform.getTranslateInstance(-x, -y);
        if (userClip != null && userClip instanceof Rectangle) {
            // Common case.
            Rectangle r = (Rectangle) userClip;
            r.x -= x;
            r.y -= y;
        } else {
            userClip = transformShape(userClip, t);
        }
        // No need to optimize baseClip: common case is Area.
        baseClip = transformShape(baseClip, t);
        wrapped.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        // Update base and user clip.
        AffineTransform t = AffineTransform.getTranslateInstance(tx, ty);
        try {
            AffineTransform i = t.createInverse();
            baseClip = transformShape(baseClip, i);
            userClip = transformShape(userClip, i);
        } catch (NoninvertibleTransformException ex) {
            assert false; // Should never happen.
        }
        wrapped.translate(tx, ty);
    }

    @Override
    public Graphics create() {
        Graphics2D wrappedClone = (Graphics2D) wrapped.create();
        
        WindowClippedGraphics g = new WindowClippedGraphics(wrappedClone,
                                                            baseClip);
        g.userClip = userClip;
        return g;
    }

    @Override
    public Graphics create(int x, int y, int w, int h) {
        // The default implementation is perfectly suitable.
        return super.create(x, y, w, h);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        wrapped.addRenderingHints(hints);
    }

    @Override
    public void draw(Shape s) {
        wrapped.draw(s);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        wrapped.drawGlyphVector(g, x, y);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return wrapped.drawImage(img, xform, obs);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        wrapped.drawImage(img, op, x, y);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        wrapped.drawRenderableImage(img, xform);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        wrapped.drawRenderedImage(img, xform);
    }

    @Override
    public void drawString(String str, int x, int y) {
        wrapped.drawString(str, x, y);
    }

    @Override
    public void drawChars(char[] c, int o, int l, int x, int y) {
        wrapped.drawChars(c, o, l, x, y);
    }

    @Override
    public void drawBytes(byte[] b, int o, int l, int x, int y) {
        wrapped.drawBytes(b, o, l, x, y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        wrapped.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        wrapped.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x,
                           float y) {
        wrapped.drawString(iterator, x, y);
    }

    @Override
    public void fill(Shape s) {
        wrapped.fill(s);
    }

    @Override
    public Color getBackground() {
        return wrapped.getBackground();
    }

    @Override
    public Composite getComposite() {
        return wrapped.getComposite();
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return wrapped.getDeviceConfiguration();
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return wrapped.getFontRenderContext();
    }

    @Override
    public Paint getPaint() {
        return wrapped.getPaint();
    }

    @Override
    public Object getRenderingHint(Key hintKey) {
        return wrapped.getRenderingHint(hintKey);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return wrapped.getRenderingHints();
    }

    @Override
    public Stroke getStroke() {
        return wrapped.getStroke();
    }

    @Override
    public AffineTransform getTransform() {
        return wrapped.getTransform();
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return wrapped.hit(rect, s, onStroke);
    }

    @Override
    public boolean hitClip(int x, int y, int w, int h) {
        return wrapped.hitClip(x, y, w, h);
    }

    @Override
    public void setBackground(Color color) {
        wrapped.setBackground(color);
    }

    @Override
    public void setComposite(Composite comp) {
        wrapped.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        wrapped.setPaint(paint);
    }

    @Override
    public void setRenderingHint(Key hintKey, Object hintValue) {
        wrapped.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        wrapped.setRenderingHints(hints);
    }

    @Override
    public void setStroke(Stroke s) {
        wrapped.setStroke(s);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        wrapped.clearRect(x, y, width, height);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        wrapped.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void dispose() {
        wrapped.dispose();
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle,
                        int arcAngle) {
         wrapped.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height,
                             ImageObserver observer) {
        return wrapped.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor,
                             ImageObserver observer) {
        return wrapped.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height,
                             Color bgcolor, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                                 observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2, Color bgcolor,
                             ImageObserver observer) {
        return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
                                 bgcolor, observer);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        wrapped.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        wrapped.drawOval(x, y, width, height);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(Polygon p) {
        wrapped.drawPolygon(p);
    }
    
    @Override
    public void fillPolygon(Polygon p) {
        wrapped.fillPolygon(p);
    }
    
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        wrapped.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle,
                        int arcAngle) {
        wrapped.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        wrapped.fillOval(x, y, width, height);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        wrapped.fillRect(x, y, width, height);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        wrapped.drawRect(x, y, width, height);
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean r) {
        wrapped.draw3DRect(x, y, width, height, r);
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean r) {
        wrapped.fill3DRect(x, y, width, height, r);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        wrapped.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public Color getColor() {
        return wrapped.getColor();
    }

    @Override
    public Font getFont() {
        return wrapped.getFont();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return wrapped.getFontMetrics(f);
    }

    @Override
    public FontMetrics getFontMetrics() {
        return wrapped.getFontMetrics();
    }

    @Override
    public void setColor(Color c) {
        wrapped.setColor(c);
    }

    @Override
    public void setFont(Font font) {
        wrapped.setFont(font);
    }

    @Override
    public void setPaintMode() {
        wrapped.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        wrapped.setXORMode(c1);
    }

    public void constrain(int x, int y, int w, int h) {

        if (wrapped instanceof ConstrainableGraphics) {

            // Update our understanding of the transform.
            AffineTransform t = AffineTransform.getTranslateInstance(-x, -y);
            if (userClip != null && userClip instanceof Rectangle) {
                // Common case.
                Rectangle r = (Rectangle) userClip;
                r.x -= x;
                r.y -= y;
            } else {
                userClip = transformShape(userClip, t);
            }
            // No need to optimize baseClip: common case is Area.
            baseClip = transformShape(baseClip, t);

            // Update base clip.
            Area newBase = new Area(baseClip);
            newBase.intersect(new Area(new Rectangle(0, 0, w, h)));
            baseClip = newBase;

            ConstrainableGraphics cg = (ConstrainableGraphics) wrapped;
            cg.constrain(x, y, w, h);
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
