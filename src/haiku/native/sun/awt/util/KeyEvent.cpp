#include "KeyEvent.h"
#include "debug_util.h"

jfieldID KeyEvent::isProxyActive_ID = NULL;
jfieldID KeyEvent::keyCode_ID = NULL;
jfieldID KeyEvent::keyChar_ID = NULL;
jfieldID KeyEvent::keyLocation_ID = NULL;

#include "java_awt_event_KeyEvent.h"

/*
 * Class:     java_awt_KeyEvent
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_event_KeyEvent_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    KeyEvent::isProxyActive_ID = jenv->GetFieldID(jklass, "isProxyActive", "Z");
    KeyEvent::keyCode_ID = jenv->GetFieldID(jklass, "keyCode", "I");
    KeyEvent::keyChar_ID = jenv->GetFieldID(jklass, "keyChar", "C");
    KeyEvent::keyLocation_ID = jenv->GetFieldID(jklass, "keyLocation", "I");
    DASSERT(KeyEvent::isProxyActive_ID != NULL);
    DASSERT(KeyEvent::keyCode_ID != NULL);
    DASSERT(KeyEvent::keyChar_ID != NULL);
    DASSERT(KeyEvent::keyLocation_ID != NULL);
}
