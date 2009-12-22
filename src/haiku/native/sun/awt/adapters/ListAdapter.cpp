#include "ListAdapter.h"
#include "List.h"
#include "ListView.h"
#include "EventEnvironment.h"
#include "KeyConversions.h"
#include <interface/ListItem.h>

/* static */ List *
ListAdapter::NewList(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	BRect frame = GetFrame(jenv, jpeer);
	// TOTRY: pass multipleMode as an argument
	bool multipleMode = jenv->GetBooleanField(jjavaObject, ListAdapter::multipleMode_ID);
	list_view_type type = (multipleMode ? B_SINGLE_SELECTION_LIST : B_MULTIPLE_SELECTION_LIST);
	List * value = new List(frame, type);
	return value;
}


ListAdapter::ListAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent)
	: ComponentAdapter(jenv, jpeer, jparent, NewList(jenv, jpeer, jparent)),
	  list(NULL)
{
	list = dynamic_cast<List*>(GetView());
	DASSERT(list != NULL);
	list->SetAdapter(this);
}


ListAdapter::~ListAdapter()
{
	// TODO: determine when (if) List should be deleted
}


// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
ListAdapter::enable()
{
	TODO();
	ListView * view = list->GetListView();
}


/* virtual */ void
ListAdapter::disable()
{
	TODO();
	ListView * view = list->GetListView();
}


/* virtual */ BPoint
ListAdapter::getPreferredSize()
{
	TODO();
	return BPoint(300, 200);
}


/* virtual */ BPoint
ListAdapter::getMinimumSize()
{
	TODO();
	return BPoint(300, 200);
}


std::vector<int>
ListAdapter::getSelectedIndexes()
{
	ListView * view = list->GetListView();
	std::vector<int> indexes;
	int32 i = 0;
	int32 selected;
	while ((selected = view->CurrentSelection(i++)) >= 0) {
		indexes.push_back(selected);
	}
	return indexes;
}

	
void
ListAdapter::add(char * item, int index)
{
	ListView * view = list->GetListView();
	if (view->LockLooper()) {
		view->AddItem(new BStringItem(item), index);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ListAdapter::delItems(int start, int end)
{
	ListView * view = list->GetListView();
	if (view->LockLooper()) {
		for (int i = end - 1 ; i >= start ; i--) {
			delete view->RemoveItem(i);
		}
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ListAdapter::removeAll()
{
	ListView * view = list->GetListView();
	if (view->LockLooper()) {
		for (int i = view->CountItems() - 1 ; i >= 0 ; i--) {
			delete view->RemoveItem(i);
		}
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ListAdapter::select(int index)
{
	ListView * view = list->GetListView();
	view->Select(index, true); // true = add to current selection
}


void
ListAdapter::deselect(int index)
{
	ListView * view = list->GetListView();
	view->Deselect(index);
}


void
ListAdapter::makeVisible(int index)
{
	ListView * view = list->GetListView();
	BRect rect = view->ItemFrame(index);
	view->ScrollTo(rect.LeftTop());
}


void
ListAdapter::setMultipleMode(bool b)
{
	ListView * view = list->GetListView();
	view->SetListType(b ? B_MULTIPLE_SELECTION_LIST : B_SINGLE_SELECTION_LIST);
}


BPoint
ListAdapter::getPreferredSize(int rows)
{
	TODO();
	return getPreferredSize();
}


BPoint
ListAdapter::getMinimumSize(int rows)
{
	TODO();
	return getMinimumSize();
}


// #pragma mark -
//
// Haiku entry points
//

std::vector<const char *>
ListAdapter::parseStrings(BMessage * message)
{
	ListView * view = list->GetListView();
	std::vector<const char *> strings;
	int32 count = 0;
	int32 index;
	while (message->FindInt32("index", count++, &index) == B_OK) {
		BStringItem * item = dynamic_cast<BStringItem*>(view->ItemAt(index));
		if (item == NULL) {
			fprintf(stdout, "%s: ignoring non-BStringItem\n", __PRETTY_FUNCTION__);
			continue;
		}
		strings.push_back(item->Text());
	}
	return strings;
}


/* virtual */ void
ListAdapter::InvocationReceived(BMessage * message)
{
	PRINT_PRETTY_FUNCTION();
	// parse the message
	std::vector<const char *> strings = parseStrings(message);
	int64 when;
	if ((message->FindInt64("when", &when) != B_OK) || (strings.size() == 0)) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	const char * string = strings[0];
	// build the event
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jint jmodifiers = ConvertModifiersToJava(modifiers());
	jstring jcommand = jenv->NewStringUTF(string);
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


/* virtual */ void
ListAdapter::SelectionReceived(BMessage * message)
{
	PRINT_PRETTY_FUNCTION();
	// parse the message
	std::vector<const char *> strings = parseStrings(message);
	int64 when;
	if (message->FindInt64("when", &when) != B_OK) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	const char * string;
	if (strings.size() > 0) {
		string = strings[0];
	} else {
		string = oldString.String();
	}
	// build the event
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jobject jtarget = GetTarget(jenv);
	jstring jtext = jenv->NewStringUTF(string);
	// TODO: compute the difference between the old indexes and the new indexes
	//       and report the item actually changed, and its actual change (could be deselect)
	jint jid = (strings.size() > 0 ? java_awt_event_ItemEvent_SELECTED
	                               : java_awt_event_ItemEvent_DESELECTED);
	jevent = environment->NewItemEvent(jtarget, java_awt_event_ItemEvent_ITEM_STATE_CHANGED,
	                                   jtext, jid);
	// TODO: (de)select the item in the java.awt.List's selection array
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// send out the appropriate event
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
	// store state
	if (oldString.String() != string) {
		oldString.SetTo(string);
	}
}


// #pragma mark -
//
// JNI
//

#include "java_awt_List.h"

jfieldID ListAdapter::rows_ID         = NULL;
jfieldID ListAdapter::multipleMode_ID = NULL;
jfieldID ListAdapter::visibleIndex_ID = NULL;

// see Java_sun_awt_haiku_BListPeer_initIDs for initialization

