#ifndef KEY_CONVERSIONS
#define KEY_CONVERSIONS

#include <jni.h>
#include <SupportDefs.h>
#include <InterfaceDefs.h> // for B_SHIFT_KEY
#include <String.h>

int32 ConvertKeyCodeToNative(jint jkeycode);

void ConvertKeyCodeToJava(int32 keycode, uint32 modifiers, jint *jkeyCode, jint *jkeyLocation);

void GetKeyChar(BString *keyChar, int32 keycode, int32 modifiers = B_SHIFT_KEY);

// use this for general Events
jint ConvertModifiersToJava(uint32 modifiers);

// use this for InputEvents (or its subclasses: MouseEvent or KeyEvent)
jint ConvertInputModifiersToJava(uint32 modifiers);

#endif // KEY_CONVERSIONS
