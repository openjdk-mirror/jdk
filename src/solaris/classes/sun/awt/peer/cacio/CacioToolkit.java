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


import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Label;
import java.awt.List;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.awt.peer.ButtonPeer;
import java.awt.peer.CanvasPeer;
import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.peer.CheckboxPeer;
import java.awt.peer.ChoicePeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FileDialogPeer;
import java.awt.peer.FramePeer;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.LabelPeer;
import java.awt.peer.ListPeer;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuItemPeer;
import java.awt.peer.MenuPeer;
import java.awt.peer.PanelPeer;
import java.awt.peer.PopupMenuPeer;
import java.awt.peer.ScrollPanePeer;
import java.awt.peer.ScrollbarPeer;
import java.awt.peer.TextAreaPeer;
import java.awt.peer.TextFieldPeer;
import java.awt.peer.WindowPeer;

import sun.awt.SunToolkit;
import sun.awt.image.OffScreenImage;

public abstract class CacioToolkit extends SunToolkit {

    public CacioToolkit() {
        CacioEventPump pump = getPlatformWindowFactory().createEventPump();
        pump.start();
    }

    @Override
    public ButtonPeer createButton(Button target) throws HeadlessException {
        CacioButtonPeer peer = new CacioButtonPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CanvasPeer createCanvas(Canvas target) {
        CacioCanvasPeer peer = new CacioCanvasPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }
   
    @Override
    public CheckboxPeer createCheckbox(Checkbox target)
            throws HeadlessException {
        CacioCheckboxPeer peer = new CacioCheckboxPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target)
            throws HeadlessException {
        CacioCheckboxMenuItemPeer peer = new CacioCheckboxMenuItemPeer(target);
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ChoicePeer createChoice(Choice target) throws HeadlessException {
        CacioChoicePeer peer = new CacioChoicePeer(target,
                getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public DialogPeer createDialog(Dialog target) throws HeadlessException {
        CacioDialogPeer peer = new CacioDialogPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public FileDialogPeer createFileDialog(FileDialog target)
            throws HeadlessException {
        CacioFileDialogPeer peer = new CacioFileDialogPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public FramePeer createFrame(Frame target) throws HeadlessException {
        CacioFramePeer peer = new CacioFramePeer(target,
                                                 getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public LabelPeer createLabel(Label target) throws HeadlessException {
        CacioLabelPeer peer = new CacioLabelPeer(target,
                                                 getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ListPeer createList(List target) throws HeadlessException {
        CacioListPeer peer = new CacioListPeer(target,
                                               getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuPeer createMenu(Menu target) throws HeadlessException {
        CacioMenuPeer peer = new CacioMenuPeer(target);
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuBarPeer createMenuBar(MenuBar target) throws HeadlessException {
        CacioMenuBarPeer peer = new CacioMenuBarPeer(target);
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuItemPeer createMenuItem(MenuItem target)
            throws HeadlessException {
        CacioMenuItemPeer peer = new CacioMenuItemPeer(target);
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public PanelPeer createPanel(Panel target) {
        CacioPanelPeer peer = new CacioPanelPeer(target,
                                                 getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public PopupMenuPeer createPopupMenu(PopupMenu target)
            throws HeadlessException {
        CacioPopupMenuPeer peer = new CacioPopupMenuPeer(target);
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ScrollPanePeer createScrollPane(ScrollPane target) {
        CacioScrollPanePeer peer = new CacioScrollPanePeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ScrollbarPeer createScrollbar(Scrollbar target)
            throws HeadlessException {
        CacioScrollBarPeer peer = new CacioScrollBarPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public TextAreaPeer createTextArea(TextArea target)
            throws HeadlessException {
        CacioTextAreaPeer peer = new CacioTextAreaPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public TextFieldPeer createTextField(TextField target)
            throws HeadlessException {
        
        CacioTextFieldPeer peer = new CacioTextFieldPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public WindowPeer createWindow(Window target) throws HeadlessException {

        if (target instanceof ProxyWindow) {
            return new ProxyWindowPeer((ProxyWindow) target);
        }
        CacioWindowPeer peer = new CacioWindowPeer(target,
                                                   getPlatformWindowFactory());
        SunToolkit.targetCreatedPeer(target, peer);
        return peer;

    }

    @Override
    public KeyboardFocusManagerPeer createKeyboardFocusManagerPeer(KeyboardFocusManager manager) throws HeadlessException {
        return CacioKeyboardFocusManagerPeer.getInstance();
    }

    static void disposePeer(Object target, Object peer) {
        SunToolkit.targetDisposedPeer(target, peer);
    }

    /*
     * For implementation that provide both ManagedWindows and direct
     * PlatformWindow support, this method returns an hint about the preferred
     * type to use. Implementation are not required to provide support for both
     * types and may ignore this hint.
     *
     * The hint is set by checking the value of the {@code cacio.usemanaged}
     * property, and is {@code false} by default.
     */
    protected static boolean useManagedWindows() {

        return Boolean.getBoolean("cacio.usemanaged");
    }

    /**
     * Sets whether Cacio should decorate windows by itself or not. The
     * default is to not decorate windows. Set this to true if your
     * backend doesn't support window decorations by itself. In this case,
     * Cacio will use Swing window decorations.
     *
     * This method must be called before creating any window, usually just
     * before creating your {@link PlatformWindowFactory}.
     *
     * @param decorate {@code true} for Cacio-decorated windows, {@code false}
     *        otherwise
     */
    protected void setDecorateWindows(boolean decorate) {
        CacioWindowPeer.setDecorateWindows(decorate);
    }

    /**
     * Sets whether Cacio should decorate windows by itself or not. The
     * default is to not decorate dialog windows. Set this to true if your
     * backend doesn't support window decorations by itself, or when
     * you disable window decoration but still want to decorate dialogs.
     * 
     * Cacio will use Swing window decorations to simulate the decoration.
     *
     * This method must be called before creating any window, usually just
     * before creating your {@link PlatformWindowFactory}.
     *
     * <strong>Note</strong>: {@link #setDecorateWindows(boolean) } changes the
     * default set by this method, so this method must be executed after
     * {@link #setDecorateWindows(boolean) } in order to have effect.
     *
     * @param decorate {@code true} for Cacio-decorated dialogs, {@code false}
     *        otherwise
     */
    protected void setDecorateDialogs(boolean decorate) {
        CacioWindowPeer.setDecorateDialogs(decorate);
    }

    @Override
    protected Object lazilyLoadDesktopProperty(String propName) {

        if (propName.equals(SunToolkit.DESKTOPFONTHINTS)) {
            if (desktopProperties.get(SunToolkit.DESKTOPFONTHINTS) == null) {
                desktopProperties.put(SunToolkit.DESKTOPFONTHINTS,
                                      SunToolkit.getDesktopFontHints());
            }
        }

        return desktopProperties.get(propName);
    }

    public abstract PlatformWindowFactory getPlatformWindowFactory();

    /**
     * Create an off-screen image base on the given component.
     *
     * @param component The component to base the off-screen image on.
     * @param width The width of the image.
     * @param hight The height of the image.
     *
     * @return New off-screen image.
     */
    public Image createOffScreenImage(Component component, int width, int height) {
        GraphicsConfiguration gc = component.getGraphicsConfiguration();
        ColorModel model = gc.getColorModel(Transparency.OPAQUE);
        WritableRaster wr =
            model.createCompatibleWritableRaster(width, height);
        return new OffScreenImage(component, model, wr,
                                  model.isAlphaPremultiplied());

    }

    @Override
    public boolean areExtraMouseButtonsEnabled() {
        return false;
    }
    
}
