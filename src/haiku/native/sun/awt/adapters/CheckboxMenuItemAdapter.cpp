#include "CheckboxMenuItemAdapter.h"
#include "EventEnvironment.h"
#include <app/Message.h>
#include <interface/MenuItem.h>
#include <cstdio>

/* static */ BMenuItem *
CheckboxMenuItemAdapter::NewCheckboxMenuItem(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass jjavaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	// TOTRY: pass state as an argument
	bool state = jenv->GetBooleanField(jjavaObject, state_ID);
	BMenuItem * value = NewMenuItem(jenv, jpeer, jparent);
	value->SetMarked(state);
	return value;
}


CheckboxMenuItemAdapter::CheckboxMenuItemAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent) 
	: MenuItemAdapter(jenv, jpeer, jparent, NewCheckboxMenuItem(jenv, jpeer, jparent))
{
	// nothing to do
}


CheckboxMenuItemAdapter::~CheckboxMenuItemAdapter()
{
	// nothing to do
}

// #pragma mark -
//
// JNI entry points
//

void
CheckboxMenuItemAdapter::setState(bool enabled)
{
	MenuItem()->SetMarked(enabled);
}


// #pragma mark -
//
// ComponentAdapter invocation handoff
//


/* virtual */ void
CheckboxMenuItemAdapter::Invoked(EventEnvironment * environment, BMessage * message)
{
	// get the new status
	bool marked = !MenuItem()->IsMarked();
	// toggle the menu item
	MenuItem()->SetMarked(marked);
	// setup scope variables
	JNIEnv * jenv = environment->env;
	jobject jtarget = GetTarget(jenv);
	jobject jevent = NULL;
	// build the event
	jint jstateChange = (marked ? java_awt_event_ItemEvent_SELECTED
	                            : java_awt_event_ItemEvent_DESELECTED);
	jevent = environment->NewItemEvent(jtarget, java_awt_event_ItemEvent_ITEM_STATE_CHANGED,
	                                   jtarget, jstateChange);
	jenv->SetIntField(jtarget, state_ID, marked); // modify java's cache of the value
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

#include "java_awt_CheckboxMenuItem.h"

jfieldID CheckboxMenuItemAdapter::state_ID = NULL;

/*
 * Class:     java_awt_CheckboxMenuItem
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_CheckboxMenuItem_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	CheckboxMenuItemAdapter::state_ID = jenv->GetFieldID(jklass, "state", "Z");
	DASSERT(CheckboxMenuItemAdapter::state_ID != NULL);
}

