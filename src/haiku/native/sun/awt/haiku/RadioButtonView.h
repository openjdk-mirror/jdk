#ifndef RADIO_BUTTON_VIEW_H
#define RADIO_BUTTON_VIEW_H

#include <interface/RadioButton.h>
#include "Adaptable.h"

ADAPTABLE(AbstractRadioButtonView, (BRect frame, const char * label), 
          BRadioButton, (frame, "RadioButtonView", label, new BMessage(B_CONTROL_INVOKED), B_FOLLOW_ALL, B_WILL_DRAW | B_NAVIGABLE));

class RadioButtonView : public AbstractRadioButtonView {
public:
	RadioButtonView(BRect frame, const char * label, bool state);
	virtual void SetAdapter(ComponentAdapter * adapter);
	virtual void MessageReceived(BMessage * message);
};

#endif // RADIO_BUTTON_VIEW_H
