#ifndef SCROLLBAR_H
#define SCROLLBAR_H

#include <interface/ScrollBar.h>
#include "Adaptable.h"

ADAPTABLE(AbstractScrollbar, (BRect frame, float min, float max, orientation direction), 
          BScrollBar, (frame, "Scrollbar", NULL, min, max, direction));

class Scrollbar : public AbstractScrollbar {
public:
	Scrollbar(BRect frame, float min, float max, orientation direction);
	virtual void SetAdapter(ComponentAdapter * adapter);
	virtual void ValueChanged(float newValue);
};

#endif // SCROLLBAR_H
