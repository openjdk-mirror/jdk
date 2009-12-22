/*
 * Copyright 2000-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.print;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.MultiDocPrintService;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;

public class HaikuPrintServiceLookup extends PrintServiceLookup {

	private PrintService defaultPrintService;
	private PrintService[] printServices;
	private MultiDocPrintService[] multiDocPrintServices;
	private boolean initialized = false;

	private synchronized void initialize() {
		if (initialized) return;
		defaultPrintService = new HaikuPrintService();
		printServices = new PrintService[1];
		printServices[0] = defaultPrintService;
		multiDocPrintServices = new MultiDocPrintService[0];
		initialized = true;
	}

	public PrintService getDefaultPrintService() {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {  
			security.checkPrintJobAccess();
		}
		if (!initialized) {
			initialize();
		}
		return defaultPrintService;
	}

	/*
	 * return empty array as don't support multi docs
	 */
	public MultiDocPrintService[] 
		getMultiDocPrintServices(DocFlavor[] flavors,
								 AttributeSet attributes) {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {  
			security.checkPrintJobAccess();
		}
		return multiDocPrintServices;
	}

	/* Want the PrintService which is default print service to have
	 * equality of reference with the equivalent in list of print services
	 * This isn't required by the API and there's a risk doing this will
	 * lead people to assume its guaranteed.
	 */
	public PrintService[] getPrintServices() {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {  
			security.checkPrintJobAccess();
		}
		if (!initialized) {
			initialize();
		}
		return printServices;
	}

	public PrintService[] getPrintServices(DocFlavor flavor,
										   AttributeSet attributes) {
		SecurityManager security = System.getSecurityManager();
		if (security != null) {  
			security.checkPrintJobAccess();
		}
		if (!initialized) {
			initialize();
		}
		PrintRequestAttributeSet requestSet = null;
		PrintServiceAttributeSet serviceSet = null;
		if (attributes != null && !attributes.isEmpty()) {
			requestSet = new HashPrintRequestAttributeSet();
			serviceSet = new HashPrintServiceAttributeSet();
			Attribute[] attrs = attributes.toArray();
			for (int i=0; i<attrs.length; i++) {
				if (attrs[i] instanceof PrintRequestAttribute) {
					requestSet.add(attrs[i]);
				} else if (attrs[i] instanceof PrintServiceAttribute) {
					serviceSet.add(attrs[i]);
				}
			}
		}
		// there's only one print service to check
		try {
			if (printServices[0].getUnsupportedAttributes(flavor, requestSet) == null) {
				return printServices;
			}
		} catch (IllegalArgumentException e) {
		}
		return new PrintService[0];		
	}
}
