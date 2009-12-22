#ifndef AWT_EVENT_H
#define AWT_EVENT_H

#include <jni.h>
#include <jni_util.h>
#include "java_awt_AWTEvent.h" // useful constants are in here

class AwtEvent {
public:
	/* java.awt.AWTEvent field ids */
	static jfieldID bdata_ID;
	static jfieldID id_ID;
	static jfieldID consumed_ID;
};

#endif // AWT_EVENT_H
