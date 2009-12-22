#include "LabelAdapter.h"
#include "Label.h"
#include <cstdio>

/* static */ Label * 
LabelAdapter::NewLabel(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	BRect frame = GetFrame(jenv, jpeer);
	// TOTRY: pass text as an argument
	jstring jtext = (jstring)jenv->GetObjectField(jjavaObject, text_ID);
	const char * text = (jtext != NULL ? jenv->GetStringUTFChars(jtext, NULL) : NULL);
	Label * value = new Label(frame, (char*)text);
	if (text != NULL) {
		jenv->ReleaseStringUTFChars(jtext, text);
	}
	return value;
}


LabelAdapter::LabelAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent) 
	: ComponentAdapter(jenv, jpeer, jparent, NewLabel(jenv, jpeer, jparent)),
	  label(NULL)
{
	label = dynamic_cast<Label*>(GetView());
	DASSERT(label != NULL);
	label->SetAdapter(this);
}


LabelAdapter::~LabelAdapter()
{
}


// #pragma mark -
//
// JNI entry points
//

void
LabelAdapter::enable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


void
LabelAdapter::disable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


void
LabelAdapter::setText(char * text)
{
	if (label->LockLooper()) {
		label->SetText(text);
		label->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
LabelAdapter::setAlignment(alignment flag)
{
	if (label->LockLooper()) {
		label->SetAlignment(flag);
		label->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


BPoint
LabelAdapter::getMinimumSize()
{
	float width, height;
	label->GetPreferredSize(&width, &height);
	return BPoint(width, height);
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Label.h"

jfieldID LabelAdapter::text_ID      = NULL;
jfieldID LabelAdapter::alignment_ID = NULL;

/*
 * Class:     java_awt_Label
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Label_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	LabelAdapter::text_ID      = jenv->GetFieldID(jklass, "text", "Ljava/lang/String;");
	LabelAdapter::alignment_ID = jenv->GetFieldID(jklass, "alignment", "I");
	DASSERT(LabelAdapter::text_ID      != NULL);
	DASSERT(LabelAdapter::alignment_ID != NULL);
}

