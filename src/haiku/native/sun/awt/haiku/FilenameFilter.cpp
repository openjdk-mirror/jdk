#include "FilenameFilter.h"
#include "Utils.h"
#include "Debug.h"
#include <storage/Path.h>

FilenameFilter::FilenameFilter(JNIEnv * jenv, jobject jfilter)
	: jfilter(NULL)
{
	PRINT_PRETTY_FUNCTION();
	this->jfilter = jenv->NewGlobalRef(jfilter);
	DASSERT(jfilter != NULL);
}


FilenameFilter::~FilenameFilter()
{
	PRINT_PRETTY_FUNCTION();
	GetEnv()->DeleteGlobalRef(jfilter);
}


/* virtual */ bool
FilenameFilter::Filter(const entry_ref * ref, BNode * node,
                       struct stat * st, const char * mimetype)
{
	// compute parameters
	BPath path(ref);
	path.GetParent(&path);
	const char * directory = path.Path();
	const char * name = ref->name;
	fprintf(stdout, "d = %s, n = %s\n", directory, name);
	// callback
	JNIEnv * jenv = GetEnv();
	jstring jpath = jenv->NewStringUTF(directory);
	if (jpath == NULL) {
		return true; // failed to allocate string
	}
	jobject jdir = jenv->NewObject(file_ID, constructor_ID, jpath); // java.io.File
	if (jenv->ExceptionCheck()) {
		jenv->ExceptionDescribe();
		jenv->ExceptionClear();
	}
	if (jdir == NULL) {
		return true; // failed to allocate object
	}
	jstring jname = jenv->NewStringUTF(name);
	if (jname == NULL) {
		return true; // failed to allocate string
	}
	DASSERT(accept_ID != NULL);
	bool value = jenv->CallBooleanMethod(jfilter, accept_ID, jdir, jname);
	if (jenv->ExceptionCheck()) {
		jenv->ExceptionDescribe();
		jenv->ExceptionClear();
	}
	jenv->DeleteLocalRef(jname);
	jenv->DeleteLocalRef(jdir);
	jenv->DeleteLocalRef(jpath);
	return value;
}
