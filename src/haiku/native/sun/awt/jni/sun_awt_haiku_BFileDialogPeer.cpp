#include "sun_awt_haiku_BFileDialogPeer.h"
#include "FileDialogAdapter.h"
#include "FilenameFilter.h"
#include <interface/Window.h>

jmethodID FilenameFilter::accept_ID      = NULL;
jclass    FilenameFilter::file_ID        = NULL;
jmethodID FilenameFilter::constructor_ID = NULL;
jmethodID FileDialogAdapter::hideAndDisposeHandler_ID = NULL;

/*
 * Class:     sun_awt_haiku_BFileDialogPeer
 * Method:    initIDs
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFileDialogPeer_initIDs
  (JNIEnv * jenv, jclass jFileDialogPeerClass, 
                  jclass jFilenameFilterClass,
                  jclass jFileClass, jclass fDialogClass)
{
	FilenameFilter::accept_ID
		= jenv->GetMethodID(jFilenameFilterClass, "accept", 
		                    "(Ljava/io/File;Ljava/lang/String;)Z");
	FilenameFilter::file_ID
		= (jclass)jenv->NewGlobalRef(jFileClass);
	FilenameFilter::constructor_ID
		= jenv->GetMethodID(jFileClass, "<init>", "(Ljava/lang/String;)V");
	FileDialogAdapter::hideAndDisposeHandler_ID
		= jenv->GetMethodID(fDialogClass, "hideAndDisposeHandler", "()V");
	DASSERT(FilenameFilter::accept_ID      != NULL);
	DASSERT(FilenameFilter::file_ID        != NULL);
	DASSERT(FilenameFilter::constructor_ID != NULL);
	DASSERT(FileDialogAdapter::hideAndDisposeHandler_ID != NULL);
}


/*
 * Class:     sun_awt_haiku_BFileDialogPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFileDialogPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BFilePanel * filePanel = FileDialogAdapter::NewFilePanel(jenv, jpeer, jparent);
	BWindow * window = filePanel->Window();
	if (window->LockLooper()) {
		new FileDialogAdapter(jenv, jpeer, jparent, filePanel);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	ObjectAdapter::getAdapter<FileDialogAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BFileDialogPeer
 * Method:    _setFile
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFileDialogPeer__1setFile
  (JNIEnv * jenv, jobject jpeer, jstring jfile)
{
	FileDialogAdapter * adapter = ObjectAdapter::getAdapter<FileDialogAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * file = UseString(jenv, jfile);
	if (file != NULL) {
		adapter->setFile((char*)file);
		ReleaseString(jenv, jfile, file);
	} else {
		adapter->setFile("");
	}
}


/*
 * Class:     sun_awt_haiku_BFileDialogPeer
 * Method:    _setDirectory
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFileDialogPeer__1setDirectory
  (JNIEnv * jenv, jobject jpeer, jstring jdirectory)
{
	FileDialogAdapter * adapter = ObjectAdapter::getAdapter<FileDialogAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * directory = UseString(jenv, jdirectory);
	if (directory != NULL) {
		adapter->setDirectory((char*)directory);
		ReleaseString(jenv, jdirectory, directory);
	} else {
		adapter->setDirectory("");
	}
}


/*
 * Class:     sun_awt_haiku_BFileDialogPeer
 * Method:    _setFilenameFilter
 * Signature: (Ljava/io/FilenameFilter;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFileDialogPeer__1setFilenameFilter
  (JNIEnv * jenv, jobject jpeer, jobject jfilter)
{
	FileDialogAdapter * adapter = ObjectAdapter::getAdapter<FileDialogAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	if (jfilter == NULL) {
		// delete existing filter (if any)
		adapter->setFilenameFilter(NULL);
	} else {
		// this call will create a global reference to the filter
		adapter->setFilenameFilter(new FilenameFilter(jenv, jfilter));
	}
}
