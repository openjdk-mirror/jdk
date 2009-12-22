package sun.awt.haiku;

import java.awt.print.Paper;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import sun.print.RasterPrinterJob;


/* Stubbed out BPrinterJob class */
public class BPrinterJob extends RasterPrinterJob {
	protected double getXRes() {
		return 0.0;
	}
	
	protected double getYRes() {
		return 0.0;
	}
	
	protected double getPhysicalPrintableX(Paper P) {
		return 0.0;
	}
		
	protected double getPhysicalPrintableY(Paper P) {
		return 0.0;
	}
	
	protected double getPhysicalPrintableWidth(Paper p) {
		return 0.0;
	}
	
	protected double getPhysicalPrintableHeight(Paper p) {
		return 0.0;
	}
	
	protected double getPhysicalPageWidth(Paper p) {
		return 0.0;
	}
	
	protected double getPhysicalPageHeight(Paper p) {
		return 0.0;
	}
	
	protected void startPage(PageFormat format, Printable painter,
							 int index) throws PrinterException 
	{
		throw new PrinterException("Haiku Printing not Implemented");
	}
	
	protected void endPage(PageFormat format, Printable painter,
							int index) throws PrinterException
	{
		throw new PrinterException("Haiku Printing not Implemented");
	}
	
	protected void printBand(byte[] data, int x, int y,
							int width, int height) throws PrinterException
	{
		throw new PrinterException("Haiku Printing not Implemented");
	}
	
	protected void startDoc() throws PrinterException {
		throw new PrinterException("Haiku Printing not Implemented");
	}
	
	protected void endDoc() throws PrinterException {
		throw new PrinterException("Haiku Printing not Implemented");
	}
	
	protected void abortDoc() {
		return;
	}
}
