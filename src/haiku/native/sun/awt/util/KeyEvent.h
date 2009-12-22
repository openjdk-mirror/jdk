#ifndef KEY_EVENT_H
#define KEY_EVENT_H

#include "jni.h"

class KeyEvent {
public:
	//
	// java.awt.KeyEvent
	//

	static jfieldID 	isProxyActive_ID;
	static jfieldID 	keyCode_ID;
	static jfieldID 	keyChar_ID;
	static jfieldID 	keyLocation_ID;
};

#endif // KEY_EVENT_H
