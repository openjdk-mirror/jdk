#include "CheckboxAdapter.h"
#include "Checkbox.h"
#include "EventEnvironment.h"
#include "KeyConversions.h"
#include <interface/CheckBox.h>

/* static */ Checkbox *
CheckboxAdapter::NewCheckbox(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	BRect frame = GetFrame(jenv, jpeer);
	// TOTRY: pass label, state, group as an argument
	jstring jlabel = (jstring)jenv->GetObjectField(jjavaObject, label_ID);
	const char * label = UseString(jenv, jlabel);
	jboolean state = jenv->GetBooleanField(jjavaObject, state_ID);
	jboolean radioMode = (jenv->GetObjectField(jjavaObject, group_ID) != NULL);
	Checkbox * value = new Checkbox(frame, (label != NULL ? label : ""), state, radioMode);
	ReleaseString(jenv, jlabel, label);
	return value;
}


CheckboxAdapter::CheckboxAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent)
	: ComponentAdapter(jenv, jpeer, jparent, NewCheckbox(jenv, jpeer, jparent)),
	  checkbox(NULL), jgroup(NULL)
{
	checkbox = dynamic_cast<Checkbox*>(GetView());
	DASSERT(checkbox != NULL);
	checkbox->SetAdapter(this);
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	setCheckboxGroup(jenv, jenv->GetObjectField(jjavaObject, group_ID));
}


CheckboxAdapter::~CheckboxAdapter()
{
	// TODO: determine when (if) Checkbox should be deleted
}


// #pragma mark -
//
// Utility functions
//

void
CheckboxAdapter::UpdateGroup(JNIEnv * jenv)
{
	jobject jtarget = GetTarget(jenv);
	jobject jcurrent = jenv->GetObjectField(jgroup, selectedCheckbox_ID);
	if (jcurrent != NULL) {
		bool targetIsCurrent = jenv->IsSameObject(jcurrent, jtarget);
		if (targetIsCurrent) {
			// done!
			jenv->DeleteLocalRef(jcurrent);
			jcurrent = NULL;
			jenv->DeleteLocalRef(jtarget);
			jtarget = NULL;
			return;
		}
		// get the peer pointer from the checkbox
		jobject jpeer = jenv->GetObjectField(jcurrent, peer_ID);
		// deselect the current checkbox
		CheckboxAdapter * adapter = ObjectAdapter::getAdapter<CheckboxAdapter>(jenv, jpeer);
		if (adapter != NULL) {
			adapter->setState(false);
		}
		jenv->DeleteLocalRef(jcurrent);
		jcurrent = NULL;
	}
	// store the new checkbox (us!)
	jenv->SetObjectField(jgroup, selectedCheckbox_ID, jtarget);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
}


// #pragma mark -
//
// JNI entry points
//

void
CheckboxAdapter::enable()
{
	if (checkbox->LockLooper()) {
		checkbox->SetEnabled(true);
		checkbox->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
CheckboxAdapter::disable()
{
	if (checkbox->LockLooper()) {
		checkbox->SetEnabled(false);
		checkbox->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
CheckboxAdapter::setState(bool state)
{
	if (checkbox->LockLooper()) {
		int32 value = (state ? B_CONTROL_ON : B_CONTROL_OFF);
		checkbox->SetValue(value);
		checkbox->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
CheckboxAdapter::setCheckboxGroup(JNIEnv * jenv, jobject jg)
{
	if (jgroup != NULL) {
		// Set the current checkbox of the old group to null if it is us,
		// then delete our global reference.
		if (checkbox->Value() == B_CONTROL_ON) {
			jenv->SetObjectField(jgroup, selectedCheckbox_ID, NULL);
		}
		jenv->DeleteGlobalRef(jgroup);
		jgroup = NULL;
	}
	if (jg != NULL) {
		// Save a global reference to the new group, and change the group's
		// current checkbox to us if we are selected.
		jgroup = jenv->NewGlobalRef(jg);
		if (checkbox->Value() == B_CONTROL_ON) {
			UpdateGroup(jenv);
		}
	}
}


void
CheckboxAdapter::setLabel(const char * label)
{
	if (checkbox->LockLooper()) {
		checkbox->SetLabel(label);
		checkbox->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


BPoint
CheckboxAdapter::getMinimumSize()
{
	BFont font;
	checkbox->GetFont(&font);
	const char * label = checkbox->Label();
	// instantiate it to get the default size
	BCheckBox checkbox(BRect(0, 0, 0, 0), "", label, NULL);
	checkbox.SetFont(&font);
	float width, height;
	checkbox.GetPreferredSize(&width, &height);
	return BPoint(width, height);
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
CheckboxAdapter::InvocationReceived(BMessage * message)
{
	PRINT_PRETTY_FUNCTION();
	// parse the message
	int32 value;
	if ((message->FindInt32("be:value", &value) != B_OK) ||
	    ((value != B_CONTROL_OFF) && (value != B_CONTROL_ON))) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	const char * label = checkbox->Label();
	bool state = (value == B_CONTROL_ON);
	// build the event
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jstring jlabel = jenv->NewStringUTF(label);
	jint jid = java_awt_event_ItemEvent_ITEM_STATE_CHANGED;
	jint jstateChange = (state ? java_awt_event_ItemEvent_SELECTED
	                           : java_awt_event_ItemEvent_DESELECTED);
	jobject jtarget = GetTarget(jenv);
	jevent = environment->NewItemEvent(jtarget, jid, jlabel, jstateChange);
	jenv->SetBooleanField(jtarget, state_ID, state);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// update the group if necessary
	if (jgroup != NULL) {
		UpdateGroup(jenv);		
	}
	// send out the appropriate event
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Checkbox.h"

jfieldID  CheckboxAdapter::label_ID = NULL;
jfieldID  CheckboxAdapter::state_ID = NULL;
jfieldID  CheckboxAdapter::group_ID = NULL;

/*
 * Class:     java_awt_Checkbox
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Checkbox_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	CheckboxAdapter::label_ID = jenv->GetFieldID(jklass, "label", "Ljava/lang/String;");
	CheckboxAdapter::state_ID = jenv->GetFieldID(jklass, "state", "Z");
	CheckboxAdapter::group_ID = jenv->GetFieldID(jklass, "group", "Ljava/awt/CheckboxGroup;");
	DASSERT(CheckboxAdapter::label_ID != NULL);
	DASSERT(CheckboxAdapter::state_ID != NULL);
	DASSERT(CheckboxAdapter::group_ID != NULL);
}

