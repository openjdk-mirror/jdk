#include "Utils.h"

JNIEnv * GetEnv() {
	JNIEnv * env;
	jvm->AttachCurrentThreadAsDaemon((void**)&env, NULL);
	return env;
}


const char *
UseString(JNIEnv * jenv, jstring jtext)
{
	if (jtext == NULL) {
		return NULL;
	}
	return jenv->GetStringUTFChars(jtext, NULL);
}


void
ReleaseString(JNIEnv * jenv, jstring jtext, const char * text)
{
	if (text != NULL) {
		jenv->ReleaseStringUTFChars(jtext, text);
	}
}


#include <app/AppDefs.h>
#include <interface/OptionControl.h>

bool
IsUselessMessage(BMessage * message)
{
	switch (message->what) {
	case B_OPTION_CONTROL_VALUE:
	case 'TICK':
	case _PING_:
		return true;
	default:
		return false;
	}
}
