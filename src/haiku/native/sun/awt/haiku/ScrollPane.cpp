#include "ScrollPane.h"

static const float SCROLL_VIEW_INSET = 2.0;

const float CONTENT_VIEW_OUTSET = 3.0;

/* static */ BRect 
ScrollPane::compute_content_view_frame(BRect scrollFrame, bool showHorizontal, bool showVertical)
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
		frame.top += SCROLL_VIEW_INSET;
		frame.bottom -= SCROLL_VIEW_INSET;
	}
	if (showVertical) {
		frame.right -= B_V_SCROLL_BAR_WIDTH;
	} else {
		frame.left += SCROLL_VIEW_INSET;
		frame.right -= SCROLL_VIEW_INSET;
	}
	fprintf(stdout, "contentViewFrame = (%f, %f, %f, %f)\n",
	        frame.left, frame.top, frame.right, frame.bottom);
	return frame;
}


/* static */ BView *
ScrollPane::NewContentView(BRect scrollFrame, bool showHorizontal, bool showVertical)
{
	BRect contentViewFrame = compute_content_view_frame(scrollFrame, showHorizontal, showVertical); 
	return new BView(contentViewFrame, "ContentView", B_FOLLOW_ALL, B_WILL_DRAW);
}


ScrollPane::ScrollPane(BRect frame, bool showScrollBars)
	: AbstractScrollPane(NewContentView(frame, showScrollBars, showScrollBars), showScrollBars, showScrollBars),
	  contentView(NULL)
{
	contentView = dynamic_cast<BView*>(Target());
	DASSERT(contentView != NULL);
	if (contentView->LockLooper()) {
		contentView->MoveBy(-CONTENT_VIEW_OUTSET, -CONTENT_VIEW_OUTSET);
		contentView->ResizeBy(CONTENT_VIEW_OUTSET, CONTENT_VIEW_OUTSET);
		contentView->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


ScrollPane::~ScrollPane()
{
	// TODO: dispose of contentView ?
}


void
ScrollPane::SetAdapter(ComponentAdapter * adapter)
{
	AbstractScrollPane::SetAdapter(adapter);
//	contentView->SetAdapter(adapter);
}


