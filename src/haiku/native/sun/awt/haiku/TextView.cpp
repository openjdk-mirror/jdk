#include "TextView.h"

const float TEXT_INSET = 3.0;

/* static */ BRect
TextView::compute_text_content_frame(BRect textViewFrame, bool showHorizontal, bool showVertical)
{
	fprintf(stdout, "### textViewFrame = (%f, %f, %f, %f) -> ",
	        textViewFrame.left, textViewFrame.top, textViewFrame.right, textViewFrame.bottom);
	BRect text = textViewFrame;
	text.InsetBy(TEXT_INSET, TEXT_INSET);
	if (showHorizontal) {
		text.right = 1500.0;
	}
	fprintf(stdout, "textContentFrame = (%f, %f, %f, %f)\n",
	        text.left, text.top, text.right, text.bottom);
	return text;
}


TextView::TextView(BRect frame, bool showHorizontal, bool showVertical)
	: AbstractTextView(frame, compute_text_content_frame(frame, showHorizontal, showVertical), showHorizontal, showVertical)
{
	if (showHorizontal) {
		SetWordWrap(false);
	}
}


/* virtual */ void
TextView::FrameResized(float width, float height)
{
	PRINT_PRETTY_FUNCTION();
	BTextView::FrameResized(width, height);
	if (DoesWordWrap()) {
		BRect textRect;
		textRect = Bounds();
		textRect.OffsetTo(B_ORIGIN);
		textRect.InsetBy(TEXT_INSET,TEXT_INSET);
		SetTextRect(textRect);
	}
}


