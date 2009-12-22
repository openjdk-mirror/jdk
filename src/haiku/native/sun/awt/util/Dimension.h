#ifndef DIMENSION_H
#define DIMENSION_H

#include <jni.h>
#include <jni_util.h>

class Dimension {
public:
	/* java.awt.Dimension field ids */
	static jfieldID width_ID;
	static jfieldID height_ID;

	static jclass		classReference;
	static jmethodID	constructor_ID;

	static jobject New(JNIEnv * jenv, int width, int height);
};

#endif // DIMENSION_H
