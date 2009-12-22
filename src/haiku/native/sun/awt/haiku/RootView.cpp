#include "RootView.h"
#include "ComponentAdapter.h"
#include "KeyboardFocusManager.h"

RootView::RootView(BRect bounds)
	: BView(bounds, "RootView", B_FOLLOW_ALL, B_WILL_DRAW),
	  adapter(NULL)
{
	// nothing to do
}


RootView::~RootView()
{
	// nothing to do
}


void
RootView::SetAdapter(ComponentAdapter * adapter)
{
	this->adapter = adapter;
}


void
RootView::Draw(BRect rect)
{
	BView::Draw(rect);
	adapter->DrawSurface(rect);
}


/* virtual */ void
RootView::MessageReceived(BMessage * message)
{
	if (IsUselessMessage(message)) {
		BView::MessageReceived(message);
		return;
	}
	fprintf(stdout, "%s:", __PRETTY_FUNCTION__);
	fprint_message(stdout, message);
	fprintf(stdout, "\n");
	BView::MessageReceived(message);
	adapter->MessageReceived(message);
}


/* virtual */ void
RootView::MouseDown(BPoint where)
{
	BView::MouseDown(where);
	adapter->MouseDown(where);
}


/* virtual */ void
RootView::MouseUp(BPoint where)
{
	BView::MouseUp(where);
	adapter->MouseUp(where);
}


/* virtual */ void
RootView::MouseMoved(BPoint where, uint32 code, const BMessage * message)
{
	BView::MouseMoved(where, code, message);
	adapter->MouseMoved(where, code, message);
}


/* virtual */ void
RootView::KeyDown(const char * bytes, int32 numBytes)
{
	BView::KeyDown(bytes, numBytes);
	adapter->KeyDown(bytes, numBytes);
}


/* virtual */ void
RootView::KeyUp(const char * bytes, int32 numBytes)
{
	BView::KeyUp(bytes, numBytes);
	adapter->KeyUp(bytes, numBytes);
}
