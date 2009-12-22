#ifndef CHOICE_H
#define CHOICE_H

#include <interface/OptionPopUp.h>
#include "Adaptable.h"

ADAPTABLE(AbstractChoice, (BRect frame), 
          BOptionPopUp, (frame, "Choice", "", new BMessage(B_CONTROL_INVOKED),
                         false, B_FOLLOW_NONE, B_WILL_DRAW));

class Choice : public AbstractChoice {
public:
	Choice(BRect frame);
	virtual void SetAdapter(ComponentAdapter * adapter);
	virtual void MessageReceived(BMessage * message);
};

#endif // CHOICE_H
