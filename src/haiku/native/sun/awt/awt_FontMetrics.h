#ifndef AWT_FONT_METRICS_H
#define AWT_FONT_METRICS_H

#include <jni.h>

class AwtFontMetrics {
	public:
		static void makeMetrics(JNIEnv * env, jobject metrics);

		/* java.awt.FontMetrics field ids */
		static jfieldID font_ID;

		/* sun.awt.haiku.BFontMetrics field ids */
		static jfieldID widths_ID;
		static jfieldID ascent_ID;
		static jfieldID descent_ID;
		static jfieldID leading_ID;
		static jfieldID height_ID;
		static jfieldID maxAscent_ID;
		static jfieldID maxDescent_ID;
		static jfieldID maxHeight_ID;
		static jfieldID maxAdvance_ID;
};

#endif // AWT_FONT_METRICS_H

