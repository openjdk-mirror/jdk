#ifndef CHECKBOX_VIEW_H
#define CHECKBOX_VIEW_H

#include <interface/CheckBox.h>
#include "Adaptable.h"

ADAPTABLE(AbstractCheckboxView, (BRect frame, const char * label), 
          BCheckBox, (frame, "CheckboxView", label, new BMessage(B_CONTROL_INVOKED), B_FOLLOW_ALL, B_WILL_DRAW | B_NAVIGABLE));

class CheckboxView : public AbstractCheckboxView {
public:
	CheckboxView(BRect frame, const char * label, bool state);
	virtual void SetAdapter(ComponentAdapter * adapter);
	virtual void MessageReceived(BMessage * message);
};

#endif // CHECKBOX_VIEW_H
