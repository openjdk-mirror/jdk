#ifndef FILENAME_FILTER
#define FILENAME_FILTER

#include <OS.h>
#include <storage/FilePanel.h>
#include "jni.h"

class FilenameFilter : public BRefFilter {
private:
	jobject jfilter;

public:
	FilenameFilter(JNIEnv * jenv, jobject jfilter);
	virtual ~FilenameFilter();
	virtual	bool Filter(const entry_ref * ref, BNode * node,
	                    struct stat * st, const char * mimetype);

public: 	// -- Field and Method ID cache
	static jmethodID accept_ID;      // java.io.FilenameFilter.accept
	static jclass    file_ID;        // java.io.File
	static jmethodID constructor_ID; // java.io.File.<init>
};

#endif // FILENAME_FILTER
