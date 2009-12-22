#include "ListView.h"
#include <interface/Window.h>
#include "debug_util.h"

ListView::ListView(BRect frame, list_view_type type) 
	: AbstractListView(frame, type)
{
	SetSelectionMessage(new BMessage(B_CONTROL_MODIFIED));
	SetInvocationMessage(new BMessage(B_CONTROL_INVOKED));
}


/* virtual */ void
ListView::SetAdapter(ComponentAdapter * adapter) {
	AbstractListView::SetAdapter(adapter);
	BWindow * window = Window();
	DASSERT(window != NULL);
	if (window->LockLooper()) {
		window->AddHandler(adapter);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	SetTarget(adapter);
}


/* virtual */ void
ListView::MessageReceived(BMessage * message) {
	if ((message->what == B_CONTROL_INVOKED) ||
	    (message->what == B_CONTROL_MODIFIED)) {
		adapter->MessageReceived(message);
	} else {
		AbstractListView::MessageReceived(message);
	}
}


