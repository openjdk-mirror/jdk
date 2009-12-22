#include "TextAreaAdapter.h"
#include "TextArea.h"
#include "java_awt_TextArea.h"
#include <interface/TextView.h>

/* static */ TextArea *
TextAreaAdapter::NewTextArea(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	// here we find out the scroll bar policy, and do the right thing in the constructor
	int scrollbarVisibility = jenv->GetIntField(jjavaObject, scrollbarVisibility_ID);
	bool showHorizontal = (scrollbarVisibility == java_awt_TextArea_SCROLLBARS_HORIZONTAL_ONLY) ||
	                      (scrollbarVisibility == java_awt_TextArea_SCROLLBARS_BOTH);
	bool showVertical = (scrollbarVisibility == java_awt_TextArea_SCROLLBARS_VERTICAL_ONLY) ||
                        (scrollbarVisibility == java_awt_TextArea_SCROLLBARS_BOTH);

	BRect frame = GetFrame(jenv, jpeer);
	TextArea * value = new TextArea(frame, showHorizontal, showVertical);
	return value;
}


TextAreaAdapter::TextAreaAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent) 
	: TextComponentAdapter(jenv, jpeer, jparent, NewTextArea(jenv, jpeer, jparent)),
	  textArea(NULL)
{
	textArea = dynamic_cast<TextArea*>(GetView());
	DASSERT(textArea != NULL);
	textArea->SetAdapter(this);
}


TextAreaAdapter::~TextAreaAdapter()
{
	// TODO: delete the TextArea if necessary?
}


// #pragma mark -
//
// JNI entry points
//

/* virtual */ BPoint
TextAreaAdapter::getPreferredSize()
{
	PRINT_PRETTY_FUNCTION();
	float width, height;
	if (textArea->LockLooper()) {
		textArea->GetPreferredSize(&width, &height);
		textArea->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	BPoint size = getMinimumSize();
	width = max_c(width, size.x);
	height = max_c(height, size.y);
	fprintf(stdout, "== %f x %f\n", width, height);
	return BPoint(width, height);
}


/* virtual */ BPoint
TextAreaAdapter::getMinimumSize()
{
	float width = 200, height = 70;
	return BPoint(width, height);
}


void
TextAreaAdapter::insert(char * text, int pos)
{
	BTextView * view = GetTextView();
	if (view->LockLooper()) {
		view->Insert(pos, text, strlen(text));
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
TextAreaAdapter::replaceRange(char * text, int start, int end)
{
	BTextView * view = GetTextView();
	if (view->LockLooper()) {
		view->Delete(start, end);
		view->Insert(start, text, strlen(text));
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


BPoint
TextAreaAdapter::getPreferredSize(int rows, int columns)
{
	fprintf(stdout, "%s(%d, %d)\n", __PRETTY_FUNCTION__, rows, columns);
	float width, height;
	if (textArea->LockLooper()) {
		textArea->GetPreferredSize(&width, &height);
		textArea->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	BPoint size = getMinimumSize(rows, columns);
	width = max_c(width, size.x);
	height = max_c(height, size.y);
	fprintf(stdout, "== %f x %f\n", width, height);
	return BPoint(width, height);
}


BPoint
TextAreaAdapter::getMinimumSize(int rows, int columns)
{
	float width = 200, height = 70;
	return BPoint(width, height);
}


// #pragma mark -
//
// JNI
//

#include "java_awt_TextArea.h"

jfieldID TextAreaAdapter::scrollbarVisibility_ID = NULL;

/*
 * Class:     java_awt_TextArea
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_TextArea_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	TextAreaAdapter::scrollbarVisibility_ID = jenv->GetFieldID(jklass, "scrollbarVisibility", "I");
	DASSERT(TextAreaAdapter::scrollbarVisibility_ID);
}

