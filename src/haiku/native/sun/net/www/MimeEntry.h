#ifndef MIME_ENTRY_H
#define MIME_ENTRY_H

#include "jvm.h"
#include "jni_util.h"

class MimeEntry {
public:
	static jfieldID typeName_ID;
	static jfieldID action_ID;
	static jfieldID command_ID;
	static jfieldID description_ID;
	static jfieldID imageFileName_ID;
	static jfieldID fileExtensions_ID;
};	

#endif // MIME_ENTRY_H
