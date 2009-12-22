#include "TextArea.h"
#include "TextView.h"

/* static */ BRect 
TextArea::compute_text_view_frame(BRect scrollFrame, bool showHorizontal, bool showVertical)
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
	fprintf(stdout, "textViewFrame = (%f, %f, %f, %f)\n",
	        frame.left, frame.top, frame.right, frame.bottom);
	return frame;
}


/* static */ TextView *
TextArea::NewTextView(BRect scrollFrame, bool showHorizontal, bool showVertical)
{
	BRect textViewFrame = compute_text_view_frame(scrollFrame, showHorizontal, showVertical); 
	return new TextView(textViewFrame, showHorizontal, showVertical);
}


TextArea::TextArea(BRect frame, bool showHorizontal, bool showVertical)
	: AbstractScrollPane(NewTextView(frame, showHorizontal, showVertical), showHorizontal, showVertical),
	  textView(NULL)
{
	textView = dynamic_cast<TextView*>(Target());
	DASSERT(textView != NULL);
}


TextArea::~TextArea()
{
	// TODO: dispose of textView ?
}


void
TextArea::SetAdapter(ComponentAdapter * adapter)
{
	AbstractScrollPane::SetAdapter(adapter);
	textView->SetAdapter(adapter);
}
