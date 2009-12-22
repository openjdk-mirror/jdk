#include "TextFieldAdapter.h"
#include "TextField.h"
#include "EventEnvironment.h"
#include "KeyConversions.h"

/* static */ TextField * 
TextFieldAdapter::NewTextField(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BRect frame = GetFrame(jenv, jpeer);
	frame.right = max_c(frame.right, frame.left + 10);
	frame.bottom = max_c(frame.bottom, frame.top + 10);
	TextField * value = new TextField(frame);
	return value;

}


TextFieldAdapter::TextFieldAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent)
	: TextComponentAdapter(jenv, jpeer, jparent, NewTextField(jenv, jpeer, jparent)),
	  textField(NULL)
{
	textField = dynamic_cast<TextField*>(GetView());
	DASSERT(textField != NULL);
	textField->SetAdapter(this);
}


TextFieldAdapter::~TextFieldAdapter()
{
	// TODO: delete the TextField if necessary?
}


// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
TextFieldAdapter::enable()
{
	if (textField->LockLooper()) {
		textField->SetEnabled(true);
		textField->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


/* virtual */ void
TextFieldAdapter::disable()
{
	if (textField->LockLooper()) {
		textField->SetEnabled(false);
		textField->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


/* virtual */ BPoint
TextFieldAdapter::getPreferredSize()
{
	TODO();
	BFont font;
	textField->GetFont(&font);
	font_height height;
	font.GetHeight(&height);
	float field_height = height.ascent + height.descent + height.leading + 7.0;
	return BPoint(200, field_height);
}


/* virtual */ BPoint
TextFieldAdapter::getMinimumSize()
{
	TODO();
	BFont font;
	textField->GetFont(&font);
	font_height height;
	font.GetHeight(&height);
	float field_height = height.ascent + height.descent + height.leading + 7.0;
	return BPoint(200, field_height);
}


void
TextFieldAdapter::setEchoChar(char echoChar)
{
	TODO();
}


BPoint
TextFieldAdapter::getPreferredSize(int columns)
{
	TODO();
	return getPreferredSize();
}


BPoint
TextFieldAdapter::getMinimumSize(int columns)
{
	TODO();
	return getMinimumSize();
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
TextFieldAdapter::InvocationReceived(BMessage * message)
{
	PRINT_PRETTY_FUNCTION();
	// parse the message
	int64 when;
	if (message->FindInt64("when", &when) != B_OK) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	// build the event
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jint jmodifiers = ConvertModifiersToJava(modifiers());
	jstring jcommand = jenv->NewStringUTF(textField->Text());
	jobject jtarget = GetTarget(jenv);
	jevent = environment->NewActionEvent(jtarget, java_awt_event_ActionEvent_ACTION_PERFORMED,
	                                              jcommand, when, jmodifiers);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// send out the appropriate event
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
}


// #pragma mark -
//
// JNI
//

#include "java_awt_TextField.h"

/*
 * Class:     java_awt_TextField
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_TextField_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// nothing to do
}

