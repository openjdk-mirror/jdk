#include "RadioButtonView.h"
#include "debug_util.h"

RadioButtonView::RadioButtonView(BRect frame, const char * label, bool state) 
	: AbstractRadioButtonView(frame, label)
{
	SetValue(state ? B_CONTROL_ON : B_CONTROL_OFF);
}


/* virtual */ void
RadioButtonView::SetAdapter(ComponentAdapter * adapter) {
	AbstractRadioButtonView::SetAdapter(adapter);
	SetTarget(adapter);
}


/* virtual */ void
RadioButtonView::MessageReceived(BMessage * message) {
	if (message->what == B_CONTROL_INVOKED) {
		adapter->MessageReceived(message);
	} else {
		AbstractRadioButtonView::MessageReceived(message);
	}
}


