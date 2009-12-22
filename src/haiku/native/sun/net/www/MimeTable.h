#ifndef MIME_TABLE_H
#define MIME_TABLE_H

#include "jvm.h"
#include "jni_util.h"

class MimeTable {
public:
	static jfieldID entries_ID;
	static jfieldID extensionMap_ID;
};	

#endif // MIME_TABLE_H
