#include "List.h"
#include "ListView.h"

/* static */ BRect 
List::compute_list_view_frame(BRect scrollFrame, bool showHorizontal, bool showVertical)
{
	fprintf(stdout, "### scrollFrame = (%f, %f, %f, %f) -> ",
	        scrollFrame.left, scrollFrame.top, scrollFrame.right, scrollFrame.bottom);
	float width  = max_c(scrollFrame.Width(),  5 * B_V_SCROLL_BAR_WIDTH);
	float height = max_c(scrollFrame.Height(), 5 * B_H_SCROLL_BAR_HEIGHT);
	BRect frame;
	frame.left = scrollFrame.left;
	frame.top  = scrollFrame.top;
	frame.right  = frame.left + width;
	frame.bottom = frame.top + height;
	if (showHorizontal) {
		frame.bottom -= B_H_SCROLL_BAR_HEIGHT;
	} else {
		frame.top += 3;
		frame.bottom -= 3;
	}
	if (showVertical) {
		frame.right -= B_V_SCROLL_BAR_WIDTH;
	} else {
		frame.left += 3;
		frame.right -= 3;
	}
	fprintf(stdout, "listViewFrame = (%f, %f, %f, %f)\n",
	        frame.left, frame.top, frame.right, frame.bottom);
	return frame;
}

static const bool showHorizontal = false;
static const bool showVertical = true;

/* static */ ListView *
List::NewListView(BRect scrollFrame, list_view_type type)
{
	BRect listViewFrame = compute_list_view_frame(scrollFrame, showHorizontal, showVertical); 
	return new ListView(listViewFrame, type);
}


List::List(BRect frame, list_view_type type)
	: AbstractScrollPane(NewListView(frame, type), showHorizontal, showVertical),
	  listView(NULL)
{
	listView = dynamic_cast<ListView*>(Target());
	DASSERT(listView != NULL);
}


List::~List()
{
	// TODO: dispose of listView ?
}


void
List::SetAdapter(ComponentAdapter * adapter)
{
	AbstractScrollPane::SetAdapter(adapter);
	listView->SetAdapter(adapter);
}
