package sun.awt.haiku;

import sun.awt.PlatformFont;
import sun.io.CharToByteConverter;

import java.awt.*;
import java.awt.peer.*;

class BFontPeer extends PlatformFont implements FontPeer {
	BFontPeer(String name, int style) {
		super(name, style);
		System.err.println("name : "+name+" style : "+style);
	}

	protected CharToByteConverter getFontCharset(String charsetName,
							   String fontName) {
		System.out.println("TODO: BFontPeer.getFontCharset(\""+charsetName+"\",\""+fontName+"\")");
		return null;
	}

}
