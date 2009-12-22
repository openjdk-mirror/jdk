#ifndef INSETS_H
#define INSETS_H

#include <jni.h>
#include <jni_util.h>
#include <interface/Rect.h>

class Insets {
public:
	/* java.awt.Insets field ids */
	static jfieldID top_ID;
	static jfieldID left_ID;
	static jfieldID bottom_ID;
	static jfieldID right_ID;

	static jclass		classReference;
	static jmethodID	constructor_ID;

	static jobject New(JNIEnv * jenv, BRect insets);
};

#endif // INSETS_H
