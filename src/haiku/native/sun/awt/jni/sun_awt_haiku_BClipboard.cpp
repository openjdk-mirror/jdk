#include "sun_awt_haiku_BClipboard.h"
#include "Debug.h"
#include "Utils.h"
#include <app/Clipboard.h>

/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BClipboard_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// nothing to do
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _createNativeClipboard
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_haiku_BClipboard__1createNativeClipboard
  (JNIEnv * jenv, jobject jpeer, jstring jname)
{
	const char * name = UseString(jenv, jname);
	BClipboard * clipboard = new BClipboard(name ? name : "");
	ReleaseString(jenv, jname, name);
	return (jlong)clipboard;
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _lock
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_haiku_BClipboard__1lock
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	if (clipboard->IsLocked()) {
		return false;
	}
	return clipboard->Lock();
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _clear
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BClipboard__1clear
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	clipboard->Clear();
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _addData
 * Signature: (JLjava/lang/String;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BClipboard__1addData
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard, jstring jmime, jbyteArray jbytes, jint length)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	jbyte bytes[length];
	jenv->GetByteArrayRegion(jbytes, 0, length, bytes);
	BMessage * data = clipboard->Data();
	if (data == NULL) {
		return;
	}
	const char * mime = UseString(jenv, jmime);
	if (mime == NULL) {
		return;
	}
	data->AddData(mime, B_MIME_TYPE, bytes, length);
	ReleaseString(jenv, jmime, mime);
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _commit
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BClipboard__1commit
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	clipboard->Commit();
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _unlock
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BClipboard__1unlock
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	clipboard->Unlock();
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _disposeImpl
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BClipboard__1disposeImpl
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	delete clipboard;
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _getType
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_haiku_BClipboard__1getType
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard, jint i)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	BMessage * data = clipboard->Data();
	if (data == NULL) {
		return NULL;
	}
	char * name;
	uint32 type;
	int32 count;
	status_t status = data->GetInfo(B_MIME_TYPE, i, &name, &type, &count);
	if (status != B_OK) {
		return NULL;
	}
	return jenv->NewStringUTF(name);
}


/*
 * Class:     sun_awt_haiku_BClipboard
 * Method:    _findData
 * Signature: (JLjava/lang/String;I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_awt_haiku_BClipboard__1findData
  (JNIEnv * jenv, jobject jpeer, jlong jclipboard, jstring jmimetype, jint i)
{
	BClipboard * clipboard = (BClipboard *)jclipboard;
	DASSERT(clipboard != NULL);
	BMessage * data = clipboard->Data();
	if (data == NULL) {
		return NULL;
	}
	const char * mimetype = UseString(jenv, jmimetype);
	if (mimetype == NULL) {
		return NULL;
	}
	const void * bytes;
	ssize_t size = -1;
	status_t status = data->FindData(mimetype, B_MIME_TYPE, i, &bytes, &size);
	fprintf(stderr, "findData: type=%s length=%ld\n", mimetype, size);
	ReleaseString(jenv, jmimetype, mimetype);
	if ((status != B_OK) || (size < 0)) {
		return NULL;
	}
	jbyteArray jbytes = jenv->NewByteArray(size);
	jenv->SetByteArrayRegion(jbytes, 0, size, (jbyte*)bytes);
	return jbytes;
}
