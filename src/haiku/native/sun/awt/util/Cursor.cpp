#include "Cursor.h"
#include "Debug.h"
#include <cstdio>

const BCursor * 
Cursor::GetCursor(JNIEnv * jenv, jobject jcursor)
{
	jint c = jenv->GetIntField(jcursor, type_ID);
	if (c == java_awt_Cursor_CUSTOM_CURSOR) {
		fprintf(stderr, "warning: custom cursor not supported?\n");
		return (BCursor*) jenv->GetLongField(jcursor, pointer_ID);
	}
	return GetCursor(c);
}


const BCursor * 
Cursor::GetCursor(jint jcursor)
{
	switch (jcursor) {
	case java_awt_Cursor_DEFAULT_CURSOR:
	case java_awt_Cursor_CROSSHAIR_CURSOR:
		return B_CURSOR_SYSTEM_DEFAULT;
	case java_awt_Cursor_TEXT_CURSOR:
		return B_CURSOR_I_BEAM;
	case java_awt_Cursor_WAIT_CURSOR:
	case java_awt_Cursor_SW_RESIZE_CURSOR:
	case java_awt_Cursor_SE_RESIZE_CURSOR:
	case java_awt_Cursor_NW_RESIZE_CURSOR:
	case java_awt_Cursor_NE_RESIZE_CURSOR:
	case java_awt_Cursor_N_RESIZE_CURSOR:
	case java_awt_Cursor_S_RESIZE_CURSOR:
	case java_awt_Cursor_W_RESIZE_CURSOR:
	case java_awt_Cursor_E_RESIZE_CURSOR:
	case java_awt_Cursor_HAND_CURSOR:
	case java_awt_Cursor_MOVE_CURSOR:
		return B_CURSOR_SYSTEM_DEFAULT;
	default:
		fprintf(stderr, "unexpected cursor type in %s\n", __PRETTY_FUNCTION__);
	}
	return NULL;
}


jobject
Cursor::NewCursor(JNIEnv * jenv, BCursor * cursor)
{
	DEBUGGER();
	return NULL;
}


jint
Cursor::NewCursor(BCursor * cursor)
{
	DEBUGGER();
	return 0;
}


void
Cursor::FinalizeCursor(JNIEnv * jenv, jobject jcursor)
{
	PRINT_PRETTY_FUNCTION();
	jint c = jenv->GetIntField(jcursor, type_ID);
	if (c == java_awt_Cursor_CUSTOM_CURSOR) {
		delete (BCursor*) jenv->GetLongField(jcursor, pointer_ID);
	}
}


// #pragma mark -
//
// JNI
//

jfieldID  Cursor::type_ID    = NULL;
jfieldID  Cursor::pointer_ID = NULL;
jfieldID  Cursor::name_ID    = NULL;

#include "java_awt_Cursor.h"

/*
 * Class:     java_awt_Cursor
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Cursor_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    Cursor::type_ID    = jenv->GetFieldID(jklass, "type", "I");
	Cursor::pointer_ID = jenv->GetFieldID(jklass, "pData", "J");
    Cursor::name_ID    = jenv->GetFieldID(jklass, "name", "Ljava/lang/String;");
    DASSERT(Cursor::type_ID    != NULL);
    DASSERT(Cursor::pointer_ID != NULL);
    DASSERT(Cursor::name_ID    != NULL);
}


/*
 * Class:     java_awt_Cursor
 * Method:    finalizeImpl
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Cursor_finalizeImpl
  (JNIEnv * jenv, jobject jcursor)
{
	Cursor::FinalizeCursor(jenv, jcursor);
}


