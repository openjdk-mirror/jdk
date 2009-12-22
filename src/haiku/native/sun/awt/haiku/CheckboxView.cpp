#include "CheckboxView.h"
#include "debug_util.h"

CheckboxView::CheckboxView(BRect frame, const char * label, bool state) 
	: AbstractCheckboxView(frame, label)
{
	SetValue(state ? B_CONTROL_ON : B_CONTROL_OFF);
}


/* virtual */ void
CheckboxView::SetAdapter(ComponentAdapter * adapter) {
	AbstractCheckboxView::SetAdapter(adapter);
	SetTarget(adapter);
}


/* virtual */ void
CheckboxView::MessageReceived(BMessage * message) {
	if (message->what == B_CONTROL_INVOKED) {
		adapter->MessageReceived(message);
	} else {
		AbstractCheckboxView::MessageReceived(message);
	}
}
