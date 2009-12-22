#ifndef SUN_AWT_UTIL_UTILS_H
#define SUN_AWT_UTIL_UTILS_H

#include "jni.h"
#include "jni_util.h"
#include <app/Message.h>

extern JavaVM * jvm;

// return the environment for this thread
JNIEnv * GetEnv();

// get a C-style string for a java string
const char * UseString(JNIEnv * jenv, jstring jtext);

// release a C-style string from a java string
void ReleaseString(JNIEnv * jenv, jstring jtext, const char * text);

// returns true if the message is never used by us
bool IsUselessMessage(BMessage * message);

#endif // SUN_AWT_UTIL_UTILS_H
