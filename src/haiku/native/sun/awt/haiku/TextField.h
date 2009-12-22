#ifndef TEXT_FIELD_H
#define TEXT_FIELD_H

#include "TextView.h"
#include <app/Invoker.h>

#include <interface/TextControl.h>
#include "Adaptable.h"

ADAPTABLE(AbstractTextField, (BRect frame), 
          BTextControl, (frame, "TextField", "", "", new BMessage(B_CONTROL_INVOKED),
                         B_FOLLOW_NONE, B_WILL_DRAW));

class TextField : public AbstractTextField {
public:
	TextField(BRect frame);
	virtual BTextView * GetTextView() { return TextView(); }
	virtual void SetAdapter(ComponentAdapter * adapter);
	virtual void MessageReceived(BMessage * message);
};

#endif // TEXT_FIELD_H
