#include "Choice.h"
#include <interface/Window.h>
#include "debug_util.h"

Choice::Choice(BRect frame) 
	: AbstractChoice(frame)
{
	// nothing to do
}


/* virtual */ void
Choice::SetAdapter(ComponentAdapter * adapter) {
	AbstractChoice::SetAdapter(adapter);
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
Choice::MessageReceived(BMessage * message) {
	if (message->what == B_CONTROL_INVOKED) {
		adapter->MessageReceived(message);
	} else {
		AbstractChoice::MessageReceived(message);
	}
}

