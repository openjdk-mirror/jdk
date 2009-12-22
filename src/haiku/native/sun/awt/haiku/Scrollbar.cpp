#include "Scrollbar.h"
#include "ScrollbarAdapter.h"
#include <interface/Window.h>
#include "debug_util.h"

Scrollbar::Scrollbar(BRect frame, float min, float max, orientation direction)
	: AbstractScrollbar(frame, min, max, direction)
{
	SetResizingMode(B_FOLLOW_NONE);
}


void
Scrollbar::SetAdapter(ComponentAdapter * adapter)
{
	DASSERT(dynamic_cast<ScrollbarAdapter*>(adapter) != NULL);
	AbstractScrollbar::SetAdapter(adapter);
	BWindow * window = Window();
	DASSERT(window != NULL);
	if (window->LockLooper()) {
		window->AddHandler(adapter);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


/* virtual */ void
Scrollbar::ValueChanged(float value)
{
	PRINT_PRETTY_FUNCTION();
	BScrollBar::ValueChanged(value);
	ScrollbarAdapter * adapter = dynamic_cast<ScrollbarAdapter*>(this->adapter);
	if (adapter != NULL) { // adapter could be null during initialization
		adapter->ValueChanged(value);
	}
}
