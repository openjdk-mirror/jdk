package sun.awt.haiku;

import java.awt.*;
import java.awt.im.*;
import java.awt.im.spi.*;
import java.util.Locale;
import java.lang.Character.Subset;

class BInputMethod implements InputMethod {

	InputMethodContext context;

    public void setInputMethodContext(InputMethodContext context) {
		this.context = context;
    }

    public boolean setLocale(Locale locale) {
		// no programmatic control
		return false;
    }

    public Locale getLocale() {
		// all locales supported
		return null;
    }
    
    public void setCharacterSubsets(Subset[] subsets) {
		// no programmatic control
    }

    public void setCompositionEnabled(boolean enable) {
		// no programmatic control
    }

    public boolean isCompositionEnabled() {
		System.err.println("BInputMethod.isCompositionEnabled");
		return false;
    }

    public void reconvert() {
		// no programmatic control
    }

    public void dispatchEvent(AWTEvent event) {
		System.err.println("BInputMethod.dispatchEvent");
    }

    /**
     * Notifies this input method of changes in the client window
     * location or state. This method is called while this input
     * method is the current input method of its input context and
     * notifications for it are enabled (see {@link
     * InputMethodContext#enableClientWindowNotification
     * InputMethodContext.enableClientWindowNotification}). Calls
     * to this method are temporarily suspended if the input context's
     * {@link java.awt.im.InputContext#removeNotify removeNotify}
     * method is called, and resume when the input method is activated
     * for a new client component. It is called in the following
     * situations:
     * <ul>
     * <li>
     * when the window containing the current client component changes
     * in location, size, visibility, iconification state, or when the
     * window is closed.</li>
     * <li>
     * from <code> enableClientWindowNotification(inputMethod,
     * true)</code> if the current client component exists,</li>
     * <li>
     * when activating the input method for the first time after it
     * called
     * <code>enableClientWindowNotification(inputMethod,
     * true)</code> if during the call no current client component was
     * available,</li>
     * <li>
     * when activating the input method for a new client component
     * after the input context's removeNotify method has been
     * called.</li>
     * </ul>
     * @param bounds client window's {@link
     * java.awt.Component#getBounds bounds} on the screen; or null if
     * the client window is iconified or invisible
     */
    public void notifyClientWindowChange(Rectangle bounds) {
		System.err.println("BInputMethod.notifyClientWindowChange("+bounds+")");
    }

    public void activate() {
		// no programmatic control
    }

    public void deactivate(boolean isTemporary) {
		// no programmatic control
    }

    public void hideWindows() {
		// no programmatic control
    }
  
    /**
     * Notifies the input method that a client component has been
     * removed from its containment hierarchy, or that input method
     * support has been disabled for the component.
     * <p>
     * This method is called by {@link java.awt.im.InputContext#removeNotify InputContext.removeNotify}.
     * <p>
     * The method is only called when the input method is inactive.
     */
    public void removeNotify() {
		System.err.println("BInputMethod.removeNotify()");
    }

    /**
     * Ends any input composition that may currently be going on in this
     * context. Depending on the platform and possibly user preferences,
     * this may commit or delete uncommitted text. Any changes to the text
     * are communicated to the active component using an input method event.
     *
     * <p>
     * A text editing component may call this in a variety of situations,
     * for example, when the user moves the insertion point within the text
     * (but outside the composed text), or when the component's text is
     * saved to a file or copied to the clipboard.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#endComposition InputContext.endComposition},
     * <li>by {@link java.awt.im.InputContext#dispatchEvent InputContext.dispatchEvent}
     *     when switching to a different client component
     * <li>when switching from this input method to a different one using the
     *     user interface or
     *     {@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod}.
     * </ul>
     */
    public void endComposition() {
		System.err.println("BInputMethod.endComposition()");
    }

    /**
     * Disposes of the input method and releases the resources used by it.
     * In particular, the input method should dispose windows and close files that are no
     * longer needed.
     * <p>
     * This method is called by {@link java.awt.im.InputContext#dispose InputContext.dispose}.
     * <p>
     * The method is only called when the input method is inactive.
     * No method of this interface is called on this instance after dispose.
     */
    public void dispose() {
		System.err.println("BInputMethod.dispose()");
    }

    /**
     * Returns a control object from this input method, or null. A
     * control object provides methods that control the behavior of the
     * input method or obtain information from the input method. The type
     * of the object is an input method specific class. Clients have to
     * compare the result against known input method control object
     * classes and cast to the appropriate class to invoke the methods
     * provided.
     * <p>
     * This method is called by
     * {@link java.awt.im.InputContext#getInputMethodControlObject InputContext.getInputMethodControlObject}.
     *
     * @return a control object from this input method, or null
     */
    public Object getControlObject() {
		System.err.println("BInputMethod.getControlObject()");
		return null;
    }

}
