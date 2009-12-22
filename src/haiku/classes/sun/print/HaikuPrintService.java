/*
 * Copyright 2000-2007 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Vector;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import javax.print.event.PrintServiceAttributeListener;

public class HaikuPrintService implements PrintService, AttributeUpdater,
										 SunPrinterJobService {

	private static final DocFlavor[] supportedDocFlavors = {
		DocFlavor.BYTE_ARRAY.POSTSCRIPT,
		DocFlavor.INPUT_STREAM.POSTSCRIPT,
		DocFlavor.URL.POSTSCRIPT,
		DocFlavor.BYTE_ARRAY.GIF,
		DocFlavor.INPUT_STREAM.GIF,
		DocFlavor.URL.GIF,
		DocFlavor.BYTE_ARRAY.JPEG,
		DocFlavor.INPUT_STREAM.JPEG,
		DocFlavor.URL.JPEG,
		DocFlavor.BYTE_ARRAY.PNG,
		DocFlavor.INPUT_STREAM.PNG,
		DocFlavor.URL.PNG,

		DocFlavor.CHAR_ARRAY.TEXT_PLAIN,
		DocFlavor.READER.TEXT_PLAIN,
		DocFlavor.STRING.TEXT_PLAIN,

		DocFlavor.BYTE_ARRAY.TEXT_PLAIN_HOST,
		DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8,
		DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16,
		DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16BE,
		DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_16LE,
		DocFlavor.BYTE_ARRAY.TEXT_PLAIN_US_ASCII,

		DocFlavor.INPUT_STREAM.TEXT_PLAIN_HOST,
		DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_8,
		DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16,
		DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16BE,
		DocFlavor.INPUT_STREAM.TEXT_PLAIN_UTF_16LE,
		DocFlavor.INPUT_STREAM.TEXT_PLAIN_US_ASCII,

		DocFlavor.URL.TEXT_PLAIN_HOST,
		DocFlavor.URL.TEXT_PLAIN_UTF_8,
		DocFlavor.URL.TEXT_PLAIN_UTF_16,
		DocFlavor.URL.TEXT_PLAIN_UTF_16BE,
		DocFlavor.URL.TEXT_PLAIN_UTF_16LE,
		DocFlavor.URL.TEXT_PLAIN_US_ASCII,

		DocFlavor.SERVICE_FORMATTED.PAGEABLE,
		DocFlavor.SERVICE_FORMATTED.PRINTABLE,

		DocFlavor.BYTE_ARRAY.AUTOSENSE,
		DocFlavor.URL.AUTOSENSE,
		DocFlavor.INPUT_STREAM.AUTOSENSE
	};

	/* let's try to support a few of these */
	private static final Class[] serviceAttrCats = {
		PrinterName.class,
		PrinterIsAcceptingJobs.class,
		QueuedJobCount.class,
	};

   /*  it turns out to be inconvenient to store the other categories
	 *  separately because many attributes are in multiple categories.
	 */
	private static final Class[] otherAttrCats = {
		Chromaticity.class,
		Copies.class,
		Destination.class,
		Fidelity.class,
		JobName.class,
		Media.class, /* have to support this somehow ... */
		MediaPrintableArea.class,
		OrientationRequested.class,
		PageRanges.class,
		PrintQuality.class,
		PrinterResolution.class,
		RequestingUserName.class,  
		SheetCollate.class,
		Sides.class,
		SunAlternateMedia.class,
	};

	private static int MAXCOPIES = 999;

	private static final MediaSizeName mediaSizes[] = {
		MediaSizeName.NA_LETTER,
		MediaSizeName.ISO_A4,
	};

    transient private PrintServiceAttributeSet lastSet;
    transient private ServiceNotifier notifier = null;

	public void addPrintServiceAttributeListener(
								 PrintServiceAttributeListener listener) {
		synchronized (this) {
			if (listener == null) {
				return;
			}
			if (notifier == null) {
				notifier = new ServiceNotifier(this);
			}
			notifier.addListener(listener);
		}
	}

	public DocPrintJob createPrintJob() {
		SecurityManager security = System.getSecurityManager();  
		if (security != null) {   
			security.checkPrintJobAccess();	
		}
		return new HaikuPrintJob(this);
	}

	public boolean equals(Object obj) {
		return (obj == this
			|| (obj instanceof HaikuPrintService &&
				((HaikuPrintService)obj).getName().equals(getName())));
	}

	public PrintServiceAttribute getAttribute(Class category) {
		if (category == null) {
			throw new NullPointerException("category");
		}
		if (!(PrintServiceAttribute.class.isAssignableFrom(category))) {
			throw new IllegalArgumentException("Not a PrintServiceAttribute");
		}
		if (category == PrinterName.class) {
				return getPrinterName();
		} else if (category == QueuedJobCount.class) {
				return getQueuedJobCount();
		} else if (category == PrinterIsAcceptingJobs.class) {
				return getPrinterIsAcceptingJobs();
		}
		return null;		
	}

	public PrintServiceAttributeSet getAttributes() {
		PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();
		attrs.add(getPrinterName());
		attrs.add(getPrinterIsAcceptingJobs());
		attrs.add(getQueuedJobCount());
		return AttributeSetUtilities.unmodifiableView(attrs);
	}

	public Object getDefaultAttributeValue(Class category) {
		if (category == null) {
			throw new NullPointerException("null category");
		}
		if (!Attribute.class.isAssignableFrom(category)) {
			throw new IllegalArgumentException(category +
											   " is not an Attribute");
		}
		if (!isAttributeCategorySupported(category)) {
			return null;
		}
		// XXX : use undocumented message to determine these?
		if (category == Copies.class) {
			return new Copies(1);
		} else if (category == Chromaticity.class) {
			return Chromaticity.COLOR;
		} else if (category == Destination.class) {
			return new Destination((new File("out.ps")).toURI());
		} else if (category == Fidelity.class) {
			return Fidelity.FIDELITY_FALSE;
		} else if (category == JobName.class) {
			return new JobName("Java Printing", null);
		} else if (category == Media.class) {
			return MediaSizeName.NA_LETTER;
		} else if (category == MediaPrintableArea.class) {
			float iw = MediaSize.NA.LETTER.getX(Size2DSyntax.INCH) - 2.0f;
			float ih = MediaSize.NA.LETTER.getY(Size2DSyntax.INCH) - 2.0f;
			return new MediaPrintableArea(0.25f, 0.25f, iw, ih,
										  MediaPrintableArea.INCH);
		} else if (category == OrientationRequested.class) {
			return OrientationRequested.PORTRAIT;
		} else if (category == PageRanges.class) {
			return new PageRanges(1, Integer.MAX_VALUE);
		} else if (category == RequestingUserName.class) {
			String userName = "";
			try {
				userName = System.getProperty("user.name","");
			} catch (SecurityException se) {
			}
			return new RequestingUserName(userName, null);
		} else if (category == SheetCollate.class) {
			return SheetCollate.UNCOLLATED;
		} else if (category == Sides.class) {
			return Sides.ONE_SIDED;
		} else if (category == PrintQuality.class) {
			return PrintQuality.NORMAL;
		} else if (category == PrinterResolution.class) {
			return new PrinterResolution(300, 300, PrinterResolution.DPI);
		}
		return null;
	}
	
	public String getName() {
		return "Haiku Print Server";
	}

	public ServiceUIFactory getServiceUIFactory() {
		// TODO: return new HaikuPrintServiceUIFactory
		return null;
	}

	public Class[] getSupportedAttributeCategories() {
		int totalCats = otherAttrCats.length;
		Class [] cats = new Class[totalCats];
		System.arraycopy(otherAttrCats, 0, cats, 0, otherAttrCats.length);
		return cats;
	}
	
	public Object getSupportedAttributeValues(Class category,
											  DocFlavor flavor,
											  AttributeSet attributes) {
		if (category == null) {
			throw new NullPointerException("null category");
		}
		if (!Attribute.class.isAssignableFrom(category)) {
			throw new IllegalArgumentException(category +
											   " does not implement Attribute");
		}
		if (flavor != null) {
			if (!isDocFlavorSupported(flavor)) {
				throw new IllegalArgumentException(flavor +
												   " is an unsupported flavor");
			} else if (isAutoSense(flavor)) {
				return null;
			}
		}
		if (!isAttributeCategorySupported(category)) {
			return null;
		}
		if (category == Chromaticity.class) {
			if (flavor == null ||
			flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
			flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
			flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
			flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
			flavor.equals(DocFlavor.URL.GIF) ||
			flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
			flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
			flavor.equals(DocFlavor.URL.JPEG) ||
			flavor.equals(DocFlavor.BYTE_ARRAY.PNG) ||
			flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
			flavor.equals(DocFlavor.URL.PNG)) {
				Chromaticity[]arr = new Chromaticity[2];
				arr[0] = Chromaticity.MONOCHROME;
				arr[1] = Chromaticity.COLOR;
				return (arr);
			} else {
				return null;
			}
		} else if (category == Destination.class) {
			return new Destination((new File("out.ps")).toURI());
		} else if (category == JobName.class) {
			return new JobName("Java Printing", null);
		} else if (category == RequestingUserName.class) {
			String userName = "";
			try {
				userName = System.getProperty("user.name", "");
			} catch (SecurityException se) {
			}
			return new RequestingUserName(userName, null);
		} else if (category == OrientationRequested.class) {
			if (flavor == null ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
				flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
				flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
				flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.PNG)	||
				flavor.equals(DocFlavor.URL.GIF) ||
				flavor.equals(DocFlavor.URL.JPEG) ||
				flavor.equals(DocFlavor.URL.PNG)) {
				OrientationRequested []arr = new OrientationRequested[2];
				arr[0] = OrientationRequested.PORTRAIT;
				arr[1] = OrientationRequested.LANDSCAPE;
				return arr;
			} else {
				return null;
			}
		} else if ((category == Copies.class) ||
				   (category == CopiesSupported.class)) {
			return new CopiesSupported(1, MAXCOPIES);
		} else if (category == Media.class) {
			Media []arr = new Media[mediaSizes.length];
			System.arraycopy(mediaSizes, 0, arr, 0, mediaSizes.length);
			return arr;
		} else if (category == Fidelity.class) {
			Fidelity []arr = new Fidelity[2];
			arr[0] = Fidelity.FIDELITY_FALSE;
			arr[1] = Fidelity.FIDELITY_TRUE;
			return arr;
		} else if (category == MediaPrintableArea.class) {
			if (attributes == null) {
				return null;
			}
			MediaSize mediaSize = (MediaSize)attributes.get(MediaSize.class);
			if (mediaSize == null) {
				Media media = (Media)attributes.get(Media.class);
				if (media != null && media instanceof MediaSizeName) {
					MediaSizeName msn = (MediaSizeName)media;
					mediaSize = MediaSize.getMediaSizeForName(msn);
				}
			}
			if (mediaSize == null) {
				return null;
			} else {
				MediaPrintableArea []arr = new MediaPrintableArea[1];
				arr[0] = new MediaPrintableArea(0.25f, 0.25f,
								mediaSize.getX(MediaSize.INCH),
								mediaSize.getY(MediaSize.INCH),
								MediaSize.INCH);
				return arr;
			}
		} else if (category == PageRanges.class) {
			if (flavor == null ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {	
				PageRanges []arr = new PageRanges[1];
				arr[0] = new PageRanges(1, Integer.MAX_VALUE);
				return arr;
			} else {
				return null;
			}
		} else if (category == SheetCollate.class) {
			if (flavor == null ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
				SheetCollate []arr = new SheetCollate[2];
				arr[0] = SheetCollate.UNCOLLATED;
				arr[1] = SheetCollate.COLLATED;
				return arr;
			} else {
				SheetCollate []arr = new SheetCollate[1];
				arr[0] = SheetCollate.UNCOLLATED;
				return arr;
			}
		} else if (category == Sides.class) {
			if (flavor == null ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)) {
				Sides []arr = new Sides[3];
				arr[0] = Sides.ONE_SIDED;
				arr[1] = Sides.TWO_SIDED_LONG_EDGE;
				arr[2] = Sides.TWO_SIDED_SHORT_EDGE;
				return arr;
			} else {
				return null;
			}
		} else if (category == PrinterResolution.class) {
			return new PrinterResolution(300, 300, PrinterResolution.DPI);
		} else if (category == ColorSupported.class) {
			return ColorSupported.SUPPORTED;
		} else if (category == SunAlternateMedia.class) {
			return new SunAlternateMedia(
				   		  (Media)getDefaultAttributeValue(Media.class));
		} else if (category == PrintQuality.class) {
			PrintQuality []arr = new PrintQuality[3];
			arr[0] = PrintQuality.DRAFT;
			arr[1] = PrintQuality.HIGH;
			arr[2] = PrintQuality.NORMAL;
			return arr;
		}
		return null;
	}
	
	public DocFlavor[] getSupportedDocFlavors() {
		int len = supportedDocFlavors.length;
		DocFlavor[] flavors = new DocFlavor[len];
		System.arraycopy(supportedDocFlavors, 0, flavors, 0, len);
		return flavors;
	}
	
	public AttributeSet getUnsupportedAttributes(DocFlavor flavor,
						 AttributeSet attributes) {
		if (flavor != null && !isDocFlavorSupported(flavor)) {
			throw new IllegalArgumentException("flavor " + flavor +
											   "is not supported");
		}
		if (attributes == null) {
			return null;
		}
		Attribute attr;
		AttributeSet unsupp = new HashAttributeSet();
		Attribute []attrs = attributes.toArray();
		for (int i=0; i<attrs.length; i++) {
			try {
			attr = attrs[i];
			if (!isAttributeCategorySupported(attr.getCategory())) {
				unsupp.add(attr);
			} else if (!isAttributeValueSupported(attr, flavor,
												  attributes)) {
				unsupp.add(attr);
			}
			} catch (ClassCastException e) {
			}
		}
		if (unsupp.isEmpty()) {
			return null;
		} else {
			return unsupp;
		}
	}

    public PrintServiceAttributeSet getUpdatedAttributes() {
		PrintServiceAttributeSet currSet = getDynamicAttributes();
		if (lastSet == null) {
		    lastSet = currSet;
		    return AttributeSetUtilities.unmodifiableView(currSet);
		} else {
		    PrintServiceAttributeSet updates =
			new HashPrintServiceAttributeSet();
		    Attribute []attrs = currSet.toArray();
		    Attribute attr;
		    for (int i=0; i<attrs.length; i++) {
				attr = attrs[i];
				if (!lastSet.containsValue(attr)) {
				    updates.add(attr);
				}
		    }
		    lastSet = currSet;
		    return AttributeSetUtilities.unmodifiableView(updates);
		}
    }

	public int hashCode() {
		return this.getClass().hashCode()+getName().hashCode();
	}

	public boolean isAttributeCategorySupported(Class category) {
		if (category == null) {
			throw new NullPointerException("null category");
		}
		if (!(Attribute.class.isAssignableFrom(category))) {
			throw new IllegalArgumentException(category +
							 " is not an Attribute");
		}
		for (int i=0;i<otherAttrCats.length;i++) {
			if (category == otherAttrCats[i]) {
			return true;
			}
		}
		return false;
	}

	public boolean isAttributeValueSupported(Attribute attr,
											 DocFlavor flavor,
											 AttributeSet attributes) {
		if (attr == null) {
			throw new NullPointerException("null attribute");
		}
		if (flavor != null) {
			if (!isDocFlavorSupported(flavor)) {
				throw new IllegalArgumentException(flavor +
												   " is an unsupported flavor");
			} else if (isAutoSense(flavor)) {
				return false;
			}
		}
		Class category = attr.getCategory();
		if (!isAttributeCategorySupported(category)) {
			return false;
		} else if (attr.getCategory() == Chromaticity.class) {
			if ((flavor == null) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
				flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
				flavor.equals(DocFlavor.URL.GIF) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
				flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
				flavor.equals(DocFlavor.URL.JPEG) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.PNG) ||
				flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
				flavor.equals(DocFlavor.URL.PNG)) {
				return attr == Chromaticity.COLOR;
			} else {
				return false;
			} 
		} else if (attr.getCategory() == Copies.class) {
			return isSupportedCopies((Copies)attr);
		} else if (attr.getCategory() == Destination.class) {
			URI uri = ((Destination)attr).getURI();
			if ("file".equals(uri.getScheme()) &&
				!(uri.getSchemeSpecificPart().equals(""))) {
				return true;
			} else {
				return false;
			}
		} else if (attr.getCategory() == Media.class &&
				   attr instanceof MediaSizeName) {
			return isSupportedMedia((MediaSizeName)attr);
		} else if (attr.getCategory() == MediaPrintableArea.class) {
			return isSupportedMediaPrintableArea((MediaPrintableArea)attr);
		} else if (attr.getCategory() == SunAlternateMedia.class) {
			Media media = ((SunAlternateMedia)attr).getMedia();
			return isAttributeValueSupported(media, flavor, attributes);
		} else if (attr.getCategory() == OrientationRequested.class) {
			if (attr == OrientationRequested.REVERSE_PORTRAIT ||
				(flavor != null) &&
				!(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE)||
				flavor.equals(DocFlavor.INPUT_STREAM.GIF) ||
				flavor.equals(DocFlavor.INPUT_STREAM.JPEG) ||
				flavor.equals(DocFlavor.INPUT_STREAM.PNG) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.GIF) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.JPEG) ||
				flavor.equals(DocFlavor.BYTE_ARRAY.PNG)	||
				flavor.equals(DocFlavor.URL.GIF) ||
				flavor.equals(DocFlavor.URL.JPEG) ||
				flavor.equals(DocFlavor.URL.PNG))) {
				return false;
			}
		} else if (attr.getCategory() == PageRanges.class) {
			if (flavor != null &&
				!(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
				return false;
			}
		} else if (attr.getCategory() == SheetCollate.class) {
			if (flavor != null &&
				!(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
				return false;
			}
	 	} else if (attr.getCategory() == Sides.class) {
			if (flavor != null &&
				!(flavor.equals(DocFlavor.SERVICE_FORMATTED.PAGEABLE) ||
				flavor.equals(DocFlavor.SERVICE_FORMATTED.PRINTABLE))) {
				return false;
			}
		} else if (attr.getCategory() == PrinterResolution.class) {
			if (attr instanceof PrinterResolution) {
				return isSupportedResolution((PrinterResolution)attr);
			}
		} else if (attr.getCategory() == ColorSupported.class) {
			if (attr instanceof ColorSupported) {
				return isSupportedColor((ColorSupported)attr);
			}
		}
		return true;
	}

	public boolean isDocFlavorSupported(DocFlavor flavor) {
		for (int f=0; f<supportedDocFlavors.length; f++) {
			if (flavor.equals(supportedDocFlavors[f])) {
			return true;
			}
		}
		return false;
	}

	public void removePrintServiceAttributeListener(
					  PrintServiceAttributeListener listener) {
		synchronized (this) {
			if (listener == null || notifier == null ) {
				return;
			}
			notifier.removeListener(listener);
			if (notifier.isEmpty()) {
				notifier.stopNotifier();
				notifier = null;
			}
		}
	}

    public boolean usesClass(Class c) {
    	throw new RuntimeException("TODO: HaikuPrintService.usesClass");
    }

    public void wakeNotifier() {
		synchronized (this) {
		    if (notifier != null) {
				notifier.wake();
		    }
		}
    }

	/* private functions below */
   
    private PrintServiceAttributeSet getDynamicAttributes() {
		PrintServiceAttributeSet attrs = new HashPrintServiceAttributeSet();
		attrs.add(getQueuedJobCount());
		attrs.add(getPrinterIsAcceptingJobs());
		return attrs;
    }

    private boolean isAutoSense(DocFlavor flavor) {
		if (flavor.equals(DocFlavor.BYTE_ARRAY.AUTOSENSE) ||
		    flavor.equals(DocFlavor.INPUT_STREAM.AUTOSENSE) ||
		    flavor.equals(DocFlavor.URL.AUTOSENSE)) {
		    return true;
		}
		else {
		    return false;
		}
    }

    private PrinterName getPrinterName() {
    	throw new RuntimeException("TODO: HaikuPrintService.getPrinterName");
    }
    
    private QueuedJobCount getQueuedJobCount() {
    	throw new RuntimeException("TODO: HaikuPrintService.getQueuedJobCount");
    }
    
    private PrinterIsAcceptingJobs getPrinterIsAcceptingJobs() {
    	throw new RuntimeException("TODO: HaikuPrintService.getPrinterIsAcceptingJobs");
    }

    private boolean isSupportedColor(ColorSupported colorSupported) {
    	throw new RuntimeException("TODO: HaikuPrintService.isSupportedColor");
    }

    private boolean isSupportedCopies(Copies copies) {
		int numCopies = copies.getValue();
		return (numCopies > 0 && numCopies < MAXCOPIES);
    }

    private boolean isSupportedMedia(MediaSizeName msn) {
    	throw new RuntimeException("TODO: HaikuPrintService.isSupportedMedia");
    }

    private boolean isSupportedMediaPrintableArea(MediaPrintableArea mpa) {
    	throw new RuntimeException("TODO: HaikuPrintService.isSupportedMediaPrintableArea");
    }

    private boolean isSupportedResolution(PrinterResolution pr) {
    	throw new RuntimeException("TODO: HaikuPrintService.isSupportedResolution");
    }
}
