#include "AwtPoint.h"

jmethodID AwtPoint::setLocationDouble_ID = NULL;

void AwtPoint::SetLocation(JNIEnv *jenv, jobject point, double x, double y) {
	if (AwtPoint::setLocationDouble_ID == NULL) {
		jclass pointKlass = jenv->GetObjectClass(point);
		if (pointKlass != NULL) {
			AwtPoint::setLocationDouble_ID = jenv->GetMethodID(pointKlass, "setLocation", "(DD)V");
			jenv->DeleteLocalRef(pointKlass);
		}
	}
	
	jenv->CallVoidMethod(point, AwtPoint::setLocationDouble_ID, (jdouble)x, (jdouble)y);
}

void AwtPoint::SetLocation(JNIEnv *jenv, jobject point, float x, float y) {
	AwtPoint::SetLocation(jenv, point, (double)x, (double)y);
}
