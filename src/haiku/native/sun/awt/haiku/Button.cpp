#include "Button.h"
#include <interface/Window.h>
#include "debug_util.h"

Button::Button(BRect frame, const char * label) 
	: AbstractButton(frame, label)
{
	// nothing to do
}


/* virtual */ void
Button::SetAdapter(ComponentAdapter * adapter) {
	AbstractButton::SetAdapter(adapter);
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
Button::MessageReceived(BMessage * message) {
	if (message->what == B_CONTROL_INVOKED) {
		adapter->MessageReceived(message);
	} else {
		AbstractButton::MessageReceived(message);
	}
}


