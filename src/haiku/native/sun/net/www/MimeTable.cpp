#include "MimeTable.h"
#include "MimeEntry.h"
#include <cassert>
#include <cstdio>
#include <Mime.h>
#include <String.h>

jfieldID MimeTable::entries_ID;
jfieldID MimeTable::extensionMap_ID;

#include "sun_net_www_MimeTable.h"
#include "sun_net_www_MimeEntry.h"

/*
 * Class:     sun_net_www_MimeTable
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_net_www_MimeTable_initIDs
  (JNIEnv * env, jclass klass)
{
	MimeTable::entries_ID = env->GetFieldID(klass, "entries", "Ljava/util/Hashtable;");
	MimeTable::extensionMap_ID = env->GetFieldID(klass, "extensionMap", "Ljava/util/Hashtable;");

	assert(MimeTable::entries_ID);
	assert(MimeTable::extensionMap_ID);
}


/*
 * Class:     sun_net_www_MimeTable
 * Method:    _addNativeTypes
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_net_www_MimeTable__1addNativeTypes
  (JNIEnv * env, jobject self)
{
	fprintf(stderr, "Java_sun_net_www_MimeTable__1addNativeTypes\n");
	BMessage types;
	if (BMimeType::GetInstalledTypes(&types) != B_OK) {
		return; // silently fail
	}
	int token = 0;
	BString type;
	while (types.FindString("types", token++, &type) == B_OK) {
		BMimeType mimeType(type.String());
		if (mimeType.InitCheck() != B_OK) {
			continue;
		}
		// TODO: create a new sun.net.www.MimeEntry, populate it,
		//       and add it to both the entries and extensionMap hashtables.
	}
}


/*
 * Class:     sun_net_www_MimeTable
 * Method:    _fillNativeFields
 * Signature: (Lsun/net/www/MimeEntry;)V
 */
JNIEXPORT void JNICALL Java_sun_net_www_MimeTable__1fillNativeFields
  (JNIEnv * env, jobject self, jobject entry)
{
//	fprintf(stderr, "Java_sun_net_www_MimeTable__1fillNativeFields\n");
	jstring jMimeType = (jstring)env->GetObjectField(entry, MimeEntry::typeName_ID);
	const char * cMimeType = env->GetStringUTFChars(jMimeType, NULL);
	BMimeType mimeType(cMimeType);
	env->ReleaseStringUTFChars(jMimeType, cMimeType);
	if (mimeType.InitCheck() != B_OK) {
		return;
	}
	if (!mimeType.IsInstalled()) {
//		fprintf(stderr, "mimetype not installed: %s\n", mimeType.Type());
		return;
	}

	char description[B_MIME_TYPE_LENGTH];
	if ((mimeType.GetLongDescription(description) == B_OK) ||
	    (mimeType.GetShortDescription(description) == B_OK)) {
		env->SetObjectField(entry, MimeEntry::description_ID, 
		                    JNU_NewStringPlatform(env, description));
    }

	BMessage extensions;
	if (mimeType.GetFileExtensions(&extensions) == B_OK) {
		BString extensionList;
		int token = 0;
		BString extension;
		bool comma = false;
		while (extensions.FindString("extensions", token++, &extension) == B_OK) {
			if (comma) {
				extensionList << ",";
			} else {
				comma = true;
			}
			extensionList << extension;
		}
		if (token > 1) {
			// TODO: combine with the existing extensions on the MimeEntry
			env->SetObjectField(entry, MimeEntry::fileExtensions_ID,
			                    JNU_NewStringPlatform(env, extensionList.String()));
		}
	}

	// TODO: add more native goodness to the fields
}

