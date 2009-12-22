#include <cassert>
#include "MimeEntry.h"

jfieldID MimeEntry::typeName_ID;
jfieldID MimeEntry::action_ID;
jfieldID MimeEntry::command_ID;
jfieldID MimeEntry::description_ID;
jfieldID MimeEntry::imageFileName_ID;
jfieldID MimeEntry::fileExtensions_ID;

#include "sun_net_www_MimeEntry.h"

/*
 * Class:     sun_net_www_MimeEntry
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_net_www_MimeEntry_initIDs
  (JNIEnv * env, jclass klass)
{
	MimeEntry::typeName_ID = env->GetFieldID(klass, "typeName", "Ljava/lang/String;");
	MimeEntry::action_ID = env->GetFieldID(klass, "action", "I");
	MimeEntry::command_ID = env->GetFieldID(klass, "command", "Ljava/lang/String;");
	MimeEntry::description_ID = env->GetFieldID(klass, "description", "Ljava/lang/String;");
	MimeEntry::imageFileName_ID = env->GetFieldID(klass, "imageFileName", "Ljava/lang/String;");
	MimeEntry::fileExtensions_ID = env->GetFieldID(klass, "fileExtensions", "[Ljava/lang/String;");

	assert(MimeEntry::typeName_ID);
	assert(MimeEntry::action_ID);
	assert(MimeEntry::command_ID);
	assert(MimeEntry::description_ID);
	assert(MimeEntry::imageFileName_ID);
	assert(MimeEntry::fileExtensions_ID);
}
