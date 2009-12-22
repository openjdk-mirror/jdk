#ifndef AWT_POINT_H
#define AWT_POINT_H

#include <jni.h>

/*
 * java.awt.Point class.
 * Unfortunately, Point dosen't have InitIDs.
 */
class AwtPoint {
public:
	static jmethodID setLocationDouble_ID;
	
	// Calls java.awt.Point.setLocation(double, double);
	static void SetLocation(JNIEnv *jenv, jobject point, double x, double y);
	static void SetLocation(JNIEnv *jenv, jobject point, float x, float y);
};

#endif // AWT_POINT_H
