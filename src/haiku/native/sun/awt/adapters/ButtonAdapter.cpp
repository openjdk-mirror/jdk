#include "ButtonAdapter.h"
#include "Button.h"
#include "EventEnvironment.h"
#include "KeyConversions.h"

/* static */ Button *
ButtonAdapter::NewButton(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	BRect frame = GetFrame(jenv, jpeer);
	// TOTRY: pass label as an argument
	jstring jlabel = (jstring)jenv->GetObjectField(jjavaObject, label_ID);
	const char * label = UseString(jenv, jlabel);
	Button * value = new Button(frame, (label != NULL ? label : ""));
	ReleaseString(jenv, jlabel, label);
	return value;
}


ButtonAdapter::ButtonAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent)
	: ComponentAdapter(jenv, jpeer, jparent, NewButton(jenv, jpeer, jparent)),
	  button(NULL)
{
	button = dynamic_cast<Button*>(GetView());
	DASSERT(button != NULL);
	button->SetAdapter(this);
}


ButtonAdapter::~ButtonAdapter()
{
	// TODO: determine when (if) Button should be deleted
}


// #pragma mark -
//
// JNI entry points
//

void
ButtonAdapter::enable()
{
	if (button->LockLooper()) {
		button->SetEnabled(true);
		button->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ButtonAdapter::disable()
{
	if (button->LockLooper()) {
		button->SetEnabled(false);
		button->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ButtonAdapter::setLabel(char * label)
{
	if (button->LockLooper()) {
		button->SetLabel(label);
		button->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


BPoint
ButtonAdapter::getMinimumSize()
{
	float width, height;
	button->GetPreferredSize(&width, &height);
	return BPoint(width, height);
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
ButtonAdapter::InvocationReceived(BMessage * message)
{
	PRINT_PRETTY_FUNCTION();
	// parse the message
	int64 when;
	if (message->FindInt64("when", &when) != B_OK) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	jint jmodifiers = ConvertModifiersToJava(modifiers());
	// build the event
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jobject jtarget = GetTarget(jenv);
	jstring jcommand = (jstring)jenv->CallObjectMethod(jtarget, command_ID);
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

#include "java_awt_Button.h"

jfieldID  ButtonAdapter::label_ID = NULL;
jmethodID ButtonAdapter::command_ID = NULL;

/*
 * Class:     java_awt_Button
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Button_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	ButtonAdapter::label_ID   = jenv->GetFieldID(jklass, "label", "Ljava/lang/String;");
	ButtonAdapter::command_ID = jenv->GetMethodID(jklass, "getActionCommand", "()Ljava/lang/String;");
	DASSERT(ButtonAdapter::label_ID   != NULL);
	DASSERT(ButtonAdapter::command_ID != NULL);
}

