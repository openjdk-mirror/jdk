#include "MenuComponentAdapter.h"
#include <View.h>
#include <cstdio>

MenuComponentAdapter::MenuComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent) 
	: ObjectAdapter(),
	  jparent(NULL)
{
	if (jparent != NULL) {
		this->jparent = jenv->NewGlobalRef(jparent);
	}
	LinkObjects(jenv, jpeer);
}


MenuComponentAdapter::~MenuComponentAdapter()
{
	UnlinkObjects();
	if (jparent != NULL) {
		JNIEnv * jenv = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
		jenv->DeleteGlobalRef(jparent);
		jparent = NULL;
	}
}


// #pragma mark -
//
// JNI
//

#include "java_awt_MenuComponent.h"

jfieldID MenuComponentAdapter::privateKey_ID = NULL;

/*
 * Class:     java_awt_MenuComponent
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_MenuComponent_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	MenuComponentAdapter::privateKey_ID = jenv->GetFieldID(jklass, "privateKey", "Ljava/lang/Object;");
	DASSERT(MenuComponentAdapter::privateKey_ID != NULL);
}

