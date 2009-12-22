#include "TextField.h"
#include <interface/Window.h>

TextField::TextField(BRect frame)
	: AbstractTextField(frame)
{
}


void
TextField::SetAdapter(ComponentAdapter * adapter)
{
	AbstractTextField::SetAdapter(adapter);
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
TextField::MessageReceived(BMessage * message) {
	if (message->what == B_CONTROL_INVOKED) {
		adapter->MessageReceived(message);
	} else {
		AbstractTextField::MessageReceived(message);
	}
}
