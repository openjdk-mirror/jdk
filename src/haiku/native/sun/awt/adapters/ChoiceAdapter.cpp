#include "ChoiceAdapter.h"
#include "Choice.h"
#include "EventEnvironment.h"
#include "KeyConversions.h"

/* static */ Choice *
ChoiceAdapter::NewChoice(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BRect frame = GetFrame(jenv, jpeer);
	Choice * value = new Choice(frame);
	return value;
}


ChoiceAdapter::ChoiceAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent)
	: ComponentAdapter(jenv, jpeer, jparent, NewChoice(jenv, jpeer, jparent)),
	  choice(NULL)
{
	choice = dynamic_cast<Choice*>(GetView());
	DASSERT(choice != NULL);
	choice->SetAdapter(this);
}


ChoiceAdapter::~ChoiceAdapter()
{
	// TODO: determine when (if) Choice should be deleted
}


// #pragma mark -
//
// JNI entry points
//

void
ChoiceAdapter::enable()
{
	if (choice->LockLooper()) {
		choice->SetEnabled(true);
		choice->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ChoiceAdapter::disable()
{
	if (choice->LockLooper()) {
		choice->SetEnabled(false);
		choice->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ChoiceAdapter::add(char * item, int index)
{
	choice->AddOptionAt(item, index, index);
}


void
ChoiceAdapter::remove(int index)
{
	choice->RemoveOptionAt(index);
}


void
ChoiceAdapter::removeAll()
{
	while (choice->CountOptions() != 0) {
		choice->RemoveOptionAt(0);
	}
}


void
ChoiceAdapter::select(int index)
{
	if (choice->LockLooper()) {
		choice->SetValue(index);
		choice->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


BPoint
ChoiceAdapter::getMinimumSize()
{
	float width, height;
	choice->GetPreferredSize(&width, &height);
	return BPoint(width, height);
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
ChoiceAdapter::InvocationReceived(BMessage * message)
{
	PRINT_PRETTY_FUNCTION();
	// parse the message
	int32 index;
	const char * name;
	int32 value;
	if ((message->FindInt32("be:value", &index) != B_OK) || 
	    (choice->GetOptionAt(index, &name, &value) == false)) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	// build the event
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jstring jlabel = jenv->NewStringUTF(name);
	jobject jtarget = GetTarget(jenv);
	jevent = environment->NewItemEvent(jtarget, java_awt_event_ItemEvent_ITEM_STATE_CHANGED,
	                                   jlabel, java_awt_event_ItemEvent_SELECTED);
	jenv->SetIntField(jtarget, selectedIndex_ID, index);
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

#include "java_awt_Choice.h"

jfieldID ChoiceAdapter::selectedIndex_ID = NULL;

// see Java_sun_awt_haiku_BChoicePeer_initIDs for initialization

