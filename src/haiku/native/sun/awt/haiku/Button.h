#ifndef BUTTON_H
#define BUTTON_H

#include <interface/Button.h>
#include "Adaptable.h"

ADAPTABLE(AbstractButton, (BRect frame, const char * label), 
          BButton, (frame, "Button", label, new BMessage(B_CONTROL_INVOKED), B_FOLLOW_NONE, B_WILL_DRAW));

class Button : public AbstractButton {
public:
	Button(BRect frame, const char * label);
	virtual void SetAdapter(ComponentAdapter * adapter);
	virtual void MessageReceived(BMessage * message);
};

#endif // BUTTON_H
