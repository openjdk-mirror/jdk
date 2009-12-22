package sun.awt.haiku;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import sun.awt.datatransfer.*;
import java.io.*;
import java.util.*;

public class BDataTransferer extends DataTransferer {
	private static class DataType {
		// indexes
		private static final ArrayList list = new ArrayList();
		private static final Hashtable table = new Hashtable();
		private static int counter = 0;
		// retrieval functions
		public static DataType getDataType(int index) {
			return (DataType)list.get(index);
		}
		public static DataType getDataType(String name) {
			return (DataType)table.get(name);
		}
		// instance data
		private String string;
		private int index = counter++;
		public DataType(String string) {
			this.string = string;
			list.add(this);
			table.put(string, this);
		}
		// accessors
		public int Index() {
			return index;
		}
		public String String() {
			return string;
		}
	};

	private static final DataType BITMAP_IMAGE = new DataType("image/x-vnd.Be-bitmap");
	private static final DataType HTML_TEXT    = new DataType("text/html");
	private static final DataType STYLED_TEXT  = new DataType("application/x-vnd.Be-text_run_array");
	private static final DataType PLAIN_TEXT   = new DataType("text/plain");

	private static BDataTransferer transferer;
	
	public static BDataTransferer getInstanceImpl() {
		if (transferer == null) {
			synchronized (BDataTransferer.class) {
				if (transferer == null) {
					transferer = new BDataTransferer();
				}
			}
		}
		return transferer;
	}
	
	public String getDefaultUnicodeEncoding() {
		return "UTF8";
	}
	
	public boolean isLocaleDependentTextFormat(long format) {
		return false;
	}
	
	public boolean isFileFormat(long format) {
		DataType type = DataType.getDataType((int)format);
		if (type == null) {
			return false;
		}
		String typeString = type.String();
		DataFlavor fileFormat = DataFlavor.javaFileListFlavor;
		String fileString = fileFormat.getMimeType();
System.err.println("VERIFY: isFileFormat(" + typeString + "), " + fileString);
		return typeString.equals(fileString);
	}
	
	public boolean isImageFormat(long format) {
		DataType type = DataType.getDataType((int)format);
		if (type == null) {
			return false;
		}
		String string = type.String();
System.err.println("VERIFY: isImageFormat(" + string + ")");
		return string.startsWith("image/");
	}
	
	protected Long getFormatForNativeAsLong(String str) {
		DataType type = DataType.getDataType(str);
		if (type == null) {
			type = new DataType(str);
		}
		return new Long(type.Index());
	}
	
	protected String getNativeForFormat(long format) {
		return DataType.getDataType((int)format).String();
	}
	
	protected Image platformImageBytesOrStreamToImage(InputStream str, byte[] bytes, long format) throws IOException {
		System.out.println("TODO: platformImageBytesOrStreamToImage");
		throw new IOException("not implemented.");
	}
	
	protected byte[] imageToPlatformBytes(Image image, long format) throws IOException {
		System.out.println("TODO: imageToPlatformBytes");
		throw new IOException("not implemented.");
	}
	
	public ToolkitThreadBlockedHandler getToolkitThreadBlockedHandler() {
		System.out.println("TODO: ToolkitThreadBlockedHandler");
		return null;
	}

	// called from BClipboard
	public byte[] translateTransferable(Transferable contents, DataFlavor flavor) 
		throws IOException
	{
		String mimeType = flavor.getMimeType();
		long format = getFormatForNativeAsLong(mimeType).longValue();
		System.err.println("mimeType: " + mimeType + " format: " + format);
		return translateTransferable(contents, flavor, format);
	}

	// called from BClipboard
	public Object translateBytes(byte[] bytes, DataFlavor flavor, Transferable localeTransferable) 
		throws IOException
	{
		String mimeType = flavor.getMimeType();
		long format = getFormatForNativeAsLong(mimeType).longValue();
		System.err.println("mimeType: " + mimeType + " format: " + format);
		return translateBytes(bytes, flavor, format, localeTransferable);
	}

}
