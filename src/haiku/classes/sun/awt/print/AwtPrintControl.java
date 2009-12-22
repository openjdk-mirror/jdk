/*
 * Copyright 2002-2003 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.awt.print;

import java.awt.*;
import java.io.*;
import java.security.*;
import java.util.*;
import java.awt.JobAttributes.*;
import java.awt.PageAttributes.*;

/**
 * A class which presents a modal print dialog to the user.
 *
 * @version 	1.1 03/10/01
 * @author 		Andrew Bachmann
 */
public class AwtPrintControl extends sun.awt.print.PrintControl {
	public AwtPrintControl(Frame dialogOwner, String doctitle,
			   Properties props) {
		super(dialogOwner, doctitle, null, null);
	}

	public AwtPrintControl(Frame dialogOwner, String doctitle,
			JobAttributes jobAttributes,
			PageAttributes pageAttributes) {
		super(dialogOwner,doctitle,jobAttributes,pageAttributes);
	}
				
	public String getDefaultPrinterName() {
		throw new UnsupportedOperationException(
		  "TODO: AwtPrintControl.getDefaultPrinterName() not yet implemented");
	}
	public boolean getCapabilities(PrinterCapabilities capabilities) {
		throw new UnsupportedOperationException(
		  "TODO: AwtPrintControl.getCapabilities(...) not yet implemented");
	}
	public void getPrinterList(PrinterListUpdatable updatable) {
		throw new UnsupportedOperationException(
		  "TODO: AwtPrintControl.getPrinterList(...) not yet implemented");
	}
}
