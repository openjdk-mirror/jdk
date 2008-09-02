/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.awt.event;

import java.awt.AWTEvent;
import java.awt.Component;

/**
 * This is used to update the AWT's knowledge about a Window's size when
 * the user changes the window bounds.
 *
 * This event is _not_ posted to the eventqueue, but rather dispatched directly
 * via Component.dispatchEvent(). It is the cleanest way we could find to update
 * the AWT's knowledge of the window size. Small testprograms showed the
 * following:
 * - Component.reshape() and its derivatives are _not_ called. This makes sense
 *   as it could end up in loops,because this calls back into the peers.
 * - Intercepting event dispatching for any events in
 *   EventQueue.dispatchEvent() showed that the size is still updated. So it
 *   is not done via an event dispatched over the eventqueue.
 *
 * Possible other candidates for implementation would have been:
 * - Call a (private) callback method in Window/Component from the native
 *   side.
 * - Call a (private) callback method in Window/Component via reflection.
 *
 * Both is uglier than sending this event directly. Note however that this
 * is impossible to test, as Component.dispatchEvent() is final and can't be
 * intercepted from outside code. But this impossibility to test the issue from
 * outside code also means that this shouldn't raise any compatibility issues.
 */
public class ComponentReshapeEvent
  extends AWTEvent
{

  public int x;
  public int y;
  public int width;
  public int height;

  public ComponentReshapeEvent(Component c, int x, int y, int width, int height)
  {
    super(c, 1999);
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public String toString() {
      return "[ComponentReshapeEvent: " + x + ", " + y + ", "
              + width + ", " + height + "]";
  }
}