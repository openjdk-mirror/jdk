#ifndef CURSOR_H
#define CURSOR_H

#include <jni.h>
#include <jni_util.h>
#include <app/Cursor.h>
#include "java_awt_Cursor.h" // useful constants are in here

class Cursor {
public:
	/* java.awt.Cursor field ids */
	static jfieldID  type_ID;
	static jfieldID  pointer_ID; // pData
	static jfieldID  name_ID;

	static const BCursor * GetCursor(JNIEnv * jenv, jobject jcursor);
	static const BCursor * GetCursor(jint jcursor);
	static jobject   NewCursor(JNIEnv * jenv, BCursor * cursor);
	static jint      NewCursor(BCursor * cursor);
	static void      FinalizeCursor(JNIEnv * jenv, jobject jcursor);
};

#endif // CURSOR_H
